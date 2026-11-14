<template>
  <div class="not-found">
    <div class="code">404</div>
    <p class="dim">页面不存在或已被移除</p>
    <router-link class="btn btn-primary" to="/">返回首页</router-link>
  </div>
</template>

<style scoped>
.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  height: 100%;
  background: var(--bg);
}

.code {
  color: var(--accent);
  font-size: 42px;
  font-weight: 700;
}
</style>
