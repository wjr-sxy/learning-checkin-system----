<template>
  <div class="profile-container">
    <div class="profile-sidebar">
      <div class="sidebar-title">个人中心</div>
      <el-menu
        :default-active="activeMenu"
        class="profile-menu"
        router
      >
        <el-menu-item index="/profile/info">
          <el-icon><User /></el-icon>
          <span>我的资料</span>
        </el-menu-item>
        <el-menu-item index="/profile/security">
          <el-icon><Lock /></el-icon>
          <span>账号安全</span>
        </el-menu-item>
        <el-menu-item index="/profile/points">
          <el-icon><Coin /></el-icon>
          <span>我的积分</span>
        </el-menu-item>
        <el-menu-item index="/profile/decorations" v-if="isStudent">
          <el-icon><MagicStick /></el-icon>
          <span>我的装扮</span>
        </el-menu-item>
        <el-menu-item index="/profile/logs">
          <el-icon><Document /></el-icon>
          <span>操作日志</span>
        </el-menu-item>
      </el-menu>
      
      <div class="sidebar-footer">
        <el-button type="default" class="back-btn" @click="goBack">
          <el-icon style="margin-right: 5px"><Back /></el-icon>
          返回首页
        </el-button>
      </div>
    </div>
    <div class="profile-content">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { User, Lock, Coin, MagicStick, Document, Back } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
const isStudent = computed(() => userStore.user?.role === 'USER')

const goBack = () => {
  const role = userStore.user?.role
  if (role === 'ADMIN') {
    router.push('/admin-dashboard')
  } else if (role === 'TEACHER') {
    router.push('/teacher-dashboard')
  } else {
    router.push('/student-dashboard')
  }
}
</script>

<style scoped>
.profile-container {
  display: flex;
  min-height: calc(100vh - 60px); /* Adjust based on top nav height */
  background-color: #f5f7fa;
  padding: 20px;
  gap: 20px;
}

.profile-sidebar {
  width: 240px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
  position: sticky;
  top: 80px; /* Adjusted for potential fixed header */
  height: fit-content;
  padding-bottom: 20px;
}

.sidebar-title {
  padding: 20px;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid #eee;
  margin-bottom: 10px;
  color: var(--el-text-color-primary);
}

.profile-menu {
  border-right: none;
}

.sidebar-footer {
  padding: 20px;
  border-top: 1px solid #eee;
}

.back-btn {
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}

.profile-content {
  flex: 1;
  min-width: 0; /* Fix flex overflow issues */
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
  padding: 30px;
  min-height: 600px;
}

/* Active menu item styling */
:deep(.el-menu-item.is-active) {
  background-color: var(--el-color-primary-light-9);
  border-left: 3px solid var(--el-color-primary);
  color: var(--el-color-primary);
}

:deep(.el-menu-item) {
  border-left: 3px solid transparent;
  margin: 4px 0;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 768px) {
  .profile-container {
    flex-direction: column;
    padding: 10px;
  }
  
  .profile-sidebar {
    width: 100%;
    position: relative;
    top: 0;
  }
  
  .profile-content {
    min-width: 100%;
  }
}
</style>
