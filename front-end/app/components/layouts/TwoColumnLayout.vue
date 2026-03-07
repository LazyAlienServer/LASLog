<script setup lang="ts">
const props = withDefaults(defineProps<{
  left?: string
  right?: string
  gap?: string
  rowGap?: string
}>(), {
  left: '55vw',
  right: 'auto',
  gap: '40px',
  rowGap: '2rem',
})
</script>

<template>
  <div class="two-column-layout" :style="{ 'gap': props.gap, '--rowGap': props.rowGap }">
    <div class="left flex flex-col" :style="{ width: props.left, gap: props.rowGap }">
      <slot name="left" />
    </div>
    <div class="right flex flex-col" :style="{ width: props.right, gap: props.rowGap }">
      <slot name="right" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.two-column-layout {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  @media (max-width: 900px) {
    flex-direction: column;
    justify-content: start;
    gap: var(--rowGap) !important;
  }
}

.left,
.right {
  @media (max-width: 900px) {
    width: auto !important;
  }
}
</style>
