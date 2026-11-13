import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '../api/client'
import MainLayout from '../layouts/MainLayout.vue'
import LoginView from '../views/LoginView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView, meta: { public: true } },
    {
      path: '/',
      component: MainLayout,
      children: [
        { path: '', name: 'home', component: () => import('../views/HomeView.vue') },
        { path: 'chat/:agentId', name: 'chat', component: () => import('../views/ChatView.vue') },
        { path: 'inspiration', name: 'inspiration', component: () => import('../views/InspirationView.vue') }
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

router.beforeEach((to) => {
  if (!to.meta.public && !getToken()) return { path: '/login' }
  if (to.path === '/login' && getToken()) return { path: '/' }
  return true
})

export default router
