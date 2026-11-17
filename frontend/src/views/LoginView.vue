<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const mode = ref<'login' | 'register'>('login')
const email = ref('')
const password = ref('')
const name = ref('')
const error = ref('')
const busy = ref(false)

async function submit(): Promise<void> {
  error.value = ''
  busy.value = true
  try {
    if (mode.value === 'login') {
      await auth.login(email.value.trim(), password.value)
    } else {
      await auth.register(email.value.trim(), password.value, name.value.trim() || '新用户')
    }

    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.push(redirect.startsWith('/login') ? '/' : redirect)
  } catch (submitError) {
    error.value = submitError instanceof Error ? submitError.message : '操作失败'
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card card">
      <div class="logo">先</div>
      <h1>先霆AI超级系统</h1>

      <div class="tabs">
        <button
          type="button"
          :class="{ active: mode === 'login' }"
          @click="mode = 'login'; error = ''"
        >
          登录
        </button>
        <button
          type="button"
          :class="{ active: mode === 'register' }"
          @click="mode = 'register'; error = ''"
        >
          注册
        </button>
      </div>

      <form class="login-form" @submit.prevent="submit">
        <label>邮箱</label>
        <input
          v-model="email"
          type="email"
          placeholder="you@example.com"
          autocomplete="username"
          required
        />

        <template v-if="mode === 'register'">
          <label>昵称</label>
          <input v-model="name" type="text" placeholder="新用户" />
        </template>

        <label>密码</label>
        <input
          v-model="password"
          type="password"
          :placeholder="mode === 'register' ? '至少 6 位' : '请输入密码'"
          autocomplete="current-password"
          required
        />

        <p v-if="error" class="error-text msg">{{ error }}</p>
        <button class="btn btn-primary submit" type="submit" :disabled="busy">
          {{ busy ? '请稍候…' : mode === 'login' ? '登录' : '注册并登录' }}
        </button>
      </form>

      <p class="hint faint">演示账号：test@xianting.cn / 123456</p>
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

.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 18px;
}

.tabs button {
  flex: 1;
  padding: 8px;
  border-radius: 8px;
  background: var(--panel-2);
  color: var(--text-dim);
}

.tabs button.active {
  background: var(--accent);
  color: #fff;
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
