<script lang="ts">
  import { goto } from '$app/navigation';
  import { onMount } from 'svelte';
  // import { state } from 'svelte';
  import { writable } from 'svelte/store';

  const formState = $state({
    copied: false,
    countdown: 5,
    username: sessionStorage.getItem('username') || ''
  });

  onMount(() => {
    const timer = setInterval(() => {
      formState.countdown--;
      if (formState.countdown === 0) {
        clearInterval(timer);
        goto('/');
      }
    }, 1000);

    return () => clearInterval(timer);
  });

  async function copyToClipboard() {
    try {
      await navigator.clipboard.writeText(formState.username);
      formState.copied = true;
      setTimeout(() => formState.copied = false, 2000);
    } catch (error) {
      console.error('Failed to copy:', error);
    }
  }
</script>

<div class="max-w-md mx-auto">
  <div class="card text-center">
    <div class="text-5xl mb-6">🎉</div>
    <h1 class="text-2xl font-bold mb-4">Registration Complete!</h1>
    
    <div class="space-y-6">
      <p class="text-gray-600 dark:text-gray-400">
        Your unique identifier is:
      </p>
      
      <div class="p-4 bg-primary-50 dark:bg-primary-900 rounded-lg">
        <p class="text-3xl font-bold text-primary-600 dark:text-primary-400">
          {formState.username}
        </p>
      </div>

      <button 
        class="btn btn-secondary w-full"
        on:click={copyToClipboard}
      >
        {formState.copied ? '✓ Copied!' : 'Copy to Clipboard'}
      </button>

      <p class="text-sm text-gray-500">
        Redirecting to homepage in {formState.countdown} seconds...
      </p>

      <div class="space-x-4">
        <a href="/" class="btn btn-primary">
          Go to Homepage
        </a>
        <a href="/login" class="btn btn-secondary">
          Login
        </a>
      </div>
    </div>
  </div>
</div>