/**
 * plugins/api.ts
 *
 * 职责：
 * 1. 生成浏览器指纹，作为 clientId 存入 fingerprintStore
 * 2. 页面挂载前通过 /checkAuth 校验 AT Cookie 是否有效，确定登录状态
 * 3. 封装全局 $api 实例，自动携带 Cookie（credentials: 'include'）
 * 4. 响应 401 时触发无感刷新（RT Cookie → 新 AT Cookie），并发请求通过 Promise 队列防抖
 *
 * 架构说明：
 *   AT 和 RT 均存储在 HttpOnly Cookie 中，前端 JS 无法读取 Token 值。
 *   登录状态通过 loggedIn ref 标记，由 checkAuth 接口确认。
 */

import type { NitroFetchOptions, NitroFetchRequest } from 'nitropack'

import FingerprintJS from '@fingerprintjs/fingerprintjs'
import { ref } from 'vue'

import { useFingerprintStore } from '~/stores/fingerprint'

// ─── 登录状态标记（由 checkAuth 接口确认） ─────────────────────────────────────
export const loggedIn = ref(false)

// ─── 初始化完成信号：middleware 需要 await 此 Promise，确保 plugin 已跑完 ──────
let _authReadyResolve!: () => void
export const authReady: Promise<void> = new Promise<void>((resolve) => {
  _authReadyResolve = resolve
})

// ─── 并发防抖锁 ───────────────────────────────────────────────────────────────
let isRefreshing = false

interface QueueItem {
  resolve: () => void
  reject: (err: unknown) => void
}
let requestsQueue: QueueItem[] = []

function flushQueue() {
  requestsQueue.forEach(item => item.resolve())
  requestsQueue = []
}

function rejectQueue(err: unknown) {
  requestsQueue.forEach(item => item.reject(err))
  requestsQueue = []
}

async function waitForRefresh(): Promise<void> {
  return new Promise<void>((resolve, reject) => {
    requestsQueue.push({ resolve, reject })
  })
}

// ─── 核心刷新函数（通过 RT Cookie 换取新 AT Cookie） ─────────────────────────
async function doRefresh(clientId: string): Promise<void> {
  const res = await $fetch<{ code: number, msg: string }>(
    '/api/login/refreshToken',
    {
      method: 'POST',
      params: { clientId },
      credentials: 'include',
    },
  )
  if (res.code !== 200) {
    throw new Error(res.msg || '刷新 Token 失败')
  }
}

// ─── Plugin 入口 ──────────────────────────────────────────────────────────────
export default defineNuxtPlugin(async (nuxtApp) => {
  // ── 0. 生成浏览器指纹，作为 clientId ───────────────────────────────────────
  const fingerprintStore = useFingerprintStore()
  if (fingerprintStore.clientId === null || fingerprintStore.clientId === '') {
    try {
      const fp = await FingerprintJS.load()
      const result = await fp.get()
      fingerprintStore.setClientId(result.visitorId)
    }
    catch (error) {
      console.error('[fingerprint] 指纹生成失败，降级使用随机 ID:', error)
      fingerprintStore.setClientId(`fallback-${Math.random().toString(36).slice(2)}`)
    }
  }
  const clientId = fingerprintStore.getClientId()

  // ── 1. 初始化：校验 AT Cookie 是否有效 ─────────────────────────────────────
  try {
    const res = await $fetch<{ code: number }>('/api/login/checkAuth', {
      params: { clientId },
      credentials: 'include',
    })
    loggedIn.value = res.code === 200
  }
  catch {
    loggedIn.value = false
  }

  // 如果 AT Cookie 无效但可能有 RT Cookie，尝试刷新一次
  if (!loggedIn.value) {
    try {
      await doRefresh(clientId)
      // 刷新成功，再次校验
      const res = await $fetch<{ code: number }>('/api/login/checkAuth', {
        params: { clientId },
        credentials: 'include',
      })
      loggedIn.value = res.code === 200
    }
    catch {
      loggedIn.value = false
    }
  }

  // ── 2. 封装内部 fetch 实例（自动携带 Cookie） ─────────────────────────────
  const _fetchInstance = $fetch.create({
    credentials: 'include',
  })

  // ── 3. 外层包装函数：实现 401 无感刷新重试 ────────────────────────────────
  const $api: typeof $fetch = (async (
    url: string,
    opts?: NitroFetchOptions<NitroFetchRequest>,
  ) => {
    try {
      return await _fetchInstance(url, opts)
    }
    catch (err: unknown) {
      if (
        err instanceof Error
        && 'status' in err
        && (err as { status: number }).status === 401
      ) {
        const router = useRouter()
        const currentClientId = fingerprintStore.getClientId()

        // ── 情形 A：刷新进行中，挂起等待 ────────────────────────────────
        if (isRefreshing) {
          try {
            await waitForRefresh()
            return await _fetchInstance(url, opts)
          }
          catch {
            throw err
          }
        }

        // ── 情形 B：触发刷新 ─────────────────────────────────────────────
        isRefreshing = true
        try {
          await doRefresh(currentClientId)
          loggedIn.value = true
          flushQueue()
          return await _fetchInstance(url, opts)
        }
        catch (refreshErr) {
          loggedIn.value = false
          rejectQueue(refreshErr)
          await router.push('/login')
          throw refreshErr
        }
        finally {
          isRefreshing = false
        }
      }

      throw err
    }
  }) as unknown as typeof $fetch

  // ── 4. 注入全局，组件内通过 const { $api } = useNuxtApp() 使用 ─────────────
  nuxtApp.provide('api', $api)

  // ── 5. 标记初始化完成，解除 middleware 的等待 ──────────────────────────────
  _authReadyResolve()
})
