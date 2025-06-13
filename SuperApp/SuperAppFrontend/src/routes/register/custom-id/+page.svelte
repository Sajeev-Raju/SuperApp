<script lang="ts">
  // import { state } from 'svelte';
  import { writable } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { Circle } from 'svelte-loading-spinners';
  import { api } from '$lib/api';

  const formState = $state({
    username: '',
    loading: false,
    error: null as string | null,
    validationResult: null as any
  });

  async function handleCheck() {
    formState.loading = true;
    formState.error = null;

    try {
      const registrationData = JSON.parse(sessionStorage.getItem('registration') || '{}');
      const response = await api.validateUsername({
        ...registrationData,
        username: formState.username
      });

      if (response.data.success) {
        formState.validationResult = response.data.data;
      } else {
        formState.error = response.data.message;
      }
    } catch (error: any) {
      formState.error = error.response?.data?.message || error.message;
    } finally {
      formState.loading = false;
    }
  }

  async function handleProceedToPayment() {
    const registrationData = JSON.parse(sessionStorage.getItem('registration') || '{}');
    
    try {
      const response = await api.initiateQRPayment({
        ...registrationData,
        username: formState.username,
        isFancy: formState.validationResult.isFancy,
        fancyType: formState.validationResult.fancyType,
        basePrice: formState.validationResult.basePrice,
        fancyPrice: formState.validationResult.fancyPrice,
        totalPrice: formState.validationResult.totalPrice
      });

      if (response.data.success) {
        sessionStorage.setItem('paymentData', JSON.stringify({
          ...registrationData,
          username: formState.username,
          isFancy: formState.validationResult.isFancy,
          fancyType: formState.validationResult.fancyType,
          basePrice: formState.validationResult.basePrice,
          fancyPrice: formState.validationResult.fancyPrice,
          totalPrice: formState.validationResult.totalPrice
        }));
        goto('/register/payment');
      } else {
        formState.error = response.data.message;
      }
    } catch (error: any) {
      formState.error = error.response?.data?.message || error.message;
    }
  }
</script>

<div class="max-w-md mx-auto">
  <div class="card">
    <h1 class="text-2xl font-bold mb-6">Create Custom ID</h1>
    
    <div class="space-y-6">
      <div>
        <label for="username" class="block text-sm font-medium mb-1">
          Enter Your Desired ID
        </label>
        <input
          type="text"
          id="username"
          bind:value={formState.username}
          class="input"
          placeholder="e.g., JOHN123"
          pattern="[A-Za-z0-9]+"
          maxlength="6"
          required
        />
        <p class="text-sm text-gray-500 mt-1">
          Only letters and numbers, max 6 characters
        </p>
      </div>

      <button 
        class="btn btn-primary w-full" 
        on:click={handleCheck}
        disabled={formState.loading || !formState.username}
      >
        {#if formState.loading}
          <Circle size="20" color="#ffffff" unit="px" duration="1s" />
        {:else}
          Check Availability
        {/if}
      </button>

      {#if formState.validationResult}
        <div class="mt-4 p-4 rounded-lg border {formState.validationResult.available ? 'border-green-500 bg-green-50 dark:bg-green-900/20' : 'border-red-500 bg-red-50 dark:bg-red-900/20'}">
          <h3 class="font-semibold mb-2">
            {formState.validationResult.available ? 'ID Available!' : 'ID Not Available'}
          </h3>
          {#if formState.validationResult.available}
            <div class="space-y-2">
              <p>Type: {formState.validationResult.isFancy ? formState.validationResult.fancyType : 'Standard'}</p>
              <p>Base Price: ₹{formState.validationResult.basePrice}</p>
              {#if formState.validationResult.isFancy}
                <p>Fancy Price: ₹{formState.validationResult.fancyPrice}</p>
              {/if}
              <p class="font-bold">Total: ₹{formState.validationResult.totalPrice}</p>
              <button 
                class="btn btn-primary w-full mt-4"
                on:click={handleProceedToPayment}
              >
                Proceed to Payment
              </button>
            </div>
          {:else}
            <p>Please try a different ID</p>
          {/if}
        </div>
      {/if}

      {#if formState.error}
        <div class="mt-4 bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded relative">
          <strong class="font-bold">Error!</strong>
          <span class="block sm:inline">{formState.error}</span>
        </div>
      {/if}
    </div>
  </div>
</div>