<script setup>
import { computed } from 'vue'
import { currentUser, isAuthenticated } from '@/stores/auth'

const user = computed(() => currentUser.value || {})
</script>

<template>
  <div class="dashboard-grid">
    <el-card shadow="hover">
      <template #header>
        <div>登录状态</div>
      </template>
      <el-result
        v-if="isAuthenticated"
        icon="success"
        title="已登录"
        sub-title="用户系统已成功连接后端 JWT"
      />
      <el-result v-else icon="warning" title="未登录" sub-title="请先登录" />
    </el-card>

    <el-card shadow="hover" style="margin-top: 24px">
      <template #header>
        <div>当前用户信息</div>
      </template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="用户ID">{{ user.id || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ user.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ user.username || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-alert
        type="info"
        show-icon
        :closable="false"
        style="margin-top: 16px"
        title="前端会在本地保存 token，刷新页面后仍保持登录状态。"
      />
    </el-card>
  </div>
</template>
