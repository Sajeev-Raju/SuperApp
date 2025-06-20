<script lang="ts">
  import { onMount } from "svelte";
  import { pollsApi } from "$api/apiClient";
  import { goto } from "$app/navigation";
  import toast from "svelte-french-toast";

  let collectionTitle = '';
  let questions = [
    { questionText: '', pollOptions: [''], selectionLimit: 1 }
  ];
  let loading = false;
  let error: string | null = null;

  function addQuestion() {
    questions = [...questions, { questionText: '', pollOptions: [''], selectionLimit: 1 }];
  }

  function removeQuestion(index: number) {
    questions = questions.filter((_, i) => i !== index);
  }

  function addOption(qIdx: number) {
    questions[qIdx].pollOptions = [...questions[qIdx].pollOptions, ''];
    questions = [...questions];
  }

  function removeOption(qIdx: number, oIdx: number) {
    questions[qIdx].pollOptions = questions[qIdx].pollOptions.filter((_, i) => i !== oIdx);
    questions = [...questions];
  }

  async function handleSubmit() {
    error = null;
    loading = true;
    try {
      if (!collectionTitle.trim()) throw new Error('Collection title is required');
      for (const q of questions) {
        if (!q.questionText.trim()) throw new Error('Each question must have text');
        if (q.pollOptions.length < 2) throw new Error('Each question must have at least 2 options');
        if (q.pollOptions.some(opt => !opt.trim())) throw new Error('All options must be filled');
        if (q.selectionLimit < 1 || q.selectionLimit > q.pollOptions.length) throw new Error('Selection limit must be between 1 and the number of options');
      }
      await pollsApi.createPollCollection({
        collectionTitle,
        questions: questions.map(q => ({
          questionText: q.questionText,
          pollOptions: q.pollOptions,
          selectionLimit: q.selectionLimit
        }))
      });
      toast.success('Poll collection created!');
      goto('/polls');
    } catch (err) {
      error = err instanceof Error ? err.message : 'Failed to create poll collection.';
    } finally {
      loading = false;
    }
  }
</script>

<div class="min-h-screen bg-gray-50 dark:bg-black pt-16 pb-20">
  <div class="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8">
    <div class="py-6">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white mb-6">Create Poll Collection</h1>
      <form on:submit|preventDefault={handleSubmit} class="space-y-8">
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Collection Title</label>
          <input type="text" bind:value={collectionTitle} class="w-full p-2 border rounded dark:bg-gray-700 dark:border-gray-600 dark:text-white" placeholder="Enter collection title" />
        </div>
        {#each questions as question, qIdx}
          <div class="p-4 border rounded-lg mb-4">
            <div class="flex justify-between items-center mb-4">
              <h2 class="text-lg font-semibold text-gray-900 dark:text-white">Question {qIdx + 1}</h2>
              {#if questions.length > 1}
                <button type="button" class="text-red-500 hover:text-red-700" on:click={() => removeQuestion(qIdx)}>Remove</button>
              {/if}
            </div>
            <div class="mb-4">
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Question Text</label>
              <input type="text" bind:value={question.questionText} class="w-full p-2 border rounded dark:bg-gray-700 dark:border-gray-600 dark:text-white" placeholder="Enter question text" />
            </div>
            <div class="mb-4">
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Options</label>
              {#each question.pollOptions as option, oIdx}
                <div class="flex gap-2 mb-2">
                  <input type="text" bind:value={question.pollOptions[oIdx]} class="flex-1 p-2 border rounded dark:bg-gray-700 dark:border-gray-600 dark:text-white" placeholder="Enter option" />
                  {#if question.pollOptions.length > 2}
                    <button type="button" class="text-red-500 hover:text-red-700" on:click={() => removeOption(qIdx, oIdx)}>Remove</button>
                  {/if}
                </div>
              {/each}
              <button type="button" class="text-primary-600 hover:text-primary-700 text-sm" on:click={() => addOption(qIdx)}>+ Add Option</button>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Selection Limit</label>
              <input type="number" bind:value={question.selectionLimit} min="1" max={question.pollOptions.length} class="w-full p-2 border rounded dark:bg-gray-700 dark:border-gray-600 dark:text-white" />
            </div>
          </div>
        {/each}
        <button type="button" class="text-primary-600 hover:text-primary-700" on:click={addQuestion}>+ Add Another Question</button>
        {#if error}
          <div class="bg-danger-50 dark:bg-danger-900/20 text-danger-800 dark:text-danger-200 p-4 rounded-lg mb-6">{error}</div>
        {/if}
        <button class="btn btn-primary w-full" type="submit" disabled={loading}>{loading ? 'Creating...' : 'Create Poll Collection'}</button>
      </form>
    </div>
  </div>
</div> 