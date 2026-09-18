<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { requestResetCode, resetPassword } from '../../api'
import { DEMO_RESET_CODE } from '../../constant'
import PasswordInput, { checkPasswordPolicy } from '../../components/password-input.vue'

const router = useRouter()

/** 两步：code = 请求验证码；reset = 填验证码与新密码 */
const step = ref<'code' | 'reset'>('code')

const email = ref('')
const code = ref('')
const password = ref('')
const confirmPassword = ref('')
const requesting = ref(false)
const submitting = ref(false)

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function validateEmail(): string | null {
  const trimmed = email.value.trim()
  if (!trimmed) return '请输入邮箱'
  if (!EMAIL_PATTERN.test(trimmed)) return '邮箱格式不正确'
  return null
}

async function handleRequestCode() {
  const invalid = validateEmail()
  if (invalid) {
    message.warning(invalid)
    return
  }

  requesting.value = true
  try {
    await requestResetCode(email.value.trim())
    // 成功后进入第二步，展示演示验证码与验证码输入框
    step.value = 'reset'
  } catch (error) {
    message.error(error instanceof Error ? error.message : '获取验证码失败，请稍后重试')
  } finally {
    requesting.value = false
  }
}

function validateReset(): string | null {
  if (!code.value.trim()) return '请输入验证码'
  const policyError = checkPasswordPolicy(password.value)
  if (policyError) return policyError
  if (password.value !== confirmPassword.value) return '两次输入的密码不一致'
  return null
}

async function handleReset() {
  const invalid = validateReset()
  if (invalid) {
    message.warning(invalid)
    return
  }

  submitting.value = true
  try {
    await resetPassword({
      email: email.value.trim(),
      code: code.value.trim(),
      // 载荷字段名为 password（api 层再映射成后端的 newPassword）
      password: password.value,
      confirmPassword: confirmPassword.value,
    })
    message.success('密码重置成功，请登录')
    await router.push('/login')
  } catch (error) {
    // 验证码错误等后端提示直接展示
    message.error(error instanceof Error ? error.message : '密码重置失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

/** 回上一步，允许改邮箱重新获取验证码 */
function backToEmail() {
  step.value = 'code'
  code.value = ''
}
</script>

<template>
  <div class="auth-page">
    <a-card class="auth-card" title="找回密码">
      <!-- 第一步：邮箱 + 获取验证码 -->
      <a-form v-if="step === 'code'" layout="vertical">
        <a-form-item label="邮箱" required>
          <a-input
            v-model:value="email"
            type="email"
            placeholder="请输入注册邮箱"
            allow-clear
            @press-enter="handleRequestCode"
          />
        </a-form-item>
        <a-button type="primary" block :loading="requesting" @click="handleRequestCode">
          获取验证码
        </a-button>
      </a-form>

      <!-- 第二步：验证码 + 新密码 -->
      <a-form v-else layout="vertical">
        <a-typography-paragraph type="secondary">
          验证码已发送至 {{ email.trim() }}，<a @click="backToEmail">重新填写邮箱</a>
        </a-typography-paragraph>
        <a-typography-paragraph>
          演示验证码：<strong>{{ DEMO_RESET_CODE }}</strong>
        </a-typography-paragraph>
        <a-form-item label="验证码" required>
          <a-input v-model:value="code" placeholder="请输入验证码" allow-clear />
        </a-form-item>
        <a-form-item label="新密码" required>
          <PasswordInput v-model="password" placeholder="请输入新密码" />
        </a-form-item>
        <a-form-item label="确认新密码" required>
          <a-input-password
            v-model:value="confirmPassword"
            placeholder="请再次输入新密码"
            @press-enter="handleReset"
          />
        </a-form-item>
        <a-button type="primary" block :loading="submitting" @click="handleReset">
          重置密码
        </a-button>
      </a-form>

      <div class="auth-links">
        <RouterLink to="/login">返回登录</RouterLink>
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
