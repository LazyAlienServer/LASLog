// @ts-check
import antfu from '@antfu/eslint-config'

import withNuxt from './.nuxt/eslint.config.mjs'

export default withNuxt(
  await antfu({
    name: 'laslog/antfu',

    ignores: [
      'pacts/**',
      'coverage/**',
      '.nuxt/**',
      '.output/**',
      'dist/**',
    ],

    formatters: true,
    vue: true,

    typescript: {
      tsconfigPath: 'tsconfig.json',
      parserOptions: {
        projectService: {
          // 不写 '*.ts'，避开了和 nuxt.config.ts 的冲突
          allowDefaultProject: ['vitest.config.ts', '*.js'],
        },
      },
    },

    rules: {
      'perfectionist/sort-imports': ['warn', { type: 'alphabetical' }],
      'vue/block-lang': ['error', { script: { lang: ['ts'] } }],
      'vue/enforce-style-attribute': ['error', { allow: ['scoped', 'module'] }],
    },
  }),
  {
    name: 'laslog/custom',
    rules: {
      'nuxt/prefer-import-meta': 'error',
    },
  },
  // vitest严格校验关闭
  {
    files: ['vitest.config.ts'],
    rules: {
      'ts/strict-boolean-expressions': 'off',
    },
  },
)
