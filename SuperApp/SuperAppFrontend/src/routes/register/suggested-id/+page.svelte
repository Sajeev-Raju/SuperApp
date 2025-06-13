<script lang="ts">
  // import { state } from 'svelte';
  import { writable } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { Circle } from 'svelte-loading-spinners';
  import { api } from '$lib/api';

  const formState = $state({
    attempt: 1,
    loading: true,
    error: null as string | null,
    suggestions: null as any,
    selectedUsername: null as string | null,
    validationResult: null as any
  });

  // Get initial suggestions
  getSuggestions();

  async function getSuggestions() {
    try {
      const registrationData = JSON.parse(sessionStorage.getItem('registration') || '{}');
      const response = await api.getSuggestions(
        { email: registrationData.email, phone: registrationData.phone },
        { attempt: formState.attempt }
      );

      if (response.data.success) {
        formState.suggestions = response.data.data;
      } else {
        formState.error = response.data.message;
      }
    } catch (error: any) {
      formState.error = error.response?.data?.message || error.message;
    } finally {
      formState.loading = false;
    }
  }

  async function handleSelectId(username: string) {
    formState.loading = true;
    formState.error = null;
    formState.selectedUsername = username;

    try {
      const registrationData = JSON.parse(sessionStorage.getItem('registration') || '{}');
      const response = await api.validateUsername({
        ...registrationData,
        username
      });

      if (response.data.success) {
        formState.validationResult = response.data.data;
        handleProceedToPayment();
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
    if (!formState.validationResult) return;

    const registrationData = JSON.parse(sessionStorage.getItem('registration') || '{}');
    
    try {
      const response = await api.initiateQRPayment({
        ...registrationData,
        username: formState.selectedUsername!,
        isFancy: formState.validationResult.isFancy,
        fancyType: formState.validationResult.fancyType,
        basePrice: formState.validationResult.basePrice,
        fancyPrice: formState.validationResult.fancyPrice,
        totalPrice: formState.validationResult.totalPrice
      });

      if (response.data.success) {
        sessionStorage.setItem('paymentData', JSON.stringify({
          ...registrationData,
          username: formState.selectedUsername,
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

<div class="max-w-2xl mx-auto">
  <div class="card">
    <h1 class="text-2xl font-bold mb-6">Suggested IDs</h1>
    <p class="text-gray-600 dark:text-gray-400 mb-6">
      Choose from our suggested IDs. You have {3 - formState.attempt + 1} attempts remaining.
    </p>

    {#if formState.loading}
      <div class="flex justify-center py-8">
        <Circle size="40" color="#0ea5e9" unit="px" duration="1s" />
      </div>
    {:else if formState.suggestions}
      <div class="space-y-8">
        <!-- Fancy IDs -->
        {#if formState.suggestions.fancyIds?.length}
          <div>
            <h2 class="text-lg font-semibold mb-4">Fancy IDs</h2>
            <div class="grid grid-cols-2 gap-4">
              {#each formState.suggestions.fancyIds as id}
                <button 
                  class="p-4 rounded-lg border-2 border-primary-500 hover:bg-primary-50 dark:hover:bg-primary-900 transition-colors"
                  on:click={() => handleSelectId(id)}
                >
                  <div class="text-xl font-bold mb-2">{id}</div>
                  <div class="text-sm text-gray-600 dark:text-gray-400">
                    Premium Selection
                  </div>
                </button>
              {/each}
            </div>
          </div>
        {/if}

        <!-- Random IDs -->
        {#if formState.suggestions.randomIds?.length}
          <div>
            <h2 class="text-lg font-semibold mb-4">Standard IDs</h2>
            <div class="grid grid-cols-2 gap-4">
              {#each formState.suggestions.randomIds as id}
                <button 
                  class="p-4 rounded-lg border hover:border-primary-500 hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors"
                  on:click={() => handleSelectId(id)}
                >
                  <div class="text-xl font-bold mb-2">{id}</div>
                  <div class="text-sm text-gray-600 dark:text-gray-400">
                    Standard Selection
                  </div>
                </button>
              {/each}
            </div>
          </div>
        {/if}

        {#if formState.attempt < 3}
          <button 
            class="btn btn-secondary w-full mt-8"
            on:click={() => {
              formState.attempt++;
              formState.loading = true;
              getSuggestions();
            }}
          >
            Try Different IDs
          </button>
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