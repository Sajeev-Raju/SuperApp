export interface UserLocation {
  latitude: number;
  longitude: number;
  locationName?: string;
}

interface UserState {
  userId: string | null;
  name: string | null;
  location: UserLocation | null;
}

function createUserStore() {
  let state: UserState = {
    userId: null,
    name: null,
    location: null
  };

  // Initialize from localStorage if available
  if (typeof localStorage !== 'undefined') {
    const storedUserId = localStorage.getItem('userId');
    const storedName = localStorage.getItem('userName');
    const storedLocation = localStorage.getItem('userLocation');
    
    if (storedUserId) {
      state.userId = storedUserId;
    }
    
    if (storedName) {
      state.name = storedName;
    }
    
    if (storedLocation) {
      try {
        state.location = JSON.parse(storedLocation);
      } catch (e) {
        console.error('Error parsing stored location:', e);
      }
    }
  }

  // Subscribers
  const subscribers: ((state: UserState) => void)[] = [];

  // Get current state
  function get() {
    return state;
  }

  // Subscribe to changes
  function subscribe(callback: (state: UserState) => void) {
    subscribers.push(callback);
    callback(state);
    
    return () => {
      const index = subscribers.indexOf(callback);
      if (index !== -1) {
        subscribers.splice(index, 1);
      }
    };
  }

  // Set user ID
  function setUserId(userId: string) {
    state = { ...state, userId };
    subscribers.forEach(callback => callback(state));
  }

  // Set user name
  function setName(name: string) {
    state = { ...state, name };
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem('userName', name);
    }
    subscribers.forEach(callback => callback(state));
  }

  // Set user location
  function setLocation(location: UserLocation) {
    state = { ...state, location };
    subscribers.forEach(callback => callback(state));
  }

  // Clear user data
  function clearUser() {
    state = { userId: null, name: null, location: null };
    subscribers.forEach(callback => callback(state));
    
    if (typeof localStorage !== 'undefined') {
      localStorage.removeItem('userId');
      localStorage.removeItem('userName');
      localStorage.removeItem('userLocation');
    }
  }

  // Calculate distance between two coordinates in kilometers
  function calculateDistance(lat1: number, lon1: number, lat2: number, lon2: number): number {
    const R = 6371; // Radius of the earth in km
    const dLat = deg2rad(lat2 - lat1);
    const dLon = deg2rad(lon2 - lon1);
    const a = 
      Math.sin(dLat/2) * Math.sin(dLat/2) +
      Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * 
      Math.sin(dLon/2) * Math.sin(dLon/2); 
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)); 
    const d = R * c; // Distance in km
    return d;
  }

  function deg2rad(deg: number): number {
    return deg * (Math.PI/180);
  }

  // Check if a location is within range (15km)
  function isLocationInRange(targetLat: number, targetLng: number): boolean {
    if (!state.location) return false;
    
    const distance = calculateDistance(
      state.location.latitude, 
      state.location.longitude,
      targetLat,
      targetLng
    );
    
    return distance <= 15; // 15km radius
  }

  return {
    subscribe,
    setUserId,
    setName,
    setLocation,
    clearUser,
    calculateDistance,
    isLocationInRange,
    get: () => state
  };
}

export const user = createUserStore();