import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  define: { global: 'globalThis' },
  resolve: {
    // rolldown 版 vite 不会自动添加 .vue 扩展名，需显式声明以支持跨目录组件引用
    extensions: ['.vue', '.mjs', '.js', '.mts', '.ts', '.jsx', '.tsx', '.json']
  },
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
