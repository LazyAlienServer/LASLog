<script lang="ts" setup>
import TwoColumnLayout from '~/components/layouts/TwoColumnLayout.vue'

definePageMeta({
  layout: 'background-home-navbar',
})
</script>

<template>
  <div id="Home" class="flex items-start justify-center">
    <TwoColumnLayout class="w-[88%] pt-10">
      <template #left>
        <Card title="物理机状态" icon="i-custom-placeholder">
          <template #body>
            <div class="pt-2">
              <div>
                <span class="text-gray-500">运行时间：</span>
                <span class="text-gray-700">{{ new Date() }}</span>
              </div>
              <div>
                <span class="text-gray-500">运行时长：</span>
                <span class="text-gray-700">{{ new Date() }}</span>
              </div>
            </div>
          </template>
        </Card>
        <Card title="物理机状态3" icon="i-custom-placeholder">
          <template #subTitle>
            <span id="sub_test">运行正常</span>
          </template>
          <template #body>
            <div class="pt-2">
              <div>
                <span class="text-gray-500">运行时间：</span>
                <span class="text-gray-700">{{ new Date() }}</span>
              </div>
              <div>
                <span class="text-gray-500">运行时长：</span>
                <span class="text-gray-700">{{ new Date() }}</span>
              </div>
            </div>
          </template>
        </Card>
      </template>
      <template #right>
        <Card title="物理机状态2" icon="i-custom-placeholder">
          <template #subTitle>
            <span id="sub_test">运行正常</span>
          </template>
          <template #body>
            <div class="pt-2">
              <div>
                <span class="text-gray-500">运行时间：</span>
                <span class="text-gray-700">{{ new Date() }}</span>
              </div>
              <div>
                <span class="text-gray-500">运行时长：</span>
                <span class="text-gray-700">{{ new Date() }}</span>
              </div>
            </div>
          </template>
        </Card>
        <Card title="物理机状态2" icon="i-custom-placeholder">
          <template #subTitle>
            <span id="sub_test">运行正常</span>
          </template>
          <template #body>
            <div class="pt-2">
              <div>
                <span class="text-gray-500">运行时间：</span>
                <span class="text-gray-700">{{ new Date() }}</span>
              </div>
              <div>
                <span class="text-gray-500">运行时长：</span>
                <span class="text-gray-700">{{ new Date() }}</span>
              </div>
            </div>
          </template>
        </Card>
        <Card title="物理机状态2" icon="i-custom-placeholder">
          <template #subTitle>
            <span id="sub_test">运行正常</span>
          </template>
          <template #body>
            <div class="pt-2">
              <div>
                <span class="text-gray-500">运行时间：</span>
                <span class="text-gray-700">{{ new Date() }}</span>
              </div>
              <div>
                <span class="text-gray-500">运行时长：</span>
                <span class="text-gray-700">{{ new Date() }}</span>
              </div>
            </div>
          </template>
        </Card>
        <Card title="物理机状态" icon="i-custom-placeholder">
          <template #body>
            <div class="pt-2">
              <div>
                <span class="text-gray-500">运行时间：</span>
                <span class="text-gray-700">{{ new Date() }}</span>
              </div>
              <div>
                <span class="text-gray-500">运行时长：</span>
                <span class="text-gray-700">{{ new Date() }}</span>
              </div>
            </div>
          </template>
        </Card>
      </template>
    </TwoColumnLayout>
  </div>
</template>

<style lang="scss" scoped>
#sub_test {
  text-wrap-mode: nowrap;
  padding-left: 3rem;
  color: #5c39a1;
  font-size: 1.8rem;
  line-height: 1.3rem;
  font-weight: 700;
  vertical-align: bottom;
  @media (max-width: 768px) {
    padding-left: 1rem;
  }
}
</style>
