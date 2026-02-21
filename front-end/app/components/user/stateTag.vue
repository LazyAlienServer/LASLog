<script setup lang="ts">
const props = defineProps<{
  tag: string
  state?: string
  stateColor?: string
}>()
</script>

<template>
  <div class="tag">
    <span class="state" :style="{ color: stateColor }">{{ props.state }}</span>
    <span class="name">{{ props.tag }}</span>
  </div>
</template>

<style lang="scss" scoped>
.tag {
  display: flex;
  flex-direction: row;
  background-color: #fafafa;
  box-shadow: 0px 0px 4px 0px #00000014 inset;

  border-radius: 5px;
}

span {
  padding: 0.2rem 0.7rem;
  font-size: 0.85rem;
}

.state {
  font-weight: 600;
}

.name {
  font-weight: 400;
  color: #0f0f0f;
}
</style>
