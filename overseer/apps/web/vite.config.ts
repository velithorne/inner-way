import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

// The dev server proxies /api/* to the local API (apps/api default bind address).
const API_TARGET = 'http://127.0.0.1:4000';

export default defineConfig({
  plugins: [react()],
  server: {
    host: '127.0.0.1',
    port: 5173,
    proxy: {
      '/api': {
        target: API_TARGET,
        changeOrigin: false,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
  build: { outDir: 'dist', sourcemap: true },
});
