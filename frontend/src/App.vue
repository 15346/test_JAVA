<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()

// 侧边导航（真实项目一般由菜单接口/权限生成）
const menus = [
  { key: '/list', label: '待办列表' },
  { key: '/operation-log', label: '操作日志' },
]
</script>

<template>
  <a-layout style="min-height: 100vh">
    <a-layout-header style="background: #001529; display: flex; align-items: center">
      <div style="color: #fff; font-size: 18px; font-weight: 600">📝 待办管理</div>
    </a-layout-header>
    <a-layout>
      <a-layout-sider theme="light">
        <a-menu mode="inline" :selected-keys="[route.path]">
          <a-menu-item v-for="m in menus" :key="m.key">
            <RouterLink :to="m.key">{{ m.label }}</RouterLink>
          </a-menu-item>
        </a-menu>
      </a-layout-sider>
      <a-layout-content style="padding: 24px">
        <RouterView />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>
