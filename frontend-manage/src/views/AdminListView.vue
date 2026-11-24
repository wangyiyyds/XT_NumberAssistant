<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { createAdmin, fetchAdmins } from '../api/admins'
import ModalDialog from '../components/ui/ModalDialog.vue'
import type { Admin } from '../types/admin'
import { toast, toastError } from '../utils/toast'

const admins = ref<Admin[]>([])
const loading = ref(false)

// 新建弹窗
const modalVisible = ref(false)
const form = ref({ username: '', password: '', name: '' })
const error = ref('')
const busy = ref(false)

async function load(): Promise<void> {
  loading.value = true
  try {
    admins.value = await fetchAdmins()
  } catch (e) {
    toastError(e, '加载管理员失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)

function openCreate(): void {
  form.value = { username: '', password: '', name: '' }
  error.value = ''
  modalVisible.value = true
}

function fmtTime(iso?: string): string {
  return iso ? iso.replace('T', ' ').slice(0, 16) : '—'
}

async function save(): Promise<void> {
  error.value = ''
  if (!form.value.username.trim()) {
    error.value = '登录账号必填'
    return
  }
  if (!form.value.password) {
    error.value = '密码必填'
    return
  }
  busy.value = true
  try {
    await createAdmin({
      username: form.value.username.trim(),
      password: form.value.password,
      name: form.value.name.trim() || undefined,
    })
    toast('管理员已创建', 'success')
    modalVisible.value = false
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <h1>管理员管理</h1>
        <p class="sub">管理端登录账号，新建后立即可以新账号登录</p>
      </div>
      <button type="button" class="btn btn-primary" @click="openCreate">新建管理员</button>
    </div>

    <div class="panel">
      <table class="data-table">
        <thead>
          <tr>
            <th>账号</th>
            <th>姓名</th>
            <th>创建时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="admin in admins" :key="admin.id">
            <td>{{ admin.username }}</td>
            <td>{{ admin.name || '—' }}</td>
            <td class="faint">{{ fmtTime(admin.createdAt) }}</td>
          </tr>
        </tbody>
      </table>
      <p v-if="!loading && !admins.length" class="empty-state">暂无管理员</p>
    </div>

    <ModalDialog :visible="modalVisible" title="新建管理员" @close="modalVisible = false">
      <div class="form-item">
        <label>登录账号<span class="req">*</span>（全表唯一）</label>
        <input v-model="form.username" type="text" placeholder="如：operator01" autocomplete="off" />
      </div>
      <div class="form-item">
        <label>密码<span class="req">*</span></label>
        <input v-model="form.password" type="password" placeholder="登录密码" autocomplete="new-password" />
      </div>
      <div class="form-item">
        <label>姓名</label>
        <input v-model="form.name" type="text" placeholder="显示名称，可选" />
      </div>
      <p v-if="error" class="error-text err">{{ error }}</p>
      <template #footer>
        <button type="button" class="btn" @click="modalVisible = false">取消</button>
        <button type="button" class="btn btn-primary" :disabled="busy" @click="save">
          {{ busy ? '保存中…' : '创建' }}
        </button>
      </template>
    </ModalDialog>
  </div>
</template>

<style scoped>
.err {
  margin: 4px 0 0;
  font-size: 13px;
}
</style>
