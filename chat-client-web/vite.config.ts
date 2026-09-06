import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue(), vueDevTools()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },

  // 代理
  server: {
    port: 5173,
    proxy: {
      // 代理后端 API
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')//别忘了，不然会直接发送http://localhost:8080/api/*给后端，多出来的api/
      },
      // 代理后端文件上传
      '/uploads': {        // ← 新增：把 /uploads 转发到后端
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // 代理 WebSocket
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
        changeOrigin: true,
      },
    },
  },
})
