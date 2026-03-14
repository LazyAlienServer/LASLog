<script setup lang="ts">
definePageMeta({
  layout: 'background-home-navbar',
})

// --- 类型定义 ---
interface WhitelistEntry {
  server: string
  status: '已添加' | '未添加' | '封禁'
  banExpireAt?: number
}

interface UserVO {
  uuid: string
  username: string
  qq: number
  minecraftIds: string[]
  minecraftUuids: string[]
  mainMinecraftUuid: string
  registerDate: number
  status: number // 0-待激活 1-已激活 2-封禁
  permission: string[]
  whitelist: string[]
}

interface RecentRegistration {
  qq: string
  direction: number
  minecraftId: string | null
  status: 'WAITING' | 'ACTIVATED' | 'INVALIDATED'
  createTime: number
  expireTime: number
  signature: string
}

interface SubAccountRequest {
  username: string
  subAccountId: string
}

interface WhitelistApplicationVO {
  id: string
  userUuid: string
  username: string
  server: string
  status: string
  createTime: number
}

// --- 后端API获取用户列表 ---
const users = ref<UserVO[]>([])
const usersLoading = ref(false)

// 所有服务器列表（白名单详情展开用）
const allServers = ['Survival', 'Mirror', 'Creative2', 'Creative', 'Storage', 'Void']

// 封禁状态缓存: key = `${minecraftUuid}:${server}`, value = banExpireAt（毫秒，-1=永久，undefined=未封禁）
const banExpireMap = ref<Record<string, number | undefined>>({})

// 格式化注册日期（毫秒时间戳 → yyyy-MM-dd）
function formatDate(timestamp: number): string {
  if (!timestamp)
    return '-'
  const d = new Date(timestamp)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

// 主 Minecraft ID（取第一个）
function getMainAccount(user: UserVO): string {
  return user.minecraftIds?.[0] || '-'
}

// 权限组展示（取第一个权限）
function getGroup(user: UserVO): string {
  return user.permission?.[0] || '-'
}

// 格式化封禁剩余时间
function formatBanRemaining(expireAt: number): string {
  if (expireAt === -1)
    return '永久'
  const diff = expireAt - Date.now()
  if (diff <= 0)
    return '已到期'
  const d = Math.floor(diff / 86400000)
  const h = Math.floor((diff % 86400000) / 3600000)
  const m = Math.floor((diff % 3600000) / 60000)
  if (d > 0)
    return `${d}d${h}h${m}m`
  if (h > 0)
    return `${h}h${m}m`
  return `${m}m`
}

// 获取某个用户+服务器的封禁到期时间
function getBanExpireAt(user: UserVO, server: string): number | undefined {
  const mcUuid = user.mainMinecraftUuid || user.minecraftUuids?.[0]
  if (!mcUuid)
    return undefined
  return banExpireMap.value[`${mcUuid}:${server}`]
}

// 构建白名单详情（基于 allServers、user.whitelist 和封禁缓存）
function getWhitelistEntries(user: UserVO): WhitelistEntry[] {
  const wl = user.whitelist || []
  return allServers.map((server) => {
    const expireAt = getBanExpireAt(user, server)
    if (expireAt !== undefined)
      return { server, status: '封禁' as const, banExpireAt: expireAt }
    return { server, status: wl.includes(server) ? '已添加' as const : '未添加' as const, banExpireAt: undefined }
  })
}

// 批量查询展开行用户的封禁状态
async function fetchBanStatus(user: UserVO) {
  const mcUuid = user.mainMinecraftUuid || user.minecraftUuids?.[0]
  if (!mcUuid)
    return
  for (const server of allServers) {
    try {
      const res = await $fetch<{ code: number, data: { status: number, banExpireAt: number | null } | null }>('/api/whitelist/status', {
        params: { minecraftUuid: mcUuid, server },
      })
      const key = `${mcUuid}:${server}`
      if (res.code === 200 && res.data?.status === -1)
        banExpireMap.value[key] = res.data.banExpireAt ?? -1
      else
        banExpireMap.value[key] = undefined
    }
    catch { /* ignore */ }
  }
}

// --- 搜索 & 分页 ---
const searchQuery = ref('')
const currentPage = ref(1)
const totalItems = ref(0)
const pageSize = ref(10)

// 加载用户列表
async function loadUsers() {
  usersLoading.value = true
  try {
    const params: Record<string, string | number> = {
      page: currentPage.value,
      size: pageSize.value,
    }
    if (searchQuery.value.trim())
      params.search = searchQuery.value.trim()

    const res = await $fetch<{
      code: number
      data: {
        records: UserVO[]
        total: number
        current: number
        size: number
        pages: number
      } | null
      msg: string
    }>('/api/user/list', { params })

    if (res.code === 200 && res.data) {
      users.value = res.data.records
      totalItems.value = res.data.total
    }
  }
  catch {
    users.value = []
    totalItems.value = 0
  }
  finally {
    usersLoading.value = false
  }
}

// --- 右侧: 激活链接生成 ---
const activateQQ = ref('')
const activateDirectionOptions = ['红石', '后勤', '其他']
const activateDirection = ref('红石')
const generatedLink = ref('')
const generateLoading = ref(false)
const generateError = ref('')

// 方向名 → 后端枚举值（索引即枚举值）
const directionMap: Record<string, number> = Object.fromEntries(
  activateDirectionOptions.map((name, i) => [name, i]),
)

async function generateLink() {
  if (!activateQQ.value.trim()) {
    generateError.value = 'QQ号不能为空'
    return
  }

  generateError.value = ''
  generateLoading.value = true

  try {
    const res = await $fetch<{ code: number, data: string | null, msg: string }>(
      '/api/register/generateLink',
      {
        method: 'POST',
        body: {
          qq: activateQQ.value.trim(),
          direction: directionMap[activateDirection.value] ?? 0,
        },
      },
    )
    if (res.code === 200 && res.data) {
      generatedLink.value = res.data
      await loadRecentRegistrations()
    }
    else {
      generateError.value = res.msg || '生成失败'
    }
  }
  catch {
    generateError.value = '请求失败，请检查网络'
  }
  finally {
    generateLoading.value = false
  }
}

// --- 右侧: 最近注册 ---
const recentRegistrations = ref<RecentRegistration[]>([])
const confirmingIndex = ref<number | null>(null)
const now = ref(Date.now())

// 需处理数 = 等待激活中的条目数
const recentRegisterPending = computed(() =>
  recentRegistrations.value.filter(r => r.status === 'WAITING' && r.expireTime > now.value).length,
)

// 格式化剩余时长
function formatRemaining(expireTime: number): string {
  const diff = expireTime - now.value
  if (diff <= 0)
    return '0m'
  const h = Math.floor(diff / 3600000)
  const m = Math.floor((diff % 3600000) / 60000)
  return h > 0 ? `${h}h${m}m` : `${m}m`
}

// 获取最近注册列表
async function loadRecentRegistrations() {
  try {
    const res = await $fetch<{
      code: number
      data: RecentRegistration[] | null
      msg: string
    }>('/api/register/recentRegistrations')
    if (res.code === 200 && res.data)
      recentRegistrations.value = res.data
  }
  catch (e) {
    console.error('loadRecentRegistrations failed:', e)
  }
}

// 状态显示文本
function getStatusText(item: RecentRegistration): string {
  if (item.status === 'ACTIVATED')
    return '已激活'
  if (item.status === 'INVALIDATED' || (item.status === 'WAITING' && item.expireTime <= now.value))
    return '激活链接已失效'
  return `等待激活 ${formatRemaining(item.expireTime)}`
}

// 状态颜色
function getRegStatusColor(item: RecentRegistration): string {
  if (item.status === 'ACTIVATED')
    return '#32A045'
  if (item.status === 'INVALIDATED' || (item.status === 'WAITING' && item.expireTime <= now.value))
    return '#646464'
  return '#646464'
}

// 是否显示下划线（仅等待激活有下划线）
function isWaitingStatus(item: RecentRegistration): boolean {
  return item.status === 'WAITING' && item.expireTime > now.value
}

// 点击状态文字
function onStatusClick(index: number) {
  const item = recentRegistrations.value[index]
  if (!item || !isWaitingStatus(item))
    return

  if (confirmingIndex.value === index) {
    // 第二次点击 → 执行失效
    doInvalidate(item.signature, index)
  }
  else {
    // 第一次点击 → 进入确认态
    confirmingIndex.value = index
  }
}

// 执行失效
async function doInvalidate(signature: string, _index: number) {
  try {
    await $fetch('/api/register/invalidateLink', {
      method: 'POST',
      body: { signature },
    })
    confirmingIndex.value = null
    await loadRecentRegistrations()
  }
  catch (e) {
    console.error('invalidateLink failed:', e)
  }
}

// 点击其他地方时重置确认态
function onDocumentClick() {
  if (confirmingIndex.value !== null)
    confirmingIndex.value = null
}

// 定时刷新
let recentTimer: ReturnType<typeof setInterval> | null = null
let nowTimer: ReturnType<typeof setInterval> | null = null

// --- 右侧: 子账户申请 ---
const subAccountRequests = ref<SubAccountRequest[]>([
  { username: 'tanh丶桁', subAccountId: 'tanh_Heng_2' },
  { username: 'tanh丶桁', subAccountId: 'tanh_Heng_3' },
  { username: 'tanh丶桁', subAccountId: 'tanh_Heng_4' },
])
const subAccountPending = ref(3)

// --- 右侧: 白名单申请 ---
const whitelistRequests = ref<WhitelistApplicationVO[]>([])
const whitelistPending = computed(() => whitelistRequests.value.length)

async function loadWhitelistApplications() {
  try {
    const res = await $fetch<{
      code: number
      data: { items: WhitelistApplicationVO[] } | null
      msg: string
    }>('/api/whitelist/applications/pending')
    if (res.code === 200 && res.data)
      whitelistRequests.value = res.data.items
    else
      whitelistRequests.value = []
  }
  catch {
    whitelistRequests.value = []
  }
}

async function reviewWhitelistApplication(id: string, approve: boolean) {
  try {
    await $fetch(`/api/whitelist/applications/${id}/review`, {
      method: 'POST',
      params: { approve },
    })
    await loadWhitelistApplications()
  }
  catch (e) {
    console.error('reviewWhitelistApplication failed:', e)
  }
}

// --- 分页 ---
const totalPages = computed(() => Math.max(1, Math.ceil(totalItems.value / pageSize.value)))

const visiblePages = computed(() => {
  const pages: (number | '...')[] = []
  const total = totalPages.value
  const current = currentPage.value

  if (total <= 5) {
    for (let i = 1; i <= total; i++) {
      pages.push(i)
    }
    return pages
  }

  // 始终显示第1页
  pages.push(1)

  if (current > 3)
    pages.push('...')

  // 当前页附近
  const start = Math.max(2, current - 1)
  const end = Math.min(total - 1, current + 1)
  for (let i = start; i <= end; i++) {
    pages.push(i)
  }

  if (current < total - 2)
    pages.push('...')

  // 始终显示最后一页
  if (total > 1)
    pages.push(total)

  return pages
})

function goToPage(page: number) {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
    loadUsers()
  }
}

// 搜索防抖
let searchTimer: ReturnType<typeof setTimeout> | null = null
watch(searchQuery, () => {
  if (searchTimer)
    clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    currentPage.value = 1
    loadUsers()
  }, 400)
})

// 页面加载时获取数据
onMounted(() => {
  loadUsers()
  loadRecentRegistrations()
  loadWhitelistApplications()
  // 每10秒刷新最近注册
  recentTimer = setInterval(loadRecentRegistrations, 10000)
  // 每分钟更新倒计时
  nowTimer = setInterval(() => {
    now.value = Date.now()
  }, 60000)
  // 点击其他地方重置确认态
  document.addEventListener('click', onDocumentClick)
})

onUnmounted(() => {
  if (recentTimer)
    clearInterval(recentTimer)
  if (nowTimer)
    clearInterval(nowTimer)
  document.removeEventListener('click', onDocumentClick)
})

// --- 展开/收起白名单详情 (只允许一个展开) ---
const expandedRow = ref<number | null>(null)

function toggleRow(index: number) {
  if (expandedRow.value === index) {
    expandedRow.value = null
  }
  else {
    expandedRow.value = index
    // 展开时查询该用户的封禁状态
    const u = users.value[index]
    if (u)
      fetchBanStatus(u)
  }
}

// --- 封禁弹窗 ---
const banDialog = ref(false)
const banDays = ref(7)
const banTarget = ref<{ user: UserVO, server: string } | null>(null)

function openBanDialog(user: UserVO, server: string) {
  banTarget.value = { user, server }
  banDays.value = 7
  banDialog.value = true
}

function closeBanDialog() {
  banDialog.value = false
  banTarget.value = null
}

// --- 白名单操作 ---
const wlActionLoading = ref<Record<string, boolean>>({})

function wlKey(user: UserVO, server: string) {
  return `${user.uuid}:${server}`
}

async function handleWhitelistAction(user: UserVO, server: string, action: string) {
  const mcUuid = user.mainMinecraftUuid || user.minecraftUuids?.[0]
  if (!mcUuid)
    return
  if (action === '封禁') {
    openBanDialog(user, server)
    return
  }
  const key = wlKey(user, server)
  wlActionLoading.value[key] = true
  try {
    let endpoint: string
    if (action === '添加')
      endpoint = '/api/whitelist/add'
    else if (action === '移除')
      endpoint = '/api/whitelist/remove'
    else
      endpoint = '/api/whitelist/unban'
    await $fetch(endpoint, {
      method: 'POST',
      body: { minecraftUuid: mcUuid, server },
    })
    // 刷新用户列表 + 封禁状态
    await loadUsers()
    const index = expandedRow.value
    const uA1 = index == null ? undefined : users.value[index]

    if (uA1)
      await fetchBanStatus(uA1)
  }
  catch (e) {
    console.error('whitelist action failed:', e)
  }
  finally {
    wlActionLoading.value[key] = false
  }
}

async function confirmBan() {
  if (!banTarget.value)
    return
  const { user, server } = banTarget.value
  const mcUuid = user.mainMinecraftUuid || user.minecraftUuids?.[0]
  if (!mcUuid)
    return
  try {
    await $fetch('/api/whitelist/ban', {
      method: 'POST',
      body: { minecraftUuid: mcUuid, server, banDays: banDays.value },
    })
    closeBanDialog()
    await loadUsers()
    await fetchBanStatus(users.value[expandedRow.value ?? 0])
  }
  catch (e) {
    console.error('ban failed:', e)
  }
}

function getStatusColor(status: string) {
  if (status === '已添加')
    return '#32A045'
  if (status === '封禁')
    return '#C00000'
  return '#646464'
}

function getActions(status: string): { label: string, color: string }[] {
  if (status === '已添加')
    return [{ label: '移除', color: '#E40000' }, { label: '封禁', color: '#C00000' }]
  if (status === '封禁')
    return [{ label: '解封', color: '#646464' }]
  return [{ label: '添加', color: '#32A045' }]
}

// --- 复制链接 ---
const linkCopied = ref(false)
async function copyLink() {
  try {
    await navigator.clipboard.writeText(generatedLink.value)
    linkCopied.value = true
    setTimeout(() => linkCopied.value = false, 2000)
  }
  catch {}
}

// --- 权限组编辑 ---
// 当前正在编辑的行索引，null 表示没有编辑中
const editingPermIndex = ref<number | null>(null)
// 编辑框中的临时文本（逗号分隔多个权限）
const editingPermValue = ref('')

function startEditPerm(index: number, user: UserVO) {
  editingPermIndex.value = index
  editingPermValue.value = (user.permission || []).join(', ')
  nextTick(() => {
    const el = document.getElementById(`perm-input-${index}`)
    if (el)
      (el as HTMLInputElement).focus()
  })
}

async function saveEditPerm(index: number) {
  if (editingPermIndex.value !== index)
    return
  const user = users.value[index]
  if (!user)
    return
  const newPerm = editingPermValue.value
    .split(',')
    .map(s => s.trim())
    .filter(s => s.length > 0)
  editingPermIndex.value = null
  try {
    await $fetch(`/api/user/${user.uuid}/permission`, {
      method: 'PUT',
      body: { permission: newPerm },
    })
    // 乐观更新本地数据
    user.permission = newPerm
  }
  catch (e) {
    console.error('更新权限组失败:', e)
    // 失败时刷新列表恢复真实数据
    await loadUsers()
  }
}

function cancelEditPerm() {
  editingPermIndex.value = null
}
</script>

<template>
  <div id="management_page">
    <div class="page-layout">
      <div class="left-container">
        <div class="management-card">
          <div class="title-bar">
            <div class="title-icon">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                <circle cx="9" cy="7" r="4" />
                <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
                <path d="M16 3.13a4 4 0 0 1 0 7.75" />
              </svg>
            </div>
            <span class="title-text">用户管理</span>
          </div>

          <div class="split-line" />

          <div class="sub-header">
            <h2 class="sub-title">
              所有用户
            </h2>
            <div class="search-area">
              <div class="search-input">
                <input v-model="searchQuery" type="text" placeholder="UUID/用户名/账户搜索">
                <svg class="search-icon" width="16" height="16" viewBox="0 0 16 16" fill="none">
                  <circle cx="7" cy="7" r="5" stroke="#262626" stroke-width="1.6" />
                  <path d="M11 11L14 14" stroke="#262626" stroke-width="1.6" stroke-linecap="round" />
                </svg>
              </div>
            </div>
          </div>

          <div class="table-container">
            <div class="table-header">
              <span class="col-uuid">UUID</span>
              <span class="col-username">用户名</span>
              <span class="col-account">账户</span>
              <span class="col-direction">方向</span>
              <span class="col-group">权限组</span>
              <span class="col-date">注册时间</span>
              <span class="col-whitelist">白名单状态</span>
            </div>

            <div v-for="(user, index) in users" :key="`${user.uuid}-${index}`" class="table-row-wrapper">
              <div class="table-row-line" />
              <div class="table-row">
                <span class="col-uuid cell-text">
                  <span class="uuid-short">{{ user.uuid.substring(0, 8) }}……</span>
                </span>
                <span class="col-username cell-text">{{ user.username }}</span>
                <span class="col-account cell-text">{{ getMainAccount(user) }}</span>
                <span class="col-direction">
                  <span class="direction-tag">
                    {{ getGroup(user) }}
                  </span>
                </span>
                <span class="col-group">
                  <template v-if="editingPermIndex === index">
                    <input
                      :id="`perm-input-${index}`"
                      v-model="editingPermValue"
                      class="perm-input"
                      type="text"
                      placeholder="权限组，逗号分隔"
                      @keydown.enter.prevent="saveEditPerm(index)"
                      @keydown.esc.prevent="cancelEditPerm()"
                      @blur="saveEditPerm(index)"
                    >
                  </template>
                  <template v-else>
                    <span class="group-text">{{ getGroup(user) }}</span>
                    <svg class="edit-icon" width="18" height="18" viewBox="0 0 18 18" fill="none" @click.stop="startEditPerm(index, user)">
                      <path d="M13.5 2.25L15.75 4.5L6.75 13.5H4.5V11.25L13.5 2.25Z" fill="#746aeb" />
                    </svg>
                  </template>
                </span>
                <span class="col-date cell-text">{{ formatDate(user.registerDate) }}</span>
                <span class="col-whitelist whitelist-toggle" @click="toggleRow(index)">
                  <span class="toggle-text">{{ expandedRow === index ? '收起' : '详情' }}</span>
                  <svg
                    class="toggle-arrow"
                    :class="{ rotated: expandedRow === index }"
                    width="20" height="20" viewBox="0 0 20 20" fill="none"
                  >
                    <path d="M5 7.5L10 12.5L15 7.5" stroke="#746aeb" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
                  </svg>
                </span>
              </div>

              <div v-if="expandedRow === index" class="uuid-full-row">
                <div class="uuid-full-gradient" />
              </div>

              <div v-if="expandedRow === index" class="whitelist-panel">
                <div class="wl-header">
                  <span class="wl-col-server">服务器</span>
                  <span class="wl-col-status">白名单状态</span>
                  <span class="wl-col-action">操作</span>
                </div>
                <div v-for="entry in getWhitelistEntries(user)" :key="entry.server" class="wl-row">
                  <span class="wl-col-server wl-server-name">{{ entry.server }}</span>
                  <span class="wl-col-status wl-status-text" :style="{ color: getStatusColor(entry.status) }">
                    {{ entry.status }}
                    <span v-if="entry.status === '封禁' && entry.banExpireAt !== undefined" class="ban-remain">
                      {{ formatBanRemaining(entry.banExpireAt) }}
                    </span>
                  </span>
                  <span class="wl-col-action wl-actions">
                    <a
                      v-for="action in getActions(entry.status)"
                      :key="action.label"
                      href="#"
                      class="action-link"
                      :style="{ color: action.color, opacity: wlActionLoading[wlKey(user, entry.server)] ? 0.5 : 1 }"
                      @click.prevent="handleWhitelistAction(user, entry.server, action.label)"
                    >
                      {{ action.label }}
                    </a>
                  </span>
                </div>
              </div>
            </div>
          </div>

          <div class="pagination">
            <button class="page-prev" :class="{ disabled: currentPage === 1 }" @click="goToPage(currentPage - 1)">
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                <path d="M10 4L6 8L10 12" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
              <span>上一页</span>
            </button>
            <div class="page-list">
              <template v-for="page in visiblePages" :key="page">
                <button
                  v-if="page !== '...'"
                  class="page-btn"
                  :class="{ active: currentPage === page }"
                  @click="goToPage(page as number)"
                >
                  {{ page }}
                </button>
                <span v-else class="page-gap">...</span>
              </template>
            </div>
            <button class="page-next" :class="{ disabled: currentPage === totalPages }" @click="goToPage(currentPage + 1)">
              <span>下一页</span>
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                <path d="M6 4L10 8L6 12" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
            </button>
          </div>
        </div>
      </div>

      <div class="right-container">
        <div class="right-card">
          <div class="card-header">
            <div class="card-title-bar">
              <div class="card-title-icon">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                  <circle cx="8.5" cy="7" r="4" />
                  <polyline points="17,11 19,13 23,9" />
                </svg>
              </div>
              <span class="card-title-text">激活链接生成</span>
            </div>
            <div class="card-split-line" />
          </div>
          <div class="activate-form">
            <div class="activate-input">
              <input v-model="activateQQ" type="text" placeholder="QQ号">
            </div>
            <div class="activate-select">
              <select v-model="activateDirection">
                <option v-for="opt in activateDirectionOptions" :key="opt" :value="opt">
                  {{ opt }}
                </option>
              </select>
              <svg class="chevron-icon" width="16" height="16" viewBox="0 0 16 16" fill="none">
                <path d="M4 6L8 10L12 6" stroke="#262626" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
            </div>
            <button class="activate-btn" :disabled="generateLoading" @click="generateLink">
              {{ generateLoading ? '...' : '生成' }}
            </button>
          </div>
          <div v-if="generateError" class="activate-error">
            {{ generateError }}
          </div>
          <div v-if="generatedLink" class="activate-link-area">
            <a :href="generatedLink" class="activate-link" target="_blank">
              {{ generatedLink }}
            </a>
            <button class="copy-btn" :title="linkCopied ? '已复制' : '复制链接'" @click="copyLink">
              <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="#005eff" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <rect x="9" y="9" width="13" height="13" rx="2" ry="2" />
                <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
              </svg>
            </button>
          </div>
        </div>

        <div class="right-card">
          <div class="card-header">
            <div class="card-title-bar">
              <div class="card-title-icon">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                  <circle cx="8.5" cy="7" r="4" />
                  <polyline points="17,11 19,13 23,9" />
                </svg>
              </div>
              <span class="card-title-text">最近注册</span>
            </div>
            <div class="card-split-line card-split-short" />
            <div class="card-info">
              <span class="card-info-label">需处理：</span>
              <span class="card-info-count">{{ recentRegisterPending }}</span>
            </div>
          </div>
          <div class="mini-table">
            <div class="mini-table-header">
              <span class="mini-col-user">QQ</span>
              <span class="mini-col-id">ID</span>
              <span class="mini-col-status">状态</span>
            </div>
            <div v-for="(item, i) in recentRegistrations" :key="item.signature" class="mini-table-row">
              <span class="mini-col-user mini-cell">{{ item.qq }}</span>
              <span class="mini-col-id mini-cell">{{ item.minecraftId || '-' }}</span>
              <span class="mini-col-status">
                <span
                  class="status-text"
                  :class="{ 'status-waiting': isWaitingStatus(item), 'status-confirming': confirmingIndex === i }"
                  :style="{ color: getRegStatusColor(item) }"
                  @click.stop="onStatusClick(i)"
                >
                  {{ confirmingIndex === i ? '是否确定失效此链接' : getStatusText(item) }}
                </span>
              </span>
            </div>
          </div>
        </div>

        <div class="right-card">
          <div class="card-header">
            <div class="card-title-bar">
              <div class="card-title-icon">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                  <circle cx="8.5" cy="7" r="4" />
                  <polyline points="17,11 19,13 23,9" />
                </svg>
              </div>
              <span class="card-title-text">白名单申请</span>
            </div>
            <div class="card-split-line card-split-short" />
            <div class="card-info">
              <span class="card-info-label">待处理：</span>
              <span class="card-info-count">{{ whitelistPending }}</span>
            </div>
          </div>
          <div class="mini-table">
            <div class="mini-table-header">
              <span class="mini-col-user">用户</span>
              <span class="mini-col-server">服务器</span>
              <span class="mini-col-action">操作</span>
            </div>
            <div v-for="item in whitelistRequests" :key="item.id" class="mini-table-row">
              <span class="mini-col-user mini-cell">{{ item.username }}</span>
              <span class="mini-col-server mini-cell">{{ item.server }}</span>
              <span class="mini-col-action mini-actions">
                <a href="#" class="action-add" @click.prevent="reviewWhitelistApplication(item.id, true)">同意</a>
                <a href="#" class="action-reject" @click.prevent="reviewWhitelistApplication(item.id, false)">拒绝</a>
              </span>
            </div>
          </div>
        </div>

        <div class="right-card">
          <div class="card-header">
            <div class="card-title-bar">
              <div class="card-title-icon">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                  <circle cx="8.5" cy="7" r="4" />
                  <line x1="20" y1="8" x2="20" y2="14" />
                  <line x1="23" y1="11" x2="17" y2="11" />
                </svg>
              </div>
              <span class="card-title-text">子账户申请</span>
            </div>
            <div class="card-split-line card-split-short" />
            <div class="card-info">
              <span class="card-info-label">待处理：</span>
              <span class="card-info-count">{{ subAccountPending }}</span>
            </div>
          </div>
          <div class="mini-table">
            <div class="mini-table-header">
              <span class="mini-col-user">用户</span>
              <span class="mini-col-subid">子账户ID</span>
              <span class="mini-col-action">操作</span>
            </div>
            <div v-for="(item, i) in subAccountRequests" :key="i" class="mini-table-row">
              <span class="mini-col-user mini-cell">{{ item.username }}</span>
              <span class="mini-col-subid mini-cell">{{ item.subAccountId }}</span>
              <span class="mini-col-action mini-actions">
                <a href="#" class="action-add" @click.prevent>添加</a>
                <a href="#" class="action-reject" @click.prevent>拒绝</a>
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 封禁弹窗 -->
    <div v-if="banDialog" class="ban-dialog-mask" @click.self="closeBanDialog">
      <div class="ban-dialog">
        <div class="ban-dialog-title">
          封禁玩家
        </div>
        <div class="ban-dialog-info">
          服务器：<strong>{{ banTarget?.server }}</strong>
        </div>
        <div class="ban-dialog-row">
          <label class="ban-dialog-label" for="ban-days-input">封禁天数（0 = 永久）</label>
          <input id="ban-days-input" v-model.number="banDays" class="ban-dialog-input" type="number" min="0" placeholder="7">
        </div>
        <div class="ban-dialog-actions">
          <button class="ban-dialog-cancel" @click="closeBanDialog">
            取消
          </button>
          <button class="ban-dialog-confirm" @click="confirmBan">
            确认封禁
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
// --- 颜色变量 ---
$primary: #746aeb;
$primary-dark: #5c39a1;
$card-bg: #ffffff;
$text-title: #0f0f0f;
$text-body: #262626;
$text-label: #646464;
$border-light: #d9d9d9;
$tag-red: #e40000;
$tag-green: #32a045;
$link-blue: #005eff;
$accent: #8194f0;

// ========================
// 页面整体布局 (设计稿 1672px, 自动缩放适配屏幕)
// ========================
#management_page {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  min-height: 100vh;
  padding: 40px 0 120px;

  @media (max-width: 1100px) {
    padding: 24px 0 60px;
    width: 100%;
  }
}

.page-layout {
  display: flex;
  gap: 40px;
  align-items: flex-start;
  transform-origin: top center;

  // 根据屏幕宽度自动缩放
  @media (max-width: 1760px) {
    transform: scale(0.85);
    margin-bottom: -150px;
  }

  @media (max-width: 1500px) {
    transform: scale(0.72);
    margin-bottom: -300px;
  }

  @media (max-width: 1280px) {
    transform: scale(0.62);
    margin-bottom: -450px;
  }

  @media (max-width: 1100px) {
    transform: none;
    margin-bottom: 0;
    flex-direction: column;
    width: 100%;
    padding: 0 16px;
    box-sizing: border-box;
    gap: 24px;
  }
}

// ========================
// 左侧: 用户管理
// ========================
.left-container {
  flex-shrink: 0;

  @media (max-width: 1100px) {
    width: 100%;
    flex-shrink: 1;
  }
}

.management-card {
  position: relative;
  width: 1056px;
  height: 800px;
  border-radius: 20px;
  overflow: visible;
  border: 1px solid transparent;
  background:
    linear-gradient(#fff, #fff) padding-box,
    linear-gradient(180deg, rgba(162, 99, 207, 0) 0%, rgba(116, 106, 235, 0.8) 100%) border-box;

  @media (max-width: 1100px) {
    width: 100%;
    height: auto;
    min-height: 600px;
    padding-bottom: 80px;
    box-sizing: border-box;
  }
}

.title-bar {
  position: absolute;
  display: flex;
  align-items: center;
  gap: 11px;
  left: 47px;
  top: 28px;

  @media (max-width: 1100px) {
    position: relative;
    left: auto;
    top: auto;
    padding: 20px 20px 0;
  }
}

.title-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 23px;
  background: $primary;
  border-radius: 5px;
}

.title-text {
  font-family: 'Poppins', sans-serif;
  font-weight: 700;
  font-size: 20px;
  line-height: 30px;
  letter-spacing: 0.05em;
  color: $primary;
}

.split-line {
  position: absolute;
  left: 46px;
  right: 34px;
  top: 71px;
  height: 0;
  border: 2px solid $primary;

  @media (max-width: 1100px) {
    position: relative;
    left: auto;
    right: auto;
    top: auto;
    margin: 8px 20px 0;
  }
}

.sub-header {
  position: absolute;
  left: 45px;
  right: 34px;
  top: 96px;
  display: flex;
  align-items: center;
  justify-content: space-between;

  @media (max-width: 1100px) {
    position: relative;
    left: auto;
    right: auto;
    top: auto;
    padding: 8px 20px 0;
    flex-wrap: wrap;
    gap: 8px;
  }
}

.sub-title {
  font-family: 'Poppins', sans-serif;
  font-weight: 700;
  font-size: 24px;
  line-height: 36px;
  letter-spacing: 0.05em;
  color: $text-title;
}

.search-area {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-select {
  position: relative;
  display: flex;
  align-items: center;
  width: 134px;
  height: 33px;
  background: $card-bg;
  border: 1px solid $border-light;
  border-radius: 8px;
  padding: 0 12px 0 16px;

  select {
    appearance: none;
    border: none;
    outline: none;
    background: transparent;
    font-family: 'Inter', sans-serif;
    font-weight: 400;
    font-size: 16px;
    line-height: 100%;
    color: #b3b3b3;
    width: 100%;
    cursor: pointer;
  }

  .chevron-icon {
    position: absolute;
    right: 12px;
    pointer-events: none;
  }
}

.search-input {
  display: flex;
  align-items: center;
  width: 264px;
  min-width: 120px;
  height: 33px;
  background: $card-bg;
  border: 1px solid $border-light;
  border-radius: 9999px;
  padding: 0 16px;
  gap: 8px;

  @media (max-width: 1100px) {
    width: 100%;
    min-width: 0;
  }

  input {
    border: none;
    outline: none;
    background: transparent;
    font-family: 'Inter', sans-serif;
    font-weight: 400;
    font-size: 16px;
    line-height: 100%;
    color: $text-body;
    flex: 1;

    &::placeholder {
      color: #b3b3b3;
    }
  }

  .search-icon {
    flex-shrink: 0;
  }
}

.table-container {
  position: absolute;
  left: 46px;
  right: 34px;
  top: 146px;
  bottom: 90px;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba($primary, 0.35) transparent;

  &::-webkit-scrollbar {
    width: 4px;
    height: 4px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba($primary, 0.35);
    border-radius: 4px;

    &:hover {
      background: rgba($primary, 0.6);
    }
  }

  @media (max-width: 1100px) {
    position: relative;
    left: auto;
    right: auto;
    top: auto;
    bottom: auto;
    margin: 8px 20px 0;
    overflow-x: auto;
    overflow-y: visible;
  }
}

.col-uuid {
  width: 142px;
  min-width: 77px;

  @media (max-width: 1100px) {
    width: 90px;
    min-width: 60px;
  }
}
.col-username {
  width: 139px;

  @media (max-width: 1100px) {
    width: 90px;
  }
}
.col-account {
  width: 163px;

  @media (max-width: 1100px) {
    width: 110px;
  }
}
.col-direction {
  width: 114px;

  @media (max-width: 1100px) {
    display: none;
  }
}
.col-group {
  width: 121px;
  display: flex;
  align-items: center;
  gap: 5px;

  @media (max-width: 1100px) {
    width: 80px;
  }
}
.col-date {
  width: 151px;

  @media (max-width: 1100px) {
    display: none;
  }
}
.col-whitelist {
  width: 100px;

  @media (max-width: 1100px) {
    width: 70px;
  }
}

.table-header {
  display: flex;
  align-items: center;
  padding-bottom: 10px;

  span {
    font-family: 'Poppins', sans-serif;
    font-weight: 600;
    font-size: 16px;
    line-height: 24px;
    letter-spacing: 0.05em;
    color: $text-label;

    @media (max-width: 1100px) {
      font-size: 13px;
    }
  }
}

.table-row-wrapper {
  position: relative;
}

.table-row-line {
  width: 100%;
  height: 0;
  border-bottom: 1px solid $border-light;
}

.table-row {
  display: flex;
  align-items: center;
  height: 44px;
}

.cell-text {
  font-family: 'Poppins', sans-serif;
  font-weight: 300;
  font-size: 16px;
  line-height: 24px;
  letter-spacing: 0.05em;
  color: $text-body;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;

  @media (max-width: 1100px) {
    font-size: 13px;
  }
}

.uuid-short {
  font-weight: 300;
}

.direction-tag {
  display: inline-block;
  padding: 0 8px;
  height: 21px;
  line-height: 21px;
  border-radius: 15px;
  font-family: 'Poppins', sans-serif;
  font-weight: 500;
  font-size: 16px;
  letter-spacing: 0.05em;
  color: #ffffff;
}

.group-text {
  font-family: 'Poppins', sans-serif;
  font-weight: 400;
  font-size: 16px;
  line-height: 24px;
  letter-spacing: 0.05em;
  color: $primary;

  @media (max-width: 1100px) {
    font-size: 13px;
  }
}

.edit-icon {
  cursor: pointer;
  opacity: 0.8;

  &:hover {
    opacity: 1;
  }
}

.perm-input {
  width: 100%;
  max-width: 110px;
  height: 26px;
  border: 1px solid $primary;
  border-radius: 5px;
  padding: 0 6px;
  font-family: 'Poppins', sans-serif;
  font-weight: 400;
  font-size: 14px;
  color: $text-body;
  outline: none;
  background: #fff;

  &:focus {
    border-color: $primary-dark;
    box-shadow: 0 0 0 2px rgba($primary, 0.15);
  }

  @media (max-width: 1100px) {
    max-width: 76px;
    font-size: 12px;
  }
}

.whitelist-toggle {
  display: flex;
  align-items: center;
  gap: 0;
  cursor: pointer;
  user-select: none;
}

.toggle-text {
  font-family: 'Poppins', sans-serif;
  font-weight: 400;
  font-size: 16px;
  line-height: 24px;
  letter-spacing: 0.05em;
  color: $primary;

  @media (max-width: 1100px) {
    font-size: 13px;
  }
}

.toggle-arrow {
  transition: transform 0.2s ease;
}

.toggle-arrow.rotated {
  transform: rotate(180deg);
}

.uuid-full-row {
  position: absolute;
  left: 0;
  top: 44px;
  height: 30px;
  display: flex;
  align-items: center;
  overflow: hidden;
  max-width: 100%;
  z-index: 5;

  @media (max-width: 1100px) {
    display: none;
  }
}

.uuid-full-text {
  font-family: 'Poppins', sans-serif;
  font-weight: 300;
  font-size: 16px;
  line-height: 24px;
  letter-spacing: 0.05em;
  color: $text-body;
}

.uuid-full-gradient {
  position: absolute;
  top: 0;
  left: 0;
  width: 481px;
  height: 100%;
  background: linear-gradient(90deg, #ffffff 76.51%, rgba(255, 255, 255, 0) 100%);
  pointer-events: none;
}

.whitelist-panel {
  position: absolute;
  right: 0;
  top: 64px;
  z-index: 10;
  width: 302px;
  background: $card-bg;
  border: 1px solid $border-light;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.25);
  border-radius: 5px;
  padding: 9px 14px;

  @media (max-width: 1100px) {
    position: relative;
    right: auto;
    top: auto;
    width: 100%;
    box-sizing: border-box;
    margin-top: 4px;
  }
}

.wl-header {
  display: flex;
  align-items: center;
  margin-bottom: 6px;

  span {
    font-family: 'Poppins', sans-serif;
    font-weight: 400;
    font-size: 14px;
    line-height: 21px;
    color: $text-label;
  }
}

.wl-col-server {
  width: 90px;
}
.wl-col-status {
  width: 90px;
}
.wl-col-action {
  flex: 1;
  min-width: 80px;
}

.wl-row {
  display: flex;
  align-items: center;
  height: 27px;

  span {
    font-family: 'Poppins', sans-serif;
    font-size: 14px;
    line-height: 21px;
  }
}

.wl-server-name {
  font-weight: 400;
  color: $text-title;
}

.wl-status-text {
  font-weight: 300;
}

.ban-remain {
  font-size: 12px;
  font-weight: 400;
  color: #c00000;
  margin-left: 4px;
  opacity: 0.85;
}

.wl-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.action-link {
  font-family: 'Poppins', sans-serif;
  font-weight: 300;
  font-size: 14px;
  line-height: 21px;
  text-decoration: underline;
  cursor: pointer;

  &:hover {
    opacity: 0.8;
  }
}

// --- 分页 ---
.pagination {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  bottom: 39px;
  display: flex;
  align-items: center;
  gap: 8px;

  @media (max-width: 1100px) {
    position: relative;
    left: auto;
    transform: none;
    bottom: auto;
    margin: 16px auto 0;
    justify-content: center;
    flex-wrap: wrap;
  }
}

.page-prev,
.page-next {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 8px 12px;
  height: 32px;
  border: none;
  background: transparent;
  border-radius: 8px;
  cursor: pointer;
  font-family: 'Inter', sans-serif;
  font-weight: 400;
  font-size: 16px;
  line-height: 100%;

  &.disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.page-prev {
  color: $text-label;
}

.page-next {
  color: $primary;
}

.page-list {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 32px;
  padding: 8px 12px;
  border: none;
  background: transparent;
  border-radius: 8px;
  cursor: pointer;
  font-family: 'Inter', sans-serif;
  font-weight: 400;
  font-size: 16px;
  line-height: 100%;
  color: $text-body;

  &.active {
    background: #5046c8;
    /* stylelint-disable-next-line color-contrast */
    color: #ffffff;
  }

  &:hover:not(.active) {
    background: rgba(#5046c8, 0.1);
  }
}

.page-gap {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 47px;
  height: 38px;
  padding: 8px 16px;
  font-family: 'Inter', sans-serif;
  font-weight: 700;
  font-size: 16px;
  line-height: 140%;
  color: $text-title;
}

// ========================
// 右侧: 4个卡片
// ========================
.right-container {
  display: flex;
  flex-direction: column;
  gap: 40px;
  width: 576px;
  flex-shrink: 0;

  @media (max-width: 1100px) {
    width: 100%;
    flex-shrink: 1;
    gap: 24px;
  }
}

.right-card {
  position: relative;
  box-sizing: border-box;
  width: 576px;
  border-radius: 20px;
  padding-bottom: 24px;
  border: 1px solid transparent;
  background:
    linear-gradient(#fff, #fff) padding-box,
    linear-gradient(180deg, rgba(162, 99, 207, 0) 0%, rgba(116, 106, 235, 0.8) 100%) border-box;

  @media (max-width: 1100px) {
    width: 100%;
  }
}

// --- 卡片通用头部 ---
.card-header {
  position: relative;
  padding: 27px 43px 0;

  @media (max-width: 1100px) {
    padding: 20px 20px 0;
  }
}

.card-title-bar {
  display: flex;
  align-items: center;
  gap: 11px;
}

.card-title-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 23px;
  background: $primary;
  border-radius: 5px;
}

.card-title-text {
  font-family: 'Poppins', sans-serif;
  font-weight: 700;
  font-size: 20px;
  line-height: 30px;
  letter-spacing: 0.05em;
  color: $primary;
}

.card-split-line {
  margin-top: 13px;
  height: 0;
  border: 2px solid $primary;
}

.card-split-short {
  margin-right: 184px;
}

.card-info {
  position: absolute;
  right: 43px;
  top: 20px;
  display: flex;
  align-items: baseline;
  gap: 4px;

  @media (max-width: 1100px) {
    right: 20px;
  }
}

.card-info-label {
  font-family: 'Poppins', sans-serif;
  font-weight: 700;
  font-size: 24px;
  line-height: 36px;
  color: $primary-dark;
}

.card-info-count {
  font-family: 'Poppins', sans-serif;
  font-weight: 700;
  font-size: 40px;
  line-height: 60px;
  color: $primary-dark;
}

// --- 卡片1: 激活链接生成 ---
.activate-form {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 19px 42px 0;

  @media (max-width: 1100px) {
    flex-wrap: wrap;
    padding: 16px 20px 0;
    gap: 10px;
  }
}

.activate-input {
  display: flex;
  align-items: center;
  width: 267px;
  height: 33px;
  background: $card-bg;
  border: 1px solid $border-light;
  border-radius: 8px;
  padding: 0 16px;

  @media (max-width: 1100px) {
    width: 100%;
    flex: 1 1 160px;
  }

  input {
    border: none;
    outline: none;
    background: transparent;
    font-family: 'Poppins', sans-serif;
    font-weight: 400;
    font-size: 16px;
    line-height: 100%;
    color: $text-body;
    width: 100%;
  }
}

.activate-select {
  position: relative;
  display: flex;
  align-items: center;
  width: 120px;
  height: 33px;
  background: $card-bg;
  border: 1px solid $border-light;
  border-radius: 8px;
  padding: 0 12px 0 16px;

  select {
    appearance: none;
    border: none;
    outline: none;
    background: transparent;
    font-family: 'Poppins', sans-serif;
    font-weight: 400;
    font-size: 16px;
    line-height: 24px;
    letter-spacing: 0.05em;
    color: $text-body;
    width: 100%;
    cursor: pointer;
  }

  .chevron-icon {
    position: absolute;
    right: 12px;
    pointer-events: none;
  }
}

.activate-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 74px;
  height: 33px;
  border: 1px solid $accent;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  font-family: 'Poppins', sans-serif;
  font-weight: 500;
  font-size: 16px;
  line-height: 24px;
  letter-spacing: 0.05em;
  color: $accent;
  transition: all 0.2s;

  &:hover:not(:disabled) {
    background: rgba($accent, 0.08);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.activate-error {
  padding: 8px 42px 0;
  font-family: 'Poppins', sans-serif;
  font-weight: 400;
  font-size: 14px;
  line-height: 21px;
  color: $tag-red;

  @media (max-width: 1100px) {
    padding: 8px 20px 0;
  }
}

.activate-link-area {
  padding: 11px 42px 0;
  display: flex;
  align-items: flex-end;
  gap: 8px;

  @media (max-width: 1100px) {
    padding: 11px 20px 0;
  }
}

.activate-link {
  font-family: 'Poppins', sans-serif;
  font-weight: 300;
  font-size: 16px;
  line-height: 24px;
  text-decoration: underline;
  color: $link-blue;
  word-break: break-all;
  flex: 1;
}

.copy-btn {
  flex-shrink: 0;
  background: none;
  border: none;
  cursor: pointer;
  padding: 2px;
  display: flex;
  align-items: center;

  &:hover {
    opacity: 0.7;
  }
}

// --- 卡片 2/3/4: 小表格 ---
.mini-table {
  padding: 10px 42px 0;

  @media (max-width: 1100px) {
    padding: 10px 20px 0;
    overflow-x: auto;
    scrollbar-width: thin;
    scrollbar-color: rgba($primary, 0.35) transparent;

    &::-webkit-scrollbar {
      width: 4px;
      height: 4px;
    }

    &::-webkit-scrollbar-track {
      background: transparent;
    }

    &::-webkit-scrollbar-thumb {
      background: rgba($primary, 0.35);
      border-radius: 4px;

      &:hover {
        background: rgba($primary, 0.6);
      }
    }
  }
}

.mini-table-header {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
  min-width: max-content;

  span {
    font-family: 'Poppins', sans-serif;
    font-weight: 600;
    font-size: 16px;
    line-height: 24px;
    letter-spacing: 0.05em;
    color: $text-label;
    flex-shrink: 0;
    white-space: nowrap;
  }
}

.mini-table-row {
  display: flex;
  align-items: center;
  height: 30px;
  min-width: max-content;
}

.mini-cell {
  font-family: 'Poppins', sans-serif;
  font-weight: 400;
  font-size: 16px;
  line-height: 24px;
  letter-spacing: 0.05em;
  color: $text-body;
  flex-shrink: 0;
  white-space: nowrap;
}

// 最近注册列宽
.mini-col-user {
  width: 154px;
}
.mini-col-id {
  width: 178px;
}
.mini-col-status {
  flex: 1;
  min-width: 160px;
  flex-shrink: 0;
  white-space: nowrap;
}
.mini-col-server {
  width: 148px;
}
.mini-col-subid {
  width: 201px;
}
.mini-col-action {
  flex: 1;
  min-width: 80px;
  flex-shrink: 0;
  white-space: nowrap;
}

.status-text {
  font-family: 'Poppins', sans-serif;
  font-weight: 400;
  font-size: 16px;
  line-height: 24px;
  letter-spacing: 0.05em;
  text-decoration: none;
  cursor: default;
}

.status-waiting {
  text-decoration: underline;
  cursor: pointer;
}

.status-confirming {
  color: #e40000 !important;
  text-decoration: underline;
  cursor: pointer;
  font-weight: 500;
}

.mini-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
  white-space: nowrap;
}

.action-add {
  font-family: 'Poppins', sans-serif;
  font-weight: 400;
  font-size: 16px;
  line-height: 24px;
  letter-spacing: 0.05em;
  text-decoration: underline;
  color: $tag-green;
  cursor: pointer;

  &:hover {
    opacity: 0.8;
  }
}

.action-reject {
  font-family: 'Poppins', sans-serif;
  font-weight: 400;
  font-size: 16px;
  line-height: 24px;
  letter-spacing: 0.05em;
  text-decoration: underline;
  color: $tag-red;
  cursor: pointer;

  &:hover {
    opacity: 0.8;
  }
}

// ========================
// 封禁弹窗
// ========================
.ban-dialog-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.ban-dialog {
  background: #fff;
  border-radius: 16px;
  padding: 32px 36px 28px;
  width: 360px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.18);
}

.ban-dialog-title {
  font-family: 'Poppins', sans-serif;
  font-weight: 700;
  font-size: 20px;
  color: $primary;
  margin-bottom: 16px;
}

.ban-dialog-info {
  font-family: 'Poppins', sans-serif;
  font-size: 15px;
  color: $text-body;
  margin-bottom: 18px;
}

.ban-dialog-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 24px;
}

.ban-dialog-label {
  font-family: 'Poppins', sans-serif;
  font-size: 14px;
  color: $text-label;
}

.ban-dialog-input {
  height: 36px;
  border: 1px solid $border-light;
  border-radius: 8px;
  padding: 0 12px;
  font-family: 'Poppins', sans-serif;
  font-size: 16px;
  color: $text-body;
  outline: none;

  &:focus {
    border-color: $primary;
  }
}

.ban-dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.ban-dialog-cancel {
  padding: 8px 20px;
  border: 1px solid $border-light;
  border-radius: 8px;
  background: transparent;
  font-family: 'Poppins', sans-serif;
  font-size: 15px;
  color: $text-label;
  cursor: pointer;

  &:hover {
    background: #f5f5f5;
  }
}

.ban-dialog-confirm {
  padding: 8px 20px;
  border: none;
  border-radius: 8px;
  background: #c00000;
  font-family: 'Poppins', sans-serif;
  font-size: 15px;
  color: #fff;
  cursor: pointer;

  &:hover {
    background: #a00000;
  }
}
</style>
