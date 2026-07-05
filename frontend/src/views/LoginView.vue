<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import { login } from '@/services/auth'
import { setAuth } from '@/stores/auth'

const router = useRouter()
const route = useRoute()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  email: '',
  password: '',
})

const rules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: ['blur', 'change'] },
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    // 表单校验通过后提交登录
    await formRef.value.validate()
    loading.value = true
    const data = await login({
      email: form.email,
      password: form.password,
    })
    // 写入本地登录态
    setAuth({
      token: data.token,
      user: {
        id: data.userId,
        email: data.email,
        username: data.username,
      },
    })
    ElMessage.success('登录成功')
    // 回到原目标或默认页
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    router.push(redirect)
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
  <el-card class="auth-card" shadow="hover">
    <template #header>
      <div>用户登录</div>
    </template>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="form.email" placeholder="请输入邮箱" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="handleSubmit">登录</el-button>
      </el-form-item>
      <div class="auth-actions">
        <span>还没有账号？</span>
        <el-button type="text" @click="goRegister">去注册</el-button>
      </div>
    </el-form>
  </el-card>
</template>
