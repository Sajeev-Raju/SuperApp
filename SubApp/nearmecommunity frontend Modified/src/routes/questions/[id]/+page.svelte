<script lang="ts">
  import { onMount } from "svelte";
  import { page } from "$app/stores";
  import { user } from "$stores/userStore";
  import { goto } from "$app/navigation";
  import apiClient from "$api/apiClient";
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
    tags: string[];
    userId: string;
    createdAt: string;
    answers?: Answer[];
  }

  let question: Question | null = null;
  let isLoading = true;
  let error: string | null = null;
  let answerContent = "";
  let isSubmittingAnswer = false;
  let replyStates: Record<number, { open: boolean; content: string }> = {};

  onMount(async () => {
    if (!$user.userId || !$user.location) {
      goto("/");
      return;
    }

    await loadQuestion();
  });

  async function loadQuestion() {
    isLoading = true;
    error = null;
    
    try {
      const response = await apiClient.qanda.getQuestion($page.params.id);
      question = response;
    } catch (err) {
      console.error("Error loading question:", err);
      error = "Failed to load question. Please try again.";
    } finally {
      isLoading = false;
    }
  }

  async function handleSubmitAnswer() {
    if (!answerContent.trim()) {
      error = "Please enter your answer.";
      return;
    }

    isSubmittingAnswer = true;
    error = null;

    try {
      await apiClient.qanda.postAnswer({
        questionId: $page.params.id,
        content: answerContent.trim()
      });
      
      answerContent = "";
      await loadQuestion(); // Reload to show the new answer
    } catch (err) {
      console.error("Error posting answer:", err);
      error = "Failed to post answer. Please try again.";
    } finally {
      isSubmittingAnswer = false;
    }
  }

  function formatDate(dateString: string): string {
    if (!dateString) return '';
    // Try to parse as ISO, else replace space with T
    let d = new Date(dateString);
    if (isNaN(d.getTime()) && dateString.includes(' ')) {
      d = new Date(dateString.replace(' ', 'T'));
    }
    return isNaN(d.getTime()) ? '' : d.toLocaleDateString();
  }

  async function handleReplySubmit(answer: Answer) {
    const state = replyStates[answer.id];
    if (!state || !state.content.trim()) return;
    try {
      await apiClient.qanda.postAnswer({
        questionId: $page.params.id,
        content: `Reply to @${answer.userId}: ${answer.description}\n${state.content.trim()}`
      });
      state.content = "";
      state.open = false;
      await loadQuestion();
    } catch (err) {
      error = "Failed to post reply. Please try again.";
    }
  }

  // Helper to parse reply answers
  function parseReply(answer: Answer): { isReply: boolean, toUser: string, original: string, reply: string } {
    const match = answer.description.match(/^Reply to @([^:]+): ([\s\S]*?)(?:\n|$)([\s\S]*)/);
    if (match) {
      return {
        isReply: true,
        toUser: match[1],
        original: match[2].trim(),
        reply: match[3].trim()
      };
    }
    return { isReply: false, toUser: '', original: '', reply: '' };
  }

  async function handleDeleteAnswer(answerId: number) {
    if (!confirm('Are you sure you want to delete this answer?')) return;
    try {
      await apiClient.qanda.deleteAnswer(answerId);
      await loadQuestion();
    } catch (err) {
      error = 'Failed to delete answer. Please try again.';
    }
  }

  // Helper to get the original answer for a reply
  function getOriginalAnswer(answer: Answer): Answer {
    if (!parseReply(answer).isReply) return answer;
    // Find the original answer in the list
    if (!question?.answers) return answer;
    const parsed = parseReply(answer);
    return (
      question.answers.find(
        a => a.userId === parsed.toUser && a.description === parsed.original && !parseReply(a).isReply
      ) || answer
    );
  }

  // No grouping: flat list like Team-BHP forum
</script>

<div class="min-h-screen bg-gray-50 dark:bg-black pt-16 pb-20">
  <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
    <div class="py-6">
      <div class="mb-8">
        <div class="flex items-center gap-4">
          <button
            on:click={() => goto("/questions")}
            class="btn btn-outline"
          >
            ← Back to Questions
          </button>
          <UserLocationBadge />
        </div>
      </div>

      {#if isLoading}
        <div class="flex justify-center py-20">
          <LoadingSpinner size="lg" />
        </div>
      {:else if error}
        <div class="bg-danger-50 dark:bg-danger-900/20 text-danger-800 dark:text-danger-200 p-4 rounded-lg mb-6">
          {error}
          <button class="underline ml-2" on:click={loadQuestion}>Try again</button>
        </div>
      {:else if !question}
        <div class="bg-white dark:bg-black rounded-xl shadow-md p-8 text-center">
          <div class="text-gray-500 dark:text-gray-400">Question not found</div>
        </div>
      {:else}
        <div class="space-y-6">
          <!-- Question -->
          <div class="bg-white dark:bg-black rounded-xl shadow-md overflow-hidden border-2 border-purple-500">
            <div class="p-6">
              <h1 class="text-2xl font-bold text-gray-900 dark:text-white mb-4">
                {question.title}
              </h1>
              <p class="text-gray-600 dark:text-gray-300 mb-4">
                {question.description}
              </p>
              <div class="flex flex-wrap gap-2 mb-4">
                {#each question.tags as tag}
                  <span class="badge badge-primary">
                    {tag}
                  </span>
                {/each}
              </div>
              <div class="flex justify-between items-center text-sm text-gray-500 dark:text-gray-400">
                <div>
                  Asked by {question.userId || "Anonymous"}
                </div>
                <div>
                  {new Date(question.createdAt).toLocaleDateString()}
                </div>
              </div>
            </div>
          </div>

          <!-- Answers -->
          <div class="bg-white dark:bg-black rounded-xl shadow-md overflow-hidden border-2 border-purple-500">
            <div class="p-6">
              <h2 class="text-xl font-semibold text-gray-900 dark:text-white mb-4">
                {question.answers?.length || 0} Answers
              </h2>

              {#if question.answers && question.answers.length > 0}
                <div class="space-y-4">
                  {#each question.answers as answer}
                    <div class="bg-gray-50 dark:bg-black/70 rounded-lg p-4 border border-gray-300 dark:border-gray-700">
                      {#if parseReply(answer).isReply}
                        <blockquote class="bg-gray-100 dark:bg-gray-800 border-l-4 border-purple-500 dark:border-purple-400 p-2 mb-2 text-sm text-gray-700 dark:text-gray-200">
                          {parseReply(answer).original}
                        </blockquote>
                        <blockquote class="bg-gray-50 dark:bg-gray-900 border-l-4 border-blue-500 dark:border-blue-400 p-2 mb-2 text-sm text-gray-800 dark:text-white">
                          {parseReply(answer).reply}
                        </blockquote>
                        <div class="mt-2 flex justify-between items-center text-sm text-gray-500 dark:text-gray-400">
                          <div>Reply by {answer.userId || "Anonymous"} to @{parseReply(answer).toUser}</div>
                          <div>{formatDate(answer.createdAt)}</div>
                        </div>
                      {:else}
                        <p class="text-gray-600 dark:text-gray-300">{answer.description}</p>
                        <div class="mt-2 flex justify-between items-center text-sm text-gray-500 dark:text-gray-400">
                          <div>Answered by {answer.userId || "Anonymous"}</div>
                          <div>{formatDate(answer.createdAt)}</div>
                        </div>
                      {/if}
                      <div class="mt-2 flex items-center gap-2">
                        <button class="btn btn-secondary" on:click={() => {
                          // Always reply to the original answer
                          const orig = getOriginalAnswer(answer);
                          replyStates[orig.id] = { open: true, content: "" };
                        }}>Reply</button>
                        {#if answer.userId === $user.userId}
                          <button class="btn btn-danger btn-xs ml-2" on:click={() => handleDeleteAnswer(answer.id)}>
                            Delete
                          </button>
                        {/if}
                      </div>
                      {#if replyStates[getOriginalAnswer(answer).id]?.open}
                        <blockquote class="bg-gray-100 dark:bg-gray-800 border-l-4 border-purple-500 dark:border-purple-400 p-2 mb-2 text-sm text-gray-700 dark:text-gray-200">
                          {getOriginalAnswer(answer).description}
                        </blockquote>
                        <textarea
                          bind:value={replyStates[getOriginalAnswer(answer).id].content}
                          rows="2"
                          placeholder="Write your reply..."
                          class="input w-full mb-2"
                        ></textarea>
                        <button class="btn btn-primary mr-2" on:click={() => handleReplySubmit(getOriginalAnswer(answer))}>Reply</button>
                        <button class="btn btn-outline" on:click={() => replyStates[getOriginalAnswer(answer).id].open = false}>Cancel</button>
                      {/if}
                    </div>
                  {/each}
                </div>
              {:else}
                <div class="text-gray-500 dark:text-gray-400 text-center py-8">
                  No answers yet. Be the first to answer!
                </div>
              {/if}

              <!-- Answer Form -->
              {#if question.userId !== $user.userId}
                <div class="mt-8">
                  <h3 class="text-lg font-medium text-gray-900 dark:text-white mb-4">
                    Your Answer
                  </h3>
                  <form on:submit|preventDefault={handleSubmitAnswer} class="space-y-4">
                    <textarea
                      bind:value={answerContent}
                      rows="4"
                      placeholder="Write your answer here..."
                      class="input w-full"
                      required
                    ></textarea>
                    <div class="flex justify-end">
                      <button
                        type="submit"
                        class="btn btn-primary"
                        disabled={isSubmittingAnswer}
                      >
                        {#if isSubmittingAnswer}
                          <LoadingSpinner size="sm" class="mr-2" />
                          Posting...
                        {:else}
                          Post Answer
                        {/if}
                      </button>
                    </div>
                  </form>
                </div>
              {/if}
            </div>
          </div>
        </div>
      {/if}
    </div>
  </div>
</div> 