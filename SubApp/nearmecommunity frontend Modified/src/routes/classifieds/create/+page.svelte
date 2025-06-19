<script lang="ts">
  import { onMount } from "svelte";
  import { user } from "$stores/userStore";
  import { goto } from "$app/navigation";
  import apiClient from "$api/apiClient";
  import LoadingSpinner from "$components/ui/LoadingSpinner.svelte";
  import UserLocationBadge from "$components/ui/UserLocationBadge.svelte";

  let title = "";
  let description = "";
  let price = "";
  let categories: string[] = [];
  let availableCategories: string[] = [
    "Electronics",
    "Furniture",
    "Clothing",
    "Books",
    "Sports",
    "Home & Garden",
    "Toys",
    "Automotive",
    "Jewelry",
    "Art & Collectibles",
    "Musical Instruments",
    "Tools",
    "Pet Supplies",
    "Health & Beauty",
    "Office Supplies"
  ];
  let selectedCategories: string[] = [];
  let imageFile: File | null = null;
  let imagePreview: string | null = null;
  let isLoading = false;
  let error: string | null = null;

  onMount(async () => {
    if (!$user.userId || !$user.location) {
      goto("/");
      return;
    }

    try {
      // Try to get categories from backend
      const backendCategories = await apiClient.classifieds.getCategories();
      // If we get categories from backend, use those instead of defaults
      if (backendCategories && backendCategories.length > 0) {
        availableCategories = backendCategories;
      }
    } catch (err) {
      console.error("Error loading categories:", err);
      // If there's an error, we'll use the default categories
      error = "Using default categories. You can still proceed.";
    }
  });

  function handleImageSelect(event: Event) {
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

  function toggleCategory(category: string) {
    if (selectedCategories.includes(category)) {
      selectedCategories = selectedCategories.filter(c => c !== category);
    } else {
      selectedCategories = [...selectedCategories, category];
    }
  }

  async function handleSubmit() {
    if (!title.trim()) {
      error = "Please enter a title.";
      return;
    }
    if (!description.trim()) {
      error = "Please enter a description.";
      return;
    }
    if (!price || isNaN(Number(price)) || Number(price) <= 0) {
      error = "Please enter a valid price greater than 0.";
      return;
    }
    if (!imageFile) {
      error = "Please select an image.";
      return;
    }
    if (selectedCategories.length === 0) {
      error = "Please select at least one category.";
      return;
    }

    isLoading = true;
    error = null;

    try {
      const formData = new FormData();
      formData.append("classified", JSON.stringify({
        title: title.trim(),
        description: description.trim(),
        price: Number(price),
        categories: selectedCategories
      }));
      formData.append("image", imageFile);

      await apiClient.classifieds.createClassified(formData);
      goto("/classifieds");
    } catch (err) {
      console.error("Error creating classified:", err);
      error = "Failed to create classified. Please try again.";
    } finally {
      isLoading = false;
    }
  }
</script>

<div class="min-h-screen bg-gray-50 dark:bg-black pt-16 pb-20">
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

      <div class="bg-white dark:bg-black rounded-xl shadow-md overflow-hidden">
        <div class="p-6">
          <h1 class="text-3xl font-bold text-gray-900 dark:text-white mb-6">
            Post a Classified
          </h1>

          {#if error}
            <div class="bg-danger-50 dark:bg-danger-900/20 text-danger-800 dark:text-danger-200 p-4 rounded-lg mb-6">
              {error}
            </div>
          {/if}

          <form on:submit|preventDefault={handleSubmit} class="space-y-6">
            <!-- Title -->
            <div>
              <label for="title" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Title
              </label>
              <input
                type="text"
                id="title"
                bind:value={title}
                class="input w-full"
                placeholder="Enter a title for your classified"
                required
              />
            </div>

            <!-- Description -->
            <div>
              <label for="description" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Description
              </label>
              <textarea
                id="description"
                bind:value={description}
                rows="4"
                class="input w-full"
                placeholder="Describe what you're selling"
                required
              ></textarea>
            </div>

            <!-- Price -->
            <div>
              <label for="price" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Price ($)
              </label>
              <input
                type="number"
                id="price"
                bind:value={price}
                class="input w-full"
                placeholder="Enter the price"
                min="0"
                step="0.01"
                required
              />
            </div>

            <!-- Categories -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Categories
              </label>
              <div class="flex flex-wrap gap-2">
                {#each availableCategories as category}
                  <button
                    type="button"
                    on:click={() => toggleCategory(category)}
                    class={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium transition-colors ${
                      selectedCategories.includes(category)
                        ? "bg-primary-100 text-primary-800 dark:bg-primary-900/40 dark:text-primary-300"
                        : "bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600"
                    }`}
                  >
                    {category}
                  </button>
                {/each}
              </div>
            </div>

            <!-- Image Upload -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Image
              </label>
              <div class="mt-1 flex justify-center px-6 pt-5 pb-6 border-2 border-gray-300 dark:border-gray-600 border-dashed rounded-lg">
                <div class="space-y-1 text-center">
                  {#if imagePreview}
                    <img
                      src={imagePreview}
                      alt="Preview"
                      class="mx-auto h-32 w-32 object-cover rounded-lg"
                    />
                  {:else}
                    <svg
                      class="mx-auto h-12 w-12 text-gray-400"
                      stroke="currentColor"
                      fill="none"
                      viewBox="0 0 48 48"
                      aria-hidden="true"
                    >
                      <path
                        d="M28 8H12a4 4 0 00-4 4v20m32-12v8m0 0v8a4 4 0 01-4 4H12a4 4 0 01-4-4v-4m32-4l-3.172-3.172a4 4 0 00-5.656 0L28 28M8 32l9.172-9.172a4 4 0 015.656 0L28 28m0 0l4 4m4-24h8m-4-4v8m-12 4h.02"
                        stroke-width="2"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                      />
                    </svg>
                  {/if}
                  <div class="flex text-sm text-gray-600 dark:text-gray-400">
                    <label
                      for="image-upload"
                      class="relative cursor-pointer bg-white dark:bg-gray-800 rounded-md font-medium text-primary-600 dark:text-primary-400 hover:text-primary-500 dark:hover:text-primary-300 focus-within:outline-none focus-within:ring-2 focus-within:ring-offset-2 focus-within:ring-primary-500"
                    >
                      <span>Upload a file</span>
                      <input
                        id="image-upload"
                        type="file"
                        accept="image/*"
                        class="sr-only"
                        on:change={handleImageSelect}
                      />
                    </label>
                    <p class="pl-1">or drag and drop</p>
                  </div>
                  <p class="text-xs text-gray-500 dark:text-gray-400">
                    PNG, JPG, GIF up to 10MB
                  </p>
                </div>
              </div>
            </div>

            <!-- Submit Button -->
            <div class="flex justify-end">
              <button
                type="submit"
                class="btn btn-primary"
                disabled={isLoading}
              >
                {#if isLoading}
                  <LoadingSpinner size="sm" class="mr-2" />
                  Posting...
                {:else}
                  Post Classified
                {/if}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</div> 