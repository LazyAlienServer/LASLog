import process from 'node:process'

// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  devtools: { enabled: true },
  ssr: false,
  debug: false,
  modules: ['@nuxt/eslint', '@nuxt/ui', '@nuxt/image', '@pinia/nuxt', '@vueuse/nuxt'],
  // 生产环境代理（nitro 服务端，build 后生效）
  nitro: {
    routeRules: {
      '/api/**': {
        proxy: `${(process.env.API_BASE_URL !== undefined && process.env.API_BASE_URL !== '') ? process.env.API_BASE_URL : 'http://localhost:8081'}/**`,
      },
    },
  },
  // 开发环境代理（仅 nuxt dev 时生效）
  vite: {
    plugins: [
    ],
    server: {
      proxy: {
        '/api': {
          target: (process.env.API_BASE_URL !== undefined && process.env.API_BASE_URL !== '') ? process.env.API_BASE_URL : 'http://116.233.20.28:8081',
          changeOrigin: true,
          rewrite: (path: string) => path.replace(/^\/api/, ''),
        },
      },
    },
  },
  icon: {
    customCollections: [
      {
        prefix: 'custom',
        dir: './app/assets/icons/',
        recursive: true,
      },
    ],
  },
  ui: {
    fonts: false,
  },
  eslint: {
    config: {
      standalone: false,
    },
  },
  css: ['~/assets/css/main.scss', '~/assets/css/import.css'],
})
