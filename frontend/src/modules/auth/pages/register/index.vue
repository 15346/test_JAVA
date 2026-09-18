<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ApiError } from '../../../../shared/http/request'
import { register } from '../../api'
import { EMAIL_UNAVAILABLE_MESSAGE } from '../../constant'
import PasswordInput, { checkPasswordPolicy } from '../../components/password-input.vue'

const router = useRouter()

const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const submitting = ref(false)

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function validate(): string | null {
  const trimmed = email.value.trim()
  if (!trimmed) return '请输入邮箱'
  if (!EMAIL_PATTERN.test(trimmed)) return '邮箱格式不正确'
  const policyError = checkPasswordPolicy(password.value)
  if (policyError) return policyError
  if (password.value !== confirmPassword.value) return '两次输入的密码不一致'
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
    // 注册成功不自动登录，也不把密码留在页面状态里
    await register({
      email: email.value.trim(),
      password: password.value,
      confirmPassword: confirmPassword.value,
    })
    message.success('注册成功，请登录')
    await router.push('/login')
  } catch (error) {
    // 邮箱冲突只暴露“邮箱不可用”，不透露该邮箱是否已注册的更多细节
    if (error instanceof ApiError && error.code === 'EMAIL_ALREADY_EXISTS') {
      message.error(EMAIL_UNAVAILABLE_MESSAGE)
    } else if (error instanceof Error) {
      message.error(error.message)
    } else {
      message.error('注册失败，请稍后重试')
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <a-card class="auth-card" title="注册">
      <a-form layout="vertical">
        <a-form-item label="邮箱" required>
          <a-input
            v-model:value="email"
            type="email"
            placeholder="请输入邮箱"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="密码" required>
          <PasswordInput v-model="password" placeholder="请输入密码" />
        </a-form-item>
        <a-form-item label="确认密码" required>
          <a-input-password
            v-model:value="confirmPassword"
            placeholder="请再次输入密码"
            @press-enter="handleSubmit"
          />
        </a-form-item>
        <a-button type="primary" block :loading="submitting" @click="handleSubmit">
          注册
        </a-button>
      </a-form>

      <div class="auth-links">
        <RouterLink to="/login">已有账号，去登录</RouterLink>
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
  justify-content: center;
  margin-top: 16px;
}
</style>
