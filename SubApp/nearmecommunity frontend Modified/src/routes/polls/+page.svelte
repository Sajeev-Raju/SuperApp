<script lang="ts">
  import { onMount } from "svelte";
  import { user } from "$stores/userStore";
  import { goto } from "$app/navigation";
  import { pollsApi } from "$api/apiClient";
  import LoadingSpinner from "$components/ui/LoadingSpinner.svelte";
  import UserLocationBadge from "$components/ui/UserLocationBadge.svelte";
  import toast from "svelte-french-toast";
  import type { PollQuestion, PollOption, VotedPoll } from '$api/apiClient';
  import { writable } from 'svelte/store';

  let polls: PollQuestion[] = [];
  let votedPolls: VotedPoll[] = [];
  let error: string | null = null;
  let showCreateModal = false;
  let questions: { questionText: string; options: string[]; selectionLimit: number; selectionMode: string }[] = [
    { questionText: '', options: [''], selectionLimit: 1, selectionMode: 'single' }
  ];
  let loading = true;
  let initialLoading = true;

  // Create a store for selected options
  const selectedOptionsStore = writable<Record<number, number[]>>({});
  let selectedOptions: Record<number, number[]> = {};

  // Subscribe to the store
  selectedOptionsStore.subscribe(value => {
    selectedOptions = value;
  });

  // Reactive declarations for vote button state
  $: voteButtonStates = Object.fromEntries(
    polls.map(poll => [
      poll.questionId,
      {
        canVote: canVote(poll.questionId),
        selectedCount: getSelectedCount(poll.questionId)
      }
    ])
  );

  onMount(async () => {
    if (!$user.userId || !$user.location) {
      goto("/");
      return;
    }
    await loadData();
    initialLoading = false;
  });

  async function loadData() {
    try {
      error = null;
      loading = true;
      // Load polls and voted polls in parallel
      const [pollsData, votedPollsData] = await Promise.all([
        pollsApi.getPolls(),
        pollsApi.getVotedPolls()
      ]);
      
      // Update the polls array with the fetched data
      if (pollsData && Array.isArray(pollsData)) {
        polls = pollsData;
      } else {
        polls = [];
      }
      
      if (votedPollsData && Array.isArray(votedPollsData)) {
        votedPolls = votedPollsData;
      } else {
        votedPolls = [];
      }
      
      console.log("Polls loaded:", polls);
      console.log("Voted polls:", votedPolls);
    } catch (err) {
      console.error("Error loading polls:", err);
      error = "Failed to load polls. Please try again.";
    } finally {
      loading = false;
    }
  }

  function addQuestion() {
    questions = [...questions, { questionText: '', options: [''], selectionLimit: 1, selectionMode: 'single' }];
  }

  function removeQuestion(index: number) {
    questions = questions.filter((_, i) => i !== index);
  }

  function addOption(questionIndex: number) {
    questions[questionIndex].options = [...questions[questionIndex].options, ''];
    questions = [...questions];
  }

  function removeOption(questionIndex: number, optionIndex: number) {
    questions[questionIndex].options = questions[questionIndex].options.filter((_, i) => i !== optionIndex);
    questions = [...questions];
  }

  async function handleSubmit() {
    try {
      loading = true;
      error = null;

      // Validate all questions and options
      for (const question of questions) {
        if (!question.questionText.trim()) {
          throw new Error('Question text is required');
        }
        if (question.options.length < 2) {
          throw new Error('At least 2 options are required');
        }
        if (question.options.some(opt => !opt.trim())) {
          throw new Error('All options must be filled');
        }
      }

      // Transform the data to match backend expectations
      const transformedQuestions = questions.map(q => ({
        questionText: q.questionText,
        pollOptions: q.options,
        selectionLimit: q.selectionLimit,
        selectionMode: q.selectionMode
      }));

      // Create all questions
      await pollsApi.createMultiplePolls(transformedQuestions);
      
      // Reset form
      questions = [{ questionText: '', options: [''], selectionLimit: 1, selectionMode: 'single' }];
      showCreateModal = false;
      
      // Refresh polls list
      await loadData();
    } catch (err) {
      console.error("Error creating polls:", err);
      error = err instanceof Error ? err.message : "Failed to create polls. Please try again.";
    } finally {
      loading = false;
    }
  }

  function updateSelectedOptions(pollId: number, newSelections: number[]) {
    selectedOptionsStore.update(store => ({
      ...store,
      [pollId]: newSelections
    }));
  }

  function handleOptionSelect(poll: PollQuestion, optionId: number) {
    if (!selectedOptions[poll.questionId]) {
      selectedOptions[poll.questionId] = [];
    }

    if (poll.selectionMode === 'single') {
      selectedOptions[poll.questionId] = [optionId];
    } else {
      const currentSelections = selectedOptions[poll.questionId];
      const index = currentSelections.indexOf(optionId);
      
      if (index === -1) {
        if (currentSelections.length < poll.selectionLimit) {
          selectedOptions[poll.questionId] = [...currentSelections, optionId];
        }
      } else {
        selectedOptions[poll.questionId] = currentSelections.filter(id => id !== optionId);
      }
    }
  }

  function canVote(pollId: number): boolean {
    const poll = polls.find(p => p.questionId === pollId);
    if (!poll) return false;
    
    const selections = selectedOptions[pollId] || [];
    
    if (poll.selectionMode === 'single') {
      return selections.length === 1;
    } else {
      return selections.length > 0 && selections.length <= poll.selectionLimit;
    }
  }

  function getSelectedCount(pollId: number): number {
    return (selectedOptions[pollId] || []).length;
  }

  async function handleVote(questionId: number) {
    const optionIds = selectedOptions[questionId] || [];
    if (optionIds.length === 0) {
      toast.error("Please select at least one option");
      return;
    }
    
    try {
      error = null;
      await pollsApi.vote(questionId.toString(), { optionIds });
      toast.success("Vote recorded successfully");
      delete selectedOptions[questionId];
      await loadData(); // Refresh the polls data
    } catch (err) {
      console.error("Error voting:", err);
      error = "Failed to record vote. Please try again.";
    }
  }

  async function handleDeletePoll(questionId: number) {
    if (!confirm("Are you sure you want to delete this poll?")) return;
    
    try {
      error = null;
      await pollsApi.deletePoll(questionId.toString());
      toast.success("Poll deleted successfully");
      await loadData(); // Refresh the polls data
    } catch (err) {
      console.error("Error deleting poll:", err);
      error = "Failed to delete poll. Please try again.";
    }
  }

  function hasVoted(questionId: number): boolean {
    return votedPolls.some(poll => poll.questionId === questionId);
  }

  function getVotedOptions(questionId: number): string[] {
    const votedPoll = votedPolls.find(poll => poll.questionId === questionId);
    return votedPoll ? votedPoll.votedOptions : [];
  }
</script>

<div class="min-h-screen bg-gray-50 dark:bg-black pt-16 pb-20">
  <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
    <div class="py-6">
      <div class="mb-8">
        <div class="flex items-center justify-between">
          <h1 class="text-3xl font-bold text-gray-900 dark:text-white">Polls</h1>
          <div class="flex items-center gap-4">
            <button
              on:click={() => showCreateModal = true}
              class="btn btn-primary"
            >
              Create Poll
            </button>
            <UserLocationBadge />
          </div>
        </div>
      </div>

      {#if initialLoading}
        <div class="flex justify-center items-center py-12">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-purple-500"></div>
        </div>
      {:else if error}
        <div class="bg-danger-50 dark:bg-danger-900/20 text-danger-800 dark:text-danger-200 p-4 rounded-lg mb-6">
          {error}
          <button class="underline ml-2" on:click={loadData}>Try again</button>
        </div>
      {:else if polls.length === 0}
        <div class="bg-white dark:bg-black rounded-xl shadow-md p-8 text-center border-2 border-purple-500 dark:border-purple-400">
          <div class="text-gray-500 dark:text-gray-400">No polls available in your area</div>
          <button
            on:click={() => showCreateModal = true}
            class="btn btn-primary mt-4"
          >
            Create the first poll
          </button>
        </div>
      {:else}
        <div class="space-y-4">
          {#each polls as poll}
            <div class="bg-white dark:bg-black rounded-lg p-6 border-2 border-gray-200 dark:border-gray-700 hover:border-purple-500 dark:hover:border-purple-400 transition-colors">
              <div class="flex justify-between items-start mb-4">
                <h3 class="text-xl font-semibold text-gray-900 dark:text-white">{poll.questionText}</h3>
                {#if poll.userId === $user.userId}
                  <button
                    on:click={() => handleDeletePoll(poll.questionId)}
                    class="text-red-500 hover:text-red-600 dark:text-red-400 dark:hover:text-red-300"
                  >
                    Delete
                  </button>
                {/if}
              </div>
              
              <div class="space-y-3">
                {#each poll.options as option}
                  <div class="flex items-center">
                    <input
                      type={poll.selectionMode === 'single' ? 'radio' : 'checkbox'}
                      name={`poll-${poll.questionId}`}
                      value={option.optionId}
                      checked={(selectedOptions[poll.questionId] || []).includes(option.optionId)}
                      disabled={hasVoted(poll.questionId)}
                      on:change={() => handleOptionSelect(poll, option.optionId)}
                      class="mr-3"
                    />
                    <label class="flex-1 text-gray-700 dark:text-gray-300">
                      {option.optionText}
                      {#if hasVoted(poll.questionId)}
                        <span class="ml-2 text-sm text-gray-500">
                          ({option.voteCount} votes)
                        </span>
                      {/if}
                    </label>
                  </div>
                {/each}
              </div>

              {#if !hasVoted(poll.questionId) && poll.userId !== $user.userId}
                <div class="mt-4 flex items-center justify-between">
                  <div class="text-sm text-gray-500">
                    {#if poll.selectionMode === 'multiple'}
                      Selected {(selectedOptions[poll.questionId] || []).length} of {poll.selectionLimit} options
                    {:else}
                      {#if (selectedOptions[poll.questionId] || []).length === 1}
                        Option selected
                      {:else}
                        Select an option
                      {/if}
                    {/if}
                  </div>
                  <button
                    class="px-4 py-2 rounded-md font-medium bg-blue-600 hover:bg-blue-700 text-white"
                    on:click={() => handleVote(poll.questionId)}
                  >
                    Vote
                  </button>
                </div>
              {/if}
            </div>
          {/each}
        </div>
      {/if}
    </div>
  </div>
</div>

<!-- Create Poll Modal -->
{#if showCreateModal}
  <div class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
    <div class="bg-white dark:bg-gray-800 rounded-lg p-6 w-full max-w-2xl max-h-[90vh] overflow-y-auto">
      <h2 class="text-2xl font-bold mb-4 text-gray-900 dark:text-white">Create Poll</h2>
      
      {#each questions as question, questionIndex}
        <div class="mb-6 p-4 border rounded-lg">
          <div class="flex justify-between items-center mb-4">
            <h3 class="text-lg font-semibold text-gray-900 dark:text-white">Question {questionIndex + 1}</h3>
            {#if questions.length > 1}
              <button 
                class="text-red-500 hover:text-red-700"
                on:click={() => removeQuestion(questionIndex)}
              >
                Remove Question
              </button>
            {/if}
          </div>
          
          <div class="mb-4">
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              Question Text
            </label>
            <input
              type="text"
              bind:value={question.questionText}
              class="w-full p-2 border rounded dark:bg-gray-700 dark:border-gray-600 dark:text-white"
              placeholder="Enter your question"
            />
          </div>
          
          <div class="mb-4">
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              Options
            </label>
            {#each question.options as option, optionIndex}
              <div class="flex gap-2 mb-2">
                <input
                  type="text"
                  bind:value={option}
                  class="flex-1 p-2 border rounded dark:bg-gray-700 dark:border-gray-600 dark:text-white"
                  placeholder="Enter option"
                />
                {#if question.options.length > 2}
                  <button
                    class="text-red-500 hover:text-red-700"
                    on:click={() => removeOption(questionIndex, optionIndex)}
                  >
                    Remove
                  </button>
                {/if}
              </div>
            {/each}
            <button
              class="text-primary-600 hover:text-primary-700 text-sm"
              on:click={() => addOption(questionIndex)}
            >
              + Add Option
            </button>
          </div>
          
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Selection Limit
              </label>
              <input
                type="number"
                bind:value={question.selectionLimit}
                min="1"
                max={question.options.length}
                class="w-full p-2 border rounded dark:bg-gray-700 dark:border-gray-600 dark:text-white"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Selection Mode
              </label>
              <select
                bind:value={question.selectionMode}
                class="w-full p-2 border rounded dark:bg-gray-700 dark:border-gray-600 dark:text-white"
              >
                <option value="single">Single Choice</option>
                <option value="multiple">Multiple Choice</option>
              </select>
            </div>
          </div>
        </div>
      {/each}
      
      <div class="flex justify-between items-center mt-4">
        <button
          class="text-primary-600 hover:text-primary-700"
          on:click={addQuestion}
        >
          + Add Another Question
        </button>
        
        <div class="flex gap-2">
          <button
            class="btn btn-secondary"
            on:click={() => {
              showCreateModal = false;
              questions = [{ questionText: '', options: [''], selectionLimit: 1, selectionMode: 'single' }];
            }}
          >
            Cancel
          </button>
          <button
            class="btn btn-primary"
            on:click={handleSubmit}
            disabled={loading}
          >
            {loading ? 'Creating...' : 'Create Poll'}
          </button>
        </div>
      </div>
    </div>
  </div>
{/if}