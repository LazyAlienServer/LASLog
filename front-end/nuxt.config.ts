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
      ignores: [
        // 1. 测试与契约自动生成的产物
        'pacts/**', // Pact 生成的 JSON 契约文件
        'coverage/**', // Vitest 生成的测试覆盖率报告

        // 2. Nuxt 核心构建产物
        '.nuxt/**', // Nuxt 开发阶段生成的临时文件
        '.output/**', // Nuxt 最终 build 出来的生产环境代码
        'dist/**', // 传统的打包输出目录

        // 3. 日志与其他不需要格式化的文件
        '**/*.log', // 本地运行或测试时产生的各类日志文件
        'node_modules/**', // 依赖包
      ],
    },
  },
  css: ['~/assets/css/main.scss', '~/assets/css/import.css'],
})
