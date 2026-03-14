import { useLocalStorage } from '@vueuse/core'
import { defineStore } from 'pinia'

export const useTokenStore = defineStore('tokens', () => {
  const clientId = useLocalStorage('clientId', '')
  const accessToken = useLocalStorage('accessToken', '')

  function setAccessToken(value: string) {
    accessToken.value = value
  }
  function setClientId(value: string) {
    clientId.value = value
  }
  return { clientId, accessToken, setClientId, setAccessToken }
})
