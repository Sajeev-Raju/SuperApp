<script lang="ts">
  import { onMount } from "svelte";
  import { user } from "$stores/userStore";
  import { goto } from "$app/navigation";
  import apiClient from "$api/apiClient";
  import LoadingSpinner from "$components/ui/LoadingSpinner.svelte";
  import UserLocationBadge from "$components/ui/UserLocationBadge.svelte";

  let title = "";
  let content = "";
  let selectedTags: string[] = [];
  let availableTags: string[] = [];
  let isLoading = false;
  let error: string | null = null;

  onMount(async () => {
    if (!$user.userId || !$user.location) {
      goto("/");
      return;
    }

    try {
      const tagsResponse = await apiClient.qanda.getTags();
      availableTags = Array.isArray(tagsResponse) ? tagsResponse : [];
    } catch (err) {
      console.error("Error loading tags:", err);
      error = "Failed to load tags. Please try again.";
    }
  });

  async function handleSubmit() {
    if (!title.trim() || !content.trim()) {
      error = "Please fill in all required fields.";
      return;
    }

    isLoading = true;
    error = null;

    try {
      await apiClient.qanda.createQuestion({
        qTitle: title.trim(),
        questionDescription: content.trim(),
        tags: selectedTags
      });
      
      goto("/questions");
    } catch (err) {
      console.error("Error creating question:", err);
      error = "Failed to create question. Please try again.";
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
</script>

<div class="min-h-screen bg-gray-50 dark:bg-gray-900 pt-16 pb-20">
  <div class="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8">
    <div class="py-6">
      <div class="mb-8">
        <h1 class="text-3xl font-bold text-gray-900 dark:text-white">Ask a Question</h1>
        <div class="mt-2">
          <UserLocationBadge />
        </div>
      </div>

      <div class="bg-white dark:bg-gray-800 rounded-xl shadow-md overflow-hidden">
        <form on:submit|preventDefault={handleSubmit} class="p-6 space-y-6">
          {#if error}
            <div class="bg-danger-50 dark:bg-danger-900/20 text-danger-800 dark:text-danger-200 p-4 rounded-lg">
              {error}
            </div>
          {/if}

          <div>
            <label for="title" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              Title
            </label>
            <input
              type="text"
              id="title"
              bind:value={title}
              placeholder="What's your question?"
              class="input w-full"
              required
            />
          </div>

          <div>
            <label for="content" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              Details
            </label>
            <textarea
              id="content"
              bind:value={content}
              rows="6"
              placeholder="Provide more details about your question..."
              class="input w-full"
              required
            ></textarea>
          </div>

          {#if availableTags.length > 0}
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Tags
              </label>
              <div class="flex flex-wrap gap-2">
                {#each availableTags as tag}
                  <button
                    type="button"
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
          {/if}

          <div class="flex justify-end gap-4">
            <button
              type="button"
              on:click={() => goto("/questions")}
              class="btn btn-outline"
            >
              Cancel
            </button>
            <button
              type="submit"
              class="btn btn-primary"
              disabled={isLoading}
            >
              {#if isLoading}
                <LoadingSpinner size="sm" class="mr-2" />
                Posting...
              {:else}
                Post Question
              {/if}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</div> 