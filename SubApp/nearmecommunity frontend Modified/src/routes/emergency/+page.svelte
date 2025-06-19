<script lang="ts">
  import { onMount } from "svelte";
  import { user } from "$stores/userStore";
  import { goto } from "$app/navigation";
  import apiClient, { emergencyApi } from "$api/apiClient";
  import type { EmergencyMessage } from "$api/apiClient";
  import LoadingSpinner from "$components/ui/LoadingSpinner.svelte";
  import UserLocationBadge from "$components/ui/UserLocationBadge.svelte";

  let messages: EmergencyMessage[] = [];
  let isLoading = true;
  let error: string | null = null;
  let showCreateModal = false;
  let title = "";
  let description = "";
  let selectedTypes: string[] = [];
  let details: Record<string, string> = {};
  let googleMapsLocation = "";
  let showQuestionModal = false;
  let selectedMessageId: number | null = null;
  let questionContent = "";
  let showNoteModal = false;
  let noteContent = "";

  const emergencyTypes = [
    "Natural Disaster",
    "Medical Emergency",
    "Security Threat",
    "Infrastructure Issue",
    "Weather Alert",
    "Other"
  ];

  onMount(async () => {
    if (!$user.userId || !$user.location) {
      error = "Please set your location before creating emergency messages.";
      goto("/");
      return;
    }

    try {
      await loadMessages();
    } catch (err) {
      console.error("Error loading messages:", err);
      error = "Failed to load emergency messages. Please try again.";
    }
  });

  async function loadMessages() {
    isLoading = true;
    error = null;
    try {
      messages = await emergencyApi.getMessages();
    } catch (err) {
      console.error("Error loading emergency messages:", err);
      error = "Failed to load emergency messages. Please try again.";
    } finally {
      isLoading = false;
    }
  }

  async function handleCreateMessage() {
    error = null;

    // Check if user has location set
    if (!$user.location) {
      error = "Please set your location before creating emergency messages.";
      return;
    }

    // Validate required fields
    if (!title.trim()) {
      error = "Please enter a title.";
      return;
    }

    if (!description.trim()) {
      error = "Please enter a description.";
      return;
    }

    if (selectedTypes.length === 0) {
      error = "Please select at least one type.";
      return;
    }

    try {
      // Create a clean details object without empty keys
      const cleanDetails = Object.entries(details).reduce((acc, [key, value]) => {
        if (key.trim() && value.trim()) {
          acc[key.trim()] = value.trim();
        }
        return acc;
      }, {} as Record<string, string>);

      // Prepare the request data
      const requestData = {
        title: title.trim(),
        description: description.trim(),
        types: selectedTypes,
        details: cleanDetails,
        googleMapsLocation: googleMapsLocation.trim() || null
      };

      console.log('Sending emergency message data:', requestData);

      const response = await emergencyApi.createMessage(requestData);
      console.log('Emergency message created:', response);

      showCreateModal = false;
      title = "";
      description = "";
      selectedTypes = [];
      details = {};
      googleMapsLocation = "";
      await loadMessages();
    } catch (err) {
      console.error("Error creating emergency message:", err);
      if (err instanceof Error) {
        error = err.message;
      } else {
        error = "Failed to create emergency message. Please try again.";
      }
    }
  }

  async function handleDeleteMessage(id: number) {
    if (!confirm("Are you sure you want to delete this emergency message?")) {
      return;
    }

    try {
      await emergencyApi.deleteMessage(id.toString());
      await loadMessages();
    } catch (err) {
      console.error("Error deleting emergency message:", err);
      error = "Failed to delete emergency message. Please try again.";
    }
  }

  async function handleAskQuestion(messageId: number | null) {
    if (!messageId) {
      error = "Invalid message ID.";
      return;
    }

    if (!questionContent.trim()) {
      error = "Please enter your question.";
      return;
    }

    try {
      await emergencyApi.askQuestion(messageId.toString(), {
        content: questionContent.trim()
      });

      showQuestionModal = false;
      questionContent = "";
      selectedMessageId = null;
      await loadMessages();
    } catch (err) {
      console.error("Error asking question:", err);
      error = "Failed to ask question. Please try again.";
    }
  }

  async function handleAddNote(messageId: number | null) {
    if (!messageId) {
      error = "Invalid message ID.";
      return;
    }

    if (!noteContent.trim()) {
      error = "Please enter a note.";
      return;
    }

    try {
      await emergencyApi.addNote(messageId.toString(), {
        content: noteContent.trim()
      });

      showNoteModal = false;
      noteContent = "";
      selectedMessageId = null;
      await loadMessages();
    } catch (err) {
      console.error("Error adding note:", err);
      error = "Failed to add note. Please try again.";
    }
  }

  function toggleType(type: string) {
    if (selectedTypes.includes(type)) {
      selectedTypes = selectedTypes.filter(t => t !== type);
    } else {
      selectedTypes = [...selectedTypes, type];
    }
  }

  function addDetail() {
    const newKey = `detail_${Object.keys(details).length + 1}`;
    details = { ...details, [newKey]: "" };
  }

  function removeDetail(key: string) {
    const newDetails = { ...details };
    delete newDetails[key];
    details = newDetails;
  }

  function updateDetailKey(oldKey: string, newKey: string) {
    if (oldKey === newKey) return;
    const value = details[oldKey];
    const newDetails = { ...details };
    delete newDetails[oldKey];
    newDetails[newKey] = value;
    details = newDetails;
  }
</script>

<div class="min-h-screen bg-gray-50 dark:bg-black pt-16 pb-20">
  <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
    <div class="py-6">
      <div class="mb-8">
        <div class="flex items-center justify-between">
          <h1 class="text-3xl font-bold text-gray-900 dark:text-white">Emergency Messages</h1>
          <div class="flex items-center gap-4">
            <button
              on:click={() => showCreateModal = true}
              class="btn btn-primary"
            >
              Create Emergency Message
            </button>
            <UserLocationBadge />
          </div>
        </div>
      </div>

      {#if isLoading}
        <div class="flex justify-center py-20">
          <LoadingSpinner size="lg" />
        </div>
      {:else if error}
        <div class="bg-danger-50 dark:bg-danger-900/20 text-danger-800 dark:text-danger-200 p-4 rounded-lg mb-6">
          {error}
          <button class="underline ml-2" on:click={loadMessages}>Try again</button>
        </div>
      {:else if messages.length === 0}
        <div class="bg-white dark:bg-black rounded-xl shadow-md p-8 text-center">
          <p class="text-gray-500 dark:text-gray-400">No emergency messages found.</p>
        </div>
      {:else}
        <div class="space-y-4">
          {#each messages as message}
            <div class="bg-white dark:bg-black rounded-lg p-6 border-2 border-gray-200 dark:border-gray-700 hover:border-purple-500 dark:hover:border-purple-400 transition-colors">
              <div class="flex justify-between items-start mb-4">
                <div>
                  <h3 class="text-xl font-semibold text-gray-900 dark:text-white mb-2">{message.title}</h3>
                  <p class="text-gray-600 dark:text-gray-300 mb-4">{message.description}</p>
                  <div class="flex flex-wrap gap-2 mb-4">
                    {#each message.types as type}
                      <span class="px-3 py-1 bg-purple-100 dark:bg-purple-900/20 text-purple-800 dark:text-purple-200 rounded-full text-sm">
                        {type}
                      </span>
                    {/each}
                  </div>
                  {#if message.googleMapsLocation}
                    <div class="mb-4">
                      <a
                        href={message.googleMapsLocation}
                        target="_blank"
                        rel="noopener noreferrer"
                        class="text-purple-500 hover:text-purple-600 dark:text-purple-400 dark:hover:text-purple-300"
                      >
                        View Location on Google Maps
                      </a>
                    </div>
                  {/if}
                </div>
                {#if message.userId === $user.userId}
                  <button
                    on:click={() => handleDeleteMessage(message.id)}
                    class="text-red-500 hover:text-red-600 dark:text-red-400 dark:hover:text-red-300"
                  >
                    Delete
                  </button>
                {/if}
              </div>

              {#if Object.keys(message.details).length > 0}
                <div class="mb-4">
                  <h3 class="text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Additional Details:</h3>
                  <dl class="grid grid-cols-1 gap-2">
                    {#each Object.entries(message.details) as [key, value]}
                      <div class="flex">
                        <dt class="font-medium text-gray-500 dark:text-gray-400">{key}:</dt>
                        <dd class="ml-2 text-gray-700 dark:text-gray-300">{value}</dd>
                      </div>
                    {/each}
                  </dl>
                </div>
              {/if}

              {#if message.notes && message.notes.length > 0}
                <div class="mb-4">
                  <h3 class="text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Notes:</h3>
                  <ul class="space-y-2">
                    {#each message.notes as note}
                      <li class="text-sm text-gray-600 dark:text-gray-400">{note}</li>
                    {/each}
                  </ul>
                </div>
              {/if}

              <div class="flex gap-4 mt-6">
                {#if message.userId === $user.userId}
                  <button
                    on:click={() => {
                      selectedMessageId = message.id;
                      showNoteModal = true;
                    }}
                    class="btn btn-outline"
                  >
                    Add Note
                  </button>
                {:else}
                  <button
                    on:click={() => {
                      selectedMessageId = message.id;
                      showQuestionModal = true;
                    }}
                    class="btn btn-outline"
                  >
                    Ask Question
                  </button>
                {/if}
              </div>
            </div>
          {/each}
        </div>
      {/if}
    </div>
  </div>
</div>

<!-- Create Emergency Message Modal -->
{#if showCreateModal}
  <div class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4">
    <div class="bg-white dark:bg-black rounded-xl shadow-xl max-w-lg w-full p-6">
      <h2 class="text-2xl font-bold text-gray-900 dark:text-white mb-4">Create Emergency Message</h2>
      
      <div class="space-y-4">
        <div>
          <label for="title" class="block text-sm font-medium text-gray-700 dark:text-gray-300">
            Title
          </label>
          <input
            type="text"
            id="title"
            bind:value={title}
            class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500"
            placeholder="Enter emergency title"
          />
        </div>

        <div>
          <label for="description" class="block text-sm font-medium text-gray-700 dark:text-gray-300">
            Description
          </label>
          <textarea
            id="description"
            bind:value={description}
            rows="3"
            class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500"
            placeholder="Enter emergency description"
          ></textarea>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            Emergency Types
          </label>
          <div class="flex flex-wrap gap-2">
            {#each emergencyTypes as type}
              <button
                type="button"
                on:click={() => toggleType(type)}
                class="px-3 py-1 rounded-full text-sm font-medium {selectedTypes.includes(type)
                  ? 'bg-primary-100 text-primary-800 dark:bg-primary-900 dark:text-primary-200'
                  : 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-200'}"
              >
                {type}
              </button>
            {/each}
          </div>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            Additional Details
          </label>
          {#each Object.entries(details) as [key, value], i}
            <div class="flex gap-2 mb-2">
              <input
                type="text"
                value={key}
                on:input={(e) => updateDetailKey(key, e.currentTarget.value)}
                class="flex-1 rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500"
                placeholder="Detail name"
              />
              <input
                type="text"
                value={value}
                on:input={(e) => {
                  details[key] = e.currentTarget.value;
                  details = details; // Trigger reactivity
                }}
                class="flex-1 rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500"
                placeholder="Detail value"
              />
              <button
                type="button"
                on:click={() => removeDetail(key)}
                class="text-red-500 hover:text-red-700"
              >
                Remove
              </button>
            </div>
          {/each}
          <button
            type="button"
            on:click={addDetail}
            class="text-primary-600 hover:text-primary-700"
          >
            + Add Detail
          </button>
        </div>

        <div>
          <label for="location" class="block text-sm font-medium text-gray-700 dark:text-gray-300">
            Google Maps Location (Optional)
          </label>
          <input
            type="url"
            id="location"
            bind:value={googleMapsLocation}
            class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500"
            placeholder="Enter Google Maps URL"
          />
        </div>
      </div>

      <div class="mt-6 flex justify-end gap-4">
        <button
          type="button"
          on:click={() => showCreateModal = false}
          class="btn btn-outline"
        >
          Cancel
        </button>
        <button
          type="button"
          on:click={handleCreateMessage}
          class="btn btn-primary"
        >
          Create Message
        </button>
      </div>
    </div>
  </div>
{/if}

<!-- Ask Question Modal -->
{#if showQuestionModal}
  <div class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4">
    <div class="bg-white dark:bg-black rounded-xl shadow-xl max-w-lg w-full p-6">
      <h2 class="text-2xl font-bold text-gray-900 dark:text-white mb-4">Ask Question</h2>
      
      <div class="space-y-4">
        <div>
          <label for="question" class="block text-sm font-medium text-gray-700 dark:text-gray-300">
            Your Question
          </label>
          <textarea
            id="question"
            bind:value={questionContent}
            rows="3"
            class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500"
            placeholder="Enter your question"
          ></textarea>
        </div>
      </div>

      <div class="mt-6 flex justify-end gap-4">
        <button
          type="button"
          on:click={() => {
            showQuestionModal = false;
            questionContent = "";
            selectedMessageId = null;
          }}
          class="btn btn-outline"
        >
          Cancel
        </button>
        <button
          type="button"
          on:click={() => handleAskQuestion(selectedMessageId)}
          class="btn btn-primary"
        >
          Ask Question
        </button>
      </div>
    </div>
  </div>
{/if}

<!-- Add Note Modal -->
{#if showNoteModal}
  <div class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4">
    <div class="bg-white dark:bg-black rounded-xl shadow-xl max-w-lg w-full p-6">
      <h2 class="text-2xl font-bold text-gray-900 dark:text-white mb-4">Add Note</h2>
      
      <div class="space-y-4">
        <div>
          <label for="note" class="block text-sm font-medium text-gray-700 dark:text-gray-300">
            Note
          </label>
          <textarea
            id="note"
            bind:value={noteContent}
            rows="3"
            class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500"
            placeholder="Enter your note"
          ></textarea>
        </div>
      </div>

      <div class="mt-6 flex justify-end gap-4">
        <button
          type="button"
          on:click={() => {
            showNoteModal = false;
            noteContent = "";
            selectedMessageId = null;
          }}
          class="btn btn-outline"
        >
          Cancel
        </button>
        <button
          type="button"
          on:click={() => handleAddNote(selectedMessageId)}
          class="btn btn-primary"
        >
          Add Note
        </button>
      </div>
    </div>
  </div>
{/if} 