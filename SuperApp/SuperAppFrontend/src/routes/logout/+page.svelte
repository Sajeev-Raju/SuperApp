<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { session, clearSession } from '$lib/stores/session';
  import { api } from '$lib/api';
  import { Circle } from 'svelte-loading-spinners';

  let loading = true;
  let error: string | null = null;

  onMount(async () => {
    try {
      // The backend will clear the HTTP-only cookies
      await api.logout();
      clearSession();
      goto('/');
    } catch (err: any) {
      error = err.response?.data?.message || err.message;
      loading = false;
    }
  });
</script>

<div class="max-w-md mx-auto">
  <div class="card text-center">
    {#if loading}
      <div class="flex flex-col items-center justify-center py-8 space-y-4">
        <Circle size="40" color="#0ea5e9" unit="px" duration="1s" />
        <p>Logging out...</p>
      </div>
    {:else if error}
      <div class="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded relative">
        <strong class="font-bold">Error!</strong>
        <span class="block sm:inline">{error}</span>
        <a href="/" class="btn btn-primary mt-4 w-full">
          Return to Homepage
        </a>
      </div>
    {/if}
  </div>
</div>