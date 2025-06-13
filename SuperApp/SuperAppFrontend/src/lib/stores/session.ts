import { writable } from 'svelte/store';
import Cookies from 'js-cookie';
import { goto } from '$app/navigation';
import { api } from '$lib/api';

interface SessionState {
  username: string | null;
  sessionId: string | null;
  darkMode: boolean;
}

const initialState: SessionState = {
  username: null,
  sessionId: null,
  darkMode: false
};

export const session = writable<SessionState>(initialState);

export function initializeSession() {
  const sessionId = Cookies.get('sessionId');
  const username = Cookies.get('username');

  if (sessionId && username) {
    validateSession(username, sessionId);
  }
}

async function validateSession(username: string, sessionId: string) {
  try {
    const response = await api.validateSession({ username, sessionId });
    if (response.data.valid) {
      session.update(s => ({ ...s, username, sessionId }));
    } else {
      clearSession();
      goto('/login');
    }
  } catch (error) {
    clearSession();
    goto('/login');
  }
}

export function setSession(username: string, sessionId: string) {
  Cookies.set('username', username);
  Cookies.set('sessionId', sessionId);
  session.update(s => ({ ...s, username, sessionId }));
}

export function clearSession() {
  Cookies.remove('username');
  Cookies.remove('sessionId');
  session.update(s => ({ ...s, username: null, sessionId: null }));
}

export function toggleDarkMode() {
  session.update(s => ({ ...s, darkMode: !s.darkMode }));
  if (typeof window !== 'undefined') {
    document.documentElement.classList.toggle('dark');
  }
}