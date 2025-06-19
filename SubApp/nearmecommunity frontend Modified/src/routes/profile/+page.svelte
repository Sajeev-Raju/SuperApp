<script lang="ts">
  import { onMount } from "svelte";
  import { user } from "$stores/userStore";
  import { goto } from "$app/navigation";
  import apiClient from "$api/apiClient";
  import LoadingSpinner from "$components/ui/LoadingSpinner.svelte";
  import UserLocationBadge from "$components/ui/UserLocationBadge.svelte";

  let activeTab = "meetups";
  let isLoading = true;
  let error: string | null = null;
  let userContent = {
    meetups: [],
    questions: [],
    classifieds: [],
    polls: [],
    emergencyMessages: []
  };

  onMount(async () => {
    if (!$user.userId) {
      goto("/");
      return;
    }

    try {
      await loadUserContent();
    } catch (err) {
      console.error("Error loading user content:", err);
      error = "Failed to load your content. Please try again.";
    } finally {
      isLoading = false;
    }
  });

  async function loadUserContent() {
    isLoading = true;
    error = null;

    try {
      // Load all user content in parallel
      const [meetups, questions, classifieds, polls, emergencyMessages] = await Promise.all([
        apiClient.meetups.getUserMeetups(),
        apiClient.qanda.getUserQuestions(),
        apiClient.classifieds.getUserClassifieds(),
        apiClient.polls.getUserPolls(),
        apiClient.emergency.getUserMessages()
      ]);

      userContent = {
        meetups,
        questions,
        classifieds,
        polls,
        emergencyMessages
      };
    } catch (err) {
      console.error("Error loading user content:", err);
      error = "Failed to load your content. Please try again.";
    } finally {
      isLoading = false;
    }
  }
</script>

<div class="min-h-screen bg-gray-50 dark:bg-black pt-16 pb-20">
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
    <div class="py-6">
      <!-- Profile Header -->
      <div class="bg-white dark:bg-black rounded-xl shadow-md p-6 mb-6 border-2 border-purple-400 dark:border-purple-500">
        <div class="flex items-center gap-6">
          <div class="h-24 w-24 rounded-full bg-purple-100 dark:bg-purple-900/40 flex items-center justify-center">
            <span class="text-3xl text-purple-600 dark:text-purple-400">
              {$user.userId.charAt(0).toUpperCase()}
            </span>
          </div>
          <div>
            <h1 class="text-2xl font-bold text-gray-900 dark:text-white">
              {$user.userId}
            </h1>
            <div class="mt-2">
              <UserLocationBadge />
            </div>
          </div>
        </div>
      </div>

      <!-- Content Tabs -->
      <div class="bg-white dark:bg-black rounded-xl shadow-md overflow-hidden mb-6 border-2 border-purple-400 dark:border-purple-500">
        <div class="border-b border-gray-200 dark:border-gray-700">
          <nav class="flex -mb-px">
            <button
              class="px-6 py-4 text-sm font-medium border-b-2 {activeTab === 'meetups' ? 'border-purple-500 text-purple-600 dark:text-purple-400' : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'}"
              on:click={() => activeTab = 'meetups'}
            >
              My Meetups
            </button>
            <button
              class="px-6 py-4 text-sm font-medium border-b-2 {activeTab === 'questions' ? 'border-purple-500 text-purple-600 dark:text-purple-400' : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'}"
              on:click={() => activeTab = 'questions'}
            >
              My Questions
            </button>
            <button
              class="px-6 py-4 text-sm font-medium border-b-2 {activeTab === 'classifieds' ? 'border-purple-500 text-purple-600 dark:text-purple-400' : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'}"
              on:click={() => activeTab = 'classifieds'}
            >
              My Classifieds
            </button>
            <button
              class="px-6 py-4 text-sm font-medium border-b-2 {activeTab === 'polls' ? 'border-purple-500 text-purple-600 dark:text-purple-400' : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'}"
              on:click={() => activeTab = 'polls'}
            >
              My Polls
            </button>
            <button
              class="px-6 py-4 text-sm font-medium border-b-2 {activeTab === 'emergency' ? 'border-purple-500 text-purple-600 dark:text-purple-400' : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'}"
              on:click={() => activeTab = 'emergency'}
            >
              Emergency Messages
            </button>
          </nav>
        </div>

        <!-- Content Section -->
        <div class="p-6">
          {#if isLoading}
            <div class="flex justify-center py-20">
              <LoadingSpinner size="lg" />
            </div>
          {:else if error}
            <div class="bg-danger-50 dark:bg-danger-900/20 text-danger-800 dark:text-danger-200 p-4 rounded-lg">
              {error}
              <button class="underline ml-2" on:click={loadUserContent}>Try again</button>
            </div>
          {:else}
            {#if activeTab === 'meetups'}
              {#if userContent.meetups.length === 0}
                <div class="text-center py-12">
                  <p class="text-gray-500 dark:text-gray-400 mb-4">You haven't created any meetups yet</p>
                  <a href="/meetups/create" class="btn btn-primary">Create Meetup</a>
                </div>
              {:else}
                <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                  {#each userContent.meetups as meetup}
                    <div class="bg-white dark:bg-black rounded-xl shadow-md overflow-hidden border border-purple-100 dark:border-purple-900/50 transition-all duration-200 hover:border-2 hover:border-purple-500 dark:hover:border-purple-400 hover:shadow-lg hover:shadow-purple-500/20 dark:hover:shadow-purple-400/20">
                      <a href={`/meetups/${meetup.id}`} class="block">
                        <div class="h-48 bg-gray-200 dark:bg-black relative overflow-hidden">
                          <img
                            src={meetup.imageUrl || 'https://images.pexels.com/photos/2774556/pexels-photo-2774556.jpeg?auto=compress&cs=tinysrgb&w=800'}
                            alt={meetup.title}
                            class="w-full h-full object-cover"
                          />
                          <div class="absolute top-0 right-0 m-2">
                            <div class="bg-white dark:bg-black text-purple-800 dark:text-purple-300 text-xs font-medium px-2 py-1 rounded shadow border border-purple-100 dark:border-purple-900/50">
                              {new Date(meetup.date).toLocaleDateString()}
                            </div>
                          </div>
                        </div>
                        <div class="p-5">
                          <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-2 line-clamp-1">
                            {meetup.title}
                          </h3>
                          <p class="text-gray-600 dark:text-gray-300 text-sm mb-3 line-clamp-2">
                            {meetup.description || "No description provided."}
                          </p>
                          <div class="flex justify-between items-center text-sm">
                            <div class="text-purple-600 dark:text-purple-400">
                              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 inline mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                              </svg>
                              {meetup.location || "Location TBD"}
                            </div>
                            <div class="text-gray-500 dark:text-gray-400">
                              {meetup.attendees?.length || 0} attending
                            </div>
                          </div>
                        </div>
                      </a>
                    </div>
                  {/each}
                </div>
              {/if}
            {:else if activeTab === 'questions'}
              {#if userContent.questions.length === 0}
                <div class="text-center py-12">
                  <p class="text-gray-500 dark:text-gray-400 mb-4">You haven't asked any questions yet</p>
                  <a href="/questions/create" class="btn btn-primary">Ask Question</a>
                </div>
              {:else}
                <div class="space-y-4">
                  {#each userContent.questions as question}
                    <div class="bg-white dark:bg-black rounded-xl shadow-md overflow-hidden border border-purple-100 dark:border-purple-900/50 transition-all duration-200 hover:border-2 hover:border-purple-500 dark:hover:border-purple-400 hover:shadow-lg hover:shadow-purple-500/20 dark:hover:shadow-purple-400/20">
                      <a href={`/questions/${question.id}`} class="block p-6">
                        <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-2">
                          {question.title}
                        </h3>
                        <p class="text-gray-600 dark:text-gray-300 text-sm mb-3 line-clamp-2">
                          {question.description || "No description provided."}
                        </p>
                        <div class="flex justify-between items-center text-sm">
                          <div class="text-purple-600 dark:text-purple-400">
                            {question.answers?.length || 0} answers
                          </div>
                          <div class="text-gray-500 dark:text-gray-400">
                            {new Date(question.createdAt).toLocaleDateString()}
                          </div>
                        </div>
                      </a>
                    </div>
                  {/each}
                </div>
              {/if}
            {:else if activeTab === 'classifieds'}
              {#if userContent.classifieds.length === 0}
                <div class="text-center py-12">
                  <p class="text-gray-500 dark:text-gray-400 mb-4">You haven't posted any classifieds yet</p>
                  <a href="/classifieds/create" class="btn btn-primary">Post Classified</a>
                </div>
              {:else}
                <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                  {#each userContent.classifieds as classified}
                    <div class="bg-white dark:bg-black rounded-xl shadow-md overflow-hidden border border-purple-100 dark:border-purple-900/50 transition-all duration-200 hover:border-2 hover:border-purple-500 dark:hover:border-purple-400 hover:shadow-lg hover:shadow-purple-500/20 dark:hover:shadow-purple-400/20">
                      <a href={`/classifieds/${classified.id}`} class="block">
                        <div class="h-48 bg-gray-200 dark:bg-black relative overflow-hidden">
                          <img
                            src={classified.imageUrlString || 'https://images.pexels.com/photos/2774556/pexels-photo-2774556.jpeg?auto=compress&cs=tinysrgb&w=800'}
                            alt={classified.title}
                            class="w-full h-full object-cover"
                          />
                          <div class="absolute top-0 right-0 m-2">
                            <div class="bg-white dark:bg-black text-purple-800 dark:text-purple-300 text-xs font-medium px-2 py-1 rounded shadow border border-purple-100 dark:border-purple-900/50">
                              ${classified.price}
                            </div>
                          </div>
                        </div>
                        <div class="p-5">
                          <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-2 line-clamp-1">
                            {classified.title}
                          </h3>
                          <p class="text-gray-600 dark:text-gray-300 text-sm mb-3 line-clamp-2">
                            {classified.description || "No description provided."}
                          </p>
                          <div class="flex justify-between items-center text-sm">
                            <div class="text-purple-600 dark:text-purple-400">
                              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 inline mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                              </svg>
                              {classified.location || "Location TBD"}
                            </div>
                            <div class="text-gray-500 dark:text-gray-400">
                              {classified.category}
                            </div>
                          </div>
                        </div>
                      </a>
                    </div>
                  {/each}
                </div>
              {/if}
            {:else if activeTab === 'polls'}
              {#if userContent.polls.length === 0}
                <div class="text-center py-12">
                  <p class="text-gray-500 dark:text-gray-400 mb-4">You haven't created any polls yet</p>
                  <a href="/polls/create" class="btn btn-primary">Create Poll</a>
                </div>
              {:else}
                <div class="space-y-4">
                  {#each userContent.polls as poll}
                    <div class="bg-white dark:bg-black rounded-xl shadow-md overflow-hidden border border-purple-100 dark:border-purple-900/50 transition-all duration-200 hover:border-2 hover:border-purple-500 dark:hover:border-purple-400 hover:shadow-lg hover:shadow-purple-500/20 dark:hover:shadow-purple-400/20">
                      <a href={`/polls/${poll.id}`} class="block p-6">
                        <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-2">
                          {poll.title}
                        </h3>
                        <p class="text-gray-600 dark:text-gray-300 text-sm mb-3">
                          {poll.description || "No description provided."}
                        </p>
                        <div class="flex justify-between items-center text-sm">
                          <div class="text-purple-600 dark:text-purple-400">
                            {poll.votes?.length || 0} votes
                          </div>
                          <div class="text-gray-500 dark:text-gray-400">
                            {new Date(poll.createdAt).toLocaleDateString()}
                          </div>
                        </div>
                      </a>
                    </div>
                  {/each}
                </div>
              {/if}
            {:else if activeTab === 'emergency'}
              {#if userContent.emergencyMessages.length === 0}
                <div class="text-center py-12">
                  <p class="text-gray-500 dark:text-gray-400 mb-4">You haven't sent any emergency messages yet</p>
                  <a href="/emergency/create" class="btn btn-primary">Send Emergency Message</a>
                </div>
              {:else}
                <div class="space-y-4">
                  {#each userContent.emergencyMessages as message}
                    <div class="bg-white dark:bg-black rounded-xl shadow-md overflow-hidden border border-purple-100 dark:border-purple-900/50 transition-all duration-200 hover:border-2 hover:border-purple-500 dark:hover:border-purple-400 hover:shadow-lg hover:shadow-purple-500/20 dark:hover:shadow-purple-400/20">
                      <div class="p-6">
                        <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-2">
                          {message.title}
                        </h3>
                        <p class="text-gray-600 dark:text-gray-300 text-sm mb-3">
                          {message.description}
                        </p>
                        <div class="flex justify-between items-center text-sm">
                          <div class="text-purple-600 dark:text-purple-400">
                            {message.severity}
                          </div>
                          <div class="text-gray-500 dark:text-gray-400">
                            {new Date(message.createdAt).toLocaleDateString()}
                          </div>
                        </div>
                      </div>
                    </div>
                  {/each}
                </div>
              {/if}
            {/if}
          {/if}
        </div>
      </div>
    </div>
  </div>
</div> 