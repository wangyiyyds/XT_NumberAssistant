<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { fetchDepartments } from '../api/departments'
import { createPlan, deletePlan, fetchPlans, setPlanDepartments, updatePlan } from '../api/plans'
import ConfirmDialog from '../components/ui/ConfirmDialog.vue'
import ModalDialog from '../components/ui/ModalDialog.vue'
import type { Department } from '../types/department'
import type { Plan } from '../types/plan'
import { toast, toastError } from '../utils/toast'

const plans = ref<Plan[]>([])
const departments = ref<Department[]>([])
const loading = ref(false)

// 新建/编辑弹窗（含开放部门勾选）
const modalVisible = ref(false)
const editing = ref<Plan | null>(null)
const form = ref({ name: '', level: 1, description: '', deptIds: [] as string[] })
const error = ref('')
const busy = ref(false)

// 删除确认
const removing = ref<Plan | null>(null)

async function load(): Promise<void> {
  loading.value = true
  try {
    const [p, d] = await Promise.all([fetchPlans(), fetchDepartments()])
    plans.value = p
    departments.value = d
  } catch (e) {
    toastError(e, '加载套餐失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)

function openCreate(): void {
  editing.value = null
  form.value = { name: '', level: plans.value.length + 1, description: '', deptIds: [] }
  error.value = ''
  modalVisible.value = true
}

function openEdit(plan: Plan): void {
  editing.value = plan
  form.value = {
    name: plan.name,
    level: plan.level,
    description: plan.description ?? '',
    deptIds: (plan.departments ?? []).map((d) => d.id),
  }
  error.value = ''
  modalVisible.value = true
}

function toggleDept(id: string): void {
  const idx = form.value.deptIds.indexOf(id)
  if (idx >= 0) form.value.deptIds.splice(idx, 1)
  else form.value.deptIds.push(id)
}

async function save(): Promise<void> {
  error.value = ''
  if (!form.value.name.trim()) {
    error.value = '套餐名称必填'
    return
  }
  if (!form.value.level || Number(form.value.level) < 1) {
    error.value = '套餐等级须为不小于 1 的整数'
    return
  }
  busy.value = true
  try {
    const body = {
      name: form.value.name.trim(),
      level: Number(form.value.level),
      description: form.value.description.trim() || undefined,
    }
    const saved = editing.value ? await updatePlan(editing.value.id, body) : await createPlan(body)
    // 基本信息保存成功后全量替换开放部门关系
    await setPlanDepartments(saved.id, form.value.deptIds)
    toast(editing.value ? '套餐已更新' : '套餐已创建', 'success')
    modalVisible.value = false
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    busy.value = false
  }
}

async function confirmRemove(): Promise<void> {
  if (!removing.value) return
  const target = removing.value
  removing.value = null
  try {
    await deletePlan(target.id)
    toast('套餐已删除', 'success')
    await load()
  } catch (e) {
    toastError(e, '删除失败')
  }
}
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <h1>套餐管理</h1>
        <p class="sub">套餐等级决定用户可访问的功能：为每个套餐勾选开放的部门（客户端左侧导航）</p>
      </div>
      <button type="button" class="btn btn-primary" @click="openCreate">新建套餐</button>
    </div>

    <div class="panel">
      <table class="data-table">
        <thead>
          <tr>
            <th>名称</th>
            <th>等级</th>
            <th>描述</th>
            <th>开放部门</th>
            <th class="ta-right">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="plan in plans" :key="plan.id">
            <td>{{ plan.name }}</td>
            <td><span class="tag tag-info">Lv.{{ plan.level }}</span></td>
            <td class="dim">{{ plan.description || '—' }}</td>
            <td>
              <div class="dept-tags">
                <span v-for="d in plan.departments ?? []" :key="d.id" class="tag">{{ d.name }}</span>
                <span v-if="!(plan.departments ?? []).length" class="faint">未配置</span>
              </div>
            </td>
            <td>
              <div class="cell-actions">
                <button type="button" class="btn btn-sm" @click="openEdit(plan)">编辑</button>
                <button type="button" class="btn btn-sm btn-danger" @click="removing = plan">删除</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-if="!loading && !plans.length" class="empty-state">暂无套餐，点击右上角新建</p>
    </div>

    <ModalDialog
      :visible="modalVisible"
      :title="editing ? `编辑套餐「${editing.name}」` : '新建套餐'"
      large
      @close="modalVisible = false"
    >
      <div class="form-row">
        <div class="form-item">
          <label>套餐名称<span class="req">*</span></label>
          <input v-model="form.name" type="text" placeholder="如：专业版" />
        </div>
        <div class="form-item">
          <label>套餐等级<span class="req">*</span>（数字越大功能越全，全表唯一）</label>
          <input v-model.number="form.level" type="number" min="1" />
        </div>
      </div>
      <div class="form-item">
        <label>描述</label>
        <input v-model="form.description" type="text" placeholder="一句话说明该套餐的能力范围" />
      </div>
      <div class="form-item">
        <label>开放部门（该套餐用户可见/可用的客户端左侧导航）</label>
        <div class="check-grid">
          <label
            v-for="d in departments"
            :key="d.id"
            class="check-item"
            :class="{ checked: form.deptIds.includes(d.id) }"
          >
            <input
              type="checkbox"
              :checked="form.deptIds.includes(d.id)"
              @change="toggleDept(d.id)"
            />
            {{ d.name }}
          </label>
        </div>
        <p v-if="!departments.length" class="faint dept-empty">暂无部门，请先到「部门管理」创建</p>
      </div>
      <p v-if="error" class="error-text err">{{ error }}</p>
      <template #footer>
        <button type="button" class="btn" @click="modalVisible = false">取消</button>
        <button type="button" class="btn btn-primary" :disabled="busy" @click="save">
          {{ busy ? '保存中…' : '保存' }}
        </button>
      </template>
    </ModalDialog>

    <ConfirmDialog
      :visible="!!removing"
      title="删除套餐"
      :message="`确定删除套餐「${removing?.name ?? ''}」吗？套餐与部门的关系将一并清除。`"
      confirm-text="删除"
      danger
      @confirm="confirmRemove"
      @cancel="removing = null"
    />
  </div>
</template>

<style scoped>
.ta-right {
  text-align: right;
}

.dept-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.err {
  margin: 4px 0 0;
  font-size: 13px;
}

.dept-empty {
  font-size: 12px;
}
</style>
