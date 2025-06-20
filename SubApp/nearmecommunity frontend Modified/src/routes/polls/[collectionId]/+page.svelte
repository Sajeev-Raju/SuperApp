<script lang="ts">
  import { onMount } from "svelte";
  import { page } from "$app/stores";
  import { pollsApi } from "$api/apiClient";
  import LoadingSpinner from "$components/ui/LoadingSpinner.svelte";
  import toast from "svelte-french-toast";
  import { writable } from 'svelte/store';
  import { user } from '$stores/userStore';

  let collectionId: number;
  let collection: any = null;
  let loading = true;
  let error: string | null = null;
  let selectedOptions: Record<number, number[]> = {};
  let selectedRadioStore = writable<Record<number, number>>({});
  let selectedRadio: Record<number, number> = {};
  let submitting = false;

  $: collectionId = +$page.params.collectionId;

  async function loadCollection() {
    try {
      loading = true;
      error = null;
      const res = await pollsApi.getPollCollectionDetails(collectionId);
      collection = res.collection;
      // Initialize selectedOptions and selectedRadio for each question
      if (collection && collection.questions) {
        let radioInit: Record<number, number> = {};
        for (const q of collection.questions) {
          selectedOptions[q.questionId] = [];
          radioInit[q.questionId] = undefined;
        }
        selectedRadioStore.set(radioInit);
      }
    } catch (err) {
      error = "Failed to load poll collection.";
    } finally {
      loading = false;
    }
  }

  onMount(loadCollection);

  function handleOptionSelectCheckbox(questionId: number, optionId: number, selectionLimit: number) {
    const current = selectedOptions[questionId] || [];
    if (current.includes(optionId)) {
      selectedOptions[questionId] = current.filter(id => id !== optionId);
    } else if (current.length < selectionLimit) {
      selectedOptions[questionId] = [...current, optionId];
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

  $: {
    // Keep selectedOptions in sync with selectedRadio for single choice
    if (collection && collection.questions) {
      selectedRadioStore.update(radio => {
        for (const q of collection.questions) {
          if (q.selectionMode === 'single') {
            if (radio[q.questionId] !== undefined) {
              selectedOptions[q.questionId] = [radio[q.questionId]];
            } else {
              selectedOptions[q.questionId] = [];
            }
          }
        }
        return radio;
      });
    }
  }

  async function handleDeleteCollection() {
    if (confirm('Are you sure you want to delete this entire poll collection?')) {
      try {
        await pollsApi.deletePollCollection(collection.collectionId);
        toast.success('Poll collection deleted');
        window.location.href = '/polls';
      } catch (err) {
        toast.error('Failed to delete poll collection.');
      }
    }
  }

  async function handleDeleteQuestion(questionId: number) {
    if (confirm('Are you sure you want to delete this question?')) {
      try {
        await pollsApi.deletePollQuestion(questionId);
        toast.success('Question deleted');
        await loadCollection();
      } catch (err) {
        toast.error('Failed to delete question.');
      }
    }
  }
</script>

<div class="min-h-screen bg-gray-50 dark:bg-black pt-16 pb-20">
  <div class="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8">
    <div class="py-6">
      <div class="flex justify-between items-center mb-6">
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">{collection?.collectionTitle}</h1>
        {#if collection?.collectionOwnerId && $user?.userId && collection.collectionOwnerId === $user.userId}
          <button class="btn text-red-600" on:click={handleDeleteCollection}>Delete Collection</button>
        {/if}
      </div>
      {#if loading}
        <LoadingSpinner />
      {:else if error}
        <div class="bg-danger-50 dark:bg-danger-900/20 text-danger-800 dark:text-danger-200 p-4 rounded-lg mb-6">{error}</div>
      {:else if collection}
        <div class="mb-6 text-gray-600 dark:text-gray-300 text-sm">By {collection.userId} | Created: {new Date(collection.createdAt).toLocaleString()}</div>
        <form on:submit|preventDefault={handleSubmit}>
          {#each collection.questions as question}
            <div class="mb-8 p-4 border rounded-lg">
              <div class="flex justify-between items-center mb-2">
                <h2 class="text-lg font-semibold text-gray-900 dark:text-white">{question.questionText}</h2>
                {#if collection?.collectionOwnerId && $user?.userId && collection.collectionOwnerId === $user.userId}
                  <button class="text-red-500 hover:text-red-700" on:click={() => handleDeleteQuestion(question.questionId)}>Delete</button>
                {/if}
              </div>
              <div class="space-y-2">
                {#if question.selectionMode === 'single'}
                  {#each question.options as option}
                    <div class="flex items-center">
                      <input
                        type="radio"
                        name={`question-${question.questionId}`}
                        value={option.optionId}
                        bind:group={$selectedRadioStore[question.questionId]}
                        class="mr-3"
                      />
                      <label class="flex-1 text-gray-700 dark:text-gray-300">{option.optionText}</label>
                    </div>
                  {/each}
                {:else}
                  {#each question.options as option}
                    <div class="flex items-center">
                      <input
                        type="checkbox"
                        name={`question-${question.questionId}`}
                        value={option.optionId}
                        checked={(selectedOptions[question.questionId] || []).includes(option.optionId)}
                        on:change={() => handleOptionSelectCheckbox(question.questionId, option.optionId, question.selectionLimit)}
                        class="mr-3"
                      />
                      <label class="flex-1 text-gray-700 dark:text-gray-300">{option.optionText}</label>
                    </div>
                  {/each}
                {/if}
              </div>
            </div>
          {/each}
          <button class="btn btn-primary w-full" type="submit" disabled={submitting}>{submitting ? 'Submitting...' : 'Submit Votes'}</button>
        </form>
      {/if}
    </div>
  </div>
</div> 