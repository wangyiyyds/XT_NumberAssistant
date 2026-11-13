<template>
  <div class="login-page">
    <div class="login-card card">
      <div class="logo">🏔</div>
      <h1>珠峰AI超级系统</h1>
      <div class="tabs">
        <button :class="{ active: mode === 'login' }" @click="mode = 'login'">登录</button>
        <button :class="{ active: mode === 'register' }" @click="mode = 'register'">注册</button>
      </div>
      <form @submit.prevent="submit">
        <label>邮箱</label>
        <input v-model="email" type="email" placeholder="you@example.com" autocomplete="username" required />
        <label v-if="mode === 'register'">昵称</label>
        <input v-if="mode === 'register'" v-model="name" type="text" placeholder="新用户" />
        <label>密码</label>
        <input v-model="password" type="password" :placeholder="mode === 'register' ? '至少6位' : '请输入密码'"
          autocomplete="current-password" required />
        <p v-if="error" class="error-text msg">{{ error }}</p>
        <button class="btn btn-primary submit" type="submit" :disabled="busy">
          {{ busy ? '请稍候…' : mode === 'login' ? '登录' : '注册并登录' }}
        </button>
      </form>
      <p class="hint faint">演示账号：test@zhufeng.cn / 123456</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

const mode = ref<'login' | 'register'>('login')
const email = ref('')
const password = ref('')
const name = ref('')
const error = ref('')
const busy = ref(false)

async function submit() {
  error.value = ''
  busy.value = true
  try {
    if (mode.value === 'login') await auth.login(email.value.trim(), password.value)
    else await auth.register(email.value.trim(), password.value, name.value.trim() || '新用户')
    router.push('/')
  } catch (e: any) {
    error.value = e?.message || '操作失败'
  } finally {
    busy.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex; align-items: center; justify-content: center;
  background: radial-gradient(ellipse at top, #2a2a2a, var(--bg));
}
.login-card { width: 380px; padding: 36px 32px; text-align: center; }
.logo { font-size: 40px; }
h1 { font-size: 20px; margin: 8px 0 20px; }
.tabs { display: flex; gap: 8px; margin-bottom: 18px; }
.tabs button {
  flex: 1; padding: 8px; border-radius: 8px;
  color: var(--text-dim); background: var(--panel-2);
}
.tabs button.active { background: var(--accent); color: #fff; }
form { display: flex; flex-direction: column; text-align: left; }
label { margin: 10px 0 5px; color: var(--text-dim); font-size: 13px; }
.submit { margin-top: 20px; width: 100%; }
.msg { margin-top: 10px; font-size: 13px; }
.hint { margin-top: 16px; font-size: 12px; }
</style>
