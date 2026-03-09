/** @type {import('stylelint').Config} */
export default {
  extends: [
    'stylelint-config-standard-vue', // 基础 Vue CSS 规范
    'stylelint-config-recess-order', // 属性自动排序
  ],
  rules: {
    // 允许空的 <style> 标签
    'no-empty-source': null,
    // 允许使用 Vue 特有的 ::v-deep 等伪类
    'selector-pseudo-class-no-unknown': [
      true,
      {
        ignorePseudoClasses: ['deep', 'global', 'slotted', 'export'],
      },
    ],
    // 允许 Element Plus 等库的自定义标签名
    'selector-type-no-unknown': [
      true,
      {
        ignoreTypes: ['/^el-/', '/^van-/'],
      },
    ],
  },
}
