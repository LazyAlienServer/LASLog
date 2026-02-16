<template>
  <div class="bg-color">
    <slot />
    <div class="background" />
  </div>
</template>

<style scoped lang="scss">
@use '~/assets/css/background.scss';

.bg-color {
  background-color: #f8f8f8;
}
</style>
