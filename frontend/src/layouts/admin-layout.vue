<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useAuth } from '../modules/auth/hooks/use-auth'

const route = useRoute()
const router = useRouter()

const { currentUser, logout } = useAuth()

const loggingOut = ref(false)

// 侧边导航（真实项目一般由菜单接口/权限生成）
const menus = [
  { key: '/list', label: '待办列表' },
  { key: '/operation-log', label: '操作日志' },
]

async function handleLogout() {
  loggingOut.value = true
  try {
    // 退出成功才清空本地状态（hook 内完成），再回登录页
    await logout()
    await router.push('/login')
  } catch (error) {
    // 接口失败时保留登录态，至少让用户知道没退成功
    message.error(error instanceof Error ? error.message : '退出登录失败，请稍后重试')
  } finally {
    loggingOut.value = false
  }
}
</script>

<template>
  <a-layout style="min-height: 100vh">
    <a-layout-header class="admin-header">
      <div class="admin-header__title">📝 待办管理</div>
      <a-space>
        <span class="admin-header__user">{{ currentUser?.email }}</span>
        <a-button size="small" :loading="loggingOut" @click="handleLogout">退出登录</a-button>
      </a-space>
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

<style scoped>
.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #001529;
}

.admin-header__title {
  color: #fff;
  font-size: 18px;
  font-weight: 600;
}

.admin-header__user {
  color: #fff;
}
</style>
