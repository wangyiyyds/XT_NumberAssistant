<script setup lang="ts">
import { onMounted } from 'vue'

import { useAuthStore } from '../../stores/auth'
import AdminHeader from './AdminHeader.vue'
import AdminSidebar from './AdminSidebar.vue'

const auth = useAuthStore()

onMounted(() => {
  if (!auth.loaded) {
    void auth.load()
  }
})
</script>

<template>
  <div class="page">
    <AdminSidebar />
    <div class="main">
      <AdminHeader />
      <div class="content">
        <router-view />
      </div>
    </div>
  </div>
</template>
