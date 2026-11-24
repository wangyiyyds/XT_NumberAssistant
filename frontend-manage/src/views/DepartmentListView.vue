<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { createDepartment, deleteDepartment, fetchDepartments, updateDepartment } from '../api/departments'
import ConfirmDialog from '../components/ui/ConfirmDialog.vue'
import ModalDialog from '../components/ui/ModalDialog.vue'
import SwitchToggle from '../components/ui/SwitchToggle.vue'
import type { Department } from '../types/department'
import { toast, toastError } from '../utils/toast'

const departments = ref<Department[]>([])
const loading = ref(false)

// 新建/编辑弹窗
const modalVisible = ref(false)
const editing = ref<Department | null>(null)
const form = ref({ name: '', enabled: true, brandOnly: false, sortOrder: 0 })
const error = ref('')
const busy = ref(false)

// 删除确认
const removing = ref<Department | null>(null)

async function load(): Promise<void> {
  loading.value = true
  try {
    departments.value = await fetchDepartments()
  } catch (e) {
    toastError(e, '加载部门失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)

function openCreate(): void {
  editing.value = null
  form.value = { name: '', enabled: true, brandOnly: false, sortOrder: departments.value.length }
  error.value = ''
  modalVisible.value = true
}

function openEdit(dept: Department): void {
  editing.value = dept
  form.value = {
    name: dept.name,
    enabled: dept.enabled ?? true,
    brandOnly: dept.brandOnly ?? false,
    sortOrder: dept.sortOrder ?? 0,
  }
  error.value = ''
  modalVisible.value = true
}

async function save(): Promise<void> {
  error.value = ''
  if (!form.value.name.trim()) {
    error.value = '部门名称必填'
    return
  }
  busy.value = true
  try {
    const body = {
      name: form.value.name.trim(),
      enabled: form.value.enabled,
      brandOnly: form.value.brandOnly,
      sortOrder: Number(form.value.sortOrder) || 0,
    }
    if (editing.value) {
      await updateDepartment(editing.value.id, body)
      toast('部门已更新', 'success')
    } else {
      await createDepartment(body)
      toast('部门已创建', 'success')
    }
    modalVisible.value = false
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    busy.value = false
  }
}

/** 行内启停：enabled 变化直接保存 */
async function toggleEnabled(dept: Department, value: boolean): Promise<void> {
  try {
    await updateDepartment(dept.id, {
      name: dept.name,
      enabled: value,
      brandOnly: dept.brandOnly,
      sortOrder: dept.sortOrder,
    })
    toast(value ? `已启用「${dept.name}」` : `已停用「${dept.name}」`, 'success')
    await load()
  } catch (e) {
    toastError(e, '操作失败')
  }
}

async function confirmRemove(): Promise<void> {
  if (!removing.value) return
  const target = removing.value
  removing.value = null
  try {
    await deleteDepartment(target.id)
    toast('部门已删除', 'success')
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
        <h1>部门管理</h1>
        <p class="sub">部门即客户端左侧导航的功能分组，套餐通过「套餐管理」勾选决定开放哪些部门</p>
      </div>
      <button type="button" class="btn btn-primary" @click="openCreate">新建部门</button>
    </div>

    <div class="panel">
      <table class="data-table">
        <thead>
          <tr>
            <th>名称</th>
            <th>启用</th>
            <th>品牌专属</th>
            <th>排序</th>
            <th class="ta-right">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="dept in departments" :key="dept.id">
            <td>{{ dept.name }}</td>
            <td>
              <SwitchToggle
                :model-value="dept.enabled ?? false"
                @update:model-value="toggleEnabled(dept, $event)"
              />
            </td>
            <td>
              <span v-if="dept.brandOnly" class="tag tag-warn">品牌专属</span>
              <span v-else class="faint">否</span>
            </td>
            <td class="dim">{{ dept.sortOrder ?? 0 }}</td>
            <td>
              <div class="cell-actions">
                <button type="button" class="btn btn-sm" @click="openEdit(dept)">编辑</button>
                <button type="button" class="btn btn-sm btn-danger" @click="removing = dept">删除</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-if="!loading && !departments.length" class="empty-state">暂无部门，点击右上角新建</p>
    </div>

    <ModalDialog
      :visible="modalVisible"
      :title="editing ? `编辑部门「${editing.name}」` : '新建部门'"
      @close="modalVisible = false"
    >
      <div class="form-item">
        <label>部门名称<span class="req">*</span></label>
        <input v-model="form.name" type="text" placeholder="如：视频部门" />
      </div>
      <div class="form-item">
        <label>排序（客户端导航按此升序）</label>
        <input v-model.number="form.sortOrder" type="number" min="0" />
      </div>
      <div class="form-item inline">
        <label>启用</label>
        <SwitchToggle v-model="form.enabled" />
      </div>
      <div class="form-item inline">
        <label>品牌专属</label>
        <SwitchToggle v-model="form.brandOnly" />
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
      title="删除部门"
      :message="`确定删除部门「${removing?.name ?? ''}」吗？套餐与该部门的关系将一并清除；部门下仍有智能体时无法删除。`"
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
