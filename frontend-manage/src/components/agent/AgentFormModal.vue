<script setup lang="ts">
import { computed, ref, watch } from 'vue'

import { createAgent, updateAgent } from '../../api/agents'
import type { AgentDetail } from '../../types/agent'
import type { Department } from '../../types/department'
import { toast } from '../../utils/toast'
import ModalDialog from '../ui/ModalDialog.vue'
import SwitchToggle from '../ui/SwitchToggle.vue'

const props = defineProps<{
  visible: boolean
  /** null = 新建模式；非空 = 编辑模式 */
  agent: AgentDetail | null
  departments: Department[]
}>()

const emit = defineEmits<{ close: []; saved: [] }>()

const isEdit = computed(() => !!props.agent)

const departmentId = ref('')
const name = ref('')
const category = ref('')
const avatar = ref('')
const description = ref('')
const isPreset = ref(true)
/** 开场白：一行一条，提交时拆成数组 */
const startersText = ref('')
const instructions = ref('')
/** 仅新建时使用：初始提示词正文，留空用默认模板，自动落为 v1 ACTIVE */
const promptContent = ref('')
const error = ref('')
const busy = ref(false)

watch(
  () => props.visible,
  (v) => {
    if (v) reset()
  },
)

function reset(): void {
  const a = props.agent
  departmentId.value = a?.departmentId ?? props.departments[0]?.id ?? ''
  name.value = a?.name ?? ''
  category.value = a?.category ?? ''
  avatar.value = a?.avatar ?? ''
  description.value = a?.description ?? ''
  isPreset.value = a?.isPreset ?? true
  startersText.value = (a?.conversationStarters ?? []).join('\n')
  instructions.value = a?.instructions ?? ''
  promptContent.value = ''
  error.value = ''
}

async function submit(): Promise<void> {
  error.value = ''
  if (!name.value.trim()) {
    error.value = '智能体名称必填'
    return
  }
  if (!departmentId.value) {
    error.value = '请选择所属部门'
    return
  }
  busy.value = true
  try {
    const body = {
      departmentId: departmentId.value,
      name: name.value.trim(),
      description: description.value.trim() || undefined,
      avatar: avatar.value.trim() || undefined,
      category: category.value.trim() || undefined,
      isPreset: isPreset.value,
      conversationStarters: startersText.value
        .split('\n')
        .map((s) => s.trim())
        .filter(Boolean),
      instructions: instructions.value.trim() || undefined,
      promptContent: promptContent.value.trim() || undefined,
    }
    if (props.agent) {
      await updateAgent(props.agent.id, body)
      toast('智能体已更新', 'success')
    } else {
      await createAgent(body)
      toast('智能体已创建，提示词 v1 已自动发布生效', 'success')
    }
    emit('saved')
    emit('close')
  } catch (e) {
    error.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <ModalDialog :visible="visible" :title="isEdit ? '编辑智能体' : '新建智能体'" large @close="emit('close')">
    <div class="form-row">
      <div class="form-item">
        <label>所属部门<span class="req">*</span></label>
        <select v-model="departmentId" class="ui-select">
          <option v-for="d in departments" :key="d.id" :value="d.id">{{ d.name }}</option>
        </select>
      </div>
      <div class="form-item">
        <label>名称<span class="req">*</span></label>
        <input v-model="name" type="text" placeholder="如：爆款选题助手" />
      </div>
    </div>
    <div class="form-row">
      <div class="form-item">
        <label>分类</label>
        <input v-model="category" type="text" placeholder="如：流量部门" />
      </div>
      <div class="form-item">
        <label>头像 URL</label>
        <input v-model="avatar" type="text" placeholder="可留空" />
      </div>
    </div>
    <div class="form-item">
      <label>简介</label>
      <textarea v-model="description" rows="2" placeholder="展示在客户端智能体卡片上"></textarea>
    </div>
    <div class="form-item">
      <label>开场白（一行一条）</label>
      <textarea v-model="startersText" rows="3" placeholder="你好，请介绍一下你能做什么&#10;帮我出一个工作方案"></textarea>
    </div>
    <div class="form-item">
      <label>兜底人设 instructions（无生效提示词时客户端使用）</label>
      <textarea v-model="instructions" rows="2" placeholder="你是「XX」，请用专业、友好的中文回答用户问题。"></textarea>
    </div>
    <div v-if="!isEdit" class="form-item">
      <label>初始提示词正文（自动落为 v1 并立即生效）</label>
      <textarea v-model="promptContent" rows="5" placeholder="留空则使用默认模板：你是「名称」，隶属于XX部门…"></textarea>
    </div>
    <div class="form-item inline">
      <label>预置智能体（客户端按预置拉取展示）</label>
      <SwitchToggle v-model="isPreset" />
    </div>
    <p v-if="error" class="error-text err">{{ error }}</p>
    <template #footer>
      <button type="button" class="btn" @click="emit('close')">取消</button>
      <button type="button" class="btn btn-primary" :disabled="busy" @click="submit">
        {{ busy ? '保存中…' : '保存' }}
      </button>
    </template>
  </ModalDialog>
</template>

<style scoped>
.form-item.inline {
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
}

.err {
  margin: 4px 0 0;
  font-size: 13px;
}
</style>
