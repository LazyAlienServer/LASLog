/**
 * stores/fingerprint.ts
 *
 * 职责：
 * 存储浏览器指纹（visitorId），作为 clientId 传给后端，
 * 用于区分同一账号在不同设备/浏览器上的登录会话。
 *
 * 生命周期：
 * - 在 plugins/api.ts 初始化阶段生成并写入
 * - 后续所有需要 clientId 的地方从此处读取
 */

import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useFingerprintStore = defineStore('fingerprint', () => {
  /** 浏览器指纹（visitorId），null 表示尚未初始化 */
  const clientId = ref<string | null>(null)

  /** 写入指纹（仅由 plugins/api.ts 调用一次） */
  function setClientId(id: string) {
    clientId.value = id
  }

  /**
   * 读取指纹，未初始化时抛出异常
   * 正常情况下 plugin 已在所有页面逻辑前初始化，不会触发
   */
  function getClientId(): string {
    if (clientId.value === null || clientId.value === '') {
      throw new Error('[fingerprint] clientId 尚未初始化，请检查 plugins/api.ts 是否正确执行')
    }
    return clientId.value
  }

  return { clientId, setClientId, getClientId }
})
