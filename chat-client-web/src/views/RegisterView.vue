<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { registerService } from '@/api/user'
import type { RegisterForm } from '@/types/api'

const router = useRouter()
// 使用FormInstance类型保护，写错字段名、方法名时编辑器会提示
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = ref<RegisterForm>({
  username: '',
  password: '',
  rePassword: '',
})


// 自定义校验注册页面密码的函数
const checkRePassword = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'));
  } else if (value !== form.value.password) {
    callback(new Error('两次输入的密码不同'));
  } else {
    callback();
  }
};

// 使用FormRules类型保护，写错字段名、方法名时编辑器会提示
const rules: FormRules<RegisterForm> = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 1, max: 20, message: '用户名 1-20 位', trigger: 'blur' },
    { pattern: /^\w+$/, message: '仅支持字母、数字、下划线', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 1, max: 20, message: '密码 1-20 位', trigger: 'blur' },
  ],
  rePassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    //使用自定义的validator表单校验器：checkRePassword方法
    { validator: checkRePassword, trigger: 'blur' },
  ],
}

async function handleRegister(): Promise<void> {
  if (!formRef.value) return
  try {
    await formRef.value.validate() //先进行表单验证
  } catch {
    return //如果表单验证失败，则返回
  }

  loading.value = true //初始化加载状态
  try {
    //调用注册接口
    await registerService({ username: form.value.username, password: form.value.password, rePassword: form.value.rePassword, })
    ElMessage.success('注册成功，请登录')
    await router.push('/login')
  } catch {
    /* ElMessage 已在拦截器统一处理 */
  } finally {
    loading.value = false //最后将加载状态设置为false
  }
}
</script>
<template>
<div class="register-page">
  <div class="register-card">
    <h2>注册</h2>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="handleRegister">
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" placeholder="字母、数字、下划线，3-20 位" :prefix-icon="User" clearable />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="form.password" type="password" placeholder="6-20 位密码" :prefix-icon="Lock" show-password />
      </el-form-item>
      <el-form-item label="确认密码" prop="rePassword">
        <el-input v-model="form.rePassword" type="password" placeholder="再次输入密码" :prefix-icon="Lock" show-password />
      </el-form-item>
      <el-button type="primary" :loading="loading" class="register-btn" @click="handleRegister"> {{ loading ? '注册中...' : '注 册' }} </el-button>
    </el-form>
    <div class="login-tip"> 已有账号？ <router-link to="/login">返回登录</router-link>
    </div>
  </div>
</div>
</template>
<style scoped>
.register-page {
  display: flex;
  align-items: center;
  /* 垂直居中 */
  justify-content: center;
  /* 水平居中 */
  min-height: 100vh;
  background: #f5f7fa;
  /* background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); */
}

.register-card {
  width: 400px;
  padding: 2.5rem;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
}

.register-card h2 {
  margin-bottom: 1.5rem;
  text-align: center;
  color: #333;
}

.register-btn {
  width: 100%;
  height: 44px;
  font-size: 1rem;
  margin-top: 0.5rem;
}

.login-tip {
  margin-top: 1.2rem;
  text-align: center;
  font-size: 0.9rem;
  color: #666;
}

.login-tip a {
  color: #667eea;
  text-decoration: none;
  font-weight: 500;
}
</style>
