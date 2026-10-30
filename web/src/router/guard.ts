import type { RouteLocationNormalized } from 'vue-router'
import type { UserDTO } from '@/api/user'
import { AUTH, isAdmin } from '@/utils/constants'

/**
 * 路由守卫依赖的最小用户状态接口，便于单测注入。
 */
export interface GuardUserStore {
  token?: string | null
  setUser(user: UserDTO): void
  resetUser(): void
}

export interface UserStatusResponse {
  data?: {
    data?: UserDTO
  }
}

export type UserStatusFetcher = () => Promise<UserStatusResponse>

/**
 * 创建全局前置守卫。
 *
 * 规则（C-2 回归点）：
 * 1. 公开页面永远放行；即使本地 Token 已失效，也只静默清理，不能把用户踢去登录页；
 * 2. 需要登录的页面：无 Token 直接跳登录并带 redirect；
 * 3. 需要管理员权限的页面：角色不符跳 403。
 */
export function createNavigationGuard(userStore: GuardUserStore, fetchUserStatus: UserStatusFetcher) {
  return async (to: Pick<RouteLocationNormalized, 'meta' | 'fullPath'>) => {
    if (!to.meta?.auth) {
      if (userStore.token) {
        try {
          const user = (await fetchUserStatus()).data?.data
          if (user) {
            userStore.setUser(user)
          } else {
            userStore.resetUser()
          }
        } catch {
          // Token 失效：静默清理，继续浏览公开页面
          userStore.resetUser()
        }
      }
      return true
    }

    if (!userStore.token) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }

    try {
      const user = (await fetchUserStatus()).data?.data
      if (!user) {
        userStore.resetUser()
        return { path: '/login', query: { redirect: to.fullPath } }
      }
      userStore.setUser(user)
      if (to.meta.auth === AUTH.ADMIN && !isAdmin(user.userRole)) {
        return { path: '/403' }
      }
      return true
    } catch {
      userStore.resetUser()
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }
}
