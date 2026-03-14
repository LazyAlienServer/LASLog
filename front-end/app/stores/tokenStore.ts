import { useLocalStorage } from '@vueuse/core'
import { defineStore } from 'pinia'

export const useTokenStore = defineStore('tokens', () => {
  const browserFinger = useLocalStorage('browserFinger', '')

  function setBrowserFinger(value: string) {
    browserFinger.value = value
  }
  return { browserFinger, setBrowserFinger }
})
