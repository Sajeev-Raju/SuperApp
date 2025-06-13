<script lang="ts">
  //import { state } from 'svelte';
  import { writable } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { Circle } from 'svelte-loading-spinners';
  import { api } from '$lib/api';

  const formState = $state({
    username: '',
    loading: false,
    error: null as string | null,
    maxSessionsReached: false
  });

  async function handleSubmit(event: Event) {
    event.preventDefault();
    formState.loading = true;
    formState.error = null;

    try {
      const response = await api.login({ username: formState.username });

      if (response.data.success) {
        if (response.data.data?.message?.includes('4 devices')) {
          formState.maxSessionsReached = true;
          formState.error = response.data.message;
        } else {
          sessionStorage.setItem('loginUsername', formState.username);
          goto('/login/verify');
        }
      } else {
        formState.error = response.data.message;
      }
    } catch (error: any) {
      formState.error = error.response?.data?.message || error.message;
    } finally {
      formState.loading = false;
    }
  }

  async function handleContinueWithLogout() {
    formState.loading = true;
    formState.error = null;

    try {
      const response = await api.continueWithOldestLogout({ username: formState.username });

      if (response.data.success) {
        goto('/login/verify');
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
    <h1 class="text-2xl font-bold mb-6">Login</h1>

    {#if !formState.maxSessionsReached}
      <form on:submit={handleSubmit} class="space-y-4">
        <div>
          <label for="username" class="block text-sm font-medium mb-1">
            Username
          </label>
          <input
            type="text"
            id="username"
            bind:value={formState.username}
            class="input"
            placeholder="Enter your username"
            required
          />
        </div>

        <button type="submit" class="btn btn-primary w-full" disabled={formState.loading}>
          {#if formState.loading}
            <Circle size="20" color="#ffffff" unit="px" duration="1s" />
          {:else}
            Send OTP
          {/if}
        </button>
      </form>
    {:else}
      <div class="bg-yellow-100 border border-yellow-400 text-yellow-700 px-4 py-3 rounded relative">
        <p class="font-bold mb-2">Maximum Sessions Reached</p>
        <p class="mb-4">You are already logged in on 4 devices.</p>
        <div class="space-y-2">
          <button 
            class="btn btn-primary w-full"
            on:click={handleContinueWithLogout}
            disabled={formState.loading}
          >
            {#if formState.loading}
              <Circle size="20" color="#ffffff" unit="px" duration="1s" />
            {:else}
              Logout Oldest Session & Continue
            {/if}
          </button>
          <button 
            class="btn btn-secondary w-full"
            on:click={() => formState.maxSessionsReached = false}
          >
            Cancel
          </button>
        </div>
      </div>
    {/if}

    {#if formState.error && !formState.maxSessionsReached}
      <div class="mt-4 bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded relative">
        <strong class="font-bold">Error!</strong>
        <span class="block sm:inline">{formState.error}</span>
      </div>
    {/if}
  </div>
</div>