<script setup lang="ts">
defineProps<{
  visible: boolean
  title: string
  large?: boolean
}>()

const emit = defineEmits<{ close: [] }>()
</script>

<template>
  <div v-if="visible" class="modal-mask" @click.self="emit('close')">
    <div class="modal-panel" :class="{ 'modal-lg': large }">
      <div class="modal-head">
        <h3>{{ title }}</h3>
        <button type="button" class="modal-close" title="关闭" @click="emit('close')">×</button>
      </div>
      <div class="modal-body">
        <slot />
      </div>
      <div v-if="$slots.footer" class="modal-foot">
        <slot name="footer" />
      </div>
    </div>
  </div>
</template>
