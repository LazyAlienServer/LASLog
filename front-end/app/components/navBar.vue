<script lang="ts" setup>
</script>

<template>
  <div class="navRoot">
    <div class="navContainer">
      <UIcon name="i-custom-main-nav-home" hover_title="主页" class="size-[1.8rem] navItem focusing" />
      <UIcon name="i-custom-main-nav-exclamation" class="size-[1.7rem] navItem" />
      <UIcon name="i-custom-main-nav-comment" class="size-[1.8rem] navItem" />
      <UIcon name="i-custom-main-nav-users" class="size-[1.7rem] navItem" />
      <UIcon name="i-custom-main-nav-clipboard" class="size-[1.8rem] navItem" />
      <UIcon name="i-custom-main-nav-tray-plus" class="size-[1.7rem] navItem" />
      <UIcon name="i-custom-main-nav-calendar-never" class="size-[1.7rem] navItem" />
      <UIcon name="i-custom-main-nav-folder" class="size-[1.7rem] navItem" />
    </div>
  </div>
</template>

<style lang="scss" scoped>
.navRoot {
  position: fixed;
  display: block;
  height: 2.2rem;
  width: 20rem;
  bottom: 2.8rem;
  background: #fafafc;
  border: 1px solid #746aeb;
  border-radius: 23px;
  box-shadow: 0px 0px 50px 2px #00000033;

  align-content: center;
  justify-items: center;
  z-index: 999999;
}

.navContainer {
  display: flex;
  flex-direction: row;
  width: 95%;
  align-items: center;
  justify-content: space-around;
}

.focusing {
  &::after {
    content: '';
    display: block;
    width: 100%;
    height: 100%;
    background: #746aeb26;
    border-radius: 10px;
  }
}
</style>
