<template>
  <ErrorBoundary>
    <router-view />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import ErrorBoundary from './components/ErrorBoundary.vue'
import { onMounted, onUnmounted, watch } from 'vue'
import { useUserStore } from './stores/user'
import { sendHeartbeat, getUserOnlineStats } from './api/stats'

const userStore = useUserStore()
let heartbeatTimer: any = null

const startHeartbeat = () => {
  if (heartbeatTimer) clearInterval(heartbeatTimer)
  
  // Initial fetch
  if (userStore.user) {
      getUserOnlineStats(userStore.user.id).then((res: any) => {
          userStore.setOnlineStats(res.data.totalSeconds, res.data.todaySeconds)
      })
  }

  heartbeatTimer = setInterval(() => {
    if (userStore.user && userStore.token) {
      const duration = 60 // 60 seconds
      sendHeartbeat({ userId: userStore.user.id, duration }).then(() => {
        userStore.updateOnlineTime(duration)
      }).catch(err => console.error('Heartbeat failed:', err.response || err))
    }
  }, 60000)
}

const stopHeartbeat = () => {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
}

watch(() => userStore.token, (newToken) => {
  if (newToken) {
    startHeartbeat()
  } else {
    stopHeartbeat()
  }
})

onMounted(() => {
  if (userStore.token) {
    startHeartbeat()
  }
})

onUnmounted(() => {
  stopHeartbeat()
})
</script>

<style>
body {
  margin: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}
</style>
