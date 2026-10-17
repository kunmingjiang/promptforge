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
  },
  build: {
    rollupOptions: {
      output: {
        // 大依赖手动分包：避免单个主 chunk 过大，利于浏览器并行加载与长缓存
        manualChunks: {
          'vendor-vue': ['vue', 'vue-router', 'pinia', 'vue-i18n'],
          'vendor-element': ['element-plus', '@element-plus/icons-vue'],
          'vendor-marked': ['marked']
        }
      }
    }
  }
})
