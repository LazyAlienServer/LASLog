<script setup lang="ts">
import NavBar from '~/components/navBar.vue'
</script>

<template>
  <div class="bg-color overflow-y-scroll">
    <slot />
    <div class="background" />
    <NavBar class="navbar" />
  </div>
</template>

<style scoped lang="scss">
@use '~/assets/css/background.scss';

.bg-color {
  background-color: #f8f8f8;
}

.navbar {
  left: 50vw;
  transform: translateX(-50%);
}
</style>
