<script lang="ts">
  import { onMount } from "svelte";
  import { user } from "$stores/userStore";
  import { goto } from "$app/navigation";
  import apiClient from "$api/apiClient";
  import LoadingSpinner from "$components/ui/LoadingSpinner.svelte";
  import toast from "svelte-french-toast";

  interface Meetup {
    id: string;
    title: string;
    description: string;
    startDate: string;
    eventAddress: string;
    tags: string[];
    attendees?: any[];
  }

  let meetups: Meetup[] = [];
  let isLoading = true;
  let error: string | null = null;

  onMount(async () => {
    if (!$user.userId) {
      goto("/");
      return;
    }

    try {
      await loadMyEvents();
    } catch (err) {
      console.error("Error loading my events:", err);
      error = "Failed to load your events. Please try again.";
    } finally {
      isLoading = false;
    }
  });

  async function loadMyEvents() {
    isLoading = true;
    error = null;
    
    try {
      meetups = await apiClient.meetups.getUserMeetups();
    } catch (err) {
      console.error("Error loading my events:", err);
      if (err instanceof Error) {
        error = err.message;
      } else {
        error = "Failed to load your events. Please try again.";
      }
      toast.error(error);
    } finally {
      isLoading = false;
    }
  }

  async function handleDeleteMeetup(id: string) {
    if (!confirm("Are you sure you want to delete this meetup?")) return;
    
    try {
      await apiClient.meetups.deleteMeetup(id);
      meetups = meetups.filter(m => m.id !== id);
      toast.success("Meetup deleted successfully");
    } catch (err) {
      console.error("Error deleting meetup:", err);
      if (err instanceof Error) {
        toast.error(err.message);
      } else {
        toast.error("Failed to delete meetup");
      }
    }
  }

  function getMeetupImageUrl(meetupId: string): string {
    return apiClient.meetups.getMeetupImageUrl(meetupId);
  }
</script>

<div class="min-h-screen bg-gray-50 dark:bg-black pt-16 pb-20">
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
    <div class="py-6">
      <div class="flex flex-col md:flex-row md:items-center md:justify-between mb-8">
        <div>
          <h1 class="text-3xl font-bold text-gray-900 dark:text-white">My Events</h1>
          <p class="mt-2 text-gray-600 dark:text-gray-400">
            Manage your organized meetups
          </p>
        </div>
        <div class="mt-4 md:mt-0">
          <a href="/meetups/create" class="btn btn-primary">
            Create New Meetup
          </a>
        </div>
      </div>

      {#if isLoading}
        <div class="flex justify-center py-20">
          <LoadingSpinner size="lg" />
        </div>
      {:else if error}
        <div class="bg-danger-50 dark:bg-danger-900/20 text-danger-800 dark:text-danger-200 p-4 rounded-lg mb-6">
          {error}
          <button class="underline ml-2" on:click={loadMyEvents}>Try again</button>
        </div>
      {:else if meetups.length === 0}
        <div class="bg-white dark:bg-black rounded-xl shadow-md p-8 text-center">
          <div class="text-gray-500 dark:text-gray-400 mb-4">You haven't organized any meetups yet</div>
          <a href="/meetups/create" class="btn btn-primary">
            Create your first meetup
          </a>
        </div>
      {:else}
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {#each meetups as meetup}
            <div class="bg-white dark:bg-black rounded-xl shadow-md overflow-hidden hover:shadow-lg transition-shadow">
              <a href={`/meetups/${meetup.id}`} class="block">
                <div class="h-48 bg-gray-200 dark:bg-black relative overflow-hidden">
                  <img
                    src={getMeetupImageUrl(meetup.id)}
                    alt={meetup.title}
                    class="w-full h-full object-cover"
                    onerror="this.src='https://images.pexels.com/photos/2774556/pexels-photo-2774556.jpeg?auto=compress&cs=tinysrgb&w=800'; this.onerror=null;"
                  />
                  <div class="absolute top-0 right-0 m-2">
                    <div class="bg-white dark:bg-gray-800 text-secondary-800 dark:text-secondary-300 text-xs font-medium px-2 py-1 rounded shadow">
                      {new Date(meetup.startDate).toLocaleDateString()}
                    </div>
                  </div>
                </div>
                <div class="p-5">
                  <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-2 line-clamp-1">
                    {meetup.title}
                  </h3>
                  <p class="text-gray-600 dark:text-gray-300 text-sm mb-3 line-clamp-2">
                    {meetup.description || "No description provided."}
                  </p>
                  <div class="flex justify-between items-center text-sm">
                    <div class="text-secondary-600 dark:text-secondary-400">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 inline mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                      </svg>
                      {meetup.eventAddress || "Location TBD"}
                    </div>
                    <div class="text-gray-500 dark:text-gray-400">
                      {meetup.attendees?.length || 0} attending
                    </div>
                  </div>
                  {#if meetup.tags && meetup.tags.length > 0}
                    <div class="mt-3 flex flex-wrap gap-1">
                      {#each meetup.tags.slice(0, 3) as tag}
                        <span class="badge badge-secondary text-xs">
                          {tag}
                        </span>
                      {/each}
                      {#if meetup.tags.length > 3}
                        <span class="badge bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300 text-xs">
                          +{meetup.tags.length - 3} more
                        </span>
                      {/if}
                    </div>
                  {/if}
                </div>
              </a>
              <div class="px-5 pb-5">
                <button
                  on:click|stopPropagation={() => handleDeleteMeetup(meetup.id)}
                  class="btn btn-danger w-full"
                >
                  Delete Meetup
                </button>
              </div>
            </div>
          {/each}
        </div>
      {/if}
    </div>
  </div>
</div> 