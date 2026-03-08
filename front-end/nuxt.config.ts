// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  devtools: { enabled: true },
  ssr: false,
  debug: false,
  modules: ['@nuxt/eslint', '@nuxt/ui', '@nuxt/image'],
  vite: {
    plugins: [
    ],
    server: {
      proxy: {
        '/api': {
          target: 'http://116.233.20.28:8081',
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
