// ============================================================
//  认证状态：模块级单例 ref，所有页面共享同一份 currentUser。
//  - 模块级 ref：任何页面 useAuth() 拿到的都是同一个用户状态；
//  - initialized：避免路由守卫每次跳转都重复请求 /me。
//  - 只有 http 层对外暴露的 ApiError 才被特殊处理，其余错误继续抛出。
// ============================================================

import { readonly, ref, type Ref } from 'vue'
import { ApiError } from '../../../shared/http/request'
import { getCurrentUser, logout as logoutApi } from '../api'
import type { AuthUser } from '../api/type'

/** 当前登录用户（模块级单例，跨页面共享） */
const currentUser = ref<AuthUser | null>(null)

/** 是否已经确定过登录态（成功加载或确认 401 后置位） */
let initialized = false

export interface UseAuthReturn {
  /** 只读的当前用户，避免调用方直接改写 */
  currentUser: Readonly<Ref<AuthUser | null>>
  /** 拉取 /me；401 视为未登录（清空并返回 null），其他错误抛出 */
  loadCurrentUser: () => Promise<AuthUser | null>
  /** 登录/注册后写入用户 */
  setCurrentUser: (user: AuthUser | null) => void
  /** 清空用户并重置已初始化标记（下次会重新请求 /me） */
  clearCurrentUser: () => void
  /** 调用退出接口后清空本地状态 */
  logout: () => Promise<void>
}

export function useAuth(): UseAuthReturn {
  async function loadCurrentUser(): Promise<AuthUser | null> {
    // 已确定过登录态就不再请求，避免守卫重复打 /me
    if (initialized) {
      return currentUser.value
    }
    try {
      const user = await getCurrentUser()
      currentUser.value = user
      initialized = true
      return user
    } catch (error) {
      // 401 = 未登录，属于正常分支；状态确定为“无用户”
      if (error instanceof ApiError && error.status === 401) {
        currentUser.value = null
        initialized = true
        return null
      }
      // 网络/服务异常等交给调用方处理
      throw error
    }
  }

  function setCurrentUser(user: AuthUser | null): void {
    currentUser.value = user
    initialized = true
  }

  function clearCurrentUser(): void {
    currentUser.value = null
    initialized = false
  }

  async function logout(): Promise<void> {
    await logoutApi()
    clearCurrentUser()
  }

  return {
    currentUser: readonly(currentUser),
    loadCurrentUser,
    setCurrentUser,
    clearCurrentUser,
    logout,
  }
}
