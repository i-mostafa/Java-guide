import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The browser only ever talks to the Vite dev server (http://localhost:5173).
// Vite forwards `/api/**` and `/.well-known/**` to the Spring Cloud Gateway on :8080,
// which routes them to auth-service / customer-service / application-service.
//
// Because the page and the API share one origin from the browser's point of view,
// no CORS preflight happens and the gateway needs no CORS config. In production the
// same trick is done by nginx (see nginx.conf), so the frontend code always uses
// relative URLs like `/api/customers/me` and never hard-codes a backend host.
const GATEWAY = 'http://localhost:8080';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': { target: GATEWAY, changeOrigin: false },
      '/.well-known': { target: GATEWAY, changeOrigin: false },
    },
  },
  preview: {
    port: 4173,
    proxy: {
      '/api': { target: GATEWAY, changeOrigin: false },
      '/.well-known': { target: GATEWAY, changeOrigin: false },
    },
  },
});
