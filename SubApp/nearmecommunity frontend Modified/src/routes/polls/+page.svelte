<script lang="ts">
  import { onMount } from "svelte";
  import { pollsApi } from "$api/apiClient";
  import LoadingSpinner from "$components/ui/LoadingSpinner.svelte";
  import UserLocationBadge from "$components/ui/UserLocationBadge.svelte";
  import { goto } from "$app/navigation";
  import toast from "svelte-french-toast";

  let collections = [];
  let page = 0;
  let size = 10;
  let totalElements = 0;
  let loading = true;
  let error: string | null = null;

  async function loadCollections() {
    try {
      loading = true;
      error = null;
      const res = await pollsApi.getPollCollections(page, size);
      collections = res.content;
      totalElements = res.totalElements;
    } catch (err) {
      error = "Failed to load poll collections.";
    } finally {
      loading = false;
    }
  }

  function handleCollectionClick(collectionId: number) {
    goto(`/polls/${collectionId}`);
  }

  onMount(loadCollections);

  function nextPage() {
    if ((page + 1) * size < totalElements) {
      page++;
      loadCollections();
    }
  }
  function prevPage() {
    if (page > 0) {
      page--;
      loadCollections();
    }
  }
</script>

<div class="min-h-screen bg-gray-50 dark:bg-black pt-16 pb-20">
  <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
    <div class="py-6">
      <div class="mb-8 flex items-center justify-between">
        <h1 class="text-3xl font-bold text-gray-900 dark:text-white">Poll Collections</h1>
        <div class="flex gap-4 items-center">
          <a href="/polls/create" class="btn btn-primary">Create Poll</a>
          <UserLocationBadge />
        </div>
      </div>
      {#if loading}
        <LoadingSpinner />
      {:else if error}
        <div class="bg-danger-50 dark:bg-danger-900/20 text-danger-800 dark:text-danger-200 p-4 rounded-lg mb-6">{error}</div>
      {:else if collections.length === 0}
        <div class="bg-white dark:bg-black rounded-xl shadow-md p-8 text-center border-2 border-purple-500 dark:border-purple-400">
          <div class="text-gray-500 dark:text-gray-400">No poll collections available in your area</div>
        </div>
      {:else}
        <div class="space-y-4">
          {#each collections as collection}
            <div class="bg-white dark:bg-black rounded-lg p-6 border-2 border-gray-200 dark:border-gray-700 hover:border-purple-500 dark:hover:border-purple-400 transition-colors cursor-pointer" on:click={() => handleCollectionClick(collection.collectionId)}>
              <div class="flex justify-between items-center mb-2">
                <h2 class="text-xl font-semibold text-gray-900 dark:text-white">{collection.collectionTitle}</h2>
                <span class="text-sm text-gray-500 dark:text-gray-400">By {collection.userId}</span>
              </div>
              <div class="text-gray-600 dark:text-gray-300 text-sm">Created: {new Date(collection.createdAt).toLocaleString()}</div>
              <div class="text-gray-500 dark:text-gray-400 text-xs mt-2">Questions: {collection.questions?.length ?? 0}</div>
            </div>
          {/each}
        </div>
        <div class="flex justify-between items-center mt-8">
          <button class="btn btn-secondary" on:click={prevPage} disabled={page === 0}>Previous</button>
          <span>Page {page + 1} of {Math.ceil(totalElements / size)}</span>
          <button class="btn btn-secondary" on:click={nextPage} disabled={(page + 1) * size >= totalElements}>Next</button>
        </div>
      {/if}
    </div>
  </div>
</div>