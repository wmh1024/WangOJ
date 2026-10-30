import { createRouter, createWebHistory } from 'vue-router'
import { routes } from '@/router/routes'
import { getUserStatus } from '@/api/user'
import { useUserStore } from '@/stores/user'
import { createNavigationGuard } from '@/router/guard'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: routes
})

/**
 * 全局前置守卫：具体规则与可单测的实现见 {@link createNavigationGuard}。
 */
router.beforeEach((to) => {
  const userStore = useUserStore()
  return createNavigationGuard(userStore, getUserStatus)(to)
})

router.afterEach((to) => {
  const name = to.name?.toString()
  document.title = name ? `${name} | WangOJ` : 'WangOJ 在线判题系统'
})

export default router
