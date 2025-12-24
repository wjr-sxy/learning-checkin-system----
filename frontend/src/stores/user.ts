import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getUserInfo } from '../api/auth'

export const useUserStore = defineStore('user', () => {
  const user = ref<any>(null)
  const token = ref(localStorage.getItem('token') || '')
  const totalOnlineSeconds = ref(0)
  const todayOnlineSeconds = ref(0)

  function setUser(userData: any) {
    user.value = userData
    if (userData.totalOnlineSeconds) {
        totalOnlineSeconds.value = userData.totalOnlineSeconds
    }
  }

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  function updateOnlineTime(duration: number) {
    totalOnlineSeconds.value += duration
    todayOnlineSeconds.value += duration
  }

  function setOnlineStats(total: number, today: number) {
    totalOnlineSeconds.value = total
    todayOnlineSeconds.value = today
  }

  async function initUser() {
    if (token.value && !user.value) {
      try {
        const res: any = await getUserInfo()
        user.value = res.data
        if (res.data.totalOnlineSeconds) {
            totalOnlineSeconds.value = res.data.totalOnlineSeconds
        }
      } catch (error) {
        console.error('Failed to fetch user info:', error)
        logout()
      }
    }
  }

  function logout() {
    user.value = null
    token.value = ''
    localStorage.removeItem('token')
    totalOnlineSeconds.value = 0
    todayOnlineSeconds.value = 0
  }

  return { user, token, totalOnlineSeconds, todayOnlineSeconds, setUser, setToken, logout, initUser, updateOnlineTime, setOnlineStats }
})
