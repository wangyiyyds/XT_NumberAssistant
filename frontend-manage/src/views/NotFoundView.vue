<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="notfound">
    <div class="empty-state">
      <p class="code">404</p>
      <p>页面不存在</p>
      <button type="button" class="btn btn-primary back" @click="router.push('/')">返回管理端</button>
    </div>
  </div>
</template>

<style scoped>
.notfound {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.code {
  font-size: 40px;
  font-weight: 700;
  color: var(--accent);
  margin-bottom: 6px;
}

.back {
  margin-top: 16px;
}
</style>
