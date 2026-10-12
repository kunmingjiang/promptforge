import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      // 开发期直连本地后端；生产环境由 Nginx 统一反代
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
