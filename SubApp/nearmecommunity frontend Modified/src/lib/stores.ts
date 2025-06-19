import { writable } from 'svelte/store';

interface User {
  id: number;
  userId: string;
  username: string;
  email: string;
  role: string;
}

export const user = writable<User | null>(null); 