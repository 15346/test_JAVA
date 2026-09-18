import { createRouter, createWebHashHistory } from 'vue-router'
import { useAuth } from '../modules/auth/hooks/use-auth'

/**
 * hash 路由（地址带 #）：构建产物由 Spring Boot 静态托管，
 * 深链刷新（如直接打开 /#/operation-log）不需要后端配 SPA 转发。
 *
 * 路由分两类：
 * - guestOnly：登录 / 注册 / 找回密码，已登录用户访问会被送回 /list；
 * - requiresAuth：后台布局及其子页面，未登录用户会被送去 /login。
 */
const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('../modules/auth/pages/login/index.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('../modules/auth/pages/register/index.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/forgot-password',
      name: 'ForgotPassword',
      component: () => import('../modules/auth/pages/forgot-password/index.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/',
      component: () => import('../layouts/admin-layout.vue'),
      meta: { requiresAuth: true },
      children: [
        { path: '', redirect: '/list' },
        {
          path: 'list',
          name: 'TodoList',
          component: () => import('../views/list/index.vue'),
        },
        {
          path: 'operation-log',
          name: 'OperationLog',
          component: () => import('../views/operation-log/index.vue'),
        },
      ],
    },
  ],
})

router.beforeEach(async (to) => {
  const { currentUser, loadCurrentUser } = useAuth()

  if (to.meta.requiresAuth) {
    if (!currentUser.value) {
      try {
        // 首次进入或刷新页面时，用 /me 恢复登录态
        await loadCurrentUser()
      } catch {
        // 网络/服务异常时按未登录处理，避免停在空白页
      }
    }
    if (!currentUser.value) {
      return { path: '/login' }
    }
  } else if (to.meta.guestOnly && currentUser.value) {
    return { path: '/list' }
  }

  return true
})

export default router
