<script lang="ts" setup>
import { computed, ref } from 'vue'

import navTitleBg from '~/assets/icons/main/nav-tooltip.svg'

const navItems = [
  { name: 'i-custom-main-nav-home', sizeClass: 'size-[1.8rem]', hoverTitle: '主页' },
  { name: 'i-custom-main-nav-exclamation', sizeClass: 'size-[1.7rem]', hoverTitle: '提醒' },
  { name: 'i-custom-main-nav-comment', sizeClass: 'size-[1.8rem]', hoverTitle: '评论' },
  { name: 'i-custom-main-nav-users', sizeClass: 'size-[1.7rem]', hoverTitle: '成员' },
  { name: 'i-custom-main-nav-clipboard', sizeClass: 'size-[1.8rem]', hoverTitle: '任务' },
  { name: 'i-custom-main-nav-tray-plus', sizeClass: 'size-[1.7rem]', hoverTitle: '新建' },
  { name: 'i-custom-main-nav-calendar-never', sizeClass: 'size-[1.7rem]', hoverTitle: '日历' },
  { name: 'i-custom-main-nav-folder', sizeClass: 'size-[1.7rem]', hoverTitle: '文件' },
]

const activeIndex = ref(0)
const hoverIndex = ref(0)
const hoverVisible = ref(false)

const navContainerStyle = computed(() => ({
  '--active-index': `${activeIndex.value}`,
  '--item-count': `${navItems.length}`,
}))

const tooltipText = computed(() => {
  return navItems[hoverIndex.value]?.hoverTitle || ''
})

const tooltipStyle = computed(() => ({
  '--hover-index': `${hoverIndex.value}`,
}))

function handleClick(index: number) {
  activeIndex.value = index
}

function handleHover(index: number) {
  hoverIndex.value = index
  hoverVisible.value = true
}

function clearHover() {
  hoverVisible.value = false
}
</script>

<template>
  <div class="navRoot">
    <div class="navContainer" :style="navContainerStyle">
      <div class="navTooltip" :class="{ navTooltipVisible: hoverVisible }" :style="tooltipStyle">
        <img :src="navTitleBg" alt="" class="navTooltipBg">
        <span class="navTooltipText">{{ tooltipText }}</span>
      </div>
      <div class="focusing" />
      <button
        v-for="(item, index) in navItems"
        :key="item.name"
        type="button"
        class="navBtn"
        @click="handleClick(index)"
        @mouseenter="handleHover(index)"
        @mouseleave="clearHover"
      >
        <UIcon
          :name="item.name"
          :hover_title="item.hoverTitle"
          class="navItem"
          :class="[item.sizeClass, { navItemActive: index === activeIndex }]"
        />
      </button>
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
  position: relative;
  display: flex;
  flex-direction: row;
  width: 95%;
  align-items: center;
  justify-content: space-between;
}

.navTooltip {
  position: absolute;
  z-index: 3;
  left: calc((100% / var(--item-count)) * (var(--hover-index) + 0.5));
  bottom: calc(100% - 1.9rem);
  width: 6.625rem;
  height: 5.875rem;
  pointer-events: none;
  opacity: 0;
  transform: translate(-50%, 0.4rem);
  transition:
    opacity 180ms ease,
    transform 240ms cubic-bezier(0.22, 1, 0.36, 1);
}

.navTooltipVisible {
  opacity: 1;
  transform: translate(-50%, 0);
}

.navTooltipBg {
  width: 100%;
  height: 100%;
  display: block;
}

.navTooltipText {
  position: absolute;
  left: 50.5%;
  top: 2.05rem;
  transform: translateX(-50%);
  width: 3.4rem;
  text-align: center;
  font-size: 0.9rem;
  letter-spacing: 0.1rem;
  line-height: 1;
  color: #746aeb;
  white-space: nowrap;
}

.navBtn {
  position: relative;
  z-index: 2;
  width: calc(100% / var(--item-count));
  height: 2rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  padding: 0;
  cursor: pointer;
}

.navItem:hover {
  filter: brightness(0) saturate(100%) invert(45%) sepia(39%) saturate(1570%) hue-rotate(214deg) brightness(97%)
    contrast(87%);
}

.navItemActive {
  filter: brightness(0) saturate(100%) invert(45%) sepia(39%) saturate(1570%) hue-rotate(214deg) brightness(97%)
    contrast(87%);
}

.focusing {
  position: absolute;
  z-index: 1;
  left: calc((100% / var(--item-count)) * var(--active-index) + 0.225rem);
  top: 50%;
  width: calc((100% / var(--item-count)) - 0.45rem);
  height: 1.8rem;
  border-radius: 10px;
  background: #746aeb26;
  transform: translateY(-50%);
  transition: left 260ms cubic-bezier(0.22, 1, 0.36, 1);
}
</style>
