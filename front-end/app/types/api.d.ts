import type { $Fetch } from 'ofetch'

// 扩展 NuxtApp，使 useNuxtApp().$api 具有完整类型提示
declare module '#app' {
  interface NuxtApp {
    $api: $Fetch
  }
}

declare module 'vue' {
  interface ComponentCustomProperties {
    $api: $Fetch
  }
}

export {}
