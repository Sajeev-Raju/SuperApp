import { writable } from 'svelte/store';
import { browser } from '$app/environment';

function createThemeStore() {
  const { subscribe, set, update } = writable('light');

  return {
    subscribe,
    toggleTheme: () => {
      if (browser) {
        update(current => {
          const newTheme = current === 'dark' ? 'light' : 'dark';
          if (newTheme === 'dark') {
            document.documentElement.classList.add('dark');
          } else {
            document.documentElement.classList.remove('dark');
          }
          localStorage.setItem('theme', newTheme);
          return newTheme;
        });
      }
    },
    initialize: () => {
      if (browser) {
        // Check localStorage for saved theme preference
        const savedTheme = localStorage.getItem('theme');
        if (savedTheme === 'dark') {
          document.documentElement.classList.add('dark');
          set('dark');
        } else {
          document.documentElement.classList.remove('dark');
          set('light');
          localStorage.setItem('theme', 'light');
        }
      }
    }
  };
}

export const theme = createThemeStore();