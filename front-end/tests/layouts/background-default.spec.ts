import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import BackgroundDefault from '../../app/layouts/background-default.vue'

describe('background-default.vue', () => {
  it('renders the bg-color container', () => {
    const wrapper = mount(BackgroundDefault)
    expect(wrapper.find('.bg-color').exists()).toBe(true)
  })

  it('renders the background div', () => {
    const wrapper = mount(BackgroundDefault)
    expect(wrapper.find('.background').exists()).toBe(true)
  })

  it('renders slot content', () => {
    const wrapper = mount(BackgroundDefault, {
      slots: { default: '<p class="test-slot">Hello</p>' },
    })
    expect(wrapper.find('.test-slot').exists()).toBe(true)
    expect(wrapper.find('.test-slot').text()).toBe('Hello')
  })

  it('slot content is placed before background div', () => {
    const wrapper = mount(BackgroundDefault, {
      slots: { default: '<p class="test-slot">Content</p>' },
    })
    const bgColor = wrapper.find('.bg-color')
    const children = bgColor.element.children
    // First child should be the slot content, last should be .background
    expect(children[children.length - 1]!.classList.contains('background')).toBe(true)
  })
})


