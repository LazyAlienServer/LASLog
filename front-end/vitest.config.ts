import vue from '@vitejs/plugin-vue'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { defineConfig } from 'vitest/config'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '/img': fileURLToPath(new URL('./public/img', import.meta.url)),
      '~': path.resolve(__dirname, 'app'),
      '@': path.resolve(__dirname, 'app'),
    },
  },
  css: {
    preprocessorOptions: {
      scss: {
        api: 'modern-compiler' as const,
        loadPaths: [path.resolve(__dirname)],
      },
    },
  },
  test: {
    globals: true,
    environment: 'happy-dom',
    coverage: {
      provider: 'v8',
      reporter: ['text', 'lcov', 'clover'],
      reportsDirectory: './coverage',
      exclude: ['node_modules/**', '.nuxt/**', 'dist/**', '**/*.d.ts'],
    },
  },
})
