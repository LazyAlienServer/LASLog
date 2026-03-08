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
    return
  }

  try{
    const res = await $fetch<{ code: number, data: { qq: string, direction: number }, msg: string }>(
      `/api/register/activate`,
      { params: { token: token.value } },
    )
    if (res.code === 200 && res.data) {
      qq.value = res.data.qq
      direction.value = directionMap[res.data.direction] ?? '未知'
    }
    else {
      tokenError.value = res.msg || '激活链接无效'
    }
  }
  catch {
    tokenError.value = '激活链接无效或已过期'
  }
  finally {
    loading.value = false
  }
})

// 防抖校验 Minecraft ID
watch(minecraftId, (val) => {
  if (mcIdCheckTimer) clearTimeout(mcIdCheckTimer)
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
  if (!username.value.trim()) return '请输入用户名'
  if (!minecraftId.value.trim()) return '请输入正版 Minecraft ID'
  if (mcIdStatus.value === 'checking') return 'Minecraft ID 正在校验中，请稍候'
  if (mcIdStatus.value === 'invalid') return 'Minecraft ID 无效，请检查后重试'
  if (!password.value) return '请输入密码'
  if (password.value.length < 6) return '密码长度不能少于6位'
  if (password.value !== confirmPassword.value) return '两次输入的密码不一致'
  return null
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
      formSuccess.value = res.msg || '注册成功！即将跳转登录页...'
      setTimeout(() => {
        navigateTo('/login')
      }, 500)
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
      <div class="register_form">
        <!-- Token 无效时显示错误 -->
        <template v-if="tokenError">
          <h1>链接无效</h1>
          <p class="register_subtitle text-red-400">
            {{ tokenError }}
          </p>
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
                <UIcon name="i-heroicons-arrow-path" class="size-[23px] animate-spin" />
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
  height: 1px;
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