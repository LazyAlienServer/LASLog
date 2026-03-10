import { useLocalStorage } from '@vueuse/core'
import { defineStore } from 'pinia'

export const useUserInfoStore = defineStore('userInfo', () => {
  const userName = useLocalStorage('userName', '')

  function setUserName(value: string) {
    userName.value = value
  }

  return { userName, setUserName }
})
