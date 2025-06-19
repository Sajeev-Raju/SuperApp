<script lang="ts">
  import { onMount } from "svelte";
  import { user } from "$stores/userStore";
  import { goto } from "$app/navigation";
  import apiClient from "$api/apiClient";
  import LoadingSpinner from "$components/ui/LoadingSpinner.svelte";
  import toast from "svelte-french-toast";

  let isLoading = false;
  let error = null;
  let imageFile: File | null = null;
  let imagePreview: string | null = null;

  let formData = {
    title: "",
    description: "",
    startDate: "",
    startTime: "",
    endDate: "",
    endTime: "",
    eventAddress: "",
    googleLocationURL: "",
    contactInfo: "",
    maxParticipants: "",
    tags: [] as string[],
    organizerName: "",
  };

  const popularTags = [
    "Community",
    "Outdoors",
    "Technology",
    "Food",
    "Arts",
    "Sports",
    "Education",
    "Business"
  ];

  onMount(() => {
    if (!$user.userId) {
      goto("/");
      return;
    }
    // Set organizer name from user store
    formData.organizerName = $user.name || "";
  });

  function handleImageChange(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      imageFile = input.files[0];
      const reader = new FileReader();
      reader.onload = (e) => {
        imagePreview = e.target?.result as string;
      };
      reader.readAsDataURL(imageFile);
    }
  }

  function toggleTag(tag: string) {
    if (formData.tags.includes(tag)) {
      formData.tags = formData.tags.filter(t => t !== tag);
    } else {
      formData.tags = [...formData.tags, tag];
    }
  }

  async function handleSubmit() {
    if (!imageFile) {
      toast.error("Please select an image for the meetup");
      return;
    }

    if (!formData.googleLocationURL) {
      toast.error("Please provide a Google Maps URL for the event location");
      return;
    }

    if (!formData.organizerName) {
      toast.error("Please enter your name as the organizer");
      return;
    }

    isLoading = true;
    error = null;

    try {
      const formDataToSend = new FormData();
      formDataToSend.append("image", imageFile);
      
      // Create meetup object with proper date formatting
      const meetupData = {
        ...formData,
        startDate: formData.startDate,
        startTime: formData.startTime,
        endDate: formData.endDate,
        endTime: formData.endTime,
        maxParticipants: formData.maxParticipants ? parseInt(formData.maxParticipants) : null,
        tags: formData.tags.join(", "), // Convert tags array to comma-separated string
        isActive: true
      };
      
      formDataToSend.append("meetup", JSON.stringify(meetupData));

      await apiClient.meetups.createMeetup(formDataToSend);
      toast.success("Meetup created successfully");
      goto("/meetups/my-events");
    } catch (err) {
      console.error("Error creating meetup:", err);
      error = "Failed to create meetup. Please try again.";
    } finally {
      isLoading = false;
    }
  }
</script>

<div class="min-h-screen bg-gray-50 dark:bg-gray-900 pt-16 pb-20">
  <div class="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8">
    <div class="py-6">
      <div class="mb-8">
        <h1 class="text-3xl font-bold text-gray-900 dark:text-white">Create New Meetup</h1>
        <p class="mt-2 text-gray-600 dark:text-gray-400">
          Fill in the details below to create a new meetup
        </p>
      </div>

      {#if error}
        <div class="bg-danger-50 dark:bg-danger-900/20 text-danger-800 dark:text-danger-200 p-4 rounded-lg mb-6">
          {error}
        </div>
      {/if}

      <form on:submit|preventDefault={handleSubmit} class="space-y-6">
        <!-- Image Upload -->
        <div class="bg-white dark:bg-gray-800 rounded-xl shadow-md p-6">
          <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">Meetup Image</h2>
          <div class="space-y-4">
            <div class="flex items-center justify-center w-full">
              <label
                for="image-upload"
                class="flex flex-col items-center justify-center w-full h-64 border-2 border-gray-300 dark:border-gray-600 border-dashed rounded-lg cursor-pointer bg-gray-50 dark:bg-gray-700 hover:bg-gray-100 dark:hover:bg-gray-600"
              >
                {#if imagePreview}
                  <img
                    src={imagePreview}
                    alt="Preview"
                    class="w-full h-full object-cover rounded-lg"
                  />
                {:else}
                  <div class="flex flex-col items-center justify-center pt-5 pb-6">
                    <svg class="w-8 h-8 mb-4 text-gray-500 dark:text-gray-400" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 16">
                      <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 13h3a3 3 0 0 0 0-6h-.025A5.56 5.56 0 0 0 16 6.5 5.5 5.5 0 0 0 5.207 5.021C5.137 5.017 5.071 5 5 5a4 4 0 0 0 0 8h2.167M10 15V6m0 0L8 8m2-2 2 2"/>
                    </svg>
                    <p class="mb-2 text-sm text-gray-500 dark:text-gray-400">
                      <span class="font-semibold">Click to upload</span> or drag and drop
                    </p>
                    <p class="text-xs text-gray-500 dark:text-gray-400">PNG, JPG or JPEG (MAX. 800x400px)</p>
                  </div>
                {/if}
                <input
                  id="image-upload"
                  type="file"
                  accept="image/*"
                  class="hidden"
                  on:change={handleImageChange}
                />
              </label>
            </div>
          </div>
        </div>

        <!-- Basic Information -->
        <div class="bg-white dark:bg-gray-800 rounded-xl shadow-md p-6">
          <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">Basic Information</h2>
          <div class="space-y-4">
            <div>
              <label for="organizerName" class="block text-sm font-medium text-gray-700 dark:text-gray-300">
                Organizer Name <span class="text-danger-500">*</span>
              </label>
              <input
                type="text"
                id="organizerName"
                bind:value={formData.organizerName}
                required
                class="input mt-1 w-full"
                placeholder="Enter your name"
              />
            </div>

            <div>
              <label for="title" class="block text-sm font-medium text-gray-700 dark:text-gray-300">Title</label>
              <input
                type="text"
                id="title"
                bind:value={formData.title}
                required
                class="input mt-1 w-full"
                placeholder="Enter meetup title"
              />
            </div>

            <div>
              <label for="description" class="block text-sm font-medium text-gray-700 dark:text-gray-300">Description</label>
              <textarea
                id="description"
                bind:value={formData.description}
                required
                rows="4"
                class="input mt-1 w-full"
                placeholder="Enter meetup description"
              ></textarea>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label for="startDate" class="block text-sm font-medium text-gray-700 dark:text-gray-300">Start Date</label>
                <input
                  type="date"
                  id="startDate"
                  bind:value={formData.startDate}
                  required
                  class="input mt-1 w-full"
                />
              </div>
              <div>
                <label for="startTime" class="block text-sm font-medium text-gray-700 dark:text-gray-300">Start Time</label>
                <input
                  type="time"
                  id="startTime"
                  bind:value={formData.startTime}
                  required
                  class="input mt-1 w-full"
                />
              </div>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label for="endDate" class="block text-sm font-medium text-gray-700 dark:text-gray-300">End Date</label>
                <input
                  type="date"
                  id="endDate"
                  bind:value={formData.endDate}
                  required
                  class="input mt-1 w-full"
                />
              </div>
              <div>
                <label for="endTime" class="block text-sm font-medium text-gray-700 dark:text-gray-300">End Time</label>
                <input
                  type="time"
                  id="endTime"
                  bind:value={formData.endTime}
                  required
                  class="input mt-1 w-full"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- Location Information -->
        <div class="bg-white dark:bg-gray-800 rounded-xl shadow-md p-6">
          <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">Location Information</h2>
          <div class="space-y-4">
            <div>
              <label for="eventAddress" class="block text-sm font-medium text-gray-700 dark:text-gray-300">Event Address</label>
              <input
                type="text"
                id="eventAddress"
                bind:value={formData.eventAddress}
                required
                class="input mt-1 w-full"
                placeholder="Enter event address"
              />
            </div>

            <div>
              <label for="googleLocationURL" class="block text-sm font-medium text-gray-700 dark:text-gray-300">
                Google Maps URL <span class="text-danger-500">*</span>
              </label>
              <input
                type="url"
                id="googleLocationURL"
                bind:value={formData.googleLocationURL}
                required
                class="input mt-1 w-full"
                placeholder="Enter Google Maps URL (required)"
              />
              <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
                Please provide a Google Maps URL for the event location. This is required for location-based features.
              </p>
            </div>
          </div>
        </div>

        <!-- Additional Information -->
        <div class="bg-white dark:bg-gray-800 rounded-xl shadow-md p-6">
          <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">Additional Information</h2>
          <div class="space-y-4">
            <div>
              <label for="contactInfo" class="block text-sm font-medium text-gray-700 dark:text-gray-300">Contact Information</label>
              <input
                type="text"
                id="contactInfo"
                bind:value={formData.contactInfo}
                class="input mt-1 w-full"
                placeholder="Enter contact information"
              />
            </div>

            <div>
              <label for="maxParticipants" class="block text-sm font-medium text-gray-700 dark:text-gray-300">Maximum Participants</label>
              <input
                type="number"
                id="maxParticipants"
                bind:value={formData.maxParticipants}
                min="1"
                class="input mt-1 w-full"
                placeholder="Enter maximum number of participants"
              />
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Tags</label>
              <div class="flex flex-wrap gap-2">
                {#each popularTags as tag}
                  <button
                    type="button"
                    on:click={() => toggleTag(tag)}
                    class={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium transition-colors ${
                      formData.tags.includes(tag)
                        ? "bg-secondary-100 text-secondary-800 dark:bg-secondary-900/40 dark:text-secondary-300"
                        : "bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600"
                    }`}
                  >
                    {tag}
                  </button>
                {/each}
              </div>
            </div>
          </div>
        </div>

        <div class="flex justify-end gap-4">
          <button
            type="button"
            on:click={() => goto("/meetups")}
            class="btn btn-outline"
          >
            Cancel
          </button>
          <button
            type="submit"
            class="btn btn-primary"
            disabled={isLoading}
          >
            {#if isLoading}
              <div class="flex items-center">
                <LoadingSpinner size="sm" color="white" />
                <span class="ml-2">Creating...</span>
              </div>
            {:else}
              Create Meetup
            {/if}
          </button>
        </div>
      </form>
    </div>
  </div>
</div> 