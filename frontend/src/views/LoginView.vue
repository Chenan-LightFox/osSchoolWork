<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import { login } from '@/services/auth'
import { setAuth } from '@/stores/auth'

const router = useRouter()

const loading = ref(false)

const form = reactive({
  email: '',
  password: '',
})

const handleLogin = async () => {
  if (!form.email) {
    ElMessage.warning('请输入邮箱')
    return
  }
  if (!form.password) {
    ElMessage.warning('请输入密码')
    return
  }
  try {
    loading.value = true
    const data = await login({
      email: form.email,
      password: form.password,
    })
    setAuth({
      token: data.token,
      user: data.user,
    })
    ElMessage.success('登录成功')
    router.push({ name: 'dashboard' })
  } catch (error) {
    if (error?.message) {
      ElMessage.error(error.message)
    }
  } finally {
    loading.value = false
  }
}

const goRegister = () => {
  router.push({ name: 'register' })
}
</script>

<template>
  <div style="position: fixed; inset: 0; display: flex; justify-content: center; align-items: center; background: #f5f7fa;">
    <el-card style="width: 100%; max-width: 400px; padding: 20px; text-align: center;">
      <h2 style="color: #409EFF; margin-bottom: 30px;">邮件系统 - 用户登录</h2>
      <el-input v-model="form.email" placeholder="请输入邮箱" style="margin-bottom: 20px;"></el-input>
      <el-input v-model="form.password" placeholder="请输入密码" show-password style="margin-bottom: 20px;"></el-input>

      <el-button type="primary" :loading="loading" style="width: 100%;" @click="handleLogin">登 录</el-button>

      <div style="text-align: center; font-size: 14px; color: #606266; margin-top: 16px;">
        <span>还没有账号？</span>
        <el-button type="link" @click="goRegister" style="padding: 0; color: #409EFF; border: none;">立即注册</el-button>
      </div>
    </el-card>
  </div>
</template>