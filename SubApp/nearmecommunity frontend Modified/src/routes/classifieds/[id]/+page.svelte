<script lang="ts">
  import { onMount } from "svelte";
  import { page } from "$app/stores";
  import { user } from "$stores/userStore";
  import { goto } from "$app/navigation";
  import apiClient from "$api/apiClient";
  import LoadingSpinner from "$components/ui/LoadingSpinner.svelte";
  import UserLocationBadge from "$components/ui/UserLocationBadge.svelte";

  interface Classified {
    id: number;
    title: string;
    description: string;
    price: number;
    imageUrlString: string;
    imageUrl?: string;
    categories: string[];
    userId: string;
    createdAt: string;
    notes?: string[];
    questions?: {
      id: number;
      content: string;
      userId: string;
      createdAt: string;
      answer?: {
        content: string;
        createdAt: string;
      };
    }[];
  }

  let classified: Classified | null = null;
  let isLoading = true;
  let error: string | null = null;
  let questionContent = "";
  let isSubmittingQuestion = false;
  let noteContent = "";
  let isSubmittingNote = false;
  let answerContent = "";
  let isSubmittingAnswer = false;
  let answeringQuestionId: number | null = null;

  onMount(async () => {
    if (!$user.userId || !$user.location) {
      goto("/");
      return;
    }

    await loadClassified();
  });

  async function loadClassified() {
    isLoading = true;
    error = null;

    try {
      const response = await apiClient.classifieds.getClassified($page.params.id);
      console.log("Classified response:", response);
      
      if (response) {
        // Fix image URL if needed
        if (response.imageUrl) {
          // Remove any leading slashes
          const imagePath = response.imageUrl.replace(/^\/+/, '');
          response.imageUrlString = `http://localhost:8080/${imagePath}`;
          console.log("Processed image URL:", response.imageUrlString);
        }
        classified = response as Classified;
        console.log("Classified data after processing:", classified);
      } else {
        error = "Classified not found";
      }
    } catch (err) {
      console.error("Error loading classified:", err);
      error = "Failed to load classified. Please try again.";
    } finally {
      isLoading = false;
    }
  }

  async function handleSubmitQuestion() {
    if (!questionContent.trim()) {
      error = "Please enter your question.";
      return;
    }

    if (!classified?.id) {
      error = "Invalid classified ID.";
      return;
    }

    isSubmittingQuestion = true;
    error = null;

    try {
      console.log("Posting question for classified:", classified.id); // Debug log
      const questionData = {
        content: questionContent.trim()
      };
      console.log("Question data:", questionData); // Debug log
      await apiClient.classifieds.askQuestion(classified.id, questionData);
      
      questionContent = "";
      await loadClassified(); // Reload to show the new question
    } catch (err) {
      console.error("Error posting question:", err);
      error = "Failed to post question. Please try again.";
    } finally {
      isSubmittingQuestion = false;
    }
  }

  async function handleSubmitAnswer(questionId: number) {
    if (!answerContent.trim()) {
      error = "Please enter your answer.";
      return;
    }

    isSubmittingAnswer = true;
    error = null;

    try {
      await apiClient.classifieds.answerQuestion(questionId, {
        content: answerContent.trim()
      });
      
      answerContent = "";
      answeringQuestionId = null;
      await loadClassified(); // Reload to show the new answer
    } catch (err) {
      console.error("Error posting answer:", err);
      error = "Failed to post answer. Please try again.";
    } finally {
      isSubmittingAnswer = false;
    }
  }

  function startAnswering(questionId: number) {
    answeringQuestionId = questionId;
    answerContent = "";
  }

  function cancelAnswering() {
    answeringQuestionId = null;
    answerContent = "";
  }

  async function handleSubmitNote() {
    if (!noteContent.trim()) {
      error = "Please enter your note.";
      return;
    }

    isSubmittingNote = true;
    error = null;

    try {
      await apiClient.classifieds.addNote($page.params.id, {
        content: noteContent.trim()
      });
      
      noteContent = "";
      await loadClassified(); // Reload to show the new note
    } catch (err) {
      console.error("Error adding note:", err);
      error = "Failed to add note. Please try again.";
    } finally {
      isSubmittingNote = false;
    }
  }

  async function handleDeleteClassified() {
    if (!confirm("Are you sure you want to delete this classified?")) {
      return;
    }

    try {
      await apiClient.classifieds.deleteClassified($page.params.id);
      goto("/classifieds");
    } catch (err) {
      console.error("Error deleting classified:", err);
      error = "Failed to delete classified. Please try again.";
    }
  }
</script>

<div class="min-h-screen bg-black pt-16 pb-20">
  <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
    <div class="py-6">
      <div class="mb-8">
        <div class="flex items-center gap-4">
          <button
            on:click={() => goto("/classifieds")}
            class="btn btn-outline"
          >
            ← Back to Classifieds
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
          <button class="underline ml-2" on:click={loadClassified}>Try again</button>
        </div>
      {:else if !classified}
        <div class="bg-black rounded-xl shadow-md p-8 text-center">
          <div class="text-gray-500 dark:text-gray-400">Classified not found</div>
        </div>
      {:else}
        <div class="space-y-6">
          <!-- Classified Details -->
          <div class="bg-black rounded-xl shadow-md overflow-hidden border-2 border-purple-500 dark:border-purple-400">
            <div class="aspect-w-16 aspect-h-9">
              <img
                src={classified.imageUrlString}
                alt={classified.title}
                class="object-cover w-full h-96"
                on:error={() => {
                  const img = document.querySelector(`img[src="${classified.imageUrlString}"]`);
                  if (img) {
                    img.setAttribute('src', 'https://via.placeholder.com/800x450?text=No+Image');
                  }
                }}
              />
            </div>
            <div class="p-6">
              <div class="flex justify-between items-start">
                <h1 class="text-3xl font-bold text-gray-900 dark:text-white mb-4">
                  {classified.title}
                </h1>
                <div class="text-2xl font-bold text-primary-600 dark:text-primary-400">
                  ${classified.price}
                </div>
              </div>
              <p class="text-gray-600 dark:text-gray-300 mb-6">
                {classified.description}
              </p>
              <div class="flex flex-wrap gap-2 mb-6">
                {#each classified.categories as category}
                  <span class="badge badge-primary">
                    {category}
                  </span>
                {/each}
              </div>
              <div class="flex justify-between items-center text-sm text-gray-500 dark:text-gray-400">
                <div>
                  Posted by {classified.userId || "Anonymous"}
                </div>
                <div>
                  {new Date(classified.createdAt).toLocaleDateString()}
                </div>
              </div>

              {#if classified.userId === $user.userId}
                <div class="mt-6">
                  <button
                    on:click={handleDeleteClassified}
                    class="btn btn-danger"
                  >
                    Delete Classified
                  </button>
                </div>
              {/if}
            </div>
          </div>

          <!-- Questions -->
          <div class="mt-8">
            <h3 class="text-lg font-semibold mb-4">Questions</h3>
            {#if classified.questions && classified.questions.length > 0}
              <div class="space-y-4">
                {#each classified.questions as question}
                  <div class="bg-black p-4 rounded-lg shadow">
                    <div class="flex justify-between items-start">
                      <div>
                        <p class="text-gray-800">{question.content}</p>
                        <p class="text-sm text-gray-500 mt-1">Asked by {question.userId}</p>
                      </div>
                      {#if question.userId === $user?.id}
                        <button
                          class="text-red-500 hover:text-red-700"
                          on:click={() => handleDeleteQuestion(question.id)}
                        >
                          Delete
                        </button>
                      {/if}
                    </div>
                  </div>
                {/each}
              </div>
            {:else}
              <p class="text-gray-500">Be the first to ask a question!</p>
            {/if}

            {#if $user && classified.userId !== $user.id}
              <form on:submit|preventDefault={handleSubmitQuestion} class="mt-4">
                <textarea
                  bind:value={questionContent}
                  placeholder="Ask a question about this classified..."
                  class="w-full p-2 border rounded-lg"
                  rows="3"
                ></textarea>
                <button
                  type="submit"
                  class="mt-2 bg-blue-500 text-white px-4 py-2 rounded-lg hover:bg-blue-600"
                >
                  Ask Question
                </button>
              </form>
            {/if}
          </div>

          <!-- Notes (only visible to owner) -->
          {#if classified.userId === $user.userId}
            <div class="bg-black rounded-xl shadow-md overflow-hidden">
              <div class="p-6">
                <h2 class="text-xl font-semibold text-gray-900 dark:text-white mb-4">
                  Notes
                </h2>

                {#if classified.notes && classified.notes.length > 0}
                  <div class="space-y-4">
                    {#each classified.notes as note}
                      <div class="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-4">
                        <p class="text-gray-600 dark:text-gray-300">{note}</p>
                      </div>
                    {/each}
                  </div>
                {:else}
                  <div class="text-gray-500 dark:text-gray-400 text-center py-4">
                    No notes yet. Add your first note!
                  </div>
                {/if}

                <!-- Note Form -->
                <div class="mt-6">
                  <h3 class="text-lg font-medium text-gray-900 dark:text-white mb-4">
                    Add a Note
                  </h3>
                  <form on:submit|preventDefault={handleSubmitNote} class="space-y-4">
                    <textarea
                      bind:value={noteContent}
                      rows="4"
                      placeholder="Type your note here..."
                      class="input w-full"
                      required
                    ></textarea>
                    <div class="flex justify-end">
                      <button
                        type="submit"
                        class="btn btn-primary"
                        disabled={isSubmittingNote}
                      >
                        {#if isSubmittingNote}
                          <LoadingSpinner size="sm" class="mr-2" />
                          Adding...
                        {:else}
                          Add Note
                        {/if}
                      </button>
                    </div>
                  </form>
                </div>
              </div>
            </div>
          {/if}
        </div>
      {/if}
    </div>
  </div>
</div> 