<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { clearAuth, currentUser, isAuthenticated } from '@/stores/auth'

const route = useRoute()
const router = useRouter()

const displayName = computed(() => {
  const user = currentUser.value
  if (!user) return ''
  return user.username || user.email || '用户'
})

const handleLogout = () => {
  clearAuth()
  if (route.name !== 'login') {
    router.push({ name: 'login' })
  }
}
</script>

<template>
  <el-container class="app-shell">
    <el-header class="app-header">
      <div class="app-brand" @click="router.push({ name: 'dashboard' })">
        邮件系统
      </div>
      <div class="app-actions">
        <template v-if="isAuthenticated">
          <span class="app-user">欢迎，{{ displayName }}</span>
          <el-button size="small" @click="handleLogout">退出登录</el-button>
        </template>
        <template v-else>
          <el-button size="small" @click="router.push({ name: 'login' })">登录</el-button>
          <el-button size="small" type="primary" @click="router.push({ name: 'register' })">
            注册
          </el-button>
        </template>
      </div>
    </el-header>
    <el-main class="app-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
  padding: 0 32px;
}

.app-brand {
  font-weight: 600;
  font-size: 18px;
  cursor: pointer;
}

.app-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-user {
  color: #606266;
  font-size: 14px;
}

.app-main {
  padding: 32px 16px 48px;
}
</style>
