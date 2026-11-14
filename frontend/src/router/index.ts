import { createRouter, createWebHistory } from 'vue-router'

import { getToken } from '../utils/storage'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    public?: boolean
  }
}

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { title: '登录', public: true },
    },
    {
      path: '/',
      name: 'home',
      component: () => import('../views/HomeView.vue'),
      meta: { title: '首页' },
    },
    {
      path: '/traffic',
      name: 'traffic',
      component: () => import('../views/TrafficView.vue'),
      meta: { title: '流量部门' },
    },
    {
      path: '/video',
      name: 'video',
      component: () => import('../views/VideoView.vue'),
      meta: { title: '视频部门' },
    },
    {
      path: '/copywriting',
      name: 'copywriting',
      component: () => import('../views/CopywritingView.vue'),
      meta: { title: '文案编导组' },
    },
    {
      path: '/live',
      name: 'live',
      component: () => import('../views/LiveView.vue'),
      meta: { title: '直播增长组' },
    },
    {
      path: '/sales',
      name: 'sales',
      component: () => import('../views/SalesView.vue'),
      meta: { title: 'AI销售部门' },
    },
    {
      path: '/cost',
      name: 'cost',
      component: () => import('../views/CostView.vue'),
      meta: { title: '职能成本部门' },
    },
    {
      path: '/boss',
      name: 'boss',
      component: () => import('../views/BossView.vue'),
      meta: { title: 'BOSS决策部门' },
    },
    {
      path: '/agent-center',
      name: 'agentCenter',
      component: () => import('../views/AgentCenterView.vue'),
      meta: { title: '智能体中心' },
    },
    {
      path: '/inspiration',
      name: 'inspiration',
      component: () => import('../views/InspirationView.vue'),
      meta: { title: '灵感中心' },
    },
    {
      path: '/chat/:agentId',
      name: 'chat',
      component: () => import('../views/ChatView.vue'),
      meta: { title: '对话' },
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'notFound',
      component: () => import('../views/NotFoundView.vue'),
      meta: { title: '页面不存在', public: true },
    },
  ],
})

router.beforeEach((to) => {
  const token = getToken()

  if (!to.meta.public && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (to.path === '/login' && token) {
    return { path: '/' }
  }

  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · 珠峰AI超级系统` : '珠峰AI超级系统'
})

export default router
