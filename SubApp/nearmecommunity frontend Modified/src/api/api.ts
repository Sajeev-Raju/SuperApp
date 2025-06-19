import axios, { AxiosRequestConfig, AxiosResponse, AxiosError } from "axios";
import { user } from "$stores/userStore";
import { get } from "svelte/store";

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "http://localhost:3000/api",
  headers: {
    "Content-Type": "application/json"
  }
});

// Add user ID to requests
api.interceptors.request.use((config: AxiosRequestConfig) => {
  const currentUser = get(user);
  if (currentUser.userId) {
    config.headers = {
      ...config.headers,
      "X-User-ID": currentUser.userId
    };
  }
  return config;
});

// Handle auth errors
api.interceptors.response.use(
  (response: AxiosResponse) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      // Clear user data and redirect to login
      user.clearUser();
      window.location.href = "/";
    }
    return Promise.reject(error);
  }
); 