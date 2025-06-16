<script lang="ts">
  import '../app.css';
  import { session, toggleDarkMode, initializeSession } from '$lib/stores/session';
  import { onMount } from 'svelte';
  import { Circle } from 'svelte-loading-spinners';

  onMount(async () => {
    // Initialize session
    await initializeSession();
    
    // Check for dark mode preference
    if (typeof window !== 'undefined') {
      const isDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      if (isDark) {
        toggleDarkMode();
      }
    }
  });
</script>

{#if $session.isLoading}
  <div class="fixed inset-0 flex items-center justify-center">
    <Circle size="60" color="#0ea5e9" unit="px" duration="1s" />
  </div>
{:else}
  <div class="min-h-screen">
    <!-- Header -->
    <header class="bg-white dark:bg-gray-800 shadow-sm">
      <nav class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        <a href="/" class="text-2xl font-bold text-primary-600 dark:text-primary-400">
          ID System
        </a>
        
        <div class="flex items-center space-x-4">
          <!-- Dark Mode Toggle -->
          <button 
            class="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700"
            on:click={toggleDarkMode}
          >
            {#if $session.darkMode}
              🌞
            {:else}
              🌙
            {/if}
          </button>

          <!-- Login/Register Buttons -->
          {#if $session.username}
            <div class="flex items-center space-x-2">
              <span class="text-sm">Welcome, {$session.username}!</span>
              <a 
                href="/logout" 
                class="btn btn-secondary text-sm"
              >
                Logout
              </a>
            </div>
          {:else}
            <div class="space-x-2">
              <a href="/login" class="btn btn-secondary text-sm">Login</a>
              <a href="/register" class="btn btn-primary text-sm">Register</a>
            </div>
          {/if}
        </div>
      </nav>
    </header>

    <!-- Main Content -->
    <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <slot />
    </main>

    <!-- Footer -->
    <footer class="bg-white dark:bg-gray-800 border-t dark:border-gray-700">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-center">
        <p class="text-sm text-gray-500 dark:text-gray-400">
          © 2023 ID System. All rights reserved.
        </p>
      </div>
    </footer>
  </div>
{/if}