<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { deleteAgent, fetchAgents } from '../api/agents'
import { fetchDepartments } from '../api/departments'
import AgentFormModal from '../components/agent/AgentFormModal.vue'
import ConfirmDialog from '../components/ui/ConfirmDialog.vue'
import Pagination from '../components/ui/Pagination.vue'
import type { AgentAdmin } from '../types/agent'
import type { Department } from '../types/department'
import { toast, toastError } from '../utils/toast'

const router = useRouter()

const departments = ref<Department[]>([])
const agents = ref<AgentAdmin[]>([])
const total = ref(0)
const loading = ref(false)

// 查询条件与分页
const departmentFilter = ref('')
const keyword = ref('')
const page = ref(1)
const size = ref(10)

// 新建/编辑弹窗
const formVisible = ref(false)

// 删除确认
const removing = ref<AgentAdmin | null>(null)

async function load(): Promise<void> {
  loading.value = true
  try {
    const result = await fetchAgents({
      page: page.value,
      size: size.value,
      departmentId: departmentFilter.value || undefined,
      keyword: keyword.value.trim() || undefined,
    })
    agents.value = result.records
    total.value = result.total
  } catch (e) {
    toastError(e, '加载智能体失败')
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
    departments.value = await fetchDepartments()
  } catch {
    // 部门加载失败不阻塞列表
  }
  await load()
})

function openCreate(): void {
  formVisible.value = true
}

async function confirmRemove(): Promise<void> {
  if (!removing.value) return
  const target = removing.value
  removing.value = null
  try {
    await deleteAgent(target.id)
    toast('智能体已删除', 'success')
    await load()
  } catch (e) {
    toastError(e, '删除失败')
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
        <h1>智能体管理</h1>
        <p class="sub">创建流程：选部门 → 填智能体信息 → 配提示词 → 自动写入提示词库（v1 立即生效）</p>
      </div>
      <button type="button" class="btn btn-primary" @click="openCreate">新建智能体</button>
    </div>

    <div class="panel">
      <div class="toolbar head-toolbar">
        <select v-model="departmentFilter" class="ui-select" @change="search">
          <option value="">全部部门</option>
          <option v-for="d in departments" :key="d.id" :value="d.id">{{ d.name }}</option>
        </select>
        <input
          v-model="keyword"
          type="search"
          placeholder="搜索智能体名称"
          @keyup.enter="search"
        />
        <button type="button" class="btn btn-primary" @click="search">查询</button>
      </div>

      <table class="data-table">
        <thead>
          <tr>
            <th>名称</th>
            <th>部门</th>
            <th>分类</th>
            <th>预置</th>
            <th>生效提示词</th>
            <th>创建时间</th>
            <th class="ta-right">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="agent in agents" :key="agent.id">
            <td>
              <span class="link" @click="router.push(`/agents/${agent.id}`)">{{ agent.name }}</span>
            </td>
            <td class="dim">{{ agent.departmentName || '—' }}</td>
            <td class="dim">{{ agent.category || '—' }}</td>
            <td>
              <span v-if="agent.isPreset" class="tag tag-info">预置</span>
              <span v-else class="faint">否</span>
            </td>
            <td>
              <span v-if="agent.activePromptVersion != null" class="tag tag-accent">
                v{{ agent.activePromptVersion }}
              </span>
              <span v-else class="tag tag-warn">未发布</span>
            </td>
            <td class="faint">{{ fmtTime(agent.createdAt) }}</td>
            <td>
              <div class="cell-actions">
                <button type="button" class="btn btn-sm" @click="router.push(`/agents/${agent.id}`)">
                  提示词
                </button>
                <button type="button" class="btn btn-sm btn-danger" @click="removing = agent">删除</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-if="!loading && !agents.length" class="empty-state">没有符合条件的智能体</p>

      <Pagination :total="total" :current="page" :size="size" @change="onPageChange" />
    </div>

    <AgentFormModal
      :visible="formVisible"
      :agent="null"
      :departments="departments"
      @close="formVisible = false"
      @saved="load"
    />

    <ConfirmDialog
      :visible="!!removing"
      title="删除智能体"
      :message="`确定删除智能体「${removing?.name ?? ''}」吗？\n其全部提示词版本将级联删除，相关历史会话将无法继续对话。`"
      confirm-text="删除"
      danger
      @confirm="confirmRemove"
      @cancel="removing = null"
    />
  </div>
</template>

<style scoped>
.head-toolbar {
  margin-bottom: 14px;
}

.ta-right {
  text-align: right;
}
</style>
