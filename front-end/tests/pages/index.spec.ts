import { mount } from '@vue/test-utils'
import { expect, it, vi } from 'vitest'
import { defineComponent, h } from 'vue'

import IndexPage from '../../app/pages/index.vue'

// ─── Mocks ───
vi.stubGlobal('definePageMeta', vi.fn())

const NuxtLinkStub = defineComponent({
  name: 'NuxtLink',
  props: ['to', 'id'],
  setup(props, { slots }) {
    return () => h('a', { href: props.to, id: props.id }, slots.default?.())
  },
})

const stubs = {
  NuxtLink: NuxtLinkStub,
  NuxtImg: defineComponent({
    name: 'NuxtImg',
    props: ['src', 'alt'],
    setup(props) {
      return () => h('img', { src: props.src, alt: props.alt })
    },
  }),
}

function createWrapper() {
  return mount(IndexPage, { global: { stubs } })
}

describe('index.vue', () => {
  it('renders the content container', () => {
    const wrapper = createWrapper()
    expect(wrapper.find('.content').exists()).toBe(true)
  })

  it('renders the main title with "LAS" and "LOG"', () => {
    const wrapper = createWrapper()
    const h1 = wrapper.find('h1')
    expect(h1.exists()).toBe(true)
    expect(h1.text()).toContain('LAS')
    expect(h1.text()).toContain('LOG')
  })

  it('renders the desktop subtitle', () => {
    const wrapper = createWrapper()
    const h2 = wrapper.find('.h2-desktop')
    expect(h2.exists()).toBe(true)
    expect(h2.text()).toContain('管理您在LAS的')
    expect(h2.text()).toContain('一切事务')
  })

  it('renders the mobile subtitle', () => {
    const wrapper = createWrapper()
    const h2 = wrapper.find('.h2-mobile')
    expect(h2.exists()).toBe(true)
    expect(h2.text()).toContain('管理您在LAS的')
    expect(h2.text()).toContain('一切事务')
  })

  it('renders the description in h3', () => {
    const wrapper = createWrapper()
    const h3 = wrapper.find('h3')
    expect(h3.exists()).toBe(true)
    expect(h3.text()).toContain('Lazy Alien Server')
    expect(h3.text()).toContain('白名单管理')
  })

  it('renders exactly four tags', () => {
    const wrapper = createWrapper()
    const tags = wrapper.findAll('.tag')
    expect(tags).toHaveLength(4)
  })

  it('renders tags with correct text', () => {
    const wrapper = createWrapper()
    const tags = wrapper.findAll('.tag')
    const texts = tags.map(t => t.text())
    expect(texts).toContain('交流')
    expect(texts).toContain('分享')
    expect(texts).toContain('资料')
    expect(texts).toContain('归档')
  })

  it('tags have correct background colors', () => {
    const wrapper = createWrapper()
    const tags = wrapper.findAll('.tag')
    const colors = tags.map(t => (t.element as HTMLElement).style.backgroundColor)
    expect(colors).toEqual(['#A263CF', '#9774DA', '#8C84E5', '#8194F0'])
  })

  it('renders the logo image', () => {
    const wrapper = createWrapper()
    const img = wrapper.find('.right-container img')
    expect(img.exists()).toBe(true)
    expect(img.attributes('alt')).toBe('LAS LOG Logo')
  })

  it('renders the "探索 LAS LOG" link', () => {
    const wrapper = createWrapper()
    const link = wrapper.find('#explore_LASLOG')
    expect(link.exists()).toBe(true)
    expect(link.text()).toBe('探索 LAS LOG')
  })

  it('"探索 LAS LOG" link points to /login', () => {
    const wrapper = createWrapper()
    const link = wrapper.find('#explore_LASLOG')
    expect(link.attributes('href')).toBe('/login')
  })

  it('renders the left and right containers', () => {
    const wrapper = createWrapper()
    expect(wrapper.find('.left-container').exists()).toBe(true)
    expect(wrapper.find('.right-container').exists()).toBe(true)
  })

  it('renders the main-container', () => {
    const wrapper = createWrapper()
    expect(wrapper.find('.main-container').exists()).toBe(true)
  })

  it('renders the button-container with two hr elements', () => {
    const wrapper = createWrapper()
    const container = wrapper.find('.button-container')
    expect(container.exists()).toBe(true)
    expect(container.findAll('hr')).toHaveLength(2)
  })

  it('renders the tag-container', () => {
    const wrapper = createWrapper()
    expect(wrapper.find('.tag-container').exists()).toBe(true)
  })
})
