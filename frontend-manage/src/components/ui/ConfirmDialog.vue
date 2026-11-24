<script setup lang="ts">
import ModalDialog from './ModalDialog.vue'

withDefaults(
  defineProps<{
    visible: boolean
    title?: string
    message: string
    confirmText?: string
    danger?: boolean
  }>(),
  { title: '确认操作', confirmText: '确定', danger: false },
)

const emit = defineEmits<{ confirm: []; cancel: [] }>()
</script>

<template>
  <ModalDialog :visible="visible" :title="title" @close="emit('cancel')">
    <p class="confirm-msg dim">{{ message }}</p>
    <template #footer>
      <button type="button" class="btn" @click="emit('cancel')">取消</button>
      <button type="button" class="btn" :class="danger ? 'btn-danger-solid' : 'btn-primary'" @click="emit('confirm')">
        {{ confirmText }}
      </button>
    </template>
  </ModalDialog>
</template>

<style scoped>
.confirm-msg {
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-line;
}

.btn-danger-solid {
  background: var(--danger);
  border-color: var(--danger);
  color: #fff;
}
.btn-danger-solid:hover {
  background: var(--danger);
  border-color: var(--danger);
  color: #fff;
  opacity: 0.85;
}
</style>
