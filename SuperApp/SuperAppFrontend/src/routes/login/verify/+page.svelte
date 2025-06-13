<script lang="ts">
  // import { state } from 'svelte';
  import { writable } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { Circle } from 'svelte-loading-spinners';
  import { api } from '$lib/api';
  import { setSession } from '$lib/stores/session';

  const formState = $state({
    otp: '',
    loading: false,
    error: null as string | null
  });

  // Check if username exists in sessionStorage
  if (!sessionStorage.getItem('loginUsername')) {
    goto('/login');
  }

  async function handleSubmit(event: Event) {
    event.preventDefault();
    formState.loading = true;
    formState.error = null;

    try {
      const response = await api.verifyLoginOtp({
        username: sessionStorage.getItem('loginUsername') || '',
        otp: formState.otp
      });

      if (response.data.success) {
        setSession(
          sessionStorage.getItem('loginUsername') || '',
          response.data.data
        );
        sessionStorage.removeItem('loginUsername');
        goto('/');
      } else {
        formState.error = response.data.message;
      }
    } catch (error: any) {
      formState.error = error.response?.data?.message || error.message;
    } finally {
      formState.loading = false;
    }
  }
</script>

<div class="max-w-md mx-auto">
  <div class="card">
    <h1 class="text-2xl font-bold mb-6">Verify OTP</h1>
    <p class="text-gray-600 dark:text-gray-400 mb-6">
      Please enter the OTP sent to your WhatsApp.
    </p>

    <form on:submit={handleSubmit} class="space-y-4">
      <div>
        <label for="otp" class="block text-sm font-medium mb-1">
          Enter OTP
        </label>
        <input
          type="text"
          id="otp"
          bind:value={formState.otp}
          class="input"
          maxlength="6"
          
          placeholder="Enter 6-digit OTP"
          required
        />
      </div>

      <button type="submit" class="btn btn-primary w-full" disabled={formState.loading}>
        {#if formState.loading}
          <Circle size="20" color="#ffffff" unit="px" duration="1s" />
        {:else}
          Verify OTP
        {/if}
      </button>
    </form>

    {#if formState.error}
      <div class="mt-4 bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded relative">
        <strong class="font-bold">Error!</strong>
        <span class="block sm:inline">{formState.error}</span>
      </div>
    {/if}
  </div>
</div>