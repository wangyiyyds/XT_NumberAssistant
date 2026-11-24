import { ref } from 'vue'

export interface ToastItem {
  id: number
  message: string
  type: 'info' | 'success' | 'error'
}

/** 极简全局 toast 状态：ToastHost 渲染，show() 触发，2.4s 自动消失 */
export const toasts = ref<ToastItem[]>([])

let seed = 0

export function toast(message: string, type: ToastItem['type'] = 'info'): void {
  const id = ++seed
  toasts.value.push({ id, message, type })
  window.setTimeout(() => {
    toasts.value = toasts.value.filter((t) => t.id !== id)
  }, 2400)
}

export function toastError(e: unknown, fallback = '操作失败'): void {
  toast(e instanceof Error && e.message ? e.message : fallback, 'error')
}
