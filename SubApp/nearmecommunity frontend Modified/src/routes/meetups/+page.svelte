<script lang="ts">
  import { onMount } from "svelte";
  import { user } from "$stores/userStore";
  import { goto } from "$app/navigation";
  import { meetupsApi } from "$api/apiClient";
  import LoadingSpinner from "$components/ui/LoadingSpinner.svelte";
  import UserLocationBadge from "$components/ui/UserLocationBadge.svelte";

  let meetups = [];
  let isLoading = true;
  let error = null;
  let searchQuery = "";
  let selectedTags: string[] = [];
  let expandedMeetupId: string | null = null;
  let currentPage = 1;
  let pageSize = 30;
  let totalPages = 1;

  onMount(() => {
    loadMeetups(1);
  });

  onMount(async () => {
    if (!$user.userId || !$user.location) {
      goto("/");
      return;
    }

    try {
      await loadMeetups();
    } catch (err) {
      console.error("Error loading meetups:", err);
      error = "Failed to load meetups. Please try again.";
    } finally {
      isLoading = false;
    }
  });

  async function loadMeetups(page = 1) {
    isLoading = true;
    error = null;
    try {
      const response = await meetupsApi.getMeetups(page - 1, pageSize);
      if (response && response.data) {
        meetups = response.data;
        currentPage = (response.currentPage ?? response.page ?? (page - 1)) + 1;
        pageSize = response.size ?? pageSize;
        totalPages = response.totalPages ?? 1;
      } else {
        meetups = [];
        totalPages = 1;
      }
    } catch (err) {
      console.error("Error loading meetups:", err);
      error = "Failed to load meetups. Please try again.";
      meetups = [];
      totalPages = 1;
    } finally {
      isLoading = false;
    }
  }

  function toggleTag(tag: string) {
    if (selectedTags.includes(tag)) {
      selectedTags = selectedTags.filter(t => t !== tag);
    } else {
      selectedTags = [...selectedTags, tag];
    }
  }

  function toggleMeetup(meetupId: string) {
    expandedMeetupId = expandedMeetupId === meetupId ? null : meetupId;
  }

  function getMeetupImageUrl(meetupId: string): string {
    return meetupsApi.getMeetupImageUrl(meetupId);
  }

  function getPageNumbers() {
    const pages = [];
    for (let i = 1; i <= totalPages; i++) {
      pages.push(i);
    }
    return pages;
  }

  function goToPage(page: number) {
    if (page >= 1 && page <= totalPages && page !== currentPage) {
      loadMeetups(page);
    }
  }

  $: if (searchQuery || selectedTags.length) {
    currentPage = 1;
  }

  $: filteredMeetups = meetups.filter(meetup => {
    // Filter by search query
    const matchesSearch = searchQuery === "" || 
      meetup.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (meetup.description && meetup.description.toLowerCase().includes(searchQuery.toLowerCase()));
    
    // Filter by tags
    const matchesTags = selectedTags.length === 0 || 
      (meetup.tags && selectedTags.every(tag => meetup.tags.includes(tag)));
    
    return matchesSearch && matchesTags;
  });
</script>

<div class="min-h-screen bg-gray-50 dark:bg-black pt-16 pb-20">
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
    <div class="py-6">
      <div class="flex flex-col md:flex-row md:items-center md:justify-between mb-8">
        <div>
          <h1 class="text-3xl font-bold text-gray-900 dark:text-white">Local Meetups</h1>
          <div class="mt-2">
            <UserLocationBadge />
          </div>
        </div>
        <div class="mt-4 md:mt-0 flex flex-wrap gap-2">
          <a href="/meetups/my-events" class="btn btn-outline">
            My Events
          </a>
          <a href="/meetups/create" class="btn btn-primary">
            Create Meetup
          </a>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-4 gap-6">
        <!-- Filters Sidebar -->
        <div class="lg:col-span-1">
          <div class="bg-white dark:bg-black rounded-xl shadow-md p-6 border-2 border-purple-500 dark:border-purple-400">
            <div class="flex items-center justify-between mb-6">
              <h2 class="text-lg font-semibold text-gray-900 dark:text-white">
                Filters
              </h2>
              <button
                on:click={() => { selectedTags = []; searchQuery = ""; }}
                class="text-sm text-primary-600 dark:text-primary-400 hover:text-primary-500 dark:hover:text-primary-300"
              >
                Clear all
              </button>
            </div>

            <!-- Search -->
            <div class="mb-6">
              <label for="search" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Search
              </label>
              <div class="relative">
                <input
                  type="text"
                  id="search"
                  bind:value={searchQuery}
                  class="input w-full pl-10"
                  placeholder="Search meetups..."
                />
                <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <svg class="h-5 w-5 text-gray-400" viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z" clip-rule="evenodd" />
                  </svg>
                </div>
              </div>
            </div>

            <!-- Categories -->
            <div class="mb-6">
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Categories
              </label>
              <div class="flex flex-wrap gap-2">
                {#each ["Community", "Outdoors", "Technology", "Food", "Arts", "Sports", "Education", "Business"] as tag}
                  <button
                    on:click={() => toggleTag(tag)}
                    class={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium transition-colors ${
                      selectedTags.includes(tag)
                        ? "bg-primary-100 text-primary-800 dark:bg-primary-900/40 dark:text-primary-300"
                        : "bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600"
                    }`}
                  >
                    {tag}
                  </button>
                {/each}
              </div>
            </div>

            <!-- Sort -->
            <div>
              <label for="sort" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Sort by
              </label>
              <select
                id="sort"
                class="input w-full"
              >
                <option value="newest">Newest first</option>
                <option value="oldest">Oldest first</option>
                <option value="closest">Closest first</option>
              </select>
            </div>
          </div>
        </div>

        <div class="lg:col-span-3">
          {#if isLoading}
            <div class="flex justify-center py-20">
              <LoadingSpinner size="lg" />
            </div>
          {:else if error}
            <div class="bg-danger-50 dark:bg-danger-900/20 text-danger-800 dark:text-danger-200 p-4 rounded-lg mb-6">
              {error}
              <button class="underline ml-2" on:click={loadMeetups}>Try again</button>
            </div>
          {:else if filteredMeetups.length === 0}
            <div class="bg-white dark:bg-black rounded-xl shadow-md p-8 text-center">
              <div class="text-gray-500 dark:text-gray-400 mb-4">No meetups found matching your criteria</div>
              {#if selectedTags.length > 0 || searchQuery}
                <button on:click={() => { selectedTags = []; searchQuery = ""; }} class="btn btn-outline">
                  Clear filters
                </button>
              {:else}
                <a href="/meetups/create" class="btn btn-primary">
                  Create the first meetup
                </a>
              {/if}
            </div>
          {:else}
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {#each filteredMeetups as meetup}
                <div 
                  class="bg-white dark:bg-black rounded-xl shadow-md overflow-hidden border border-purple-100 dark:border-purple-900/50 transition-all duration-200 hover:border-2 hover:border-purple-500 dark:hover:border-purple-400 hover:shadow-lg hover:shadow-purple-500/20 dark:hover:shadow-purple-400/20 cursor-pointer"
                  on:click={() => toggleMeetup(meetup.id)}
                >
                  <div class="h-48 bg-gray-200 dark:bg-black relative overflow-hidden">
                    <img
                      src={getMeetupImageUrl(meetup.id)}
                      alt={meetup.title}
                      class="w-full h-full object-cover"
                      onerror="this.src='https://images.pexels.com/photos/2774556/pexels-photo-2774556.jpeg?auto=compress&cs=tinysrgb&w=800'; this.onerror=null;"
                    />
                    <div class="absolute top-0 right-0 m-2">
                      <div class="bg-white dark:bg-gray-900 text-purple-800 dark:text-purple-300 text-xs font-medium px-2 py-1 rounded shadow border border-purple-100 dark:border-purple-900/50">
                        {new Date(meetup.date).toLocaleDateString()}
                      </div>
                    </div>
                  </div>
                  <div class="p-5">
                    <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-2 line-clamp-1">
                      {meetup.title}
                    </h3>
                    {#if expandedMeetupId === meetup.id}
                      <div class="mt-4">
                        <p class="text-gray-600 dark:text-gray-300 text-sm mb-3">
                          {meetup.description || "No description provided."}
                        </p>
                        <div class="flex justify-between items-center text-sm">
                          <div class="text-purple-600 dark:text-purple-400">
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 inline mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                            </svg>
                            {meetup.location || "Location TBD"}
                          </div>
                          <div class="text-gray-500 dark:text-gray-400">
                            {meetup.attendees?.length || 0} attending
                          </div>
                        </div>
                        {#if meetup.tags && meetup.tags.length > 0}
                          <div class="mt-3 flex flex-wrap gap-1">
                            {#each meetup.tags.slice(0, 3) as tag}
                              <span class="badge badge-primary text-xs">
                                {tag}
                              </span>
                            {/each}
                            {#if meetup.tags.length > 3}
                              <span class="badge bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-300 text-xs">
                                +{meetup.tags.length - 3} more
                              </span>
                            {/if}
                          </div>
                        {/if}
                        <div class="mt-4">
                          <a href={`/meetups/${meetup.id}`} class="btn btn-outline" on:click|stopPropagation>
                            View Details
                          </a>
                        </div>
                      </div>
                    {:else}
                      <div class="flex justify-between items-center text-sm">
                        <div class="text-purple-600 dark:text-purple-400">
                          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 inline mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                          </svg>
                          {meetup.location || "Location TBD"}
                        </div>
                        <div class="text-gray-500 dark:text-gray-400">
                          {meetup.attendees?.length || 0} attending
                        </div>
                      </div>
                      {#if meetup.tags && meetup.tags.length > 0}
                        <div class="mt-3 flex flex-wrap gap-1">
                          {#each meetup.tags.slice(0, 3) as tag}
                            <span class="badge badge-primary text-xs">
                              {tag}
                            </span>
                          {/each}
                          {#if meetup.tags.length > 3}
                            <span class="badge bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-300 text-xs">
                              +{meetup.tags.length - 3} more
                            </span>
                          {/if}
                        </div>
                      {/if}
                    {/if}
                  </div>
                </div>
              {/each}
            </div>
          {/if}

          <!-- Pagination Controls -->
          {#if totalPages > 1}
            <div class="flex justify-center mt-8 items-center gap-4">
              <button
                on:click={() => goToPage(currentPage - 1)}
                disabled={currentPage === 1}
                class="px-4 py-2 rounded-lg bg-purple-500 text-white disabled:opacity-50 disabled:cursor-not-allowed hover:bg-purple-600 transition-colors"
              >
                Previous
              </button>
              <span class="text-gray-700 dark:text-gray-300 text-sm">
                Page {currentPage} of {totalPages}
              </span>
              <button
                on:click={() => goToPage(currentPage + 1)}
                disabled={currentPage === totalPages}
                class="px-4 py-2 rounded-lg bg-purple-500 text-white disabled:opacity-50 disabled:cursor-not-allowed hover:bg-purple-600 transition-colors"
              >
                Next
              </button>
            </div>
          {/if}
        </div>
      </div>
    </div>
  </div>
</div>