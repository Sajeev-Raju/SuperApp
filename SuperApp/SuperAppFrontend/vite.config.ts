import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';

export default defineConfig({
  plugins: [sveltekit()],
  build: {
    sourcemap: false
  },
  server: {
    fs: {
      strict: false
    },
    host: true, // allow external access
    port: 5173,
    allowedHosts: ['myzip.in'] // this line allows custom domain
  }
});
