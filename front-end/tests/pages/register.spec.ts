import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { computed, defineComponent, h, nextTick, onMounted, ref, watch } from 'vue'

// ─── Component stubs ───
import RegisterPage from '../../app/pages/register.vue'
// ─── Mocks (must be set up before importing the component) ───

// Expose Vue composition APIs as globals (Nuxt auto-imports them)
vi.stubGlobal('computed', computed)
vi.stubGlobal('ref', ref)
vi.stubGlobal('onMounted', onMounted)
vi.stubGlobal('watch', watch)

const mockFetch = vi.fn()
vi.stubGlobal('$fetch', mockFetch)

const mockNavigateTo = vi.fn()
vi.stubGlobal('navigateTo', mockNavigateTo)

vi.stubGlobal('definePageMeta', vi.fn())

const mockRouteQuery = ref<Record<string, string>>({})
vi.stubGlobal('useRoute', () => ({ query: mockRouteQuery.value }))

const UInputStub = defineComponent({
  name: 'UInput',
  props: ['modelValue', 'type', 'icon', 'placeholder', 'autofocus', 'ui'],
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    return () =>
      h('input', {
        value: props.modelValue,
        type: props.type,
        placeholder: props.placeholder,
        onInput: (e: Event) => emit('update:modelValue', (e.target as HTMLInputElement).value),
      })
  },
})

const UButtonStub = defineComponent({
  name: 'UButton',
  props: ['loading', 'disabled', 'ui'],
  emits: ['click'],
  setup(props, { slots, emit }) {
    return () =>
      h(
        'button',
        { disabled: props.disabled === true ? true : undefined, onClick: () => emit('click') },
        slots.default?.(),
      )
  },
})

const UIconStub = defineComponent({
  name: 'UIcon',
  props: ['name'],
  setup(props) {
    return () => h('span', { class: 'u-icon' }, props.name as string)
  },
})

const NuxtImgStub = defineComponent({
  name: 'NuxtImg',
  props: ['src', 'alt', 'preload'],
  setup(props) {
    return () => h('img', { src: props.src, alt: props.alt })
  },
})

// ─── Helpers ───

const stubs = {
  UInput: UInputStub,
  UButton: UButtonStub,
  UIcon: UIconStub,
  NuxtImg: NuxtImgStub,
}

function createWrapper() {
  return mount(RegisterPage, {
    global: {
      stubs,
      mocks: { navigateTo: mockNavigateTo },
    },
  })
}

async function setInput(wrapper: ReturnType<typeof mount>, index: number, value: string) {
  const input = wrapper.findAll('input')[index]
  ;(input.element as HTMLInputElement).value = value
  await input.trigger('input')
}

// ─── Tests ───

describe('register.vue', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    mockFetch.mockReset()
    mockNavigateTo.mockReset()
    mockRouteQuery.value = {}
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  // ── 1. Token validation on mount ──

  describe('token validation on mount', () => {
    it('shows error when token is missing from query', async () => {
      mockRouteQuery.value = {}
      const wrapper = createWrapper()
      await flushPromises()

      expect(wrapper.text()).toContain('链接无效')
      expect(wrapper.text()).toContain('缺少激活链接参数')
      expect(mockFetch).not.toHaveBeenCalled()
    })

    it('shows error when token is empty string', async () => {
      mockRouteQuery.value = { token: '' }
      const wrapper = createWrapper()
      await flushPromises()

      expect(wrapper.text()).toContain('缺少激活链接参数')
    })

    it('shows loading text before API resolves', async () => {
      mockRouteQuery.value = { token: 'abc' }
      mockFetch.mockReturnValueOnce(new Promise(() => {})) // never resolves

      const wrapper = createWrapper()
      await nextTick()

      expect(wrapper.text()).toContain('正在验证激活链接...')
      expect(wrapper.find('.input_group').exists()).toBe(false)
    })

    it('calls activate API and displays user info on success', async () => {
      mockRouteQuery.value = { token: 'good-token' }
      mockFetch.mockResolvedValueOnce({
        code: 200,
        data: { qq: '123456789', direction: 0 },
        msg: 'success',
      })

      const wrapper = createWrapper()
      await flushPromises()

      expect(mockFetch).toHaveBeenCalledWith('/api/register/activate', {
        params: { token: 'good-token' },
      })
      expect(wrapper.text()).toContain('注册账号')
      expect(wrapper.text()).toContain('123456789')
      expect(wrapper.text()).toContain('红石')
    })

    it('maps direction=1 to "后勤"', async () => {
      mockRouteQuery.value = { token: 't' }
      mockFetch.mockResolvedValueOnce({ code: 200, data: { qq: 'q', direction: 1 }, msg: '' })
      const wrapper = createWrapper()
      await flushPromises()
      expect(wrapper.text()).toContain('后勤')
    })

    it('maps direction=2 to "其他"', async () => {
      mockRouteQuery.value = { token: 't' }
      mockFetch.mockResolvedValueOnce({ code: 200, data: { qq: 'q', direction: 2 }, msg: '' })
      const wrapper = createWrapper()
      await flushPromises()
      expect(wrapper.text()).toContain('其他')
    })

    it('maps direction=99 to "未知"', async () => {
      mockRouteQuery.value = { token: 't' }
      mockFetch.mockResolvedValueOnce({ code: 200, data: { qq: 'q', direction: 99 }, msg: '' })
      const wrapper = createWrapper()
      await flushPromises()
      expect(wrapper.text()).toContain('未知')
    })

    it('shows tokenError when API returns non-200 code with msg', async () => {
      mockRouteQuery.value = { token: 'expired' }
      mockFetch.mockResolvedValueOnce({ code: 400, data: null, msg: '链接已过期' })

      const wrapper = createWrapper()
      await flushPromises()

      expect(wrapper.text()).toContain('链接无效')
      expect(wrapper.text()).toContain('链接已过期')
    })

    it('shows default tokenError when API returns non-200 without msg', async () => {
      mockRouteQuery.value = { token: 'bad' }
      mockFetch.mockResolvedValueOnce({ code: 400, data: null, msg: '' })

      const wrapper = createWrapper()
      await flushPromises()

      expect(wrapper.text()).toContain('激活链接无效')
    })

    it('shows tokenError when API returns 200 but no data', async () => {
      mockRouteQuery.value = { token: 'nd' }
      mockFetch.mockResolvedValueOnce({ code: 200, data: null, msg: '数据为空' })

      const wrapper = createWrapper()
      await flushPromises()

      expect(wrapper.text()).toContain('数据为空')
    })

    it('shows tokenError when API throws an exception', async () => {
      mockRouteQuery.value = { token: 'err' }
      mockFetch.mockRejectedValueOnce(new Error('Network error'))

      const wrapper = createWrapper()
      await flushPromises()

      expect(wrapper.text()).toContain('激活链接无效或已过期')
    })

    it('sets loading=false after successful API call', async () => {
      mockRouteQuery.value = { token: 't' }
      mockFetch.mockResolvedValueOnce({ code: 200, data: { qq: 'q', direction: 0 }, msg: '' })

      const wrapper = createWrapper()
      await flushPromises()

      expect(wrapper.find('.input_group').exists()).toBe(true)
    })

    it('sets loading=false after failed API call', async () => {
      mockRouteQuery.value = { token: 't' }
      mockFetch.mockRejectedValueOnce(new Error('fail'))

      const wrapper = createWrapper()
      await flushPromises()

      expect(wrapper.text()).not.toContain('正在验证激活链接...')
    })
  })

  // ── 2. Minecraft ID debounce validation ──

  describe('minecraft ID debounce validation', () => {
    async function setupForm() {
      mockRouteQuery.value = { token: 'ok' }
      mockFetch.mockResolvedValueOnce({ code: 200, data: { qq: '111', direction: 0 }, msg: '' })
      const wrapper = createWrapper()
      await flushPromises()
      return wrapper
    }

    it('shows checking spinner icon on input', async () => {
      const wrapper = await setupForm()
      await setInput(wrapper, 1, 'Steve')
      await nextTick()
      const checkingIcon = wrapper.find('.mc-id-icon .u-icon')
      expect(checkingIcon.exists()).toBe(true)
      expect(wrapper.find('img[alt="valid"]').exists()).toBe(false)
      expect(wrapper.find('img[alt="invalid"]').exists()).toBe(false)
    })

    it('transitions from checking to valid icon', async () => {
      const wrapper = await setupForm()
      mockFetch.mockResolvedValueOnce({ code: 200, data: 'uuid', msg: '' })

      await setInput(wrapper, 1, 'Steve')
      await nextTick()
      expect(wrapper.find('.mc-id-icon .u-icon').exists()).toBe(true)

      vi.advanceTimersByTime(600)
      await flushPromises()
      expect(wrapper.find('.mc-id-icon .u-icon').exists()).toBe(false)
      expect(wrapper.find('img[alt="valid"]').exists()).toBe(true)
    })

    it('transitions from checking to invalid icon', async () => {
      const wrapper = await setupForm()
      mockFetch.mockResolvedValueOnce({ code: 200, data: null, msg: '' })

      await setInput(wrapper, 1, 'Bad')
      await nextTick()
      expect(wrapper.find('.mc-id-icon .u-icon').exists()).toBe(true)

      vi.advanceTimersByTime(600)
      await flushPromises()
      expect(wrapper.find('.mc-id-icon .u-icon').exists()).toBe(false)
      expect(wrapper.find('img[alt="invalid"]').exists()).toBe(true)
    })

    it('shows valid icon after debounce when API returns data', async () => {
      const wrapper = await setupForm()
      mockFetch.mockResolvedValueOnce({ code: 200, data: 'some-uuid', msg: '' })

      await setInput(wrapper, 1, 'Steve')
      await nextTick()
      vi.advanceTimersByTime(600)
      await flushPromises()

      expect(wrapper.find('img[alt="valid"]').exists()).toBe(true)
      expect(wrapper.find('img[alt="invalid"]').exists()).toBe(false)
    })

    it('shows invalid icon when API returns null data', async () => {
      const wrapper = await setupForm()
      mockFetch.mockResolvedValueOnce({ code: 200, data: null, msg: '' })

      await setInput(wrapper, 1, 'BadPlayer')
      await nextTick()
      vi.advanceTimersByTime(600)
      await flushPromises()

      expect(wrapper.find('img[alt="invalid"]').exists()).toBe(true)
      expect(wrapper.find('img[alt="valid"]').exists()).toBe(false)
    })

    it('shows invalid icon when API returns non-200', async () => {
      const wrapper = await setupForm()
      mockFetch.mockResolvedValueOnce({ code: 404, data: null, msg: '' })

      await setInput(wrapper, 1, 'X')
      await nextTick()
      vi.advanceTimersByTime(600)
      await flushPromises()

      expect(wrapper.find('img[alt="invalid"]').exists()).toBe(true)
    })

    it('shows invalid icon when API throws', async () => {
      const wrapper = await setupForm()
      mockFetch.mockRejectedValueOnce(new Error('timeout'))

      await setInput(wrapper, 1, 'P')
      await nextTick()
      vi.advanceTimersByTime(600)
      await flushPromises()

      expect(wrapper.find('img[alt="invalid"]').exists()).toBe(true)
    })

    it('resets to idle when MC ID is cleared', async () => {
      const wrapper = await setupForm()
      mockFetch.mockResolvedValueOnce({ code: 200, data: 'uuid', msg: '' })

      await setInput(wrapper, 1, 'Steve')
      await nextTick()
      vi.advanceTimersByTime(600)
      await flushPromises()
      expect(wrapper.find('img[alt="valid"]').exists()).toBe(true)

      await setInput(wrapper, 1, '')
      await nextTick()

      expect(wrapper.find('img[alt="valid"]').exists()).toBe(false)
      expect(wrapper.find('img[alt="invalid"]').exists()).toBe(false)
    })

    it('resets to idle when MC ID is only whitespace', async () => {
      const wrapper = await setupForm()
      await setInput(wrapper, 1, '   ')
      await nextTick()

      expect(wrapper.find('img[alt="valid"]').exists()).toBe(false)
      expect(wrapper.find('img[alt="invalid"]').exists()).toBe(false)
    })

    it('debounces rapid inputs – only calls API once', async () => {
      const wrapper = await setupForm()
      mockFetch.mockResolvedValue({ code: 200, data: 'uuid', msg: '' })

      for (const char of ['S', 'St', 'Ste', 'Steve']) {
        await setInput(wrapper, 1, char)
        await nextTick()
        vi.advanceTimersByTime(200)
      }

      vi.advanceTimersByTime(600)
      await flushPromises()

      const mcCalls = (mockFetch.mock.calls).filter(
        (c: any[]) => c[0] === '/api/register/check-mc-id',
      )
      expect(mcCalls).toHaveLength(1)
      expect((mcCalls[0][1] as { params: { username: string } }).params.username).toBe('Steve')
    })

    it('trims the MC ID before calling API', async () => {
      const wrapper = await setupForm()
      mockFetch.mockResolvedValueOnce({ code: 200, data: 'uuid', msg: '' })

      await setInput(wrapper, 1, '  Steve  ')
      await nextTick()
      vi.advanceTimersByTime(600)
      await flushPromises()

      const mcCalls2 = (mockFetch.mock.calls).filter(
        (c: any[]) => c[0] === '/api/register/check-mc-id',
      )
      expect((mcCalls2[0][1] as { params: { username: string } }).params.username).toBe('Steve')
    })

    it('clears formError when MC ID changes', async () => {
      const wrapper = await setupForm()

      await wrapper.find('button').trigger('click')
      await nextTick()
      expect(wrapper.text()).toContain('请输入用户名')

      await setInput(wrapper, 1, 'x')
      await nextTick()

      expect(wrapper.text()).not.toContain('请输入用户名')
    })
  })

  // ── 3. Form validation ──

  describe('form validation', () => {
    async function setupForm() {
      mockRouteQuery.value = { token: 'ok' }
      mockFetch.mockResolvedValueOnce({ code: 200, data: { qq: '111', direction: 0 }, msg: '' })
      const wrapper = createWrapper()
      await flushPromises()
      return wrapper
    }

    it('error: empty username', async () => {
      const wrapper = await setupForm()
      await wrapper.find('button').trigger('click')
      await nextTick()
      expect(wrapper.text()).toContain('请输入用户名')
    })

    it('error: whitespace-only username', async () => {
      const wrapper = await setupForm()
      await setInput(wrapper, 0, '   ')
      await wrapper.find('button').trigger('click')
      await nextTick()
      expect(wrapper.text()).toContain('请输入用户名')
    })

    it('error: empty MC ID', async () => {
      const wrapper = await setupForm()
      await setInput(wrapper, 0, 'user')
      await wrapper.find('button').trigger('click')
      await nextTick()
      expect(wrapper.text()).toContain('请输入正版 Minecraft ID')
    })

    it('error: whitespace-only MC ID', async () => {
      const wrapper = await setupForm()
      await setInput(wrapper, 0, 'user')
      await setInput(wrapper, 1, '   ')
      await nextTick()
      vi.advanceTimersByTime(600)
      await flushPromises()
      await wrapper.find('button').trigger('click')
      await nextTick()
      expect(wrapper.text()).toContain('请输入正版 Minecraft ID')
    })

    it('error: MC ID still checking', async () => {
      const wrapper = await setupForm()
      await setInput(wrapper, 0, 'user')
      await setInput(wrapper, 1, 'Steve')
      await nextTick()
      // Don't advance timer – still in "checking" state
      await wrapper.find('button').trigger('click')
      await nextTick()
      expect(wrapper.text()).toContain('Minecraft ID 正在校验中，请稍候')
    })

    it('error: MC ID invalid', async () => {
      const wrapper = await setupForm()
      mockFetch.mockResolvedValueOnce({ code: 200, data: null, msg: '' })
      await setInput(wrapper, 0, 'user')
      await setInput(wrapper, 1, 'Bad')
      await nextTick()
      vi.advanceTimersByTime(600)
      await flushPromises()

      await wrapper.find('button').trigger('click')
      await nextTick()
      expect(wrapper.text()).toContain('Minecraft ID 无效，请检查后重试')
    })

    it('error: empty password', async () => {
      const wrapper = await setupForm()
      mockFetch.mockResolvedValueOnce({ code: 200, data: 'uuid', msg: '' })
      await setInput(wrapper, 0, 'user')
      await setInput(wrapper, 1, 'Steve')
      await nextTick()
      vi.advanceTimersByTime(600)
      await flushPromises()

      await wrapper.find('button').trigger('click')
      await nextTick()
      expect(wrapper.text()).toContain('请输入密码')
    })

    it('error: password less than 6 characters', async () => {
      const wrapper = await setupForm()
      mockFetch.mockResolvedValueOnce({ code: 200, data: 'uuid', msg: '' })
      await setInput(wrapper, 0, 'user')
      await setInput(wrapper, 1, 'Steve')
      await nextTick()
      vi.advanceTimersByTime(600)
      await flushPromises()
      await setInput(wrapper, 2, '12345')

      await wrapper.find('button').trigger('click')
      await nextTick()
      expect(wrapper.text()).toContain('密码长度不能少于6位')
    })

    it('error: passwords do not match', async () => {
      const wrapper = await setupForm()
      mockFetch.mockResolvedValueOnce({ code: 200, data: 'uuid', msg: '' })
      await setInput(wrapper, 0, 'user')
      await setInput(wrapper, 1, 'Steve')
      await nextTick()
      vi.advanceTimersByTime(600)
      await flushPromises()
      await setInput(wrapper, 2, 'password123')
      await setInput(wrapper, 3, 'different')

      await wrapper.find('button').trigger('click')
      await nextTick()
      expect(wrapper.text()).toContain('两次输入的密码不一致')
    })

    it('password exactly 6 chars passes length check', async () => {
      const wrapper = await setupForm()
      mockFetch.mockResolvedValueOnce({ code: 200, data: 'uuid', msg: '' })
      await setInput(wrapper, 0, 'user')
      await setInput(wrapper, 1, 'Steve')
      await nextTick()
      vi.advanceTimersByTime(600)
      await flushPromises()
      await setInput(wrapper, 2, '123456')
      await setInput(wrapper, 3, '123456')

      mockFetch.mockResolvedValueOnce({ code: 200, msg: '注册成功' })
      await wrapper.find('button').trigger('click')
      await flushPromises()

      expect(wrapper.text()).not.toContain('密码长度不能少于6位')
    })

    it('does not call API when validation fails', async () => {
      const wrapper = await setupForm()
      const callsBefore = mockFetch.mock.calls.length

      await wrapper.find('button').trigger('click')
      await flushPromises()

      expect(mockFetch.mock.calls.length).toBe(callsBefore)
    })
  })

  // ── 4. Registration submission ──

  describe('handleRegister', () => {
    async function setupValidForm() {
      mockRouteQuery.value = { token: 'tok123' }
      mockFetch.mockResolvedValueOnce({ code: 200, data: { qq: '111', direction: 0 }, msg: '' })
      const wrapper = createWrapper()
      await flushPromises()

      mockFetch.mockResolvedValueOnce({ code: 200, data: 'uuid', msg: '' })
      await setInput(wrapper, 0, 'testuser')
      await setInput(wrapper, 1, 'Steve')
      await nextTick()
      vi.advanceTimersByTime(600)
      await flushPromises()

      await setInput(wrapper, 2, 'password123')
      await setInput(wrapper, 3, 'password123')
      return wrapper
    }

    it('sends correct data on successful submission', async () => {
      const wrapper = await setupValidForm()
      mockFetch.mockResolvedValueOnce({ code: 200, msg: '注册成功！' })

      await wrapper.find('button').trigger('click')
      await flushPromises()

      const regCall = (mockFetch.mock.calls).find(
        (c: any[]) => c[0] === '/api/register/complete',
      )
      expect(regCall).toBeTruthy()
      expect(regCall?.[1]).toEqual({
        method: 'POST',
        body: {
          token: 'tok123',
          username: 'testuser',
          minecraftId: 'Steve',
          password: 'password123',
        },
      })
    })

    it('shows success animation on 200 response', async () => {
      const wrapper = await setupValidForm()
      mockFetch.mockResolvedValueOnce({ code: 200, msg: '注册成功！' })

      await wrapper.find('button').trigger('click')
      await flushPromises()

      expect(wrapper.find('.success-container').exists()).toBe(true)
      expect(wrapper.find('.success-svg').exists()).toBe(true)
    })

    it('shows success text after animation delay', async () => {
      const wrapper = await setupValidForm()
      mockFetch.mockResolvedValueOnce({ code: 200, msg: '' })

      await wrapper.find('button').trigger('click')
      await flushPromises()

      // Text not visible yet
      expect(wrapper.find('.success-info').classes()).not.toContain('show')

      // Advance to show text phase
      vi.advanceTimersByTime(2500)
      await nextTick()
      expect(wrapper.find('.success-info').classes()).toContain('show')
      expect(wrapper.text()).toContain('您的账号已注册完成')
    })

    it('navigates to /login via button click after success', async () => {
      const wrapper = await setupValidForm()
      mockFetch.mockResolvedValueOnce({ code: 200, msg: '成功' })

      await wrapper.find('button').trigger('click')
      await flushPromises()

      // Show login button
      vi.advanceTimersByTime(3000)
      await nextTick()

      const loginBtn = wrapper.find('.success-login-btn')
      expect(loginBtn.exists()).toBe(true)
      await loginBtn.trigger('click')
      expect(mockNavigateTo).toHaveBeenCalledWith('/login')
    })

    it('shows error message on non-200 response', async () => {
      const wrapper = await setupValidForm()
      mockFetch.mockResolvedValueOnce({ code: 400, msg: '用户名已存在' })

      await wrapper.find('button').trigger('click')
      await flushPromises()

      expect(wrapper.text()).toContain('用户名已存在')
      expect(mockNavigateTo).not.toHaveBeenCalled()
    })

    it('shows default error when non-200 response has empty msg', async () => {
      const wrapper = await setupValidForm()
      mockFetch.mockResolvedValueOnce({ code: 500, msg: '' })

      await wrapper.find('button').trigger('click')
      await flushPromises()

      expect(wrapper.text()).toContain('注册失败，请稍后再试')
    })

    it('shows network error when API throws', async () => {
      const wrapper = await setupValidForm()
      mockFetch.mockRejectedValueOnce(new Error('Network'))

      await wrapper.find('button').trigger('click')
      await flushPromises()

      expect(wrapper.text()).toContain('网络异常，请稍后再试')
    })

    it('disables button while submitting', async () => {
      const wrapper = await setupValidForm()
      mockFetch.mockReturnValueOnce(new Promise(() => {}))

      await wrapper.find('button').trigger('click')
      await nextTick()

      expect(wrapper.find('button').attributes('disabled')).toBeDefined()
    })

    it('re-enables button after successful submission', async () => {
      const wrapper = await setupValidForm()
      mockFetch.mockResolvedValueOnce({ code: 200, msg: '成功' })

      await wrapper.find('button').trigger('click')
      await flushPromises()

      expect(wrapper.find('button').attributes('disabled')).toBeUndefined()
    })

    it('re-enables button after failed submission', async () => {
      const wrapper = await setupValidForm()
      mockFetch.mockRejectedValueOnce(new Error('fail'))

      await wrapper.find('button').trigger('click')
      await flushPromises()

      expect(wrapper.find('button').attributes('disabled')).toBeUndefined()
    })

    it('clears previous error on new submission', async () => {
      const wrapper = await setupValidForm()

      mockFetch.mockResolvedValueOnce({ code: 400, msg: '旧错误' })
      await wrapper.find('button').trigger('click')
      await flushPromises()
      expect(wrapper.text()).toContain('旧错误')

      mockFetch.mockResolvedValueOnce({ code: 200, msg: '成功' })
      await wrapper.find('button').trigger('click')
      await flushPromises()
      expect(wrapper.text()).not.toContain('旧错误')
    })

    it('replaces form with success animation on successful submission', async () => {
      const wrapper = await setupValidForm()

      mockFetch.mockResolvedValueOnce({ code: 200, msg: '成功' })
      await wrapper.find('button').trigger('click')
      await flushPromises()

      // Form is gone, success animation is shown
      expect(wrapper.find('.input_group').exists()).toBe(false)
      expect(wrapper.find('.success-container').exists()).toBe(true)
    })

    it('trims username and minecraftId in submission body', async () => {
      mockRouteQuery.value = { token: 'tok' }
      mockFetch.mockResolvedValueOnce({ code: 200, data: { qq: '1', direction: 0 }, msg: '' })
      const wrapper = createWrapper()
      await flushPromises()

      mockFetch.mockResolvedValueOnce({ code: 200, data: 'uuid', msg: '' })
      await setInput(wrapper, 0, '  spacedUser  ')
      await setInput(wrapper, 1, '  Steve  ')
      await nextTick()
      vi.advanceTimersByTime(600)
      await flushPromises()
      await setInput(wrapper, 2, 'password123')
      await setInput(wrapper, 3, 'password123')

      mockFetch.mockResolvedValueOnce({ code: 200, msg: '注册成功' })
      await wrapper.find('button').trigger('click')
      await flushPromises()

      const regCall = (mockFetch.mock.calls).find(
        (c: any[]) => c[0] === '/api/register/complete',
      )
      const regBody = (regCall?.[1] as { body: { username: string, minecraftId: string } } | undefined)?.body
      expect(regBody?.username).toBe('spacedUser')
      expect(regBody?.minecraftId).toBe('Steve')
    })
  })

  // ── 5. UI rendering ──

  describe('ui rendering', () => {
    it('renders register card with illustration', async () => {
      mockRouteQuery.value = { token: 't' }
      mockFetch.mockResolvedValueOnce({ code: 200, data: { qq: '1', direction: 0 }, msg: '' })
      const wrapper = createWrapper()
      await flushPromises()

      expect(wrapper.find('.register_card').exists()).toBe(true)
      expect(wrapper.find('.illustration_wrapper').exists()).toBe(true)
      expect(wrapper.find('img[alt="register illustration"]').exists()).toBe(true)
    })

    it('renders four input fields when form is active', async () => {
      mockRouteQuery.value = { token: 't' }
      mockFetch.mockResolvedValueOnce({ code: 200, data: { qq: '1', direction: 0 }, msg: '' })
      const wrapper = createWrapper()
      await flushPromises()

      expect(wrapper.findAll('input')).toHaveLength(4)
    })

    it('renders register button', async () => {
      mockRouteQuery.value = { token: 't' }
      mockFetch.mockResolvedValueOnce({ code: 200, data: { qq: '1', direction: 0 }, msg: '' })
      const wrapper = createWrapper()
      await flushPromises()

      const btn = wrapper.find('button')
      expect(btn.exists()).toBe(true)
      expect(btn.text()).toContain('注')
      expect(btn.text()).toContain('册')
    })

    it('does not render form or button when loading', async () => {
      mockRouteQuery.value = { token: 't' }
      mockFetch.mockReturnValueOnce(new Promise(() => {}))
      const wrapper = createWrapper()
      await nextTick()

      expect(wrapper.find('.input_group').exists()).toBe(false)
      expect(wrapper.find('button').exists()).toBe(false)
    })

    it('does not render form when tokenError is set', async () => {
      mockRouteQuery.value = { token: 't' }
      mockFetch.mockResolvedValueOnce({ code: 400, data: null, msg: '无效' })
      const wrapper = createWrapper()
      await flushPromises()

      expect(wrapper.find('.input_group').exists()).toBe(false)
      expect(wrapper.find('button').exists()).toBe(false)
    })

    it('shows spinner icon when mcIdStatus is checking', async () => {
      mockRouteQuery.value = { token: 't' }
      mockFetch.mockResolvedValueOnce({ code: 200, data: { qq: '1', direction: 0 }, msg: '' })
      const wrapper = createWrapper()
      await flushPromises()

      await setInput(wrapper, 1, 'Steve')
      await nextTick()

      expect(wrapper.find('.mc-id-icon').exists()).toBe(true)
    })

    it('does not show any MC ID icon when idle', async () => {
      mockRouteQuery.value = { token: 't' }
      mockFetch.mockResolvedValueOnce({ code: 200, data: { qq: '1', direction: 0 }, msg: '' })
      const wrapper = createWrapper()
      await flushPromises()

      expect(wrapper.find('img[alt="valid"]').exists()).toBe(false)
      expect(wrapper.find('img[alt="invalid"]').exists()).toBe(false)
    })

    it('shows formError in red text', async () => {
      mockRouteQuery.value = { token: 't' }
      mockFetch.mockResolvedValueOnce({ code: 200, data: { qq: '1', direction: 0 }, msg: '' })
      const wrapper = createWrapper()
      await flushPromises()

      await wrapper.find('button').trigger('click')
      await nextTick()

      const errP = wrapper.find('.form-message.text-red-400')
      expect(errP.exists()).toBe(true)
    })

    it('shows success animation with SVG on successful registration', async () => {
      mockRouteQuery.value = { token: 'tok' }
      mockFetch.mockResolvedValueOnce({ code: 200, data: { qq: '1', direction: 0 }, msg: '' })
      const wrapper = createWrapper()
      await flushPromises()

      mockFetch.mockResolvedValueOnce({ code: 200, data: 'uuid', msg: '' })
      await setInput(wrapper, 0, 'user')
      await setInput(wrapper, 1, 'Steve')
      await nextTick()
      vi.advanceTimersByTime(600)
      await flushPromises()
      await setInput(wrapper, 2, 'password123')
      await setInput(wrapper, 3, 'password123')

      mockFetch.mockResolvedValueOnce({ code: 200, msg: '注册成功' })
      await wrapper.find('button').trigger('click')
      await flushPromises()

      expect(wrapper.find('.success-container').exists()).toBe(true)
      expect(wrapper.find('.success-svg').exists()).toBe(true)
      expect(wrapper.find('.register_form--success').exists()).toBe(true)
    })

    it('hides formError when no error', async () => {
      mockRouteQuery.value = { token: 't' }
      mockFetch.mockResolvedValueOnce({ code: 200, data: { qq: '1', direction: 0 }, msg: '' })
      const wrapper = createWrapper()
      await flushPromises()

      expect(wrapper.find('.form-message.text-red-400').exists()).toBe(false)
    })

    it('hides formSuccess when no success', async () => {
      mockRouteQuery.value = { token: 't' }
      mockFetch.mockResolvedValueOnce({ code: 200, data: { qq: '1', direction: 0 }, msg: '' })
      const wrapper = createWrapper()
      await flushPromises()

      expect(wrapper.find('.form-message.text-green-500').exists()).toBe(false)
    })

    it('renders #register_page root element', async () => {
      mockRouteQuery.value = { token: 't' }
      mockFetch.mockResolvedValueOnce({ code: 200, data: { qq: '1', direction: 0 }, msg: '' })
      const wrapper = createWrapper()
      await flushPromises()

      expect(wrapper.find('#register_page').exists()).toBe(true)
    })
  })

  // ── 6. Error animation ──

  describe('error animation', () => {
    it('renders error SVG container when token is missing', async () => {
      mockRouteQuery.value = {}
      const wrapper = createWrapper()
      await flushPromises()

      expect(wrapper.find('.error-container').exists()).toBe(true)
      expect(wrapper.find('.error-svg').exists()).toBe(true)
    })

    it('renders error SVG container when API returns error', async () => {
      mockRouteQuery.value = { token: 'bad' }
      mockFetch.mockResolvedValueOnce({ code: 400, data: null, msg: '链接已过期' })
      const wrapper = createWrapper()
      await flushPromises()

      expect(wrapper.find('.error-container').exists()).toBe(true)
      expect(wrapper.find('.error-svg').exists()).toBe(true)
    })

    it('renders error SVG container when API throws', async () => {
      mockRouteQuery.value = { token: 'err' }
      mockFetch.mockRejectedValueOnce(new Error('fail'))
      const wrapper = createWrapper()
      await flushPromises()

      expect(wrapper.find('.error-container').exists()).toBe(true)
    })

    it('animates error circle after 300ms', async () => {
      mockRouteQuery.value = {}
      const wrapper = createWrapper()
      await flushPromises()

      expect(wrapper.find('.error-circle-path').classes()).not.toContain('animate-error-circle')

      vi.advanceTimersByTime(300)
      await nextTick()

      expect(wrapper.find('.error-circle-path').classes()).toContain('animate-error-circle')
    })

    it('animates first cross line after 1000ms', async () => {
      mockRouteQuery.value = {}
      const wrapper = createWrapper()
      await flushPromises()

      expect(wrapper.find('.cross-line-1').classes()).not.toContain('animate-cross-1')

      vi.advanceTimersByTime(1000)
      await nextTick()

      expect(wrapper.find('.cross-line-1').classes()).toContain('animate-cross-1')
    })

    it('animates second cross line after 1400ms', async () => {
      mockRouteQuery.value = {}
      const wrapper = createWrapper()
      await flushPromises()

      expect(wrapper.find('.cross-line-2').classes()).not.toContain('animate-cross-2')

      vi.advanceTimersByTime(1400)
      await nextTick()

      expect(wrapper.find('.cross-line-2').classes()).toContain('animate-cross-2')
    })

    it('shows error text after 1900ms', async () => {
      mockRouteQuery.value = {}
      const wrapper = createWrapper()
      await flushPromises()

      expect(wrapper.find('.error-info').classes()).not.toContain('show')

      vi.advanceTimersByTime(1900)
      await nextTick()

      expect(wrapper.find('.error-info').classes()).toContain('show')
    })

    it('error text displays correct token error message', async () => {
      mockRouteQuery.value = {}
      const wrapper = createWrapper()
      await flushPromises()

      vi.advanceTimersByTime(1900)
      await nextTick()

      expect(wrapper.find('.error-title').text()).toBe('链接无效')
      expect(wrapper.find('.error-hint').text()).toBe('缺少激活链接参数')
    })

    it('error text displays API error message', async () => {
      mockRouteQuery.value = { token: 'x' }
      mockFetch.mockResolvedValueOnce({ code: 400, data: null, msg: '链接已过期' })
      const wrapper = createWrapper()
      await flushPromises()

      vi.advanceTimersByTime(1900)
      await nextTick()

      expect(wrapper.find('.error-hint').text()).toBe('链接已过期')
    })

    it('does not show error animation when token is valid', async () => {
      mockRouteQuery.value = { token: 'good' }
      mockFetch.mockResolvedValueOnce({ code: 200, data: { qq: '1', direction: 0 }, msg: '' })
      const wrapper = createWrapper()
      await flushPromises()

      expect(wrapper.find('.error-container').exists()).toBe(false)
      expect(wrapper.find('.error-svg').exists()).toBe(false)
    })
  })

  // ── 7. Success animation phases ──

  describe('success animation phases', () => {
    async function setupValidForm() {
      mockRouteQuery.value = { token: 'tok' }
      mockFetch.mockResolvedValueOnce({ code: 200, data: { qq: '1', direction: 0 }, msg: '' })
      const wrapper = createWrapper()
      await flushPromises()

      mockFetch.mockResolvedValueOnce({ code: 200, data: 'uuid', msg: '' })
      await setInput(wrapper, 0, 'user')
      await setInput(wrapper, 1, 'Steve')
      await nextTick()
      vi.advanceTimersByTime(600)
      await flushPromises()
      await setInput(wrapper, 2, 'password123')
      await setInput(wrapper, 3, 'password123')
      return wrapper
    }

    it('circle appears after 300ms', async () => {
      const wrapper = await setupValidForm()
      mockFetch.mockResolvedValueOnce({ code: 200, msg: '' })
      await wrapper.find('button').trigger('click')
      await flushPromises()

      expect(wrapper.find('.circle-path').classes()).not.toContain('animate-circle')

      vi.advanceTimersByTime(300)
      await nextTick()

      expect(wrapper.find('.circle-path').classes()).toContain('animate-circle')
    })

    it('check appears after 1000ms', async () => {
      const wrapper = await setupValidForm()
      mockFetch.mockResolvedValueOnce({ code: 200, msg: '' })
      await wrapper.find('button').trigger('click')
      await flushPromises()

      expect(wrapper.find('.check-path').classes()).not.toContain('animate-check')

      vi.advanceTimersByTime(1000)
      await nextTick()

      expect(wrapper.find('.check-path').classes()).toContain('animate-check')
    })

    it('container floats up after 2000ms', async () => {
      const wrapper = await setupValidForm()
      mockFetch.mockResolvedValueOnce({ code: 200, msg: '' })
      await wrapper.find('button').trigger('click')
      await flushPromises()

      expect(wrapper.find('.success-container').classes()).not.toContain('float-up')

      vi.advanceTimersByTime(2000)
      await nextTick()

      expect(wrapper.find('.success-container').classes()).toContain('float-up')
    })

    it('login button appears after 2800ms', async () => {
      const wrapper = await setupValidForm()
      mockFetch.mockResolvedValueOnce({ code: 200, msg: '' })
      await wrapper.find('button').trigger('click')
      await flushPromises()

      expect(wrapper.find('.success-login-btn').classes()).not.toContain('show')

      vi.advanceTimersByTime(2800)
      await nextTick()

      expect(wrapper.find('.success-login-btn').classes()).toContain('show')
    })
  })

  // ── 8. Test mode token ──

  describe('test mode token', () => {
    it('uses test data when token is "test"', async () => {
      mockRouteQuery.value = { token: 'test' }
      mockFetch.mockResolvedValueOnce({ code: 200, data: { qq: 'ignored', direction: 0 }, msg: '' })
      const wrapper = createWrapper()
      await flushPromises()

      expect(wrapper.text()).toContain('114514')
      expect(wrapper.text()).toContain('测试')
    })
  })
})
