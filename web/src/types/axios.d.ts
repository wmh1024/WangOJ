import 'axios'

declare module 'axios' {
  export interface AxiosRequestConfig {
    /** 401 时只清理本地状态，不自动跳转登录页（供路由守卫刷新用户状态使用） */
    skipAuthRedirect?: boolean
  }
}
