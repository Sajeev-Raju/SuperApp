import { writable } from 'svelte/store';
import { goto } from '$app/navigation';
import { api } from '$lib/api';

interface SessionState {
  username: string | null;
  sessionId: string | null;
  darkMode: boolean;
  isLoading: boolean;
}

const initialState: SessionState = {
  username: null,
  sessionId: null,
  darkMode: false,
  isLoading: true
};

export const session = writable<SessionState>(initialState);

export async function initializeSession() {
  try {
    // The backend will automatically send cookies with the request
    const response = await api.validateSession();
    if (response.data.valid) {
      session.update(s => ({ 
        ...s, 
        username: response.data.username,
        sessionId: response.data.sessionId,
        isLoading: false 
      }));
    } else {
      clearSession();
    }
  } catch (error) {
    clearSession();
  }
}

export function setSession(username: string, sessionId: string) {
  session.update(s => ({ ...s, username, sessionId }));
}

export function clearSession() {
  session.update(s => ({ ...s, username: null, sessionId: null, isLoading: false }));
}

export function toggleDarkMode() {
  session.update(s => ({ ...s, darkMode: !s.darkMode }));
  if (typeof window !== 'undefined') {
    document.documentElement.classList.toggle('dark');
  }
}