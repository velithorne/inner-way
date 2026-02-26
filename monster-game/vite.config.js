import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'

export default defineConfig({
  server: {
    allowedHosts: true,
  },
  plugins: [
    react(),
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['favicon.ico'],
      manifest: {
        name: 'Monster Soul - Your Digital Companion',
        short_name: 'Monster Soul',
        description: 'Create your monster from your face, care for it, and battle in 3D!',
        theme_color: '#1a0a2e',
        background_color: '#0d0218',
        display: 'standalone',
        orientation: 'portrait',
      }
    })
  ],
})
