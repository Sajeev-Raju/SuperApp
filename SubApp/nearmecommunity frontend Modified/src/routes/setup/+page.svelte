<script lang="ts">
  import { onMount } from "svelte";
  import { user } from "$stores/userStore";
  import { goto } from "$app/navigation";
  import { browser } from "$app/environment";
  import toast from "svelte-french-toast";
  import LoadingSpinner from "$components/ui/LoadingSpinner.svelte";

  let mapContainer: HTMLElement;
  let map: google.maps.Map;
  let marker: google.maps.Marker | null = null;
  let selectedLocation: { lat: number; lng: number } | null = null;
  let locationName = "";
  let isLoading = false;
  let scriptLoaded = false;
  let mapInitialized = false;

  // Define the initMap function globally for Google Maps callback
  if (browser) {
    window.initMap = function() {};
  }

  onMount(() => {
    if (browser) {
      // Check if user ID exists in localStorage
      const storedUserId = localStorage.getItem("userId");
      if (!storedUserId) {
        goto("/");
        return;
      }

      user.setUserId(storedUserId);

      // Load Google Maps script
      const script = document.createElement("script");
      script.src = "https://maps.googleapis.com/maps/api/js?key=AIzaSyA_qj85kGT9hBxTy988qafXIGsijaDerII&callback=initMap";
      script.defer = true;
      
      // Define the callback globally
      window.initMap = () => {
        scriptLoaded = true;
        initializeMap();
      };
      
      document.head.appendChild(script);
    }
  });

  function initializeMap() {
    if (!mapContainer || mapInitialized) return;

    try {
      // Default center: Hyderabad (17.385044, 78.486671)
      const hyderabad = { lat: 17.385044, lng: 78.486671 };
      
      map = new google.maps.Map(mapContainer, {
        center: hyderabad,
        zoom: 12,
        mapTypeControl: true,
        streetViewControl: false,
        fullscreenControl: true,
        zoomControl: true,
      });

      // Add click event listener to map
      map.addListener("click", (event: google.maps.MapMouseEvent) => {
        const clickedLocation = event.latLng;
        if (clickedLocation) {
          placeMarker(clickedLocation);
          selectedLocation = {
            lat: clickedLocation.lat(),
            lng: clickedLocation.lng()
          };
        }
      });

      mapInitialized = true;
    } catch (error) {
      console.error("Error initializing map:", error);
      toast.error("Failed to initialize map. Please refresh the page.");
    }
  }

  function placeMarker(location: google.maps.LatLng) {
    // Remove previous marker if exists
    if (marker) {
      marker.setMap(null);
    }
    
    // Create new marker
    marker = new google.maps.Marker({
      position: location,
      map: map,
      animation: google.maps.Animation.DROP,
      draggable: true,
    });

    // Add drag event listener
    marker.addListener("dragend", () => {
      const position = marker?.getPosition();
      if (position) {
        selectedLocation = {
          lat: position.lat(),
          lng: position.lng()
        };
      }
    });

    // Center map on marker
    map.panTo(location);
  }

  async function saveLocation() {
    if (!selectedLocation) {
      toast.error("Please select a location on the map");
      return;
    }

    isLoading = true;

    try {
      const userId = $user.userId;
      if (!userId) {
        throw new Error("User ID not found");
      }

      const response = await fetch("http://localhost:8080/api/location/save", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-User-ID": userId
        },
        body: JSON.stringify({
          userId,
          latitude: selectedLocation.lat,
          longitude: selectedLocation.lng,
          locationName: locationName || "My Location"
        })
      });

      if (!response.ok) {
        throw new Error(`Failed to save location: ${response.status}`);
      }

      // Update user store and localStorage
      user.setLocation({
        latitude: selectedLocation.lat,
        longitude: selectedLocation.lng,
        locationName: locationName || "My Location"
      });

      localStorage.setItem("userLocation", JSON.stringify({
        latitude: selectedLocation.lat,
        longitude: selectedLocation.lng,
        locationName: locationName || "My Location"
      }));

      toast.success("Location saved successfully");
      goto("/dashboard");
    } catch (error) {
      console.error("Error saving location:", error);
      toast.error("Failed to save location. Please try again.");
    } finally {
      isLoading = false;
    }
  }
</script>

<div class="min-h-screen bg-gray-50 dark:bg-gray-900 p-4">
  <div class="max-w-4xl mx-auto">
    <div class="bg-white dark:bg-gray-800 rounded-xl shadow-lg overflow-hidden">
      <div class="p-6">
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white mb-4">Set Your Location</h1>
        <p class="text-gray-600 dark:text-gray-300 mb-6">
          NearMe is a location-based community app. Please select your location on the map below.
          All content will be restricted to a 15km radius around your location.
        </p>
        
        <div 
          bind:this={mapContainer} 
          class="w-full h-96 rounded-lg overflow-hidden mb-6 bg-gray-100 dark:bg-gray-700"
        >
          {#if !scriptLoaded}
            <div class="h-full flex flex-col items-center justify-center">
              <LoadingSpinner size="lg" />
              <p class="mt-4 text-gray-600 dark:text-gray-300">Loading map...</p>
            </div>
          {/if}
        </div>
        
        <div class="mb-6">
          <label for="locationName" class="label">Location Name (Optional)</label>
          <input
            id="locationName"
            type="text"
            bind:value={locationName}
            placeholder="Home, Work, etc."
            class="input"
          />
          <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Give your location a name for easier identification
          </p>
        </div>
        
        <div class="flex space-x-4">
          <button 
            on:click={saveLocation} 
            class="btn btn-primary flex items-center justify-center"
            disabled={isLoading || !selectedLocation}
          >
            {#if isLoading}
              <LoadingSpinner size="sm" color="white" />
              <span class="ml-2">Saving...</span>
            {:else}
              Save Location
            {/if}
          </button>
          
          <button 
            on:click={() => goto("/")}
            class="btn btn-outline"
            disabled={isLoading}
          >
            Cancel
          </button>
        </div>
      </div>
      
      <div class="bg-gray-50 dark:bg-gray-700 px-6 py-4">
        <div class="flex items-center">
          <div class="flex-shrink-0 text-warning-500">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd" />
            </svg>
          </div>
          <div class="ml-3">
            <p class="text-sm text-gray-700 dark:text-gray-300">
              Your location will be used to show relevant community content within a 15km radius.
              You'll only need to set this once.
            </p>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>