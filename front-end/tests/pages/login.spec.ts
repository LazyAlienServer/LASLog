import { expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'

// ─── Mocks ───
vi.stubGlobal('definePageMeta', vi.fn())

// ─── Component stubs ───
import LoginPage from '../../app/pages/login.vue'

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
  props: ['ui'],
  setup(_props, { slots }) {
    return () => h('button', {}, slots.default?.())
  },
})

const ULinkStub = defineComponent({
  name: 'ULink',
  props: ['to', 'class'],
  setup(props, { slots }) {
    return () => h('a', { href: props.to, class: props.class }, slots.default?.())
  },
})

const NuxtImgStub = defineComponent({
  name: 'NuxtImg',
  props: ['src', 'alt', 'preload', 'class'],
  setup(props) {
    return () => h('img', { src: props.src, alt: props.alt, class: props.class })
  },
})

const stubs = {
  UInput: UInputStub,
  UButton: UButtonStub,
  ULink: ULinkStub,
  NuxtImg: NuxtImgStub,
}

function createWrapper() {
  return mount(LoginPage, { global: { stubs } })
}

describe('login.vue', () => {
  it('renders the login page root element', () => {
    const wrapper = createWrapper()
    expect(wrapper.find('#login_page').exists()).toBe(true)
  })

  it('renders the login card', () => {
    const wrapper = createWrapper()
    expect(wrapper.find('.login_card').exists()).toBe(true)
  })

  it('renders the login illustration image', () => {
    const wrapper = createWrapper()
    const img = wrapper.find('img[alt="login illustration"]')
    expect(img.exists()).toBe(true)
    expect(img.attributes('src')).toBe('/img/login-illustration.jpg')
  })

  it('renders the title "账户验证"', () => {
    const wrapper = createWrapper()
    expect(wrapper.find('h1').text()).toBe('账户验证')
  })

  it('renders username input with correct placeholder', () => {
    const wrapper = createWrapper()
    const inputs = wrapper.findAll('input')
    const usernameInput = inputs.find(
      i => (i.element as HTMLInputElement).placeholder === '用户名',
    )
    expect(usernameInput).toBeTruthy()
  })

  it('renders password input with correct placeholder', () => {
    const wrapper = createWrapper()
    const inputs = wrapper.findAll('input')
    const passwordInput = inputs.find(
      i => (i.element as HTMLInputElement).placeholder === '密码',
    )
    expect(passwordInput).toBeTruthy()
  })

  it('password input has type="password"', () => {
    const wrapper = createWrapper()
    const inputs = wrapper.findAll('input')
    const passwordInput = inputs.find(
      i => (i.element as HTMLInputElement).placeholder === '密码',
    )
    expect(passwordInput?.attributes('type')).toBe('password')
  })

  it('username input has type="text"', () => {
    const wrapper = createWrapper()
    const inputs = wrapper.findAll('input')
    const usernameInput = inputs.find(
      i => (i.element as HTMLInputElement).placeholder === '用户名',
    )
    expect(usernameInput?.attributes('type')).toBe('text')
  })

  it('renders exactly two input fields', () => {
    const wrapper = createWrapper()
    expect(wrapper.findAll('input')).toHaveLength(2)
  })

  it('renders the login button with text "登录"', () => {
    const wrapper = createWrapper()
    const btn = wrapper.find('button')
    expect(btn.exists()).toBe(true)
    expect(btn.text()).toContain('登录')
  })

  it('renders "激活账号" link', () => {
    const wrapper = createWrapper()
    const link = wrapper.find('a.active_account')
    expect(link.exists()).toBe(true)
    expect(link.text()).toBe('激活账号')
  })

  it('"激活账号" link points to "/"', () => {
    const wrapper = createWrapper()
    const link = wrapper.find('a.active_account')
    expect(link.attributes('href')).toBe('/')
  })

  it('renders login_form container', () => {
    const wrapper = createWrapper()
    expect(wrapper.find('.login_form').exists()).toBe(true)
  })

  it('has flex centering classes on root', () => {
    const wrapper = createWrapper()
    const root = wrapper.find('#login_page')
    expect(root.classes()).toContain('flex')
    expect(root.classes()).toContain('items-center')
    expect(root.classes()).toContain('justify-center')
  })
})
