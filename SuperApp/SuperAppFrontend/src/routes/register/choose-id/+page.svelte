<script lang="ts">
    import { goto } from '$app/navigation';
    import { Circle } from 'svelte-loading-spinners';
  
    function handleAutoGenerate() {
      goto('/register/auto-id');
    }
  
    function handleCustomId() {
      goto('/register/custom-id');
    }
  
    function handleSuggestedIds() {
      goto('/register/suggested-id');
    }
  </script>
  
  <div class="max-w-md mx-auto">
    <div class="card">
      <h1 class="text-2xl font-bold mb-6">Choose Your ID Type</h1>
      <p class="text-gray-600 dark:text-gray-400 mb-8">
        Select how you want to get your unique identifier:
      </p>
  
      <div class="space-y-4">
        <button 
          class="btn btn-primary w-full flex items-center justify-center space-x-2"
          on:click={handleAutoGenerate}
        >
          <span class="text-xl">🎲</span>
          <span>Auto-Generate ID</span>
        </button>
  
        <button 
          class="btn btn-primary w-full flex items-center justify-center space-x-2"
          on:click={handleCustomId}
        >
          <span class="text-xl">✏️</span>
          <span>Create Custom ID</span>
        </button>
  
        <button 
          class="btn btn-primary w-full flex items-center justify-center space-x-2"
          on:click={handleSuggestedIds}
        >
          <span class="text-xl">🎯</span>
          <span>View Suggested IDs</span>
        </button>
      </div>
  
      <div class="mt-8 space-y-4">
        <div class="p-4 bg-gray-50 dark:bg-gray-800 rounded-lg">
          <h3 class="font-semibold mb-2">🎲 Auto-Generate ID</h3>
          <p class="text-sm text-gray-600 dark:text-gray-400">
            Let our system generate a unique identifier for you instantly.
          </p>
        </div>
  
        <div class="p-4 bg-gray-50 dark:bg-gray-800 rounded-lg">
          <h3 class="font-semibold mb-2">✏️ Custom ID</h3>
          <p class="text-sm text-gray-600 dark:text-gray-400">
            Create your own unique identifier. Premium fees may apply for fancy patterns.
          </p>
        </div>
  
        <div class="p-4 bg-gray-50 dark:bg-gray-800 rounded-lg">
          <h3 class="font-semibold mb-2">🎯 Suggested IDs</h3>
          <p class="text-sm text-gray-600 dark:text-gray-400">
            Choose from our curated list of unique identifiers. Up to 3 attempts available.
          </p>
        </div>
      </div>
    </div>
  </div>