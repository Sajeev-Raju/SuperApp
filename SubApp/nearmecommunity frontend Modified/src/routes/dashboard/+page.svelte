<script lang="ts">
  import { onMount } from "svelte";
  import { user } from "$stores/userStore";
  import { goto } from "$app/navigation";
  import { browser } from "$app/environment";
  import ModuleCard from "$components/dashboard/ModuleCard.svelte";
  import UserLocationBadge from "$components/ui/UserLocationBadge.svelte";
  import LoadingSpinner from "$components/ui/LoadingSpinner.svelte";

  let isLoading = true;

  onMount(async () => {
    if (browser) {
      if (!$user.userId) {
        goto("/");
        return;
      }

      // Check if location is stored in localStorage
      const storedLocation = localStorage.getItem('userLocation');
      if (!storedLocation) {
        goto("/setup");
        return;
      }

      try {
        const location = JSON.parse(storedLocation);
        if (!location.latitude || !location.longitude) {
          goto("/setup");
          return;
        }
      } catch (e) {
        console.error('Error parsing stored location:', e);
        goto("/setup");
        return;
      }

      isLoading = false;
    }
  });

  const modules = [
    {
      title: "Q&A",
      description: "Ask and answer questions in your local area",
      icon: "question-mark",
      color: "primary",
      link: "/questions"
    },
    {
      title: "Meetups",
      description: "Create and join local events",
      icon: "calendar",
      color: "secondary",
      link: "/meetups"
    },
    {
      title: "Classifieds",
      description: "Buy, sell, and trade locally",
      icon: "tag",
      color: "accent",
      link: "/classifieds"
    },
    {
      title: "Polls",
      description: "Participate in local polls and surveys",
      icon: "chart",
      color: "success",
      link: "/polls"
    },
    {
      title: "Emergency",
      description: "Emergency alerts and communications",
      icon: "alert",
      color: "danger",
      link: "/emergency"
    },
    {
      title: "Businesses",
      description: "Discover and manage local businesses",
      icon: "store",
      color: "warning",
      link: "/business"
    }
  ];
</script>

<div class="min-h-screen bg-white dark:bg-black pt-16 pb-20">
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
    <div class="py-6">
      <div class="flex flex-col md:flex-row md:items-center md:justify-between mb-8">
        <div>
          <h1 class="text-3xl font-bold text-gray-900 dark:text-white">Dashboard</h1>
          <div class="mt-2">
            <UserLocationBadge />
          </div>
        </div>
      </div>

      {#if isLoading}
        <div class="flex justify-center py-20">
          <LoadingSpinner size="lg" />
        </div>
      {:else}
        <div class="grid grid-cols-1 gap-6">
          <div>
            <div class="bg-white dark:bg-black rounded-xl shadow-md overflow-hidden mb-6 border-2 border-purple-500 dark:border-purple-400">
              <div class="p-6">
                <h2 class="text-xl font-semibold text-gray-900 dark:text-white mb-4">Welcome to Your Local Community</h2>
                <p class="text-gray-600 dark:text-gray-300">
                  Discover what's happening in your area. Connect with neighbors, find local events, and more.
                </p>
              </div>
            </div>

            <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
              {#each modules as module}
                <ModuleCard {...module} />
              {/each}
            </div>
          </div>
        </div>
      {/if}
    </div>
  </div>
</div>