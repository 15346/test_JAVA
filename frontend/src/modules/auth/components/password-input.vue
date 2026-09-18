<script lang="ts">
// ============================================================
//  密码策略校验（与后端 PasswordPolicy 保持一致）：
//  - 至少 8 位；
//  - 不能包含空白字符；
//  - 字母 / 数字 / 符号三类中至少满足两类。
//  放在 SFC 的普通 <script> 里导出，注册页与找回密码页共用，
//  避免两处规则各写一遍产生偏差。
// ============================================================

/** 校验密码：合规返回 null，否则返回具体的错误文案 */
export function checkPasswordPolicy(value: string): string | null {
  if (value.length < 8) {
    return '密码至少 8 位'
  }
  if (/\s/.test(value)) {
    return '密码不能包含空格'
  }
  const categories = [/[A-Za-z]/, /\d/, /[^A-Za-z\d]/].filter((pattern) =>
    pattern.test(value),
  ).length
  if (categories < 2) {
    return '密码需包含字母、数字、符号中的至少两类'
  }
  return null
}
</script>

<script setup lang="ts">
// 展示型组件：只负责输入框 + 规则提示，不做拦截式校验（何时校验由页面决定）
defineProps<{
  placeholder?: string
}>()

const model = defineModel<string>({ default: '' })

const hintText = '至少 8 位，不能包含空格，需包含字母、数字、符号中的至少两类'
</script>

<template>
  <div>
    <a-input-password
      v-model:value="model"
      :placeholder="placeholder ?? '请输入密码'"
    />
    <div class="password-input__hint">{{ hintText }}</div>
  </div>
</template>

<style scoped>
.password-input__hint {
  margin-top: 4px;
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
  line-height: 1.5;
}
</style>
