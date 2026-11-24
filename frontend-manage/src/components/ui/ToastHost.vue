<script setup lang="ts">
import { toasts } from '../../utils/toast'
</script>

<template>
  <div class="toast-host">
    <div v-for="t in toasts" :key="t.id" class="toast" :class="t.type">{{ t.message }}</div>
  </div>
</template>
