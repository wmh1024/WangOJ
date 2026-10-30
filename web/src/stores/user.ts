import { ref } from 'vue'
import { defineStore } from 'pinia'
import { AUTH } from '@/utils/constants'
import type { UserDTO } from '@/api/user'

export const useUserStore = defineStore(
  'user',
  () => {
    const userId = ref()
    const userAccount = ref()
    const userRole = ref<AUTH>()
    const token = ref()

    const setToken = (newToken: string) => {
      token.value = newToken
    }

    const setUser = (newUser: UserDTO) => {
      userId.value = newUser.id
      userAccount.value = newUser.userAccount
      userRole.value = newUser.userRole
    }

    const resetUser = () => {
      userId.value = undefined
      userAccount.value = undefined
      userRole.value = undefined
      token.value = undefined
    }
    return { userId, userAccount, token, userRole, setUser, setToken, resetUser }
  },
  {
    // 持久化到 sessionStorage：关闭标签页即失效，缩小 Token 被 XSS 读取的窗口
    persist: {
      storage: sessionStorage,
      key: 'wangoj-user'
    }
  }
)
