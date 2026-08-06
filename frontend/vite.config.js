import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  define: { global: 'globalThis' },
  build: {
    // 已按需引入 echarts/图标；vendor 分包后 gzip 约 240KB，属于可接受的演示规模
    chunkSizeWarningLimit: 800
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8088',
        changeOrigin: true
      },
      '/ws': {
        target: 'http://localhost:8088',
        ws: true
      }
    }
  }
})
