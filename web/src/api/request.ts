import axios from 'axios'
import { useUserStore } from '@/stores/user'
import router from '@/router'
import { shouldRedirectToLogin } from '@/api/authRedirect'
import { Message } from '@arco-design/web-vue'

// 默认走同源相对路径：开发环境由 Vite 代理 /api，生产环境由 Nginx 反代 /api
const baseURL = import.meta.env.VITE_API_BASE_URL || ''

const request = axios.create({
  // 基础地址
  baseURL,
  // 超时时间
  timeout: 200000
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    // 携带token
    const useStore = useUserStore()
    if (useStore?.token) {
      config.headers.Authorization = useStore.token
    }
    return config
  },
  (err) => Promise.reject(err)
)

// 响应拦截器
request.interceptors.response.use(
  (res) => {
    // 摘取核心响应数据
    if (res.data.code === 200) {
      return res
    } else {
      // 处理业务失败
      Message.error(res.data.msg || '服务异常')
      return Promise.reject(res.data)
    }
  },
  (err) => {
    if (!err.response) {
      // 网络错误 / 被取消 / 超时
      Message.error(err.message || '网络异常，请稍后重试')
      return Promise.reject(err)
    }
    const { status, data } = err.response
    // 后端统一响应体：{ code, msg, data }
    const msg = data?.msg || data?.message || ''
    if (status === 401) {
      // Token 失效：清掉本地状态，避免带着坏 Token 反复 401。
      // 只有当前路由本身需要登录时才跳转，公开页（首页/题库/提交记录）不能被锁死。
      useUserStore().resetUser()
      const current = router.currentRoute.value
      if (shouldRedirectToLogin({
        requiresAuth: current.meta?.auth,
        path: current.path,
        skipAuthRedirect: err.config?.skipAuthRedirect
      })) {
        router.push({ path: '/login', query: { redirect: current.fullPath } })
      }
    } else if (status === 403) {
      Message.error(msg || '无权限访问')
    } else if (status === 404) {
      Message.error(msg || '资源不存在')
    } else if (status === 429) {
      Message.error(msg || '操作过于频繁，请稍后再试')
    } else {
      Message.error(msg || err.message || '服务异常')
    }
    return Promise.reject(err)
  }
)

export default request
export { baseURL }
