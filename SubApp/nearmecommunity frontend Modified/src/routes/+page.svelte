<script lang="ts">
  import { onMount } from "svelte";
  import { user } from "$stores/userStore";
  import { goto } from "$app/navigation";
  import { browser } from "$app/environment";
  import Logo from "$components/ui/Logo.svelte";

  let userId = "";
  let loading = false;
  let error = "";

  onMount(() => {
    if (browser) {
      // Check if user ID exists in localStorage
      const storedUserId = localStorage.getItem("userId");
      if (storedUserId) {
        user.setUserId(storedUserId);
        
        // Check if user has location
        const hasLocation = localStorage.getItem("userLocation");
        if (hasLocation) {
          goto("/dashboard");
        } else {
          goto("/setup");
        }
      }
    }
  });

  async function handleSubmit() {
    if (!userId.trim()) {
      error = "Please enter a User ID";
      return;
    }

    loading = true;
    error = "";

    try {
      // Save user ID to store and localStorage
      user.setUserId(userId);
      localStorage.setItem("userId", userId);
      
      // Check if user has location
      const response = await fetch(`http://localhost:8080/api/location/${userId}`);
      const locationData = await response.json();
      
      if (locationData && locationData.latitude && locationData.longitude) {
        user.setLocation({
          latitude: locationData.latitude,
          longitude: locationData.longitude,
          locationName: locationData.locationName || "My Location"
        });
        localStorage.setItem("userLocation", JSON.stringify({
          latitude: locationData.latitude,
          longitude: locationData.longitude,
          locationName: locationData.locationName || "My Location"
        }));
        goto("/dashboard");
      } else {
        goto("/setup");
      }
    } catch (error) {
      console.error("Error:", error);
      goto("/setup");
    } finally {
      loading = false;
    }
  }
</script>

<div class="min-h-screen flex flex-col items-center justify-center p-4 bg-gradient-to-br from-primary-50 to-secondary-50 dark:from-gray-900 dark:to-gray-800">
  <div class="w-full max-w-md p-8 space-y-8 bg-white dark:bg-gray-800 rounded-xl shadow-lg">
    <div class="text-center">
      <Logo size="lg" />
      <h1 class="mt-6 text-3xl font-bold text-gray-900 dark:text-white">Welcome to NearMe</h1>
      <p class="mt-2 text-gray-600 dark:text-gray-300">Your hyperlocal community platform</p>
    </div>
    
    <form class="mt-8 space-y-6" on:submit|preventDefault={handleSubmit}>
      <div>
        <label for="userId" class="label">Enter Your User ID</label>
        <input
          id="userId"
          name="userId"
          type="text"
          required
          bind:value={userId}
          class="input"
          placeholder="User ID"
          disabled={loading}
        />
        {#if error}
          <p class="mt-1 text-sm text-danger-600">{error}</p>
        {/if}
      </div>
      
      <button
        type="submit"
        class="btn btn-primary w-full"
        disabled={loading}
      >
        {#if loading}
          <span class="inline-block animate-spin mr-2">⟳</span>
          Loading...
        {:else}
          Continue
        {/if}
      </button>
    </form>
    
    <div class="mt-6 text-center text-sm">
      <p class="text-gray-600 dark:text-gray-400">
        Connect with your local community within a 15km radius
      </p>
    </div>
  </div>
  
  <div class="mt-8 text-center max-w-md">
    <h2 class="text-xl font-semibold mb-4 text-gray-800 dark:text-gray-200">What's NearMe?</h2>
    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
      <div class="p-4 bg-white dark:bg-gray-800 rounded-lg shadow">
        <div class="text-primary-500 text-2xl mb-2">Q&A</div>
        <p class="text-gray-600 dark:text-gray-300">Ask and answer questions in your local area</p>
      </div>
      <div class="p-4 bg-white dark:bg-gray-800 rounded-lg shadow">
        <div class="text-secondary-500 text-2xl mb-2">Meetups</div>
        <p class="text-gray-600 dark:text-gray-300">Create and join local events</p>
      </div>
      <div class="p-4 bg-white dark:bg-gray-800 rounded-lg shadow">
        <div class="text-accent-500 text-2xl mb-2">Classifieds</div>
        <p class="text-gray-600 dark:text-gray-300">Buy, sell, and trade locally</p>
      </div>
      <div class="p-4 bg-white dark:bg-gray-800 rounded-lg shadow">
        <div class="text-warning-500 text-2xl mb-2">Community</div>
        <p class="text-gray-600 dark:text-gray-300">Polls, alerts, and local discussions</p>
      </div>
    </div>
  </div>
</div>