<script lang="ts">
  // import { state } from 'svelte';
  import { writable } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { Circle } from 'svelte-loading-spinners';
  import { api } from '$lib/api';

  const formState = $state({
    username: '',
    loading: true,
    error: null as string | null,
    validationResult: null as any
  });

  // Generate ID on page load
  generateUsername();

  async function generateUsername() {
    try {
      const response = await api.generateUsername();
      if (response.data.success) {
        formState.username = response.data.data;
        const validationResponse = await api.checkUsername({ username: formState.username });
        formState.validationResult = validationResponse.data.data;
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
    <h1 class="text-2xl font-bold mb-6">Auto-Generated ID</h1>

    {#if formState.loading}
      <div class="flex justify-center py-8">
        <Circle size="40" color="#0ea5e9" unit="px" duration="1s" />
      </div>
    {:else if formState.validationResult}
      <div class="text-center space-y-6">
        <div class="p-6 rounded-lg bg-gray-50 dark:bg-gray-800">
          <h2 class="text-3xl font-bold text-primary-600 dark:text-primary-400">
            {formState.username}
          </h2>
        </div>

        <div class="space-y-2">
          <p class="font-semibold">Price Breakdown:</p>
          <p>Base Price: ₹{formState.validationResult.basePrice}</p>
          {#if formState.validationResult.isFancy}
            <p>Fancy Price ({formState.validationResult.fancyType}): ₹{formState.validationResult.fancyPrice}</p>
          {/if}
          <p class="text-lg font-bold">Total: ₹{formState.validationResult.totalPrice}</p>
        </div>

        <button 
          class="btn btn-primary w-full"
          on:click={handleProceedToPayment}
        >
          Proceed to Payment
        </button>

        <button 
          class="btn btn-secondary w-full"
          on:click={() => {
            formState.loading = true;
            generateUsername();
          }}
        >
          Generate Another ID
        </button>
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