<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { loginService } from '@/api/user'
import { useUserStore } from '@/stores/userStore'
import type { LoginForm } from '@/types/api'
import { getUserInfoService } from '../api/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = ref<LoginForm>({
  username: '',
  password: '',
  // remember: false,
})

// 表单校验规则
const rules: FormRules<LoginForm> = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 1, max: 20, message: '用户名 1-20 位', trigger: 'blur' },
    { pattern: /^\w+$/, message: '仅支持字母、数字、下划线', trigger: 'blur', },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 1, max: 20, message: '密码 1-20 位', trigger: 'blur' },
  ],
}

//登录操作
async function handleLogin(): Promise<void> {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  //try/catch包裹一层
  try {
    //调用接口获取token
    const token = await loginService({ username: form.value.username, password: form.value.password, })
    //将token信息使用pinia进行状态管理
    userStore.setUser({ token })
    //根据token获取用户信息
    const userInfo = await getUserInfoService()
    //使用pinia进行用户信息管理
    userStore.setUser({
      token,
      userid: userInfo.userid,
      username: userInfo.username,
      nickname: userInfo.nickname,
      avatar_url: userInfo.avatar_url,
    })

    ElMessage.success('登录成功')

    const redirect = (route.query.redirect as string) || '/main/chat'
    await router.push(redirect)

  } catch {
    /* ElMessage 已在拦截器统一处理,不需要再进行处理，反而会报两次错 */
    // ElMessage.error('登录失败'+err)
  } finally {
    loading.value = false
  }
}
</script>
<template>
<div class="login-page">
  <!-- 表单 -->
  <div class="login-right">
    <div class="login-card">
      <h2>登录</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="User" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" :prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <!-- <div class="form-footer">
          <el-checkbox v-model="form.remember">记住我</el-checkbox>
          <a href="#" class="forgot-link">忘记密码？</a>
        </div> -->
        <el-button type="primary" :loading="loading" class="login-btn" @click="handleLogin"> {{ loading ? '登录中...' : '登 录' }} </el-button>
      </el-form>
      <div class="register-tip"> 没有账号？ <router-link to="/register">立即注册</router-link>
      </div>
    </div>
  </div>
</div>
</template>
<style scoped>
.login-page {
  display: flex;
  align-items: center;
  /* 垂直居中 */
  justify-content: center;
  /* 水平居中 */
  min-height: 100vh;
  background: #f5f7fa;
}

.login-right {
  width: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}

.login-card {
  width: 400px;
  padding: 2.5rem;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
}

.login-card h2 {
  margin-bottom: 1.5rem;
  font-size: 1.5rem;
  text-align: center;
  color: #333;
}

.form-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.forgot-link {
  color: #667eea;
  font-size: 0.9rem;
  text-decoration: none;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 1rem;
}

.register-tip {
  margin-top: 1.2rem;
  text-align: center;
  font-size: 0.9rem;
  color: #666;
}

.register-tip a {
  color: #667eea;
  text-decoration: none;
  font-weight: 500;
}

@media (max-width: 768px) {
  .login-left {
    display: none;
  }

  .login-right {
    width: 100%;
  }
}
</style>
