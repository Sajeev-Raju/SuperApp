<!-- Business Details Page -->
<script lang="ts">
  import { onMount } from 'svelte';
  import { page } from '$app/stores';
  import { user } from '$stores/userStore';
  import { goto } from '$app/navigation';
  import { toast } from 'svelte-french-toast';
  import { browser } from '$app/environment';

  interface Business {
    businessId: number;
    userId: string;
    name: string;
    title: string;
    tags: string;
    description: string;
    address: string;
    mobileNumber: string;
    timings: string;
    googlemapsURL: string;
    createdAt: string;
    image: string;
    notification: {
      message: string;
      createdAt: string;
    } | null;
    questions: any[];
    active: boolean;
  }

  const API_URL = 'http://localhost:8080/api/business';
  let business: Business | null = null;
  let loading = true;
  let error: string | null = null;

  // Fetch business details
  async function fetchBusinessDetails() {
    try {
      loading = true;
      error = null;
      const businessId = $page.params.id;
      const userData = user.get();
      
      const response = await fetch(`${API_URL}/${businessId}`, {
        headers: {
          'X-User-ID': userData.userId || ''
        }
      });
      
      const result = await response.json();

      if (!response.ok) {
        throw new Error(result.message || 'Failed to fetch business details');
      }

      if (result.success && result.data && result.data.length > 0) {
        business = result.data[0];
      } else {
        throw new Error('Business not found');
      }
    } catch (e) {
      error = e instanceof Error ? e.message : 'An error occurred';
      console.error('Error fetching business details:', e);
      toast.error(error);
    } finally {
      loading = false;
    }
  }

  // Call fetchBusinessDetails when the component mounts
  onMount(() => {
    fetchBusinessDetails();
  });

  // Also fetch when user changes
  $: if ($user?.userId) {
    fetchBusinessDetails();
  }
</script>

<div class="min-h-screen bg-gray-50 dark:bg-black pt-20 pb-8">
  <div class="container mx-auto px-4">
    {#if loading}
      <div class="flex justify-center items-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-purple-500"></div>
      </div>
    {:else if error}
      <div class="text-center p-4">
        <p class="text-red-500 dark:text-red-400">{error}</p>
        <button on:click={fetchBusinessDetails} class="mt-2 px-4 py-2 bg-purple-500 text-white rounded hover:bg-purple-600">
          Try Again
        </button>
      </div>
    {:else if !business}
      <div class="text-center p-4">
        <p class="text-gray-500 dark:text-gray-400">Business not found.</p>
        <a href="/business" class="mt-4 inline-block px-4 py-2 bg-purple-500 text-white rounded hover:bg-purple-600">
          Back to Businesses
        </a>
      </div>
    {:else}
      <div class="max-w-4xl mx-auto">
        <div class="bg-white dark:bg-black rounded-xl shadow-md overflow-hidden border-2 border-purple-500 dark:border-purple-400">
          {#if business.image}
            <img 
              src={business.image} 
              alt={business.name} 
              class="w-full h-64 object-cover"
              on:error={(e) => {
                // Hide the image and show fallback when image fails to load
                e.target.style.display = 'none';
                e.target.nextElementSibling.style.display = 'flex';
              }}
            />
            <div class="w-full h-64 bg-gray-200 dark:bg-purple-900/20 flex items-center justify-center" style="display: none;">
              <span class="text-gray-400 dark:text-purple-400">No image available</span>
            </div>
          {:else}
            <div class="w-full h-64 bg-gray-200 dark:bg-purple-900/20 flex items-center justify-center">
              <span class="text-gray-400 dark:text-purple-400">No image available</span>
            </div>
          {/if}
          <div class="p-6">
            <div class="flex justify-between items-start mb-6">
              <div>
                <h1 class="text-3xl font-bold text-gray-900 dark:text-white mb-2">{business.name}</h1>
                <p class="text-xl text-gray-600 dark:text-gray-300">{business.title}</p>
              </div>
              {#if business.userId === $user.userId}
                <div class="flex gap-2">
                  <a href={`/business/${business.businessId}/edit`} class="px-4 py-2 bg-purple-500 text-white rounded hover:bg-purple-600">
                    Edit
                  </a>
                  <button
                    on:click={() => {
                      if (confirm('Are you sure you want to deactivate this business?')) {
                        // Add deactivate functionality
                      }
                    }}
                    class="px-4 py-2 bg-red-500 text-white rounded hover:bg-red-600"
                  >
                    Deactivate
                  </button>
                </div>
              {/if}
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
              <div>
                <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-2">Details</h2>
                <div class="space-y-2">
                  {#if business.address}
                    <p class="text-gray-600 dark:text-gray-300">
                      <span class="font-medium">Address:</span> {business.address}
                    </p>
                  {/if}
                  {#if business.mobileNumber}
                    <p class="text-gray-600 dark:text-gray-300">
                      <span class="font-medium">Phone:</span> {business.mobileNumber}
                    </p>
                  {/if}
                  {#if business.timings}
                    <p class="text-gray-600 dark:text-gray-300">
                      <span class="font-medium">Timings:</span> {business.timings}
                    </p>
                  {/if}
                </div>
              </div>

              <div>
                <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-2">Tags</h2>
                <div class="flex flex-wrap gap-2">
                  {#each (business.tags ? business.tags.split(',').map(tag => tag.trim()).filter(tag => tag.length > 0) : []) as tag}
                    <span class="px-3 py-1 bg-purple-100 dark:bg-purple-900/20 text-purple-800 dark:text-purple-200 rounded-full">
                      {tag}
                    </span>
                  {/each}
                </div>
              </div>
            </div>

            {#if business.description}
              <div class="mb-6">
                <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-2">Description</h2>
                <p class="text-gray-600 dark:text-gray-300">{business.description}</p>
              </div>
            {/if}

            {#if business.googlemapsURL}
              <div class="mb-6">
                <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-2">Location</h2>
                <a
                  href={business.googlemapsURL}
                  target="_blank"
                  rel="noopener noreferrer"
                  class="text-purple-500 hover:text-purple-600 dark:text-purple-400 dark:hover:text-purple-300"
                >
                  View on Google Maps
                </a>
              </div>
            {/if}

            <div class="flex justify-between items-center">
              <a href="/business" class="text-purple-500 hover:text-purple-600 dark:text-purple-400 dark:hover:text-purple-300">
                ← Back to Businesses
              </a>
              <p class="text-sm text-gray-500 dark:text-gray-400">
                Created on {new Date(business.createdAt).toLocaleDateString()}
              </p>
            </div>
          </div>
        </div>
      </div>
    {/if}
  </div>
</div> 