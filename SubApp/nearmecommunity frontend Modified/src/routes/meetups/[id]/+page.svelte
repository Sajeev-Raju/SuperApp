<script lang="ts">
  import { onMount } from "svelte";
  import { page } from "$app/stores";
  import { user } from "$stores/userStore";
  import { goto } from "$app/navigation";
  import apiClient from "$api/apiClient";
  import LoadingSpinner from "$components/ui/LoadingSpinner.svelte";
  import toast from "svelte-french-toast";

  let meetup = null;
  let isLoading = true;
  let error = null;
  let newNote = "";
  let newQuestion = "";
  let questions = [];

  onMount(async () => {
    if (!$user.userId) {
      goto("/");
      return;
    }

    try {
      await loadMeetup();
      await loadQuestions();
    } catch (err) {
      console.error("Error loading meetup:", err);
      error = "Failed to load meetup details. Please try again.";
    } finally {
      isLoading = false;
    }
  });

  async function loadMeetup() {
    const meetupId = $page.params.id;
    try {
      meetup = await apiClient.meetups.getMeetup(meetupId);
    } catch (err) {
      throw new Error("Failed to load meetup");
    }
  }

  async function loadQuestions() {
    const meetupId = $page.params.id;
    try {
      questions = await apiClient.meetups.getMeetupQuestions(meetupId);
    } catch (err) {
      console.error("Error loading questions:", err);
    }
  }

  async function handleUpdateNote() {
    if (!newNote.trim()) return;
    
    try {
      await apiClient.meetups.updateMeetupNote($page.params.id, { note: newNote });
      meetup.notes = [...(meetup.notes || []), newNote];
      newNote = "";
      toast.success("Note added successfully");
    } catch (err) {
      toast.error("Failed to add note");
    }
  }

  async function handlePostQuestion() {
    if (!newQuestion.trim()) return;
    
    try {
      await apiClient.meetups.postMeetupQuestion($page.params.id, { content: newQuestion });
      await loadQuestions();
      newQuestion = "";
      toast.success("Question posted successfully");
    } catch (err) {
      toast.error("Failed to post question");
    }
  }

  async function handleDeleteMeetup() {
    if (!confirm("Are you sure you want to delete this meetup?")) return;
    
    try {
      await apiClient.meetups.deleteMeetup($page.params.id);
      toast.success("Meetup deleted successfully");
      goto("/meetups");
    } catch (err) {
      toast.error("Failed to delete meetup");
    }
  }

  function getMeetupImageUrl(meetupId: string): string {
    return apiClient.meetups.getMeetupImageUrl(meetupId);
  }
</script>

<div class="min-h-screen bg-gray-50 dark:bg-black pt-16 pb-20">
  <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
    {#if isLoading}
      <div class="flex justify-center py-20">
        <LoadingSpinner size="lg" />
      </div>
    {:else if error}
      <div class="bg-danger-50 dark:bg-danger-900/20 text-danger-800 dark:text-danger-200 p-4 rounded-lg mb-6">
        {error}
        <button class="underline ml-2" on:click={loadMeetup}>Try again</button>
      </div>
    {:else if meetup}
      <div class="bg-white dark:bg-black rounded-xl shadow-md overflow-hidden border-2 border-purple-500 dark:border-purple-400">
        <!-- Meetup Image -->
        <div class="h-64 bg-gray-200 dark:bg-black relative">
          <img
            src={getMeetupImageUrl(meetup.id)}
            alt={meetup.title}
            class="w-full h-full object-cover"
            onerror="this.src='https://images.pexels.com/photos/2774556/pexels-photo-2774556.jpeg?auto=compress&cs=tinysrgb&w=800'; this.onerror=null;"
          />
        </div>

        <!-- Meetup Details -->
        <div class="p-6">
          <div class="flex justify-between items-start mb-4">
            <h1 class="text-3xl font-bold text-gray-900 dark:text-white">{meetup.title}</h1>
            {#if meetup.organizerId === $user.userId}
              <button
                on:click={handleDeleteMeetup}
                class="btn btn-danger"
              >
                Delete Meetup
              </button>
            {/if}
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
            <div>
              <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-2">Event Details</h2>
              <div class="space-y-2">
                <p class="text-gray-600 dark:text-gray-300">
                  <span class="font-medium">Date:</span> {new Date(meetup.startDate).toLocaleDateString()}
                </p>
                <p class="text-gray-600 dark:text-gray-300">
                  <span class="font-medium">Time:</span> {meetup.startTime} - {meetup.endTime}
                </p>
                <p class="text-gray-600 dark:text-gray-300">
                  <span class="font-medium">Location:</span> {meetup.eventAddress}
                </p>
                {#if meetup.googleLocationURL}
                  <a
                    href={meetup.googleLocationURL}
                    target="_blank"
                    rel="noopener noreferrer"
                    class="text-secondary-600 dark:text-secondary-400 hover:underline"
                  >
                    View on Google Maps
                  </a>
                {/if}
              </div>
            </div>

            <div>
              <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-2">Organizer</h2>
              <p class="text-gray-600 dark:text-gray-300">{meetup.organizerName}</p>
              {#if meetup.contactInfo}
                <p class="text-gray-600 dark:text-gray-300 mt-2">
                  <span class="font-medium">Contact:</span> {meetup.contactInfo}
                </p>
              {/if}
            </div>
          </div>

          <div class="mb-6">
            <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-2">Description</h2>
            <p class="text-gray-600 dark:text-gray-300 whitespace-pre-wrap">{meetup.description}</p>
          </div>

          {#if meetup.tags && meetup.tags.length > 0}
            <div class="mb-6">
              <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-2">Tags</h2>
              <div class="flex flex-wrap gap-2">
                {#each meetup.tags as tag}
                  <span class="badge badge-secondary">{tag}</span>
                {/each}
              </div>
            </div>
          {/if}

          <!-- Notes Section -->
          {#if meetup.organizerId === $user.userId}
            <div class="mb-6">
              <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-2">Notes</h2>
              <div class="space-y-4">
                {#if meetup.notes && meetup.notes.length > 0}
                  {#each meetup.notes as note}
                    <div class="bg-gray-50 dark:bg-gray-700 p-3 rounded-lg">
                      <p class="text-gray-600 dark:text-gray-300">{note}</p>
                    </div>
                  {/each}
                {:else}
                  <p class="text-gray-500 dark:text-gray-400">No notes yet</p>
                {/if}
                
                <div class="flex gap-2">
                  <input
                    type="text"
                    bind:value={newNote}
                    placeholder="Add a note..."
                    class="input flex-1"
                  />
                  <button
                    on:click={handleUpdateNote}
                    class="btn btn-secondary"
                    disabled={!newNote.trim()}
                  >
                    Add Note
                  </button>
                </div>
              </div>
            </div>
          {/if}

          <!-- Questions Section -->
          <div>
            <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-2">Questions</h2>
            <div class="space-y-4">
              {#if questions && questions.length > 0}
                {#each questions as question}
                  <div class="bg-gray-50 dark:bg-gray-700 p-4 rounded-lg">
                    <p class="text-gray-600 dark:text-gray-300">{question.content}</p>
                    <p class="text-sm text-gray-500 dark:text-gray-400 mt-2">
                      Asked by {question.userName}
                    </p>
                    {#if question.answer}
                      <div class="mt-2 pl-4 border-l-2 border-secondary-200 dark:border-secondary-700">
                        <p class="text-gray-600 dark:text-gray-300">{question.answer}</p>
                        <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">
                          Answered by {meetup.organizerName}
                        </p>
                      </div>
                    {/if}
                  </div>
                {/each}
              {:else}
                <p class="text-gray-500 dark:text-gray-400">No questions yet</p>
              {/if}
              
              {#if meetup.organizerId !== $user.userId}
                <div class="flex gap-2">
                  <input
                    type="text"
                    bind:value={newQuestion}
                    placeholder="Ask a question..."
                    class="input flex-1"
                  />
                  <button
                    on:click={handlePostQuestion}
                    class="btn btn-secondary"
                    disabled={!newQuestion.trim()}
                  >
                    Ask Question
                  </button>
                </div>
              {/if}
            </div>
          </div>
        </div>
      </div>
    {/if}
  </div>
</div> 