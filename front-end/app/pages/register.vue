<script setup lang="ts">
definePageMeta({
  layout: 'background-default',
})

const route = useRoute()
const token = computed(() => (route.query.token as string) || '')

const username = ref('')
const minecraftId = ref('')
const password = ref('')
const confirmPassword = ref('')

const qq = ref('')
const direction = ref('')
const loading = ref(true)
const submitting = ref(false)
const tokenError = ref('')
const formError = ref('')
const formSuccess = ref('')
const mcIdStatus = ref<'idle' | 'checking' | 'valid' | 'invalid'>('idle')
const mcIdMessage = ref('')

// 注册成功动画状态
const registerSuccess = ref(false)
const showCircle = ref(false)
const showCheck = ref(false)
const floatUp = ref(false)
const showSuccessText = ref(false)
const showLoginButton = ref(false)

// 链接失效动画状态
const showErrorCircle = ref(false)
const showCrossLine1 = ref(false)
const showCrossLine2 = ref(false)
const showErrorText = ref(false)

let mcIdCheckTimer: ReturnType<typeof setTimeout> | null = null

const directionMap: Record<number, string> = {
  0: '红石',
  1: '后勤',
  2: '其他',
}

onMounted(async () => {
  if (!token.value) {
    tokenError.value = '缺少激活链接参数'
    loading.value = false
    playErrorAnimation()
    return
  }

  try {
    const res = await $fetch<{ code: number, data: { qq: string, direction: number }, msg: string }>(
      `/api/register/activate`,
      { params: { token: token.value } },
    )
    if (token.value=="test"){
      qq.value = '114514'
      direction.value = '测试'
    }else if (res.code === 200 && res.data) {
      qq.value = res.data.qq
      direction.value = directionMap[res.data.direction] ?? '未知'
    }
    else {
      tokenError.value = res.msg || '激活链接无效'
      playErrorAnimation()
    }
  }
  catch {
    tokenError.value = '激活链接无效或已过期'
    playErrorAnimation()
  }
  finally {
    loading.value = false
  }
})

// 防抖校验 Minecraft ID
watch(minecraftId, (val) => {
  if (mcIdCheckTimer)
    clearTimeout(mcIdCheckTimer)
  formError.value = ''

  if (!val.trim()) {
    mcIdStatus.value = 'idle'
    mcIdMessage.value = ''
    return
  }

  mcIdStatus.value = 'checking'
  mcIdMessage.value = '正在校验...'

  mcIdCheckTimer = setTimeout(async () => {
    try {
      const res = await $fetch<{ code: number, data: string | null, msg: string }>(
        `/api/register/check-mc-id`,
        { params: { username: val.trim() } },
      )
      if (res.code === 200 && res.data) {
        mcIdStatus.value = 'valid'
        mcIdMessage.value = '✓ Minecraft ID 校验通过'
      }
      else {
        mcIdStatus.value = 'invalid'
        mcIdMessage.value = '✗ Minecraft ID 不存在'
      }
    }
    catch {
      mcIdStatus.value = 'invalid'
      mcIdMessage.value = '✗ 校验失败，请稍后再试'
    }
  }, 600)
})

function validateForm(): string | null {
  if (!username.value.trim())
    return '请输入用户名'
  if (!minecraftId.value.trim())
    return '请输入正版 Minecraft ID'
  if (mcIdStatus.value === 'checking')
    return 'Minecraft ID 正在校验中，请稍候'
  if (mcIdStatus.value === 'invalid')
    return 'Minecraft ID 无效，请检查后重试'
  if (!password.value)
    return '请输入密码'
  if (password.value.length < 6)
    return '密码长度不能少于6位'
  if (password.value !== confirmPassword.value)
    return '两次输入的密码不一致'
  return null
}

function playErrorAnimation() {
  // 阶段1: 圆圈出现
  setTimeout(() => {
    showErrorCircle.value = true
  }, 300)

  // 阶段2: 左上→右下叉线
  setTimeout(() => {
    showCrossLine1.value = true
  }, 1000)

  // 阶段3: 右上→左下叉线
  setTimeout(() => {
    showCrossLine2.value = true
  }, 1400)

  // 阶段4: 文字出现
  setTimeout(() => {
    showErrorText.value = true
  }, 1900)
}

function playSuccessAnimation() {
  registerSuccess.value = true

  // 阶段1: 圆圈出现
  setTimeout(() => {
    showCircle.value = true
  }, 300)

  // 阶段2: 打勾动画
  setTimeout(() => {
    showCheck.value = true
  }, 1000)

  // 阶段3: 整体上浮
  setTimeout(() => {
    floatUp.value = true
  }, 2000)

  // 阶段4: 文字出现
  setTimeout(() => {
    showSuccessText.value = true
  }, 2400)

  // 阶段5: 按钮出现
  setTimeout(() => {
    showLoginButton.value = true
  }, 2800)
}

async function handleRegister() {
  formError.value = ''
  formSuccess.value = ''

  const error = validateForm()
  if (error) {
    formError.value = error
    return
  }

  submitting.value = true
  try {
    const res = await $fetch<{ code: number, msg: string }>(
      `/api/register/complete`,
      {
        method: 'POST',
        body: {
          token: token.value,
          username: username.value.trim(),
          minecraftId: minecraftId.value.trim(),
          password: password.value,
        },
      },
    )
    if (res.code === 200) {
      playSuccessAnimation()
    }
    else {
      formError.value = res.msg || '注册失败，请稍后再试'
    }
  }
  catch {
    formError.value = '网络异常，请稍后再试'
  }
  finally {
    submitting.value = false
  }
}
</script>

<template>
  <div id="register_page" class="flex items-center justify-center h-screen">
    <div class="register_card">
      <div class="illustration_wrapper">
        <NuxtImg class="register_illustration" preload src="/img/register.png" alt="register illustration" />
      </div>
      <div class="register_form" :class="{ 'register_form--success': registerSuccess }">
        <!-- 注册成功动画 -->
        <template v-if="registerSuccess">
          <div class="success-container" :class="{ 'float-up': floatUp }">
            <!-- 拆分的 SVG: 圆圈 + 对勾 -->
            <svg
              class="success-svg"
              width="62"
              height="62"
              viewBox="0 0 62 62"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
            >
              <defs>
                <linearGradient id="successGradient" x1="30.75" y1="2" x2="30.75" y2="59.5" gradientUnits="userSpaceOnUse">
                  <stop stop-color="#8295FF"  offset=""/>
                  <stop offset="1" stop-color="#C97CFC" />
                </linearGradient>
              </defs>
              <!-- 圆圈（顺时针绘制） -->
              <path
                class="circle-path"
                :class="{ 'animate-circle': showCircle }"
                d="M42.4513 4.48896C36.7883 1.96567 30.4613 1.34057 24.414 2.70688C18.3668 4.07318 12.9231 7.35769 8.89505 12.0705C4.86697 16.7834 2.47024 22.6721 2.06232 28.8584C1.6544 35.0446 3.25714 41.1971 6.63151 46.398C10.0059 51.599 14.9711 55.5699 20.7866 57.7184C26.6021 59.8669 32.9564 60.078 38.9017 58.3202C44.8469 56.5623 50.0647 52.9298 53.7768 47.9642C57.4889 42.9986 59.4965 36.9662 59.5 30.7665V28.1215"
                stroke="url(#successGradient)"
                stroke-width="4"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
              <!-- 对勾（从左下到右上绘制） -->
              <path
                class="check-path"
                :class="{ 'animate-check': showCheck }"
                d="M22.125 27.9202L30.75 36.5452L59.5 7.76646"
                stroke="url(#successGradient)"
                stroke-width="4"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </div>

          <!-- 文字与按钮 -->
          <div class="success-info" :class="{ show: showSuccessText }">
            <h2 class="success-title">
              您的账号已注册完成
            </h2>
            <p class="success-hint">
              使用 QQ号 或 Minecraft ID 登录
            </p>
            <p class="success-hint">
              首次登录成功后即可获取内群群号
            </p>
          </div>

          <UButton
            class="success-login-btn"
            :class="{ show: showLoginButton }"
            :ui="{ base: 'justify-center button_wrapper hover:opacity-90' }"
            @click="navigateTo('/login')"
          >
            前往登录
          </UButton>
        </template>

        <!-- Token 无效时显示错误动画 -->
        <template v-else-if="tokenError">
          <div class="error-container">
            <svg
              class="error-svg"
              width="62"
              height="62"
              viewBox="0 0 62 62"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
            >
              <!-- 红色圆圈 -->
              <circle
                class="error-circle-path"
                :class="{ 'animate-error-circle': showErrorCircle }"
                cx="31"
                cy="31"
                r="28"
                stroke="#E85454"
                stroke-width="4"
                stroke-linecap="round"
                fill="none"
              />
              <!-- 左上→右下叉线 -->
              <line
                class="cross-line-1"
                :class="{ 'animate-cross-1': showCrossLine1 }"
                x1="20"
                y1="20"
                x2="42"
                y2="42"
                stroke="#E85454"
                stroke-width="4"
                stroke-linecap="round"
              />
              <!-- 右上→左下叉线 -->
              <line
                class="cross-line-2"
                :class="{ 'animate-cross-2': showCrossLine2 }"
                x1="42"
                y1="20"
                x2="20"
                y2="42"
                stroke="#E85454"
                stroke-width="4"
                stroke-linecap="round"
              />
            </svg>
          </div>

          <div class="error-info" :class="{ show: showErrorText }">
            <h2 class="error-title">
              链接无效
            </h2>
            <p class="error-hint">
              {{ tokenError }}
            </p>
          </div>
        </template>

        <!-- 正常注册表单 -->
        <template v-else-if="!loading">
          <h1>注册账号</h1>
          <p class="register_subtitle">
            账号QQ: {{ qq }}&ensp;审核方向: {{ direction }}
          </p>
          <div class="input_group">
            <UInput
              v-model="username"
              type="text"
              icon="i-custom-login-username"
              placeholder="用户名"
              :autofocus="true"
              :ui="{ root: 'relative input_wrapper w-full',
                     base: 'ps-[51px] bg-transparent font-light text-lg ring-0 focus-visible:ring-0',
                     leadingIcon: 'size-[25px] left-[7px]' }"
            />
            <div class="relative w-full">
              <UInput
                v-model="minecraftId"
                type="text"
                icon="i-custom-register-minecraft"
                placeholder="正版 Minecraft ID"
                :ui="{ root: 'relative input_wrapper w-full',
                       base: 'ps-[51px] pe-[30px] bg-transparent font-light text-lg ring-0 focus-visible:ring-0',
                       leadingIcon: 'size-[25px] left-[7px]' }"
              />
              <span v-if="mcIdStatus === 'valid'" class="mc-id-icon">
                <img src="/img/check-icon.svg" alt="valid" width="23" height="23">
              </span>
              <span v-else-if="mcIdStatus === 'invalid'" class="mc-id-icon">
                <img src="/img/infomation-icon.svg" alt="invalid" width="23" height="23">
              </span>
              <span v-else-if="mcIdStatus === 'checking'" class="mc-id-icon text-gray-400">
                <UIcon name="i-heroicons-arrow-path" class="size-5.75 animate-spin" />
              </span>
            </div>
            <UInput
              v-model="password"
              type="password"
              icon="i-custom-login-password"
              placeholder="密码"
              :ui="{ root: 'relative input_wrapper w-full',
                     base: 'ps-[51px] bg-transparent font-light text-lg ring-0 focus-visible:ring-0',
                     leadingIcon: 'size-[25px] left-[7px]' }"
            />
            <UInput
              v-model="confirmPassword"
              type="password"
              icon="i-custom-login-password"
              placeholder="确认密码"
              :ui="{ root: 'relative input_wrapper w-full',
                     base: 'ps-[51px] bg-transparent font-light text-lg ring-0 focus-visible:ring-0',
                     leadingIcon: 'size-[25px] left-[7px]' }"
            />
          </div>
          <p v-if="formError" class="form-message text-red-400">
            {{ formError }}
          </p>
          <p v-if="formSuccess" class="form-message text-green-500">
            {{ formSuccess }}
          </p>
          <UButton
            :loading="submitting"
            :disabled="submitting"
            :ui="{
              base: 'justify-center button_wrapper hover:opacity-90',
            }"
            @click="handleRegister"
          >
            注&ensp;册
          </UButton>
        </template>

        <!-- 加载中 -->
        <template v-else>
          <p class="register_subtitle">
            正在验证激活链接...
          </p>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
$gradient-title: linear-gradient(125.94deg, #8295ff 4.86%, #c97cfc 93.29%);
$gradient-subtitle: linear-gradient(91.92deg, #8295ff 0%, #c97cfc 100%);
$gradient-button: linear-gradient(125.94deg, #e0d1eb 4.86%, #e2b8ff 93.29%);

/* ── Card ── */
.register_card {
  position: fixed;
  display: flex;
  flex-direction: row;
  width: 860px;
  height: 488px;
  background: #fff;
  box-shadow: 0 0 40px rgba(0, 0, 0, 0.25);
  border-radius: 34px;
  overflow: hidden;
}

/* ── Left illustration ── */
.illustration_wrapper {
  position: relative;
  width: 353px;
  height: 100%;
  flex-shrink: 0;
}

.register_illustration {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 37px 0 0 37px;
}

.register_logo {
  position: absolute;
  top: 33px;
  left: 33px;
  width: 3rem;
  height: 3rem;
  z-index: 1;
  filter: brightness(0) invert(1);
}

/* ── Right form area ── */
.register_form {
  position: relative;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0;
  padding: 54px 0 48px;
  transition: background-color 0.6s ease;

  :deep(.button_wrapper) {
    width: 329px;
    height: 46px;
    background: $gradient-button;
    border-radius: 10px;
    border: none;
    cursor: pointer;
    font-family: 'Poppins', sans-serif;
    font-weight: 400;
    font-size: 20px;
    letter-spacing: 20px;
    color: #fff;
  }
}

/* 注册成功：右侧全白 */
.register_form--success {
  background-color: #fff;
}

/* ── Title ── */
.register_form h1 {
  font-family: 'Poppins', sans-serif;
  font-weight: 400;
  font-size: 30px;
  letter-spacing: 10px;
  text-align: center;
  background: $gradient-title;
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  border: none;
  text-shadow: none;
  margin: 0 0 0 10px;
}

/* ── Subtitle (QQ + direction) ── */
.register_subtitle {
  font-family: 'Poppins', sans-serif;
  font-weight: 300;
  font-size: 16px;
  line-height: 32px;
  letter-spacing: 2px;
  text-align: center;
  background: $gradient-subtitle;
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin: 0 0 8px;
}

/* ── Input group ── */
.input_group {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  width: 373px;
  gap: 40px;
  margin-bottom: 24px;
}

.input_wrapper::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 2px;
  transform: scaleY(0.5);
  background: $gradient-title;
}

:deep(.input_wrapper input) {
  font-family: 'Poppins', sans-serif;
  font-weight: 300;
  font-size: 18px;
  line-height: 26px;
  color: #383838;

  &::placeholder {
    color: #808080;
    font-weight: 300;
  }
}

/* ── MC ID validation icon ── */
.mc-id-icon {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  width: 23px;
  height: 23px;
}

/* ── Error / success message ── */
.form-message {
  font-family: 'Poppins', sans-serif;
  font-size: 13px;
  font-weight: 400;
  margin: -8px 0 0;
  width: 373px;
  text-align: left;
}

/* ─────────────────────────────── */
/* ── Success Animation Styles ── */
/* ─────────────────────────────── */

.success-container {
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.8s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.success-container.float-up {
  transform: translateY(-40px);
}

.success-svg {
  width: 62px;
  height: 62px;
}

/* 圆圈: stroke-dasharray 描边动画（顺时针） */
.circle-path {
  stroke-dasharray: 200;
  stroke-dashoffset: 200;
  opacity: 0;
  transition: none;
}

.circle-path.animate-circle {
  opacity: 1;
  stroke-dashoffset: 0;
  transition:
    opacity 0.3s ease,
    stroke-dashoffset 0.8s cubic-bezier(0.65, 0, 0.35, 1);
}

/* 对勾: stroke-dasharray 打勾动画 */
.check-path {
  stroke-dasharray: 60;
  stroke-dashoffset: 60;
  opacity: 0;
  transition: none;
}

.check-path.animate-check {
  opacity: 1;
  stroke-dashoffset: 0;
  transition:
    opacity 0.2s ease,
    stroke-dashoffset 0.5s cubic-bezier(0.65, 0, 0.35, 1);
}

/* 成功文字 */
.success-info {
  text-align: center;
  opacity: 0;
  transform: translateY(10px);
  transition:
    opacity 0.5s ease,
    transform 0.5s ease;
  margin-top: 20px;
}

.success-info.show {
  opacity: 1;
  transform: translateY(0);
}

.success-title {
  font-family: 'Poppins', sans-serif;
  font-weight: 400;
  font-size: 22px;
  letter-spacing: 4px;
  background: $gradient-title;
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin: 0 0 12px;
}

.success-hint {
  font-family: 'Poppins', sans-serif;
  font-weight: 300;
  font-size: 13px;
  line-height: 22px;
  letter-spacing: 1px;
  color: #a0a0a0;
  margin: 0;
}

/* 前往登录按钮 */
.success-login-btn {
  margin-top: 28px;
  opacity: 0;
  transform: translateY(10px);
  transition:
    opacity 0.5s ease,
    transform 0.5s ease;

  :deep(.button_wrapper) {
    letter-spacing: 8px;
    padding-left: 8px;
  }
}

.success-login-btn.show {
  opacity: 1;
  transform: translateY(0);
}

/* ─────────────────────────────── */
/* ── Error Animation Styles ──── */
/* ─────────────────────────────── */

.error-container {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.error-svg {
  width: 62px;
  height: 62px;
}

/* 红色圆圈: stroke-dasharray 描边动画 */
.error-circle-path {
  stroke-dasharray: 176; /* 2 * π * 28 ≈ 176 */
  stroke-dashoffset: 176;
  opacity: 0;
  transition: none;
}

.error-circle-path.animate-error-circle {
  opacity: 1;
  stroke-dashoffset: 0;
  transition:
    opacity 0.3s ease,
    stroke-dashoffset 0.8s cubic-bezier(0.65, 0, 0.35, 1);
}

/* 叉线1: 左上→右下 */
.cross-line-1 {
  stroke-dasharray: 32; /* sqrt((42-20)^2 + (42-20)^2) ≈ 31.1 */
  stroke-dashoffset: 32;
  opacity: 0;
  transition: none;
}

.cross-line-1.animate-cross-1 {
  opacity: 1;
  stroke-dashoffset: 0;
  transition:
    opacity 0.2s ease,
    stroke-dashoffset 0.35s cubic-bezier(0.65, 0, 0.35, 1);
}

/* 叉线2: 右上→左下 */
.cross-line-2 {
  stroke-dasharray: 32;
  stroke-dashoffset: 32;
  opacity: 0;
  transition: none;
}

.cross-line-2.animate-cross-2 {
  opacity: 1;
  stroke-dashoffset: 0;
  transition:
    opacity 0.2s ease,
    stroke-dashoffset 0.35s cubic-bezier(0.65, 0, 0.35, 1);
}

/* 错误文字 */
.error-info {
  text-align: center;
  opacity: 0;
  transform: translateY(10px);
  transition:
    opacity 0.5s ease,
    transform 0.5s ease;
}

.error-info.show {
  opacity: 1;
  transform: translateY(0);
}

.error-title {
  font-family: 'Poppins', sans-serif;
  font-weight: 400;
  font-size: 22px;
  letter-spacing: 4px;
  color: #E85454;
  margin: 0 0 12px;
}

.error-hint {
  font-family: 'Poppins', sans-serif;
  font-weight: 300;
  font-size: 13px;
  line-height: 22px;
  letter-spacing: 1px;
  color: #a0a0a0;
  margin: 0;
}

/* ── Responsive ── */
@media (max-width: 900px) {
  .illustration_wrapper {
    display: none;
  }

  .register_card {
    width: 507px;
  }
}

@media (max-width: 520px) {
  #register_page {
    background-color: white;
  }

  .register_card {
    width: 100%;
    height: 100%;
    border-radius: 0;
    box-shadow: none;
  }

  .input_group {
    width: 85%;
  }
}
</style>
