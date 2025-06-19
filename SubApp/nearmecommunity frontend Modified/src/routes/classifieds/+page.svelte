<script lang="ts">
  import { onMount } from "svelte";
  import { user } from "$stores/userStore";
  import { goto } from "$app/navigation";
  import { classifiedsApi } from "$api/apiClient";
  import LoadingSpinner from "$components/ui/LoadingSpinner.svelte";
  import UserLocationBadge from "$components/ui/UserLocationBadge.svelte";
  import type { Classified } from "$types/classified";

  let classifieds: Classified[] = [];
  let isLoading = true;
  let error: string | null = null;
  let searchQuery = "";
  let selectedCategories: string[] = [];
  let availableCategories: string[] = [];
  let sortBy: "newest" | "oldest" | "price-low" | "price-high" = "newest";
  let priceRange = { min: "", max: "" };

  onMount(async () => {
    if (!$user.userId || !$user.location) {
      goto("/");
      return;
    }

    try {
      const categories = await classifiedsApi.getCategories();
      availableCategories = categories;
      await loadClassifieds();
    } catch (err) {
      console.error("Error loading data:", err);
      error = "Failed to load data. Please try again.";
      availableCategories = [];
    } finally {
      isLoading = false;
    }
  });

  async function loadClassifieds() {
    isLoading = true;
    error = null;

    try {
      const response = await classifiedsApi.getClassifieds();
      console.log("Classifieds response:", response); // Debug log
      
      if (response && response.data) {
        classifieds = response.data;
      } else {
        classifieds = [];
      }
      
      console.log("Processed classifieds:", classifieds); // Debug log
    } catch (err) {
      console.error("Error loading classifieds:", err);
      error = "Failed to load classifieds. Please try again.";
      classifieds = [];
    } finally {
      isLoading = false;
    }
  }

  function toggleCategory(category: string) {
    if (selectedCategories.includes(category)) {
      selectedCategories = selectedCategories.filter(c => c !== category);
    } else {
      selectedCategories = [...selectedCategories, category];
    }
    loadClassifieds();
  }

  function handleSearch() {
    loadClassifieds();
  }

  function handleSortChange() {
    loadClassifieds();
  }

  function handlePriceRangeChange() {
    loadClassifieds();
  }

  function clearFilters() {
    searchQuery = "";
    selectedCategories = [];
    sortBy = "newest";
    priceRange = { min: "", max: "" };
    loadClassifieds();
  }
</script>

<div class="min-h-screen bg-gray-50 dark:bg-black pt-16 pb-20">
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
    <div class="py-6">
      <div class="mb-8">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-4">
            <h1 class="text-3xl font-bold text-gray-900 dark:text-white">
              Classifieds
            </h1>
            <UserLocationBadge />
          </div>
          <button
            on:click={() => goto("/classifieds/create")}
            class="btn btn-primary"
          >
            Post a Classified
          </button>
        </div>
      </div>

      {#if error}
        <div class="bg-danger-50 dark:bg-danger-900/20 text-danger-800 dark:text-danger-200 p-4 rounded-lg mb-6">
          {error}
          <button class="underline ml-2" on:click={loadClassifieds}>Try again</button>
        </div>
      {/if}

      <div class="grid grid-cols-1 lg:grid-cols-4 gap-6">
        <!-- Filters Sidebar -->
        <div class="lg:col-span-1">
          <div class="bg-white dark:bg-black rounded-xl shadow-md p-6 border-2 border-purple-500 dark:border-purple-400">
            <div class="flex items-center justify-between mb-6">
              <h2 class="text-lg font-semibold text-gray-900 dark:text-white">
                Filters
              </h2>
              <button
                on:click={clearFilters}
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
                  on:keydown={(e) => e.key === "Enter" && handleSearch()}
                  class="input w-full pl-10"
                  placeholder="Search classifieds..."
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
                {#each availableCategories as category}
                  <button
                    type="button"
                    on:click={() => toggleCategory(category)}
                    class={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium transition-colors ${
                      selectedCategories.includes(category)
                        ? "bg-primary-100 text-primary-800 dark:bg-primary-900/40 dark:text-primary-300"
                        : "bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600"
                    }`}
                  >
                    {category}
                  </button>
                {/each}
              </div>
            </div>

            <!-- Price Range -->
            <div class="mb-6">
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Price Range
              </label>
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <input
                    type="number"
                    bind:value={priceRange.min}
                    on:change={handlePriceRangeChange}
                    class="input w-full"
                    placeholder="Min"
                    min="0"
                    step="0.01"
                  />
                </div>
                <div>
                  <input
                    type="number"
                    bind:value={priceRange.max}
                    on:change={handlePriceRangeChange}
                    class="input w-full"
                    placeholder="Max"
                    min="0"
                    step="0.01"
                  />
                </div>
              </div>
            </div>

            <!-- Sort -->
            <div>
              <label for="sort" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Sort by
              </label>
              <select
                id="sort"
                bind:value={sortBy}
                on:change={handleSortChange}
                class="input w-full"
              >
                <option value="newest">Newest first</option>
                <option value="oldest">Oldest first</option>
                <option value="price-low">Price: Low to High</option>
                <option value="price-high">Price: High to Low</option>
              </select>
            </div>
          </div>
        </div>

        <!-- Classifieds Grid -->
        <div class="lg:col-span-3">
          {#if isLoading}
            <div class="flex justify-center py-20">
              <LoadingSpinner size="lg" />
            </div>
          {:else if !classifieds || !Array.isArray(classifieds) || classifieds.length === 0}
            <div class="bg-white dark:bg-black rounded-xl shadow-md p-8 text-center">
              <div class="text-gray-500 dark:text-gray-400">No classifieds found</div>
            </div>
          {:else}
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {#each classifieds as classified}
                <div class="bg-white dark:bg-black rounded-xl shadow-md overflow-hidden border border-purple-100 dark:border-purple-900/50 transition-all duration-200 hover:border-2 hover:border-purple-500 dark:hover:border-purple-400 hover:shadow-lg hover:shadow-purple-500/20 dark:hover:shadow-purple-400/20">
                  <a href={`/classifieds/${classified.id}`} class="block">
                    <div class="h-48 bg-gray-200 dark:bg-black relative overflow-hidden">
                      <img
                        src={classified.imageUrlString || 'https://images.pexels.com/photos/2774556/pexels-photo-2774556.jpeg?auto=compress&cs=tinysrgb&w=800'}
                        alt={classified.title}
                        class="w-full h-full object-cover"
                      />
                      <div class="absolute top-0 right-0 m-2">
                        <div class="bg-white dark:bg-gray-900 text-purple-800 dark:text-purple-300 text-xs font-medium px-2 py-1 rounded shadow border border-purple-100 dark:border-purple-900/50">
                          ${classified.price}
                        </div>
                      </div>
                    </div>
                    <div class="p-5">
                      <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-2 line-clamp-1">
                        {classified.title}
                      </h3>
                      <p class="text-gray-600 dark:text-gray-300 text-sm mb-3 line-clamp-2">
                        {classified.description || "No description provided."}
                      </p>
                      <div class="flex justify-between items-center text-sm">
                        <div class="text-purple-600 dark:text-purple-400">
                          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 inline mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                          </svg>
                          {classified.location || "Location TBD"}
                        </div>
                        <div class="text-gray-500 dark:text-gray-400">
                          {classified.category}
                        </div>
                      </div>
                      {#if classified.tags && classified.tags.length > 0}
                        <div class="mt-3 flex flex-wrap gap-1">
                          {#each classified.tags.slice(0, 3) as tag}
                            <span class="badge badge-primary text-xs">
                              {tag}
                            </span>
                          {/each}
                          {#if classified.tags.length > 3}
                            <span class="badge bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-300 text-xs">
                              +{classified.tags.length - 3} more
                            </span>
                          {/if}
                        </div>
                      {/if}
                    </div>
                  </a>
                </div>
              {/each}
            </div>
          {/if}
        </div>
      </div>
    </div>
  </div>
</div> 