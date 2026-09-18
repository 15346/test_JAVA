// ============================================================
//  认证接口层：只管“怎么调”，类型在 ./type.ts。
//  统一走 shared/http 的 request，不直接调用 fetch。
// ============================================================

import { request } from '../../../shared/http/request'
import type {
  AuthUser,
  LoginPayload,
  RegisterPayload,
  ResetPasswordPayload,
} from './type'

const API = '/api/auth'

/** 注册 —— POST /api/auth/register，成功后返回当前用户 */
export function register(payload: RegisterPayload): Promise<AuthUser> {
  return request<AuthUser>(`${API}/register`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

/** 登录 —— POST /api/auth/login */
export function login(payload: LoginPayload): Promise<AuthUser> {
  return request<AuthUser>(`${API}/login`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

/** 退出登录 —— POST /api/auth/logout */
export function logout(): Promise<void> {
  return request<void>(`${API}/logout`, { method: 'POST' })
}

/** 获取当前登录用户 —— GET /api/auth/me */
export function getCurrentUser(): Promise<AuthUser> {
  return request<AuthUser>(`${API}/me`, { method: 'GET' })
}

/** 申请重置验证码 —— POST /api/auth/password/reset-code */
export function requestResetCode(email: string): Promise<void> {
  return request<void>(`${API}/password/reset-code`, {
    method: 'POST',
    body: JSON.stringify({ email }),
  })
}

/** 重置密码 —— POST /api/auth/password/reset（接口体字段名为 newPassword） */
export function resetPassword(payload: ResetPasswordPayload): Promise<void> {
  return request<void>(`${API}/password/reset`, {
    method: 'POST',
    body: JSON.stringify({
      email: payload.email,
      code: payload.code,
      newPassword: payload.password,
      confirmPassword: payload.confirmPassword,
    }),
  })
}
