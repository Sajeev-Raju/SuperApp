<script lang="ts">
  import { onMount } from "svelte";
  import { page } from "$app/stores";
  import { pollsApi } from "$api/apiClient";
  import LoadingSpinner from "$components/ui/LoadingSpinner.svelte";
  import toast from "svelte-french-toast";

  let collectionId: number;
  let collection: any = null;
  let loading = true;
  let error: string | null = null;
  let selectedOptions: Record<number, number[]> = {};
  let submitting = false;

  $: collectionId = +$page.params.collectionId;

  async function loadCollection() {
    try {
      loading = true;
      error = null;
      const res = await pollsApi.getPollCollectionDetails(collectionId);
      collection = res.collection;
      // Initialize selectedOptions for each question
      if (collection && collection.questions) {
        for (const q of collection.questions) {
          selectedOptions[q.questionId] = [];
        }
      }
    } catch (err) {
      error = "Failed to load poll collection.";
    } finally {
      loading = false;
    }
  }

  onMount(loadCollection);

  function handleOptionSelect(questionId: number, optionId: number, selectionMode: string, selectionLimit: number) {
    if (selectionMode === 'single') {
      selectedOptions[questionId] = [optionId];
    } else {
      const current = selectedOptions[questionId] || [];
      if (current.includes(optionId)) {
        selectedOptions[questionId] = current.filter(id => id !== optionId);
      } else if (current.length < selectionLimit) {
        selectedOptions[questionId] = [...current, optionId];
      }
    }
  }

  async function handleSubmit() {
    submitting = true;
    try {
      for (const q of collection.questions) {
        const optionIds = selectedOptions[q.questionId];
        if (optionIds && optionIds.length > 0) {
          await pollsApi.vote(q.questionId.toString(), { optionIds });
        }
      }
      toast.success("Votes submitted successfully!");
      await loadCollection();
    } catch (err) {
      toast.error("Failed to submit votes. Please try again.");
    } finally {
      submitting = false;
    }
  }
</script>

<div class="min-h-screen bg-gray-50 dark:bg-black pt-16 pb-20">
  <div class="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8">
    <div class="py-6">
      {#if loading}
        <LoadingSpinner />
      {:else if error}
        <div class="bg-danger-50 dark:bg-danger-900/20 text-danger-800 dark:text-danger-200 p-4 rounded-lg mb-6">{error}</div>
      {:else if collection}
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white mb-4">{collection.collectionTitle}</h1>
        <div class="mb-6 text-gray-600 dark:text-gray-300 text-sm">By {collection.userId} | Created: {new Date(collection.createdAt).toLocaleString()}</div>
        <form on:submit|preventDefault={handleSubmit}>
          {#each collection.questions as question}
            <div class="mb-8 p-4 border rounded-lg">
              <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-2">{question.questionText}</h2>
              <div class="space-y-2">
                {#each question.options as option}
                  <div class="flex items-center">
                    <input
                      type={question.selectionMode === 'single' ? 'radio' : 'checkbox'}
                      name={`question-${question.questionId}`}
                      value={option.optionId}
                      checked={(selectedOptions[question.questionId] || []).includes(option.optionId)}
                      on:change={() => handleOptionSelect(question.questionId, option.optionId, question.selectionMode, question.selectionLimit)}
                      class="mr-3"
                    />
                    <label class="flex-1 text-gray-700 dark:text-gray-300">{option.optionText}</label>
                  </div>
                {/each}
              </div>
            </div>
          {/each}
          <button class="btn btn-primary w-full" type="submit" disabled={submitting}>{submitting ? 'Submitting...' : 'Submit Votes'}</button>
        </form>
      {/if}
    </div>
  </div>
</div> 