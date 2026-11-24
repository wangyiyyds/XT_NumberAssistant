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
      component: () => import('../components/layout/AdminLayout.vue'),
      children: [
        { path: '', redirect: '/agents' },
        {
          path: 'agents',
          name: 'agents',
          component: () => import('../views/AgentListView.vue'),
          meta: { title: '智能体管理' },
        },
        {
          path: 'agents/:agentId',
          name: 'agentDetail',
          component: () => import('../views/AgentDetailView.vue'),
          meta: { title: '智能体详情' },
        },
        {
          path: 'plans',
          name: 'plans',
          component: () => import('../views/PlanListView.vue'),
          meta: { title: '套餐管理' },
        },
        {
          path: 'departments',
          name: 'departments',
          component: () => import('../views/DepartmentListView.vue'),
          meta: { title: '部门管理' },
        },
        {
          path: 'users',
          name: 'users',
          component: () => import('../views/UserListView.vue'),
          meta: { title: '用户管理' },
        },
        {
          path: 'menus',
          name: 'menus',
          component: () => import('../views/MenuListView.vue'),
          meta: { title: '菜单管理' },
        },
        {
          path: 'admins',
          name: 'admins',
          component: () => import('../views/AdminListView.vue'),
          meta: { title: '管理员管理' },
        },
      ],
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
  document.title = to.meta.title ? `${to.meta.title} · 先霆数字人助手管理端` : '先霆数字人助手管理端'
})

export default router
