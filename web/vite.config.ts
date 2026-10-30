import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import { readFileSync } from 'node:fs'

const pkg = JSON.parse(readFileSync(new URL('./package.json', import.meta.url), 'utf-8'))

// https://vitejs.dev/config/
export default defineConfig({
  define: {
    __APP_VERSION__: JSON.stringify(pkg.version)
  },
  plugins: [
    vue()
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      // 开发环境把 /api 代理到后端，避免跨域配置
      '/api': {
        target: process.env.VITE_PROXY_TARGET || 'http://localhost:8123',
        changeOrigin: true
      }
    }
  },
  css: {
    postcss: {
      // plugins: [tailwindcss, autoprefixer]
    },
    preprocessorOptions: {
      less: {
        modifyVars: {
          hack: `true; @import (reference) "${resolve(
            'src/assets/variables.less'
          )}";`
        },
        // https://lesscss.org/usage/#less-options
        math: 'parens-division',
        javascriptEnabled: true
      }
    }
  },
  build: {
    rollupOptions: {
      output: {
        // 把体积较大的第三方库拆成独立 chunk，避免单个 900kB+ 的入口包
        manualChunks(id) {
          if (!id.includes('node_modules')) return
          const modulePath = id.replace(/\\/g, '/')
          if (modulePath.includes('/ace-builds/')) {
            const file = modulePath.split('/').pop() || ''
            if (file.startsWith('mode-')) return 'vendor-ace-modes'
            if (file.startsWith('theme-')) return 'vendor-ace-themes'
            if (file.startsWith('snippets-')) return 'vendor-ace-snippets'
            if (file.startsWith('ext-')) return 'vendor-ace-ext'
            if (file.startsWith('worker-')) return 'vendor-ace-worker'
            return 'vendor-ace-core'
          }
          if (modulePath.includes('/highlight.js/lib/languages/')) {
            const language = modulePath.split('/').pop() || 'misc'
            return `vendor-hl-${language.charAt(0).toLowerCase()}`
          }
          if (modulePath.includes('/highlight.js/')) return 'vendor-highlight-core'
          if (modulePath.includes('/bytemd/') || modulePath.includes('/@bytemd/')) return 'vendor-bytemd'
          if (modulePath.includes('/@arco-design/web-vue/es/icon/')) return 'vendor-arco-icon'
          if (modulePath.includes('/@arco-design/web-vue/')) {
            const match = modulePath.match(/@arco-design\/web-vue\/es\/([^/]+)/)
            const component = match ? match[1] : 'core'
            return `vendor-arco-${component.replace(/[^a-zA-Z0-9_-]/g, '-')}`
          }
          if (
            modulePath.includes('/vue/') ||
            modulePath.includes('/@vue/') ||
            modulePath.includes('/vue-router/') ||
            modulePath.includes('/pinia/') ||
            modulePath.includes('/pinia-plugin-persistedstate/')
          ) {
            return 'vendor-vue'
          }
          if (modulePath.includes('/axios/')) return 'vendor-axios'
          // 其余依赖按包名拆分，避免一个 vendor-misc 再次膨胀到 500kB+
          const afterNodeModules = modulePath.split('/node_modules/').pop() || ''
          const parts = afterNodeModules.split('/')
          const packageName = parts[0].startsWith('@')
            ? parts.slice(0, 2).join('-').slice(1)
            : parts[0]
          return `vendor-${packageName.replace(/[^a-zA-Z0-9_-]/g, '-')}`
        }
      }
    }
  }
})
