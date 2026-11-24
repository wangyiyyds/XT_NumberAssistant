<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const username = ref('')
const password = ref('')
const error = ref('')
const busy = ref(false)

async function submit(): Promise<void> {
  error.value = ''
  busy.value = true
  try {
    await auth.login(username.value.trim(), password.value)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.push(redirect.startsWith('/login') ? '/' : redirect)
  } catch (submitError) {
    error.value = submitError instanceof Error ? submitError.message : '登录失败'
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card card">
      <div class="logo">先</div>
      <h1>先霆数字人助手管理端</h1>

      <form class="login-form" @submit.prevent="submit">
        <label>用户名</label>
        <input
          v-model="username"
          type="text"
          placeholder="请输入管理员用户名"
          autocomplete="username"
          required
        />

        <label>密码</label>
        <input
          v-model="password"
          type="password"
          placeholder="请输入密码"
          autocomplete="current-password"
          required
        />

        <p v-if="error" class="error-text msg">{{ error }}</p>
        <button class="btn btn-primary submit" type="submit" :disabled="busy">
          {{ busy ? '请稍候…' : '登录' }}
        </button>
      </form>

      <p class="hint faint">初始账号见管理端 DataSeeder 种子（admin）</p>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: radial-gradient(ellipse at top, #eaf3fe 0%, var(--bg) 60%);
}

.login-card {
  width: 380px;
  padding: 36px 32px;
  text-align: center;
}

.logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: var(--accent);
  color: #fff;
  font-size: 26px;
  font-weight: 700;
}

h1 {
  margin: 8px 0 20px;
  font-size: 20px;
}

form {
  display: flex;
  flex-direction: column;
  text-align: left;
}

label {
  margin: 10px 0 5px;
  color: var(--text-dim);
  font-size: 13px;
}

.submit {
  width: 100%;
  margin-top: 20px;
}

.msg {
  margin-top: 10px;
  font-size: 13px;
}

.hint {
  margin-top: 16px;
  font-size: 12px;
}
</style>
