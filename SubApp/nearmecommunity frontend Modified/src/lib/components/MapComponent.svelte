<script lang="ts">
  import { onMount } from 'svelte';
  import { createEventDispatcher } from 'svelte';

  export let apiKey: string = '';
  export let initialLocation: { lat: number; lng: number } = { lat: 17.385044, lng: 78.486671 };
  export let zoom: number = 12;

  const dispatch = createEventDispatcher();
  let map: google.maps.Map;
  let marker: google.maps.Marker | null = null;

  onMount(() => {
    // Load Google Maps script asynchronously
    const script = document.createElement('script');
    script.src = `https://maps.googleapis.com/maps/api/js?key=${apiKey}&callback=initMap`;
    script.async = true;
    script.defer = true;

    (window as any).initMap = () => {
      map = new (window as any).google.maps.Map(document.getElementById('map'), {
        center: initialLocation,
        zoom,
        mapTypeControl: true,
        streetViewControl: false,
        fullscreenControl: true,
        zoomControl: true
      });

      // Add click event listener to map
      map.addListener('click', (event: any) => {
        const clickedLocation = event.latLng;
        if (clickedLocation) {
          placeMarker(clickedLocation);
          dispatch('mapClick', {
            lat: clickedLocation.lat(),
            lng: clickedLocation.lng()
          });
        }
      });
    };

    document.head.appendChild(script);
  });

  function placeMarker(location: google.maps.LatLng) {
    // Remove previous marker if exists
    if (marker) {
      marker.setMap(null);
    }
    
    // Create new marker
    marker = new (window as any).google.maps.Marker({
      position: location,
      map: map,
      animation: (window as any).google.maps.Animation.DROP,
      draggable: true
    });

    // Add drag event listener
    marker.addListener('dragend', () => {
      const position = marker?.getPosition();
      if (position) {
        dispatch('mapClick', {
          lat: position.lat(),
          lng: position.lng()
        });
      }
    });

    // Center map on marker
    map.panTo(location);
  }
</script>

<div id="map" style="width: 100%; height: 100%;"></div> 