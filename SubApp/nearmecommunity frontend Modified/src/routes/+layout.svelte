<script lang="ts">
  import "../app.css";
  import { onMount } from "svelte";
  import { navigating } from "$app/stores";
  import { user } from "$stores/userStore";
  import { theme } from "$stores/themeStore";
  import Navbar from "$components/layout/Navbar.svelte";
  import Footer from "$components/layout/Footer.svelte";
  import LoadingSpinner from "$components/ui/LoadingSpinner.svelte";
  import { Toaster } from "svelte-french-toast";
  import { page } from "$app/stores";
  import { browser } from "$app/environment";
  import { goto } from "$app/navigation";

  onMount(async () => {
    if (browser) {
      // Initialize theme to light mode
      theme.initialize();
      
      // Check if user ID exists in localStorage
      const storedUserId = localStorage.getItem("userId");
      if (storedUserId) {
        user.setUserId(storedUserId);
        
        // Check if user has location
        try {
          const response = await fetch(`http://localhost:8080/api/location/${storedUserId}`);
          const locationData = await response.json();
          
          if (locationData && locationData.latitude && locationData.longitude) {
            user.setLocation({
              latitude: locationData.latitude,
              longitude: locationData.longitude,
              locationName: locationData.locationName || "My Location"
            });
          } else if ($page.url.pathname !== "/setup") {
            // Redirect to setup if no location
            goto("/setup");
          }
        } catch (error) {
          console.error("Error fetching user location:", error);
          if ($page.url.pathname !== "/setup") {
            goto("/setup");
          }
        }
      } else if ($page.url.pathname !== "/") {
        // Redirect to home if no user ID
        goto("/");
      }
    }
  });

  // Check if we need to show the navigation
  $: showNav = browser && $user.userId && $user.location && $page.url.pathname !== "/";
</script>

{#if $navigating}
  <div class="fixed inset-0 flex items-center justify-center bg-white bg-opacity-75 z-50">
    <LoadingSpinner size="lg" />
  </div>
{/if}

{#if showNav}
  <Navbar />
{/if}

<main class="min-h-screen pb-16">
  <slot />
</main>

{#if showNav}
  <Footer />
{/if}

<Toaster position="top-right" />