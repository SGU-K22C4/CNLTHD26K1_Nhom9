import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    proxy: {
      '/api/v1/auth': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api/v1/users': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api/v1/products': {
        target: 'http://localhost:8082',
        changeOrigin: true,
      },
      '/api/v1/categories': {
        target: 'http://localhost:8082',
        changeOrigin: true,
      },
      '/api/v1/wishlists': {
        target: 'http://localhost:8082',
        changeOrigin: true,
      },
      '/api/v1/cart': {
        target: 'http://localhost:8083',
        changeOrigin: true,
      },
      '/api/v1/orders': {
        target: 'http://localhost:8084',
        changeOrigin: true,
      },
      '/api/v1/payments': {
        target: 'http://localhost:8084',
        changeOrigin: true,
      },
      '/api/v1/promotions': {
        target: 'http://localhost:8085',
        changeOrigin: true,
      },
      '/api/v1/reviews': {
        target: 'http://localhost:8086',
        changeOrigin: true,
      },
      '/api/v1/chatbot': {
        target: 'http://localhost:8087',
        changeOrigin: true,
      },
    },
  },
})
