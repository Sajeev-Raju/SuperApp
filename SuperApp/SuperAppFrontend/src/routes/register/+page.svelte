<script lang="ts">
  // let myState = $state(initialValue);
  import { writable } from 'svelte/store';
  //let myStore = writable(initialValue);
  import { goto } from '$app/navigation';
  import { Circle } from 'svelte-loading-spinners';
  import { api } from '$lib/api';

  const formState = $state({
    email: '',
    phone: '',
    loading: false,
    error: null as string | null
  });

  async function handleSubmit() {
    formState.loading = true;
    formState.error = null;

    try {
      const response = await api.startRegistration({
        email: formState.email,
        phone: formState.phone
      });

      if (response.data.success) {
        sessionStorage.setItem('registration', JSON.stringify({
          email: formState.email,
          phone: formState.phone
        }));
        goto('/register/otp');
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
    <h1 class="text-2xl font-bold mb-6">Register</h1>

    <form on:submit|preventDefault={handleSubmit} class="space-y-4">
      <div>
        <label for="email" class="block text-sm font-medium mb-1">
          Email Address
        </label>
        <input
          type="email"
          id="email"
          bind:value={formState.email}
          class="input"
          required
        />
      </div>

      <div>
        <label for="phone" class="block text-sm font-medium mb-1">
          Phone Number
        </label>
        <input
          type="tel"
          id="phone"
          bind:value={formState.phone}
          class="input"
          placeholder="+1234567890"
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
          Start Registration
        {/if}
      </button>
    </form>
  </div>
</div>