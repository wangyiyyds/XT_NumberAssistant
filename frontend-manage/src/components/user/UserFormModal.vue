<script setup lang="ts">
import { ref, watch } from 'vue'

import { createUser } from '../../api/users'
import type { Plan } from '../../types/plan'
import { toast } from '../../utils/toast'
import ModalDialog from '../ui/ModalDialog.vue'

const props = defineProps<{
  visible: boolean
  plans: Plan[]
}>()

const emit = defineEmits<{ close: []; saved: [] }>()

const email = ref('')
const password = ref('')
const name = ref('')
const planId = ref('')
const error = ref('')
const busy = ref(false)

watch(
  () => props.visible,
  (v) => {
    if (v) reset()
  },
)

function reset(): void {
  email.value = ''
  password.value = ''
  name.value = ''
  planId.value = ''
  error.value = ''
}

async function submit(): Promise<void> {
  error.value = ''
  if (!email.value.trim()) {
    error.value = '邮箱必填'
    return
  }
  if (password.value.length < 6) {
    error.value = '初始密码至少6位'
    return
  }
  busy.value = true
  try {
    await createUser({
      email: email.value.trim(),
      password: password.value,
      name: name.value.trim() || undefined,
      planId: planId.value || undefined,
    })
    toast(`已创建用户「${email.value.trim()}」`, 'success')
    emit('saved')
    emit('close')
  } catch (e) {
    error.value = e instanceof Error ? e.message : '创建失败'
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <ModalDialog :visible="visible" title="添加用户" @close="emit('close')">
    <div class="form-row">
      <div class="form-item">
        <label>邮箱<span class="req">*</span></label>
        <input v-model="email" type="text" placeholder="登录邮箱，需唯一" />
      </div>
      <div class="form-item">
        <label>昵称</label>
        <input v-model="name" type="text" placeholder="留空默认「新用户」" />
      </div>
    </div>
    <div class="form-item">
      <label>初始密码<span class="req">*</span></label>
      <input v-model="password" type="password" placeholder="至少 6 位" />
    </div>
    <div class="form-item">
      <label>套餐（其等级决定该用户可访问的部门）</label>
      <select v-model="planId" class="ui-select">
        <option value="">暂不分配</option>
        <option v-for="p in plans" :key="p.id" :value="p.id">{{ p.name }}（Lv.{{ p.level }}）</option>
      </select>
    </div>
    <p class="hint">创建后自动初始化积分记录，邮箱即可在客户端登录。</p>
    <p v-if="error" class="error-text err">{{ error }}</p>
    <template #footer>
      <button type="button" class="btn" @click="emit('close')">取消</button>
      <button type="button" class="btn btn-primary" :disabled="busy" @click="submit">
        {{ busy ? '创建中…' : '创建' }}
      </button>
    </template>
  </ModalDialog>
</template>

<style scoped>
.err {
  margin: 4px 0 0;
  font-size: 13px;
}

.hint {
  margin: 10px 0 0;
  font-size: 12px;
  color: var(--text-dim, #8a94a6);
}
</style>
