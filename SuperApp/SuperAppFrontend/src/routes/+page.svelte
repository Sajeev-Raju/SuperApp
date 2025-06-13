<script lang="ts">
  import { session } from '$lib/stores/session';

  const apps = [
    { id: 1, name: 'Chat App', icon: '💬', comingSoon: true },
    { id: 2, name: 'File Share', icon: '📁', comingSoon: true },
    { id: 3, name: 'Notes', icon: '📝', comingSoon: true },
    { id: 4, name: 'Calendar', icon: '📅', comingSoon: true },
    { id: 5, name: 'Tasks', icon: '✅', comingSoon: true },
    { id: 6, name: 'Photos', icon: '📸', comingSoon: true }
  ];
</script>

<div class="space-y-8">
  <!-- Hero Section -->
  <section class="text-center py-12 space-y-4">
    <h1 class="text-4xl font-bold text-gray-900 dark:text-white">
      Welcome to ID System
    </h1>
    <p class="text-xl text-gray-600 dark:text-gray-300 max-w-2xl mx-auto">
      Your gateway to multiple applications with a single unique identifier.
      {#if !$session.username}
        Register now to get started!
      {/if}
    </p>
    {#if !$session.username}
      <div class="mt-8">
        <a href="/register" class="btn btn-primary text-lg px-8 py-3">
          Get Your Unique ID
        </a>
      </div>
    {/if}
  </section>

  <!-- Apps Grid -->
  <section class="mt-12">
    <h2 class="text-2xl font-bold mb-6">Available Applications</h2>
    <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-6">
      {#each apps as app}
        <div class="card hover:shadow-xl transition-shadow relative overflow-hidden">
          {#if app.comingSoon}
            <div class="absolute top-2 right-2 bg-primary-500 text-white text-xs px-2 py-1 rounded-full">
              Coming Soon
            </div>
          {/if}
          <div class="text-4xl mb-4">{app.icon}</div>
          <h3 class="text-xl font-semibold mb-2">{app.name}</h3>
          <p class="text-gray-600 dark:text-gray-300">
            Access {app.name} with your unique identifier.
          </p>
          <button 
            class="btn btn-secondary mt-4 w-full"
            disabled={app.comingSoon || !$session.username}
          >
            {#if !$session.username}
              Register to Access
            {:else if app.comingSoon}
              Coming Soon
            {:else}
              Launch App
            {/if}
          </button>
        </div>
      {/each}
    </div>
  </section>

  <!-- Features Section -->
  <section class="mt-16">
    <h2 class="text-2xl font-bold mb-6">Why Choose ID System?</h2>
    <div class="grid grid-cols-1 md:grid-cols-3 gap-8">
      <div class="card">
        <div class="text-3xl mb-4">🔐</div>
        <h3 class="text-xl font-semibold mb-2">Secure Access</h3>
        <p class="text-gray-600 dark:text-gray-300">
          Multi-factor authentication and secure session management.
        </p>
      </div>
      <div class="card">
        <div class="text-3xl mb-4">🎯</div>
        <h3 class="text-xl font-semibold mb-2">Custom IDs</h3>
        <p class="text-gray-600 dark:text-gray-300">
          Choose from suggested IDs or create your own fancy identifier.
        </p>
      </div>
      <div class="card">
        <div class="text-3xl mb-4">🔄</div>
        <h3 class="text-xl font-semibold mb-2">Single Sign-On</h3>
        <p class="text-gray-600 dark:text-gray-300">
          Access all applications with one unique identifier.
        </p>
      </div>
    </div>
  </section>
</div>