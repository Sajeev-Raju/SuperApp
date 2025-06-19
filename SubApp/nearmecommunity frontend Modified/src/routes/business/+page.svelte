<script lang="ts">
  import { onMount } from 'svelte';
  import { businessApi, type Business } from '$api/apiClient';
  import { user } from '$stores/userStore';
  import { goto } from '$app/navigation';

  let businesses: Business[] = [];
  let loading = true;
  let error: string | null = null;
  let currentPage = 1;
  let pageSize = 10;
  let totalPages = 1;
  let totalItems = 0;

  async function fetchBusinesses(page = 1) {
    loading = true;
    error = null;
    try {
      const userId = $user.userId;
      if (!userId) {
        goto('/');
        return;
      }
      const res = await businessApi.getBusinesses(page - 1, pageSize);
      if (res && res.success && Array.isArray(res.data)) {
        businesses = res.data.map(b => ({
          ...b,
          imageUrl: b.image // Use backend-provided image URL directly
        }));
        currentPage = (res.page ?? (page - 1)) + 1;
        pageSize = res.size ?? pageSize;
        totalPages = res.totalPages ?? 1;
        totalItems = res.count ?? businesses.length;
      } else {
        businesses = [];
        totalPages = 1;
        totalItems = 0;
      }
    } catch (err) {
      error = 'Failed to load businesses.';
      businesses = [];
      totalPages = 1;
      totalItems = 0;
    } finally {
      loading = false;
    }
  }

  function goToPage(page: number) {
    if (page >= 1 && page <= totalPages && page !== currentPage) {
      fetchBusinesses(page);
    }
  }

  onMount(() => {
    fetchBusinesses();
  });
</script>

<div class="min-h-screen bg-gray-50 dark:bg-black pt-20 pb-8">
  <div class="container mx-auto px-4">
    <h2 class="text-2xl font-bold text-gray-900 dark:text-white mb-6">Businesses</h2>
    {#if loading}
      <div class="flex justify-center items-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-purple-500"></div>
      </div>
    {:else if error}
      <div class="text-center p-4">
        <p class="text-red-500 dark:text-red-400">{error}</p>
        <button on:click={() => fetchBusinesses(currentPage)} class="mt-2 px-4 py-2 bg-purple-500 text-white rounded hover:bg-purple-600">
          Try Again
        </button>
      </div>
    {:else if businesses.length === 0}
      <div class="text-center p-4">
        <p class="text-gray-500 dark:text-gray-400">No businesses found.</p>
      </div>
    {:else}
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {#each businesses as business}
          <div class="bg-white dark:bg-black rounded-xl shadow-md overflow-hidden border-2 border-gray-200 dark:border-gray-700 hover:border-purple-500 dark:hover:border-purple-400 transition-colors">
            {#if business.imageUrl}
              <img
                src={business.imageUrl}
                alt={business.name}
                class="w-full h-64 object-cover"
                on:error={(e) => { e.target.style.display = 'none'; e.target.nextElementSibling.style.display = 'flex'; }}
              />
              <div class="w-full h-64 bg-gray-200 dark:bg-purple-900/20 flex items-center justify-center" style="display: none;">
                <span class="text-gray-400 dark:text-purple-400">No image available</span>
              </div>
            {:else}
              <div class="w-full h-64 bg-gray-200 dark:bg-purple-900/20 flex items-center justify-center">
                <span class="text-gray-400 dark:text-purple-400">No image available</span>
              </div>
            {/if}
            <div class="p-6 flex flex-col items-center">
              <h3 class="text-xl font-semibold text-gray-900 dark:text-white mb-4 text-center">{business.title}</h3>
              <button
                on:click={() => goto(`/business/${business.businessId}`)}
                class="px-4 py-2 bg-purple-500 text-white rounded hover:bg-purple-600 transition-colors"
              >
                View Details
              </button>
            </div>
          </div>
        {/each}
      </div>
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
    {/if}
  </div>
</div>
