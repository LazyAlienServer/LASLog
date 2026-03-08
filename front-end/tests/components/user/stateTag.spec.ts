import { expect, it } from 'vitest'
import { mount } from '@vue/test-utils'

import StateTag from '../../../app/components/user/stateTag.vue'

function createWrapper(props: { tag: string, state?: string, stateColor?: string }) {
  return mount(StateTag, { props })
}

describe('stateTag.vue', () => {
  it('renders the tag container', () => {
    const wrapper = createWrapper({ tag: '测试' })
    expect(wrapper.find('.tag').exists()).toBe(true)
  })

  it('renders the tag name', () => {
    const wrapper = createWrapper({ tag: '白名单' })
    expect(wrapper.find('.name').text()).toBe('白名单')
  })

  it('renders the state text when provided', () => {
    const wrapper = createWrapper({ tag: '白名单', state: '通过' })
    expect(wrapper.find('.state').text()).toBe('通过')
  })

  it('renders empty state when state is not provided', () => {
    const wrapper = createWrapper({ tag: '白名单' })
    expect(wrapper.find('.state').text()).toBe('')
  })

  it('applies stateColor as inline color style', () => {
    const wrapper = createWrapper({ tag: '白名单', state: '通过', stateColor: '#00FF00' })
    const stateEl = wrapper.find('.state')
    expect(stateEl.attributes('style')).toContain('color')
    expect(stateEl.attributes('style')).toContain('#00FF00')
  })

  it('does not set color when stateColor is not provided', () => {
    const wrapper = createWrapper({ tag: '白名单', state: '通过' })
    const stateEl = wrapper.find('.state')
    const style = stateEl.attributes('style')
    expect(style == null || !style.includes('color:')).toBe(true)
  })

  it('renders both state and name spans inside .tag', () => {
    const wrapper = createWrapper({ tag: '归档', state: '已完成' })
    const tag = wrapper.find('.tag')
    expect(tag.findAll('span')).toHaveLength(2)
  })

  it('renders with different tag text', () => {
    const wrapper = createWrapper({ tag: '插件管理' })
    expect(wrapper.find('.name').text()).toBe('插件管理')
  })

  it('renders with different state text', () => {
    const wrapper = createWrapper({ tag: '申请', state: '待审核' })
    expect(wrapper.find('.state').text()).toBe('待审核')
  })

  it('applies custom stateColor correctly', () => {
    const wrapper = createWrapper({ tag: 't', state: 's', stateColor: 'red' })
    const stateEl = wrapper.find('.state')
    expect(stateEl.attributes('style')).toContain('color: red')
  })

  it('renders tag and state in correct order', () => {
    const wrapper = createWrapper({ tag: '归档', state: '完成' })
    const spans = wrapper.findAll('span')
    expect(spans[0].classes()).toContain('state')
    expect(spans[1].classes()).toContain('name')
  })
})
