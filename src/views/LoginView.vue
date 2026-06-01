<template>
  <div style="height: 100vh; display: flex; justify-content: center; align-items: center; background: #f5f7fa;">
    <el-card style="width: 400px; padding: 20px; text-align: center;">
      <h2 style="color: #409EFF; margin-bottom: 30px;">邮件系统 - 用户登录</h2>
      <el-input placeholder="请输入用户名" style="margin-bottom: 20px;"></el-input>
      <el-input placeholder="请输入密码" show-password style="margin-bottom: 20px;"></el-input>
      
      <el-button type="primary" style="width: 100%;" @click="handleLogin">登 录</el-button>

      <div style="text-align: center; font-size: 14px; color: #606266; margin-top: 16px;">
        <span>还没有账号？</span>
        <el-button type="link" @click="goRegister" style="padding: 0; color: #409EFF;">立即注册</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const handleLogin = () => {
  router.push('/dashboard') 
}

const goRegister = () => {
  router.push('/register')
}
</script>