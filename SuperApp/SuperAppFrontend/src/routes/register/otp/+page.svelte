<script lang="ts">
  //import { state } from 'svelte';
  import { writable } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { Circle } from 'svelte-loading-spinners';
  import { api } from '$lib/api';
  import { onMount } from 'svelte';

  const formState = $state({
    emailOtp: '',
    phoneOtp: '',
    loading: false,
    error: null as string | null,
    registrationData: null as { email: string; phone: string } | null
  });

  onMount(() => {
    const data = sessionStorage.getItem('registration');
    if (!data) {
      goto('/register');
      return;
    }
    formState.registrationData = JSON.parse(data);
  });

  async function handleSubmit(event: Event) {
    event.preventDefault();
    
    if (!formState.registrationData) {
      goto('/register');
      return;
    }

    formState.loading = true;
    formState.error = null;

    try {
      const response = await api.verifyOtp({
        email: formState.registrationData.email,
        phone: formState.registrationData.phone,
        emailOtp: formState.emailOtp,
        phoneOtp: formState.phoneOtp
      });

      if (response.data.success) {
        goto('/register/choose-id');
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
      Please enter the OTP sent to your email and phone.
    </p>

    <form on:submit={handleSubmit} class="space-y-4">
      <div>
        <label for="emailOtp" class="block text-sm font-medium mb-1">
          Email OTP
        </label>
        <input
          type="text"
          inputmode="numeric"
          id="emailOtp"
          bind:value={formState.emailOtp}
          class="input"
          minlength="6"
          maxlength="6"
          placeholder="Enter 6-digit OTP"
          required
        />
      </div>

      <div>
        <label for="phoneOtp" class="block text-sm font-medium mb-1">
          Phone OTP
        </label>
        <input
          type="text"
          inputmode="numeric"
          id="phoneOtp"
          bind:value={formState.phoneOtp}
          class="input"
          minlength="6"
          maxlength="6"
         
          placeholder="Enter 6-digit OTP"
          required
        />
      </div>

      {#if formState.error}
        <div class="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded relative">
          <strong class="font-bold">Error!</strong>
          <span class="block sm:inline">{formState.error}</span>
        </div>
      {/if}

      <button type="submit" class="btn btn-primary w-full" disabled={formState.loading}>
        {#if formState.loading}
          <Circle size="20" color="#ffffff" unit="px" duration="1s" />
        {:else}
          Verify OTP
        {/if}
      </button>
    </form>
  </div>
</div>