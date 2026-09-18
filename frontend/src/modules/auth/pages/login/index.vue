<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ApiError } from '../../../../shared/http/request'
import { login } from '../../api'
import { useAuth } from '../../hooks/use-auth'

const router = useRouter()
const { setCurrentUser } = useAuth()

const email = ref('')
const password = ref('')
const submitting = ref(false)

/** 提交前只做非空校验，格式与账号有效性交给后端 */
function validate(): string | null {
  if (!email.value.trim()) return '请输入邮箱'
  if (!password.value) return '请输入密码'
  return null
}

async function handleSubmit() {
  const invalid = validate()
  if (invalid) {
    message.warning(invalid)
    return
  }

  submitting.value = true
  try {
    const user = await login({ email: email.value.trim(), password: password.value })
    setCurrentUser(user)
    await router.push('/list')
  } catch (error) {
    // 只展示后端 message，不展示堆栈或原始响应
    if (error instanceof ApiError && error.code === 'INVALID_CREDENTIALS') {
      message.error('邮箱或密码错误')
    } else if (error instanceof Error) {
      message.error(error.message)
    } else {
      message.error('登录失败，请稍后重试')
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <a-card class="auth-card" title="登录">
      <a-form layout="vertical">
        <a-form-item label="邮箱" required>
          <a-input
            v-model:value="email"
            type="email"
            placeholder="请输入邮箱"
            allow-clear
            @press-enter="handleSubmit"
          />
        </a-form-item>
        <a-form-item label="密码" required>
          <a-input-password
            v-model:value="password"
            placeholder="请输入密码"
            @press-enter="handleSubmit"
          />
        </a-form-item>
        <a-button type="primary" block :loading="submitting" @click="handleSubmit">
          登录
        </a-button>
      </a-form>

      <div class="auth-links">
        <RouterLink to="/register">注册账号</RouterLink>
        <RouterLink to="/forgot-password">忘记密码？</RouterLink>
      </div>
    </a-card>
  </div>
</template>

<style scoped>
.auth-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: #f0f2f5;
}

.auth-card {
  width: 380px;
}

.auth-links {
  display: flex;
  justify-content: space-between;
  margin-top: 16px;
}
</style>
