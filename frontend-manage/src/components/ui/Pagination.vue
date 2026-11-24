<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  total: number
  current: number
  size: number
}>()

const emit = defineEmits<{ change: [page: number] }>()

const pages = computed(() => Math.max(1, Math.ceil(props.total / props.size)))

/** 当前页 ±2 的页码窗口 */
const window_ = computed(() => {
  const list: number[] = []
  const from = Math.max(1, props.current - 2)
  const to = Math.min(pages.value, props.current + 2)
  for (let p = from; p <= to; p++) list.push(p)
  return list
})

function go(p: number): void {
  if (p >= 1 && p <= pages.value && p !== props.current) {
    emit('change', p)
  }
}
</script>

<template>
  <div v-if="total > 0" class="pager">
    <span class="pager-total">共 {{ total }} 条</span>
    <button type="button" :disabled="current <= 1" @click="go(current - 1)">上一页</button>
    <button
      v-for="p in window_"
      :key="p"
      type="button"
      :class="{ current: p === current }"
      @click="go(p)"
    >
      {{ p }}
    </button>
    <button type="button" :disabled="current >= pages" @click="go(current + 1)">下一页</button>
  </div>
</template>
