import path from 'node:path'
import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'


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
        api: 'modern-compiler',
        loadPaths: [path.resolve(__dirname)],
      },
    },
  },
  test: {
    // 允许在测试文件中直接使用 describe, it, expect 而不需要手动 import
    globals: true,
    environment: 'happy-dom',
    coverage: {
      provider: 'v8', // 或者 'istanbul'
      reporter: ['text', 'lcov', 'clover'], // 必须包含 lcov
      reportsDirectory: './coverage',
      exclude: ['node_modules/**', '.nuxt/**', 'dist/**', '**/*.d.ts'],
    },
  },
})
