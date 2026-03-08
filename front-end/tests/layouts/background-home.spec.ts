import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import BackgroundHome from '../../app/layouts/background-home.vue'

describe('background-home.vue', () => {
  it('renders the bg-color container', () => {
    const wrapper = mount(BackgroundHome)
    expect(wrapper.find('.bg-color').exists()).toBe(true)
  })

  it('renders the background div', () => {
    const wrapper = mount(BackgroundHome)
    expect(wrapper.find('.background').exists()).toBe(true)
  })

  it('renders slot content', () => {
    const wrapper = mount(BackgroundHome, {
      slots: { default: '<div class="home-content">Home</div>' },
    })
    expect(wrapper.find('.home-content').exists()).toBe(true)
    expect(wrapper.find('.home-content').text()).toBe('Home')
  })

  it('slot content is placed before background div', () => {
    const wrapper = mount(BackgroundHome, {
      slots: { default: '<p class="test-slot">Content</p>' },
    })
    const bgColor = wrapper.find('.bg-color')
    const lastChild = bgColor.element.children[bgColor.element.children.length - 1]
    expect(lastChild?.classList.contains('background')).toBe(true)
  })
})
