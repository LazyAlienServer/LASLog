// @ts-check
import antfu from '@antfu/eslint-config'

import withNuxt from './.nuxt/eslint.config.mjs'

export default withNuxt(
  await antfu({
    name: 'endifield_imitation/antfu',
    formatters: true,
    vue: true, // To enable accessibility in Vue, use the option {a11y: true}
    typescript: {
      tsconfigPath: 'tsconfig.json',
    },
    rules: {
      'perfectionist/sort-imports': ['warn', { type: 'alphabetical' }],
      'vue/block-lang': ['error', { script: { lang: ['ts'] } }],
      'vue/enforce-style-attribute': ['error', { allow: ['scoped', 'module'] }],
    },
  }),
  {
    name: 'endifield_imitation/custom',
    rules: {
      'nuxt/prefer-import-meta': 'error',
    },
  },
)