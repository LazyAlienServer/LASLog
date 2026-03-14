/**
 * middleware/auth.global.ts
 *
 * 全局路由守卫。
 *
 * 执行时机：plugins/api.ts（异步 Plugin）已阻塞完毕，
 * loggedIn 的值已确定（true = 已登录，false = 未登录）。
 *
 * 白名单路由（无需登录即可访问）：
 *   /login、/register、/（首页）
 *
 * 规则：
 *   • 未登录 → 访问受保护路由 → 跳转 /login
 *   • 已登录 → 访问 /login     → 跳转 /
 */

import { authReady, loggedIn } from '~/plugins/api'

// 无需登录即可访问的路由
const PUBLIC_EXACT = new Set(['/', '/login', '/register'])
const PUBLIC_PREFIXES = ['/login/', '/register/']

function isPublic(path: string): boolean {
  if (PUBLIC_EXACT.has(path))
    return true
  return PUBLIC_PREFIXES.some(prefix => path.startsWith(prefix))
}

export default defineNuxtRouteMiddleware(async (to) => {
  // 等待 plugin 初始化完毕（checkAuth 完成），再读 loggedIn
  await authReady

  // 已登录用户访问登录页 → 重定向到首页
  if (loggedIn.value && to.path === '/login') {
    return navigateTo('/dashboard/home')
  }

  // 未登录用户访问受保护路由 → 重定向到登录页
  if (!loggedIn.value && !isPublic(to.path)) {
    return navigateTo('/login')
  }
})
