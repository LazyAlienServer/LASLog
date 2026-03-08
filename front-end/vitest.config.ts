import vue from '@vitejs/plugin-vue'
import path from 'node:path'
import { defineConfig } from 'vitest/config'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '~': path.resolve(__dirname, 'app'),
      '@': path.resolve(__dirname, 'app'),
    },
  },
  css: {
    preprocessorOptions: {
      scss: {
        // @ts-ignore
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
