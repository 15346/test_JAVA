import { createRouter, createWebHashHistory } from 'vue-router'

/**
 * hash 路由（地址带 #）：构建产物由 Spring Boot 静态托管，
 * 深链刷新（如直接打开 /#/operation-log）不需要后端配 SPA 转发。
 */
const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', redirect: '/list' },
    {
      path: '/list',
      name: 'TodoList',
      component: () => import('../views/list/index.vue'),
    },
    {
      path: '/operation-log',
      name: 'OperationLog',
      component: () => import('../views/operation-log/index.vue'),
    },
  ],
})

export default router
