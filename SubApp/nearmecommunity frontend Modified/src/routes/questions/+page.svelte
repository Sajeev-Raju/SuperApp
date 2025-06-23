<script lang="ts">
  import { onMount } from "svelte";
  import { user } from "$stores/userStore";
  import { goto } from "$app/navigation";
  import { qandaApi } from "$api/apiClient";
  import LoadingSpinner from "$components/ui/LoadingSpinner.svelte";
  import UserLocationBadge from "$components/ui/UserLocationBadge.svelte";

  interface Answer {
    id: number;
    userId: string;
    description: string;
    createdAt: string;
  }

  interface Question {
    id: number;
    title: string;
    description: string;
    tags: string;
    userId: string;
    createdAt: string;
    answers?: Answer[];
  }

  let questions: Question[] = [];
  let isLoading = true;
  let error: string | null = null;
  let searchQuery = "";
  let selectedTags: string[] = [];
  let availableTags: string[] = [];
  let expandedQuestionId: number | null = null;
  let currentPage = 1;
  let pageSize = 50;
  let totalPages = 1;

  // Helper function to safely split tags
  function getQuestionTags(tags: string | null | undefined): string[] {
    if (!tags) return [];
    return tags.split(',').map(tag => tag.trim()).filter(tag => tag.length > 0);
  }

  onMount(async () => {
    if (!$user.userId || !$user.location) {
      goto("/");
      return;
    }

    try {
      // Load tags
      const tagsResponse = await qandaApi.getTags();
      availableTags = Array.isArray(tagsResponse) ? tagsResponse : [];
      
      // Load questions
      await loadQuestions();
    } catch (err) {
      console.error("Error loading data:", err);
      error = "Failed to load questions. Please try again.";
    } finally {
      isLoading = false;
    }
  });

  onMount(() => {
    loadQuestions(1);
  });

  async function loadQuestions(page = 1) {
    isLoading = true;
    error = null;
    try {
      const response = await qandaApi.getQuestions(page - 1, pageSize);
      if (response && response.data) {
        questions = response.data;
        // Fetch answer counts for each question
        await Promise.all(
          questions.map(async (q, idx) => {
            try {
              const details = await qandaApi.getQuestion(q.id);
              questions[idx].answers = details.answers || [];
            } catch (e) {
              questions[idx].answers = [];
            }
          })
        );
        currentPage = (response.currentPage ?? response.page ?? (page - 1)) + 1;
        pageSize = response.size ?? pageSize;
        totalPages = response.totalPages ?? 1;
      } else {
        questions = [];
        totalPages = 1;
        error = "No questions found. Make sure you have set your location and are within range of other users.";
      }
    } catch (err) {
      console.error("Error loading questions:", err);
      error = "Failed to load questions. Please try again.";
      questions = [];
      totalPages = 1;
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

  function toggleQuestion(questionId: number) {
    expandedQuestionId = expandedQuestionId === questionId ? null : questionId;
  }

  function getPageNumbers() {
    const pages = [];
    for (let i = 1; i <= totalPages; i++) {
      pages.push(i);
    }
    return pages;
  }

  function goToPage(page: number) {
    if (page >= 1 && page <= totalPages && page !== currentPage) {
      loadQuestions(page);
    }
  }

  $: filteredQuestions = questions.filter(question => {
    // Filter by search query
    const matchesSearch = searchQuery === "" || 
      question.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      question.description.toLowerCase().includes(searchQuery.toLowerCase());
    
    // Filter by tags
    const questionTags = getQuestionTags(question.tags);
    const matchesTags = selectedTags.length === 0 || 
      selectedTags.every(tag => questionTags.includes(tag));
    
    return matchesSearch && matchesTags;
  });

  $: if (searchQuery || selectedTags.length) {
    currentPage = 1;
  }
</script>

<div class="min-h-screen bg-gray-50 dark:bg-black pt-16 pb-20">
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
    <div class="py-6">
      <div class="flex flex-col md:flex-row md:items-center md:justify-between mb-8">
        <div>
          <h1 class="text-3xl font-bold text-gray-900 dark:text-white">Questions & Answers</h1>
          <div class="mt-2">
            <UserLocationBadge />
          </div>
        </div>
        <div class="mt-4 md:mt-0">
          <a href="/questions/create" class="btn btn-primary">
            Ask a Question
          </a>
        </div>
      </div>

      <div class="bg-white dark:bg-black rounded-xl shadow-md overflow-hidden mb-6 border-2 border-purple-500 dark:border-purple-400">
        <div class="p-6">
          <div class="flex flex-col md:flex-row md:items-center gap-4">
            <div class="flex-1">
              <div class="relative">
                <input
                  type="text"
                  bind:value={searchQuery}
                  placeholder="Search questions..."
                  class="input pl-10"
                />
                <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                  </svg>
                </div>
              </div>
            </div>
          </div>

          {#if availableTags.length > 0}
            <div class="mt-4">
              <div class="text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Filter by tags:</div>
              <div class="flex flex-wrap gap-2">
                {#each availableTags as tag}
                  <button
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
        </div>
      </div>

      {#if isLoading}
        <div class="flex justify-center py-20">
          <LoadingSpinner size="lg" />
        </div>
      {:else if error}
        <div class="bg-danger-50 dark:bg-danger-900/20 text-danger-800 dark:text-danger-200 p-4 rounded-lg mb-6">
          {error}
          <button class="underline ml-2" on:click={loadQuestions}>Try again</button>
        </div>
      {:else if filteredQuestions.length === 0}
        <div class="bg-white dark:bg-gray-800 rounded-xl shadow-md p-8 text-center">
          <div class="text-gray-500 dark:text-gray-400 mb-4">No questions found matching your criteria</div>
          {#if selectedTags.length > 0 || searchQuery}
            <button on:click={() => { selectedTags = []; searchQuery = ""; }} class="btn btn-outline">
              Clear filters
            </button>
          {:else}
            <a href="/questions/create" class="btn btn-primary">
              Ask the first question
            </a>
          {/if}
        </div>
      {:else}
        <div class="space-y-4">
          {#each filteredQuestions as question}
            <div 
              class="bg-white dark:bg-black rounded-xl shadow-md overflow-hidden border border-purple-100 dark:border-purple-900/50 transition-all duration-200 hover:border-2 hover:border-purple-500 dark:hover:border-purple-400 hover:shadow-lg hover:shadow-purple-500/20 dark:hover:shadow-purple-400/20 cursor-pointer"
              on:click={() => toggleQuestion(question.id)}
            >
              <div class="p-6">
                <div class="flex justify-between items-start">
                  <h3 class="text-xl font-semibold text-gray-900 dark:text-white">
                    {question.title}
                  </h3>
                  <div class="flex items-center text-gray-500 dark:text-gray-400 text-sm">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                    </svg>
                    {question.answers?.length || 0} answers
                  </div>
                </div>

                {#if expandedQuestionId === question.id}
                  <div class="mt-4">
                    <p class="text-gray-600 dark:text-gray-300">
                      {question.description}
                    </p>
                    <div class="mt-4 flex flex-wrap gap-2">
                      {#each getQuestionTags(question.tags) as tag}
                        <span class="badge badge-primary">
                          {tag}
                        </span>
                      {/each}
                    </div>
                    <div class="mt-4 flex justify-between items-center text-sm text-gray-500 dark:text-gray-400">
                      <div>
                        Asked by {question.userId || "Anonymous"}
                      </div>
                      <div>
                        {new Date(question.createdAt).toLocaleDateString()}
                      </div>
                    </div>

                    {#if question.answers && question.answers.length > 0}
                      <div class="mt-6 space-y-4">
                        <h4 class="text-lg font-medium text-gray-900 dark:text-white">Answers</h4>
                        {#each question.answers as answer}
                          <div class="bg-gray-50 dark:bg-gray-800 rounded-lg p-4 border border-purple-100 dark:border-purple-900/50">
                            <p class="text-gray-600 dark:text-gray-300">{answer.description}</p>
                            <div class="mt-2 flex justify-between items-center text-sm text-gray-500 dark:text-gray-400">
                              <div>Answered by {answer.userId || "Anonymous"}</div>
                              <div>{new Date(answer.createdAt).toLocaleDateString()}</div>
                            </div>
                          </div>
                        {/each}
                      </div>
                    {/if}

                    <div class="mt-6">
                      <a href={`/questions/${question.id}`} class="btn btn-outline" on:click|stopPropagation>
                        {question.answers?.length ? 'View Answers' : 'Answer Question'}
                      </a>
                    </div>
                  </div>
                {/if}
              </div>
            </div>
          {/each}
        </div>
      {/if}

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
    </div>
  </div>
</div>