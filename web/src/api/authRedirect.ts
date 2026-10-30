/**
 * 401 时是否需要跳转登录页。
 *
 * 公开页的 Token 可能是过期残留，路由守卫会清理后继续放行；
 * 因此这里必须同时判断：
 * 1. 当前路由本身是否要求登录；
 * 2. 这次请求是否显式要求“静默处理鉴权失败”（例如守卫刷新用户状态）。
 */
export interface AuthRedirectContext {
  requiresAuth?: unknown
  path?: string
  skipAuthRedirect?: boolean
}

export function shouldRedirectToLogin(context: AuthRedirectContext): boolean {
  return (
    !context.skipAuthRedirect &&
    Boolean(context.requiresAuth) &&
    context.path !== '/login'
  )
}
