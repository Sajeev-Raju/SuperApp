<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { toast } from 'svelte-french-toast';
  import { user } from '$stores/userStore';

  interface Business {
    businessId: number;
    userId: string;
    name: string;
    title: string;
    tags: string;
    description: string;
    image: string | null;
    googlemapsURL: string;
    longitude: number | null;
    latitude: number | null;
    address: string;
    mobileNumber: string;
    timings: string;
    active: boolean;
  }

  const API_URL = 'http://localhost:8080/api/business';
  let businesses: Business[] = [];
  let allTags: string[] = [];
  let selectedTags: string[] = [];
  let loading = true;
  let error: string | null = null;
  let searchQuery = '';
  let viewMode: 'grid' | 'list' = 'grid';
  let showCreateModal = false;
  let myBusinessesOnly = false;
  let currentPage = 1;
  let totalPages = 1;
  let pageSize = 10;
  let totalItems = 0;
  let showAllTags = false;
  let tagSearchQuery = '';
  let maxDisplayTags = 10;

  function checkUser() {
    const userId = localStorage.getItem('userId');
    if (!userId) {
      toast.error('Please log in to continue');
      goto('/');
      return false;
    }
    return true;
  }

  function handleBusinessClick(businessId: number) {
    goto(`/business/${businessId}`);
  }

  $: filteredTags = allTags
    .filter(tag => tag.toLowerCase().includes(tagSearchQuery.toLowerCase()))
    .sort((a, b) => {
      const countA = businesses.filter(business => 
        (business.tags ?? '').split(',').map(t => t.trim().toLowerCase()).includes(a.toLowerCase())
      ).length;
      const countB = businesses.filter(business => 
        (business.tags ?? '').split(',').map(t => t.trim().toLowerCase()).includes(b.toLowerCase())
      ).length;
      return countB - countA;
    });

  $: displayTags = showAllTags ? filteredTags : filteredTags.slice(0, maxDisplayTags);

  $: filteredBusinesses = businesses.filter(business => {
    const searchLower = searchQuery.toLowerCase();
    const matchesSearch = 
      business.name.toLowerCase().includes(searchLower) ||
      business.title.toLowerCase().includes(searchLower) ||
      business.description.toLowerCase().includes(searchLower) ||
      business.address.toLowerCase().includes(searchLower);
    const businessTags = (business.tags ?? '').split(',').map(tag => tag.trim().toLowerCase());
    const matchesTags = selectedTags.length === 0 || 
      selectedTags.every(tag => businessTags.includes(tag.toLowerCase()));
    return matchesSearch && matchesTags;
  });

  async function fetchBusinesses() {
    try {
      loading = true;
      error = null;
      const userData = user.get();
      const headers = {
        'X-User-ID': userData.userId || ''
      };
      const response = await fetch(`${API_URL}/?page=${currentPage - 1}&size=${pageSize}`, { headers });
      if (!response.ok) {
        throw new Error('Failed to fetch businesses');
      }
      const data = await response.json();
      if (data.success) {
        businesses = (data.data || []).map(b => ({
          ...b,
          image: b.image // Use backend-provided image URL directly
        }));
        totalItems = data.count ?? businesses.length;
        totalPages = data.totalPages ?? Math.ceil(totalItems / pageSize);
        if (currentPage > totalPages && totalPages > 0) {
          currentPage = totalPages;
        }
        const tagSet = new Set<string>();
        businesses.forEach(business => {
          if (business.tags) {
            (business.tags ?? '').split(',').forEach(tag => tagSet.add(tag.trim()));
          }
        });
        allTags = Array.from(tagSet);
      } else {
        throw new Error(data.message || 'Failed to fetch businesses');
      }
    } catch (err) {
      console.error('Error fetching businesses:', err);
      error = err instanceof Error ? err.message : 'Failed to fetch businesses';
    } finally {
      loading = false;
    }
  }

  async function fetchBusinessesByTags(tags: string[]) {
    if (!checkUser()) return;
    try {
      loading = true;
      error = null;
      const userId = localStorage.getItem('userId');
      const response = await fetch(`${API_URL}/tags=${tags.join(',')}?page=${currentPage - 1}&size=${pageSize}`, {
        headers: {
          'X-User-ID': userId || ''
        }
      });
      const result = await response.json();
      if (result.success && Array.isArray(result.data)) {
        businesses = result.data.map(b => ({
          ...b,
          image: b.image
        }));
        totalItems = result.count ?? businesses.length;
        totalPages = result.totalPages ?? Math.ceil(totalItems / pageSize);
        if (currentPage > totalPages && totalPages > 0) {
          currentPage = totalPages;
        }
        const tagSet = new Set<string>();
        businesses.forEach(business => {
          if (business.tags) {
            (business.tags ?? '').split(',').forEach(tag => tagSet.add(tag.trim()));
          }
        });
        allTags = Array.from(tagSet);
        toast.success('Businesses filtered by tags');
      } else {
        throw new Error(result.message || 'Failed to fetch businesses by tags');
      }
    } catch (e) {
      error = e instanceof Error ? e.message : 'An error occurred';
      console.error('Error fetching businesses by tags:', e);
      toast.error(error);
    } finally {
      loading = false;
    }
  }

  async function fetchMyBusinesses() {
    if (!checkUser()) return;
    try {
      loading = true;
      error = null;
      const userId = localStorage.getItem('userId');
      const response = await fetch(`${API_URL}/mine?page=${currentPage - 1}&size=${pageSize}`, {
        headers: {
          'X-User-ID': userId || ''
        }
      });
      const result = await response.json();
      if (result.success && Array.isArray(result.data)) {
        businesses = result.data.map(b => ({
          ...b,
          image: b.image
        }));
        totalItems = result.count ?? businesses.length;
        totalPages = result.totalPages ?? Math.ceil(totalItems / pageSize);
        if (currentPage > totalPages && totalPages > 0) {
          currentPage = totalPages;
        }
        const tagSet = new Set<string>();
        businesses.forEach(business => {
          if (business.tags) {
            (business.tags ?? '').split(',').forEach(tag => tagSet.add(tag.trim()));
          }
        });
        allTags = Array.from(tagSet);
        toast.success('Showing your businesses');
      } else {
        throw new Error(result.message || 'Failed to fetch my businesses');
      }
    } catch (e) {
      error = e instanceof Error ? e.message : 'An error occurred';
      console.error('Error fetching my businesses:', e);
      toast.error(error);
    } finally {
      loading = false;
    }
  }

  function toggleBusinessView() {
    myBusinessesOnly = !myBusinessesOnly;
    currentPage = 1;
    if (myBusinessesOnly) {
      fetchMyBusinesses();
    } else {
      fetchBusinesses();
    }
  }

  function goToPage(page: number) {
    if (page >= 1 && page <= totalPages && page !== currentPage) {
      currentPage = page;
      if (myBusinessesOnly) {
        fetchMyBusinesses();
      } else if (selectedTags.length > 0) {
        fetchBusinessesByTags(selectedTags);
      } else {
        fetchBusinesses();
      }
    }
  }

  $: if (searchQuery || selectedTags.length) {
    currentPage = 1;
    if (myBusinessesOnly) {
      fetchMyBusinesses();
    } else if (selectedTags.length > 0) {
      fetchBusinessesByTags(selectedTags);
    } else {
      fetchBusinesses();
    }
  }

  onMount(() => {
    fetchBusinesses();
  });
</script>

<div class="min-h-screen bg-gray-50 dark:bg-black pt-20 pb-8">
  <div class="container mx-auto px-4">
    <div class="flex justify-between items-center mb-6">
      <h2 class="text-2xl font-bold text-gray-900 dark:text-white">Businesses</h2>
      <div class="flex items-center gap-4">
        <button
          on:click={toggleBusinessView}
          class="px-4 py-2 bg-purple-500 text-white rounded hover:bg-purple-600 transition-colors"
        >
          {myBusinessesOnly ? 'All Businesses' : 'My Businesses'}
        </button>
        <a
          href="/business/create"
          class="px-4 py-2 bg-purple-500 text-white rounded hover:bg-purple-600"
        >
          Register Business
        </a>
      </div>
    </div>

    <!-- Search and Filter Section -->
    <div class="mb-6">
      <div class="flex flex-col md:flex-row gap-4">
        <div class="flex-1">
          <input
            type="text"
            bind:value={searchQuery}
            placeholder="Search businesses..."
            class="w-full px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-purple-500"
          />
        </div>
        <div class="flex items-center gap-2 bg-gray-200 dark:bg-gray-800 rounded-lg p-1">
          <button
            class="px-3 py-1 rounded-md {viewMode === 'grid' ? 'bg-white dark:bg-purple-500 text-purple-500 dark:text-white shadow' : 'text-gray-600 dark:text-gray-300'}"
            on:click={() => viewMode = 'grid'}
            title="Grid View"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
              <path d="M5 3a2 2 0 00-2 2v2a2 2 0 002 2h2a2 2 0 002-2V5a2 2 0 00-2-2H5zM5 11a2 2 0 00-2 2v2a2 2 0 002 2h2a2 2 0 002-2v-2a2 2 0 00-2-2H5zM11 5a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V5zM11 13a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" />
            </svg>
          </button>
          <button
            class="px-3 py-1 rounded-md {viewMode === 'list' ? 'bg-white dark:bg-purple-500 text-purple-500 dark:text-white shadow' : 'text-gray-600 dark:text-gray-300'}"
            on:click={() => viewMode = 'list'}
            title="List View"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd" d="M3 5a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zM3 10a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zM3 15a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1z" clip-rule="evenodd" />
            </svg>
          </button>
        </div>
      </div>
    </div>

    <!-- Tags Section -->
    <div class="mb-6">
      <div class="mb-4">
        <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-3">Filter by Tags</h3>
        <div class="mb-3">
          <input
            type="text"
            bind:value={tagSearchQuery}
            placeholder="Search tags..."
            class="w-full max-w-md px-3 py-2 text-sm rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-purple-500"
          />
        </div>
        {#if selectedTags.length > 0}
          <div class="mb-3">
            <div class="flex flex-wrap gap-2">
              <span class="text-sm text-gray-600 dark:text-gray-400 mr-2">Selected:</span>
              {#each selectedTags as tag}
                <span class="px-3 py-1 bg-purple-500 text-white text-sm rounded-full flex items-center gap-2">
                  {tag}
                  <button
                    on:click={() => {
                      selectedTags = selectedTags.filter(t => t !== tag);
                    }}
                    class="text-white hover:text-purple-200 ml-1"
                  >
                    ×
                  </button>
                </span>
              {/each}
              <button
                on:click={() => selectedTags = []}
                class="px-3 py-1 bg-gray-500 text-white text-sm rounded-full hover:bg-gray-600"
              >
                Clear all
              </button>
            </div>
          </div>
        {/if}
        <div class="flex flex-wrap gap-2">
          {#each displayTags as tag}
            <button
              class="px-3 py-1 rounded-full text-sm transition-colors {
                selectedTags.includes(tag) 
                  ? 'bg-purple-500 text-white' 
                  : 'bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-purple-100 dark:hover:bg-purple-900 hover:text-purple-700 dark:hover:text-purple-300'
              }"
              on:click={() => {
                if (selectedTags.includes(tag)) {
                  selectedTags = selectedTags.filter(t => t !== tag);
                } else {
                  selectedTags = [...selectedTags, tag];
                }
              }}
            >
              {tag}
              <span class="ml-1 text-xs opacity-75">
                ({businesses.filter(business => 
                  (business.tags ?? '').split(',').map(t => t.trim().toLowerCase()).includes(tag.toLowerCase())
                ).length})
              </span>
            </button>
          {/each}
          {#if filteredTags.length > maxDisplayTags}
            <button
              on:click={() => showAllTags = !showAllTags}
              class="px-3 py-1 rounded-full text-sm bg-blue-100 dark:bg-blue-900 text-blue-700 dark:text-blue-300 hover:bg-blue-200 dark:hover:bg-blue-800 transition-colors"
            >
              {showAllTags ? `Show less (${filteredTags.length - maxDisplayTags} hidden)` : `Show ${filteredTags.length - maxDisplayTags} more tags`}
            </button>
          {/if}
        </div>
        {#if filteredTags.length === 0 && tagSearchQuery}
          <p class="text-gray-500 dark:text-gray-400 text-sm mt-2">
            No tags found matching "{tagSearchQuery}"
          </p>
        {/if}
      </div>
    </div>

    <!-- Business List -->
    {#if loading}
      <div class="flex justify-center items-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-purple-500"></div>
      </div>
    {:else if error}
      <div class="text-center p-4">
        <p class="text-red-500 dark:text-red-400">{error}</p>
        <button on:click={fetchBusinesses} class="mt-2 px-4 py-2 bg-purple-500 text-white rounded hover:bg-purple-600">
          Try Again
        </button>
      </div>
    {:else if filteredBusinesses.length === 0}
      <div class="text-center p-4">
        <p class="text-gray-500 dark:text-gray-400">No businesses found.</p>
      </div>
    {:else}
      {#if viewMode === 'grid'}
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {#each filteredBusinesses as business}
            <div class="bg-white dark:bg-black rounded-xl shadow-md overflow-hidden border-2 border-gray-200 dark:border-gray-700 hover:border-purple-500 dark:hover:border-purple-400 transition-colors">
              {#if business.image}
                <img src={business.image} alt={business.name} class="w-full h-64 object-cover" />
              {:else}
                <div class="w-full h-64 bg-gray-200 dark:bg-purple-900/20 flex items-center justify-center">
                  <span class="text-gray-400 dark:text-purple-400">No image available</span>
                </div>
              {/if}
              <div class="p-6 flex flex-col items-center">
                <h3 class="text-xl font-semibold text-gray-900 dark:text-white mb-4 text-center">{business.title}</h3>
                <button
                  on:click={() => handleBusinessClick(business.businessId)}
                  class="px-4 py-2 bg-purple-500 text-white rounded hover:bg-purple-600 transition-colors"
                >
                  View Details
                </button>
              </div>
            </div>
          {/each}
        </div>
      {:else}
        <div class="space-y-2">
          {#each filteredBusinesses as business}
            <div
              class="bg-white dark:bg-black rounded-lg p-4 border-2 border-gray-200 dark:border-gray-700 hover:border-purple-500 dark:hover:border-purple-400 transition-colors cursor-pointer"
              on:click={() => handleBusinessClick(business.businessId)}
            >
              <div class="flex justify-between items-center">
                <div class="flex items-center gap-4">
                  {#if business.image}
                    <img
                      src={business.image}
                      alt={business.name}
                      class="w-12 h-12 object-cover rounded-full"
                    />
                  {:else}
                    <div class="w-12 h-12 bg-gray-200 dark:bg-purple-900/20 rounded-full flex items-center justify-center">
                      <span class="text-gray-400 dark:text-purple-400 text-sm">No img</span>
                    </div>
                  {/if}
                  <div>
                    <h3 class="text-lg font-semibold text-gray-900 dark:text-white">{business.name}</h3>
                    <p class="text-sm text-gray-600 dark:text-gray-300">{business.title}</p>
                  </div>
                </div>
              </div>
            </div>
          {/each}
        </div>
      {/if}
    {/if}

    <!-- Pagination Controls -->
    {#if !loading && !error && totalPages > 1}
      <div class="flex flex-col sm:flex-row justify-between items-center mt-8 gap-4">
        <div class="text-sm text-gray-600 dark:text-gray-400">
          Showing {Math.min(((currentPage - 1) * pageSize) + 1, totalItems)} to {Math.min(currentPage * pageSize, totalItems)} of {totalItems} businesses
        </div>
        <div class="flex items-center gap-2">
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
      </div>
    {/if}
  </div>
</div>
