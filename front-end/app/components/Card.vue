<script lang="ts" setup>
const props = withDefaults(defineProps<{
  title: string
  icon?: string
  iconColor?: string
  iconSize?: string
}>(), {
  iconColor: '#746AEB',
  iconSize: '70%',
})

const iconClass = computed(() => {
  return {
    backgroundColor: props.iconColor,
    backgroundSize: props.iconSize,
  }
})
</script>

<template>
  <div id="card">
    <div id="content">
      <header id="head">
        <div id="title">
          <UIcon v-if="props.icon" :name="props.icon" :style="iconClass" class="size-5 rounded-[5px] bg-no-repeat bg-center pb-0.5 align-bottom" />
          <span class="title_text">{{ props.title }}</span>
        </div>
        <div id="subtitle">
          <slot name="subTitle" />
        </div>
      </header>
      <div id="body">
        <slot name="body" />
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
#card {
  box-sizing: border-box;
  width: 100%;
  height: min-content;
  border-radius: 20px;
  border: 2px solid transparent;
  justify-items: center;
  overflow: hidden;

  /* 白色卡片背景 + 渐变边框 */
  background:
    linear-gradient(#fff, #fff) padding-box,
    linear-gradient(180deg, rgba(162, 99, 207, 0) 0%, rgba(116, 106, 235, 0.8) 100%) border-box;
}

#head {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  height: min-content;
}

#content {
  width: 100%;
  box-sizing: border-box;
  padding: clamp(12px, 2.5vw, 20px) clamp(10px, 2vw, 16px);
  display: flex;
  flex-direction: column;
}

#title {
  color: #746aeb;
  position: relative;
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 0.65rem;
  padding-bottom: 0.6rem;
  width: 100%;
  height: 100%;
}

#title::after {
  content: '';
  position: absolute;
  width: 100%;
  top: 100%;
  height: 2px;
  background-color: #746aeb;
}

#title .title_text {
  font-size: 1rem;
  letter-spacing: 0.05rem;
  font-weight: 700;
  text-wrap-mode: nowrap;
  transform: translateY(1px);
}
</style>
