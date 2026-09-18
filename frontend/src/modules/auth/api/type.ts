// ============================================================
//  认证模块的数据形状：与后端接口一一对应。
// ============================================================

/** 目前只有普通用户一种角色 */
export type Role = 'USER'

export interface AuthUser {
  id: number
  email: string
  role: Role
}

export interface RegisterPayload {
  email: string
  password: string
  confirmPassword: string
}

export interface LoginPayload {
  email: string
  password: string
}

export interface ResetPasswordPayload extends RegisterPayload {
  code: string
}
