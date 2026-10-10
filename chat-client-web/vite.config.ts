import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import * as sassEmbedded from 'sass-embedded'

// https://vite.dev/config/
export default defineConfig({
  // 依赖导入
  plugins: [
    vue(),
    vueDevTools(),

  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },

  // 依赖预构建：显式列出常用依赖，避免 Vite 启动时自动扫描
  optimizeDeps: {
    include: [
      'vue',
      'pinia',
      'vue-router',
      'axios',
      'element-plus',
      '@element-plus/icons-vue',
    ],
  },

  // SCSS编译配置：使用sass-embedded（Rust实现，比Dart Sass快3-5倍）
  css: {
    preprocessorOptions: {
      scss: {
        api: 'modern-compiler',
        implementation: sassEmbedded,//使用sass-embedded编译scss，关键提速
        silenceDeprecations: ['legacy-js-api', 'import', 'global-builtin'],
      },
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
      // 代理后端文件上传,把/uploads转发到后端
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // 代理WebSocket的请求。
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
        changeOrigin: true,
      },
    },
  },
})
