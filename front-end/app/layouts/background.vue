<template>
  <div class="bg-color">
    <slot />
    <div class="background" />
  </div>
</template>

<style scoped lang="scss">
.background {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-image: linear-gradient(rgba(223, 234, 250, 0.3), rgba(224, 209, 235, 0.3));
  mask-image: url('/img/background-spot.svg');
  mask-repeat: repeat;
  mask-size: 67px 67px;
  z-index: -1;
  pointer-events: none;
}
.bg-color {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: var(--bg-color);
  z-index: -2;
}
</style>
