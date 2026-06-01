<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import { register } from '@/services/auth'
import { setAuth } from '@/stores/auth'

const router = useRouter()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  email: '',
  username: '',
  password: '',
  confirmPassword: '',
})

const validateConfirm = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入密码'))
    return
  }
  if (value !== form.password) {
    callback(new Error('两次密码不一致'))
    return
  }
  callback()
}

const rules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: ['blur', 'change'] },
  ],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 32, message: '用户名长度应在2-32之间', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度应在6-32之间', trigger: 'blur' },
  ],
  confirmPassword: [{ validator: validateConfirm, trigger: ['blur', 'change'] }],
}

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    loading.value = true
    const data = await register({
      email: form.email,
      username: form.username,
      password: form.password,
    })
    setAuth({
      token: data.token,
      user: {
        id: data.userId,
        email: data.email,
        username: data.username,
      },
    })
    ElMessage.success('注册成功')
    router.push({ name: 'dashboard' })
  } catch (error) {
    if (error?.message) {
      ElMessage.error(error.message)
    }
  } finally {
    loading.value = false
  }
}

const goLogin = () => {
  router.push({ name: 'login' })
}
</script>

<template>
  <div style="height: 100vh; display: flex; justify-content: center; align-items: center; background: #f5f7fa;">
    <el-card style="width: 440px; padding: 10px 20px; border-radius: 8px;" shadow="always">
      <h2 style="color: #409EFF; text-align: center; margin-bottom: 24px;">邮件系统 - 用户注册</h2>
      
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" show-password />
        </el-form-item>
        
        <el-form-item style="margin-top: 30px;">
          <el-button type="primary" :loading="loading" @click="handleSubmit" style="width: 100%;">注 册</el-button>
        </el-form-item>
        
        <div style="text-align: center; font-size: 14px; color: #606266; margin-top: 16px;">
          <span>已有账号？</span>
          <el-button type="link" @click="goLogin" style="padding: 0; color: #409EFF;">去登录</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>