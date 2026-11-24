<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { fetchPlans } from '../api/plans'
import { assignPlan, fetchUsers, setUserDisabled } from '../api/users'
import UserFormModal from '../components/user/UserFormModal.vue'
import Pagination from '../components/ui/Pagination.vue'
import SwitchToggle from '../components/ui/SwitchToggle.vue'
import type { Plan } from '../types/plan'
import type { UserAdmin } from '../types/user'
import { toast, toastError } from '../utils/toast'

const plans = ref<Plan[]>([])
const users = ref<UserAdmin[]>([])
const total = ref(0)
const loading = ref(false)

// 查询条件与分页
const keyword = ref('')
const planFilter = ref('')
const page = ref(1)
const size = ref(10)

// 添加用户弹窗
const showCreate = ref(false)

async function load(): Promise<void> {
  loading.value = true
  try {
    const result = await fetchUsers({
      page: page.value,
      size: size.value,
      keyword: keyword.value.trim() || undefined,
      planId: planFilter.value || undefined,
    })
    users.value = result.records
    total.value = result.total
  } catch (e) {
    toastError(e, '加载用户失败')
  } finally {
    loading.value = false
  }
}

function search(): void {
  page.value = 1
  void load()
}

function onPageChange(p: number): void {
  page.value = p
  void load()
}

onMounted(async () => {
  try {
    plans.value = await fetchPlans()
  } catch {
    // 套餐加载失败不阻塞用户列表
  }
  await load()
})

/** 行内直选套餐：plan_id 即该用户的功能权限依据 */
async function changePlan(user: UserAdmin, planPublicId: string): Promise<void> {
  try {
    await assignPlan(user.id, planPublicId || null)
    toast(planPublicId ? `已将「${user.email}」设置为对应套餐` : `已清除「${user.email}」的套餐`, 'success')
    await load()
  } catch (e) {
    toastError(e, '设置套餐失败')
    await load()
  }
}

async function toggleDisabled(user: UserAdmin, value: boolean): Promise<void> {
  try {
    await setUserDisabled(user.id, value)
    toast(value ? `已禁用「${user.email}」` : `已启用「${user.email}」`, 'success')
    await load()
  } catch (e) {
    toastError(e, '操作失败')
  }
}

function fmtTime(iso?: string): string {
  return iso ? iso.replace('T', ' ').slice(0, 16) : '—'
}
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <h1>用户管理</h1>
        <p class="sub">给用户分配套餐：套餐等级决定该用户在客户端可访问哪些部门（功能）</p>
      </div>
      <button type="button" class="btn btn-primary" @click="showCreate = true">添加用户</button>
    </div>

    <div class="panel">
      <div class="toolbar head-toolbar">
        <input
          v-model="keyword"
          type="search"
          placeholder="搜索邮箱 / 昵称"
          @keyup.enter="search"
        />
        <select v-model="planFilter" class="ui-select">
          <option value="">全部套餐</option>
          <option v-for="p in plans" :key="p.id" :value="p.id">{{ p.name }}（Lv.{{ p.level }}）</option>
        </select>
        <button type="button" class="btn btn-primary" @click="search">查询</button>
      </div>

      <table class="data-table">
        <thead>
          <tr>
            <th>邮箱</th>
            <th>昵称</th>
            <th>套餐</th>
            <th>积分余额</th>
            <th>禁用</th>
            <th>注册时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.id">
            <td>{{ user.email }}</td>
            <td class="dim">{{ user.name || '—' }}</td>
            <td>
              <select
                class="ui-select ui-select-sm"
                :value="user.planId ?? ''"
                @change="changePlan(user, ($event.target as HTMLSelectElement).value)"
              >
                <option value="">未分配</option>
                <option v-for="p in plans" :key="p.id" :value="p.id">{{ p.name }}</option>
              </select>
            </td>
            <td class="dim">{{ user.tokenCredits ?? '—' }}</td>
            <td>
              <SwitchToggle
                :model-value="user.disabled ?? false"
                @update:model-value="toggleDisabled(user, $event)"
              />
            </td>
            <td class="faint">{{ fmtTime(user.createdAt) }}</td>
          </tr>
        </tbody>
      </table>
      <p v-if="!loading && !users.length" class="empty-state">没有符合条件的用户</p>

      <Pagination :total="total" :current="page" :size="size" @change="onPageChange" />
    </div>

    <UserFormModal
      :visible="showCreate"
      :plans="plans"
      @close="showCreate = false"
      @saved="load"
    />
  </div>
</template>

<style scoped>
.head-toolbar {
  margin-bottom: 14px;
}
</style>
