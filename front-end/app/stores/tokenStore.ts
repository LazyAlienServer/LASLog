import { useLocalStorage } from '@vueuse/core'
import { defineStore } from 'pinia'

export const useTokenStore = defineStore('tokens', () => {
  const browserFinger = useLocalStorage('browserFinger', '')
  const accessToken = useLocalStorage('accessToken', '')

  function setAccessToken(value: string) {
    accessToken.value = value
  }
  function setBrowserFinger(value: string) {
    browserFinger.value = value
  }
  return { browserFinger, accessToken, setBrowserFinger, setAccessToken }
})
