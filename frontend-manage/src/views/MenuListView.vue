<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { createMenu, deleteMenu, fetchMenus, updateMenu } from '../api/menu'
import { ICON_NAMES, resolveIcon } from '../constants/icons'
import ModalDialog from '../components/ui/ModalDialog.vue'
import ConfirmDialog from '../components/ui/ConfirmDialog.vue'
import { useMenuStore } from '../stores/menu'
import type { Menu } from '../types/menu'
import { toast, toastError } from '../utils/toast'

const menus = ref<Menu[]>([])
const loading = ref(false)
const menuStore = useMenuStore()

// 新建/编辑弹窗
const modalVisible = ref(false)
const editing = ref<Menu | null>(null)
const form = ref({ name: '', path: '', icon: 'Menu', sortOrder: 1 })
const error = ref('')
const busy = ref(false)

// 删除确认
const removing = ref<Menu | null>(null)

async function load(): Promise<void> {
  loading.value = true
  try {
    menus.value = await fetchMenus()
  } catch (e) {
    toastError(e, '加载菜单失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)

function nextSort(): number {
  return menus.value.reduce((max, m) => Math.max(max, m.sortOrder), 0) + 1
}

function openCreate(): void {
  editing.value = null
  form.value = { name: '', path: '', icon: 'Menu', sortOrder: nextSort() }
  error.value = ''
  modalVisible.value = true
}

function openEdit(menu: Menu): void {
  editing.value = menu
  form.value = {
    name: menu.name,
    path: menu.path,
    icon: menu.icon || 'Menu',
    sortOrder: menu.sortOrder,
  }
  error.value = ''
  modalVisible.value = true
}

async function save(): Promise<void> {
  error.value = ''
  if (!form.value.name.trim()) {
    error.value = '菜单名称必填'
    return
  }
  const path = form.value.path.trim()
  if (!path) {
    error.value = '路由路径必填'
    return
  }
  if (!path.startsWith('/')) {
    error.value = '路由路径须以 / 开头'
    return
  }
  if (!form.value.sortOrder || Number(form.value.sortOrder) < 1) {
    error.value = '排序须为不小于 1 的整数'
    return
  }
  busy.value = true
  try {
    const body = {
      name: form.value.name.trim(),
      path,
      icon: form.value.icon,
      sortOrder: Number(form.value.sortOrder),
    }
    if (editing.value) {
      await updateMenu(editing.value.id, body)
    } else {
      await createMenu(body)
    }
    toast(editing.value ? '菜单已更新' : '菜单已创建', 'success')
    modalVisible.value = false
    await Promise.all([load(), menuStore.reload()])
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
    await deleteMenu(target.id)
    toast('菜单已删除', 'success')
    await Promise.all([load(), menuStore.reload()])
  } catch (e) {
    toastError(e, '删除失败')
  }
}
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <h1>菜单管理</h1>
        <p class="sub">左侧导航从数据库读取，增删改后立即生效；菜单路径对应的前端页面需已开发</p>
      </div>
      <button type="button" class="btn btn-primary" @click="openCreate">新建菜单</button>
    </div>

    <div class="panel">
      <table class="data-table">
        <thead>
          <tr>
            <th>图标</th>
            <th>名称</th>
            <th>路由路径</th>
            <th>排序</th>
            <th class="ta-right">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="menu in menus" :key="menu.id">
            <td>
              <span class="app-icon icon-cell"><component :is="resolveIcon(menu.icon)" /></span>
            </td>
            <td>{{ menu.name }}</td>
            <td class="dim"><code>{{ menu.path }}</code></td>
            <td><span class="tag tag-info">{{ menu.sortOrder }}</span></td>
            <td>
              <div class="cell-actions">
                <button type="button" class="btn btn-sm" @click="openEdit(menu)">编辑</button>
                <button type="button" class="btn btn-sm btn-danger" @click="removing = menu">删除</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-if="!loading && !menus.length" class="empty-state">暂无菜单，点击右上角新建</p>
    </div>

    <ModalDialog
      :visible="modalVisible"
      :title="editing ? `编辑菜单「${editing.name}」` : '新建菜单'"
      @close="modalVisible = false"
    >
      <div class="form-row">
        <div class="form-item">
          <label>菜单名称<span class="req">*</span></label>
          <input v-model="form.name" type="text" placeholder="如：智能体管理" />
        </div>
        <div class="form-item">
          <label>路由路径<span class="req">*</span>（以 / 开头，全表唯一）</label>
          <input v-model="form.path" type="text" placeholder="如：/agents" />
        </div>
      </div>
      <div class="form-row">
        <div class="form-item">
          <label>图标</label>
          <div class="icon-picker">
            <span class="app-icon icon-preview"><component :is="resolveIcon(form.icon)" /></span>
            <select v-model="form.icon" class="ui-select">
              <option v-for="name in ICON_NAMES" :key="name" :value="name">{{ name }}</option>
            </select>
          </div>
        </div>
        <div class="form-item">
          <label>排序<span class="req">*</span>（数字越小越靠上）</label>
          <input v-model.number="form.sortOrder" type="number" min="1" />
        </div>
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
      title="删除菜单"
      :message="`确定删除菜单「${removing?.name ?? ''}」吗？删除后左侧导航将不再显示该入口。`"
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

.icon-cell {
  font-size: 17px;
  color: var(--accent);
}

.icon-picker {
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon-preview {
  flex: 0 0 auto;
  font-size: 18px;
  color: var(--accent);
}

.err {
  margin: 4px 0 0;
  font-size: 13px;
}
</style>
