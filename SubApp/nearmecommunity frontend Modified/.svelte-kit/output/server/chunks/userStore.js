function createUserStore() {
  let state = {
    userId: null,
    location: null
  };
  if (typeof localStorage !== "undefined") {
    const storedUserId = localStorage.getItem("userId");
    const storedLocation = localStorage.getItem("userLocation");
    if (storedUserId) {
      state.userId = storedUserId;
    }
    if (storedLocation) {
      try {
        state.location = JSON.parse(storedLocation);
      } catch (e) {
        console.error("Error parsing stored location:", e);
      }
    }
  }
  const subscribers = [];
  function subscribe(callback) {
    subscribers.push(callback);
    callback(state);
    return () => {
      const index = subscribers.indexOf(callback);
      if (index !== -1) {
        subscribers.splice(index, 1);
      }
    };
  }
  function setUserId(userId) {
    state = { ...state, userId };
    subscribers.forEach((callback) => callback(state));
  }
  function setLocation(location) {
    state = { ...state, location };
    subscribers.forEach((callback) => callback(state));
  }
  function clearUser() {
    state = { userId: null, location: null };
    subscribers.forEach((callback) => callback(state));
    if (typeof localStorage !== "undefined") {
      localStorage.removeItem("userId");
      localStorage.removeItem("userLocation");
    }
  }
  function calculateDistance(lat1, lon1, lat2, lon2) {
    const R = 6371;
    const dLat = deg2rad(lat2 - lat1);
    const dLon = deg2rad(lon2 - lon1);
    const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    const d = R * c;
    return d;
  }
  function deg2rad(deg) {
    return deg * (Math.PI / 180);
  }
  function isLocationInRange(targetLat, targetLng) {
    if (!state.location) return false;
    const distance = calculateDistance(
      state.location.latitude,
      state.location.longitude,
      targetLat,
      targetLng
    );
    return distance <= 15;
  }
  return {
    subscribe,
    setUserId,
    setLocation,
    clearUser,
    calculateDistance,
    isLocationInRange,
    get: () => state
  };
}
const user = createUserStore();
export {
  user as u
};
