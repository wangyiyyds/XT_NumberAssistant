<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { fetchAgent } from '../api/agents'
import { fetchDepartments } from '../api/departments'
import {
  createDraft,
  fetchPrompt,
  fetchPrompts,
  publishPrompt,
  rollbackPrompt,
  updateDraft,
} from '../api/prompts'
import AgentFormModal from '../components/agent/AgentFormModal.vue'
import ConfirmDialog from '../components/ui/ConfirmDialog.vue'
import ModalDialog from '../components/ui/ModalDialog.vue'
import type { AgentDetail } from '../types/agent'
import type { Department } from '../types/department'
import type { Prompt, PromptDetail } from '../types/prompt'
import { toast, toastError } from '../utils/toast'

const route = useRoute()
const router = useRouter()
const agentId = String(route.params.agentId ?? '')

const agent = ref<AgentDetail | null>(null)
const prompts = ref<Prompt[]>([])
const departments = ref<Department[]>([])
const loading = ref(false)
const notFound = ref(false)

// 编辑智能体弹窗
const editVisible = ref(false)

// 新建/编辑草稿弹窗
const draftVisible = ref(false)
const draftEditing = ref<Prompt | null>(null)
const draftForm = ref({ name: '', versionTag: '', description: '', content: '' })
const draftError = ref('')
const draftBusy = ref(false)

// 查看正文弹窗
const viewing = ref<PromptDetail | null>(null)

// 发布/回滚确认
const confirming = ref<{ type: 'publish' | 'rollback'; prompt: Prompt } | null>(null)

async function load(): Promise<void> {
  loading.value = true
  try {
    const [a, p] = await Promise.all([fetchAgent(agentId), fetchPrompts(agentId)])
    agent.value = a
    prompts.value = p
    notFound.value = false
  } catch (e) {
    if (e instanceof Error && (e as Error & { status?: number }).status === 404) {
      notFound.value = true
    } else {
      toastError(e, '加载智能体失败')
    }
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await load()
  try {
    departments.value = await fetchDepartments()
  } catch {
    // 部门加载失败只影响编辑弹窗
  }
})

function openEditAgent(): void {
  editVisible.value = true
}

function openCreateDraft(): void {
  draftEditing.value = null
  draftForm.value = { name: '', versionTag: '', description: '', content: '' }
  draftError.value = ''
  draftVisible.value = true
}

async function openEditDraft(p: Prompt): Promise<void> {
  try {
    const detail = await fetchPrompt(p.id)
    draftEditing.value = p
    draftForm.value = {
      name: detail.name ?? '',
      versionTag: detail.versionTag ?? '',
      description: detail.description ?? '',
      content: detail.content ?? '',
    }
    draftError.value = ''
    draftVisible.value = true
  } catch (e) {
    toastError(e, '加载版本详情失败')
  }
}

async function saveDraft(): Promise<void> {
  draftError.value = ''
  if (!draftForm.value.content.trim()) {
    draftError.value = '提示词正文必填'
    return
  }
  draftBusy.value = true
  try {
    const body = {
      name: draftForm.value.name.trim() || undefined,
      versionTag: draftForm.value.versionTag.trim() || undefined,
      description: draftForm.value.description.trim() || undefined,
      content: draftForm.value.content,
    }
    if (draftEditing.value) {
      await updateDraft(draftEditing.value.id, body)
      toast('草稿已保存', 'success')
    } else {
      await createDraft(agentId, body)
      toast('新版本草稿已创建（版本号自动 +1）', 'success')
    }
    draftVisible.value = false
    await load()
  } catch (e) {
    draftError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    draftBusy.value = false
  }
}

async function viewVersion(p: Prompt): Promise<void> {
  try {
    viewing.value = await fetchPrompt(p.id)
  } catch (e) {
    toastError(e, '加载版本详情失败')
  }
}

async function doConfirm(): Promise<void> {
  const action = confirming.value
  if (!action) return
  confirming.value = null
  try {
    if (action.type === 'publish') {
      await publishPrompt(action.prompt.id)
      toast(`v${action.prompt.version} 已发布生效，客户端将使用新提示词`, 'success')
    } else {
      await rollbackPrompt(action.prompt.id)
      toast(`已回滚：以 v${action.prompt.version} 内容生成新版本并发布`, 'success')
    }
    await load()
  } catch (e) {
    toastError(e, action.type === 'publish' ? '发布失败' : '回滚失败')
    await load()
  }
}

function statusClass(status: string): string {
  if (status === 'ACTIVE') return 'tag tag-accent'
  if (status === 'ARCHIVED') return 'tag tag-warn'
  return 'tag'
}

function statusText(status: string): string {
  if (status === 'ACTIVE') return '生效中'
  if (status === 'ARCHIVED') return '已归档'
  return '草稿'
}

function fmtTime(iso?: string): string {
  return iso ? iso.replace('T', ' ').slice(0, 16) : '—'
}
</script>

<template>
  <div v-if="notFound" class="empty-state">
    <p>智能体不存在或已被删除</p>
    <button type="button" class="btn back" @click="router.push('/agents')">返回列表</button>
  </div>

  <div v-else>
    <div class="page-head">
      <div>
        <h1>
          <button type="button" class="back-link" @click="router.push('/agents')">智能体管理</button>
          <span class="sep">/</span>
          {{ agent?.name ?? '…' }}
        </h1>
        <p class="sub">提示词版本自动迭代：新建草稿 → 发布置为生效 → 旧版本自动归档，客户端即时生效</p>
      </div>
      <button type="button" class="btn" :disabled="!agent" @click="openEditAgent">编辑智能体</button>
    </div>

    <div class="panel">
      <div class="panel-head"><h2>基本信息</h2></div>
      <div v-if="agent" class="info-grid">
        <div class="info-item"><span class="faint">部门</span>{{ agent.departmentName || '—' }}</div>
        <div class="info-item"><span class="faint">分类</span>{{ agent.category || '—' }}</div>
        <div class="info-item">
          <span class="faint">预置</span>{{ agent.isPreset ? '是' : '否' }}
        </div>
        <div class="info-item">
          <span class="faint">当前生效</span>
          <span v-if="agent.activePrompt" class="tag tag-accent">
            v{{ agent.activePrompt.version }}
          </span>
          <span v-else class="tag tag-warn">未发布</span>
        </div>
        <div class="info-item wide"><span class="faint">简介</span>{{ agent.description || '—' }}</div>
        <div class="info-item wide">
          <span class="faint">开场白</span>
          <span v-if="(agent.conversationStarters ?? []).length">
            {{ (agent.conversationStarters ?? []).join('；') }}
          </span>
          <span v-else>—</span>
        </div>
      </div>
      <p v-else class="empty-state">{{ loading ? '加载中…' : '暂无数据' }}</p>
    </div>

    <div class="panel">
      <div class="panel-head">
        <h2>提示词版本（共 {{ prompts.length }} 个）</h2>
        <button type="button" class="btn btn-primary btn-sm" @click="openCreateDraft">新建版本</button>
      </div>

      <table class="data-table">
        <thead>
          <tr>
            <th>版本</th>
            <th>名称</th>
            <th>状态</th>
            <th>血缘</th>
            <th>更新人</th>
            <th>创建时间</th>
            <th>发布时间</th>
            <th class="ta-right">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in prompts" :key="p.id">
            <td>
              v{{ p.version }}
              <span v-if="p.versionTag" class="tag ver-tag">{{ p.versionTag }}</span>
            </td>
            <td class="dim">{{ p.name || '—' }}</td>
            <td><span :class="statusClass(p.status)">{{ statusText(p.status) }}</span></td>
            <td class="faint">{{ p.parentVersion != null ? `基于 v${p.parentVersion}` : '首版' }}</td>
            <td class="faint">{{ p.updatedBy || p.createdBy || '—' }}</td>
            <td class="faint">{{ fmtTime(p.createdAt) }}</td>
            <td class="faint">{{ fmtTime(p.publishedAt) }}</td>
            <td>
              <div class="cell-actions">
                <button type="button" class="btn btn-sm" @click="viewVersion(p)">查看</button>
                <button v-if="p.status === 'DRAFT'" type="button" class="btn btn-sm" @click="openEditDraft(p)">
                  编辑
                </button>
                <button
                  v-if="p.status === 'DRAFT'"
                  type="button"
                  class="btn btn-sm btn-primary"
                  @click="confirming = { type: 'publish', prompt: p }"
                >
                  发布
                </button>
                <button
                  v-if="p.status === 'ARCHIVED'"
                  type="button"
                  class="btn btn-sm"
                  @click="confirming = { type: 'rollback', prompt: p }"
                >
                  回滚
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-if="!loading && !prompts.length" class="empty-state">暂无提示词版本，点击右上角新建</p>
    </div>

    <AgentFormModal
      :visible="editVisible"
      :agent="agent"
      :departments="departments"
      @close="editVisible = false"
      @saved="load"
    />

    <ModalDialog
      :visible="draftVisible"
      :title="draftEditing ? `编辑草稿 v${draftEditing.version}` : '新建提示词版本（草稿）'"
      large
      @close="draftVisible = false"
    >
      <div class="form-row">
        <div class="form-item">
          <label>版本名称</label>
          <input v-model="draftForm.name" type="text" placeholder="如：带货话术强化版" />
        </div>
        <div class="form-item">
          <label>版本标签</label>
          <input v-model="draftForm.versionTag" type="text" placeholder="如：beta" />
        </div>
      </div>
      <div class="form-item">
        <label>版本说明</label>
        <input v-model="draftForm.description" type="text" placeholder="这次改了什么" />
      </div>
      <div class="form-item">
        <label>提示词正文（作为 system prompt 提交给大模型）<span class="req">*</span></label>
        <textarea v-model="draftForm.content" rows="12" placeholder="你是「XX」……"></textarea>
      </div>
      <p v-if="draftError" class="error-text err">{{ draftError }}</p>
      <template #footer>
        <button type="button" class="btn" @click="draftVisible = false">取消</button>
        <button type="button" class="btn btn-primary" :disabled="draftBusy" @click="saveDraft">
          {{ draftBusy ? '保存中…' : '保存草稿' }}
        </button>
      </template>
    </ModalDialog>

    <ModalDialog :visible="!!viewing" :title="`v${viewing?.version ?? ''} 提示词正文`" large @close="viewing = null">
      <pre class="prompt-pre">{{ viewing?.content }}</pre>
    </ModalDialog>

    <ConfirmDialog
      :visible="confirming?.type === 'publish'"
      title="发布提示词"
      :message="`确定发布 v${confirming?.prompt.version ?? ''} 吗？\n当前生效版本将自动归档，客户端后续对话立即使用新版本。`"
      confirm-text="发布"
      @confirm="doConfirm"
      @cancel="confirming = null"
    />
    <ConfirmDialog
      :visible="confirming?.type === 'rollback'"
      title="回滚提示词"
      :message="`将以 v${confirming?.prompt.version ?? ''} 的内容生成一个新版本并立即发布，当前生效版本自动归档。历史版本本身不会被修改。`"
      confirm-text="回滚"
      danger
      @confirm="doConfirm"
      @cancel="confirming = null"
    />
  </div>
</template>

<style scoped>
.back-link {
  color: var(--text-faint);
  font-size: 14px;
  font-weight: 400;
}
.back-link:hover {
  color: var(--accent);
}

.sep {
  margin: 0 6px;
  color: var(--text-faint);
  font-weight: 400;
}

.back {
  margin-top: 12px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 10px 16px;
  font-size: 13px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-item.wide {
  grid-column: 1 / -1;
  align-items: baseline;
}

.info-item .faint {
  flex: 0 0 70px;
  font-size: 12px;
}

.ta-right {
  text-align: right;
}

.ver-tag {
  margin-left: 4px;
}

.err {
  margin: 4px 0 0;
  font-size: 13px;
}
</style>
