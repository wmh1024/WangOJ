<script lang="ts" setup>
import { computed } from 'vue'
import { routes } from '@/router/routes'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { AUTH } from '@/utils/constants'
import { IconExport, IconUser } from '@arco-design/web-vue/es/icon'
import { getUserLogout } from '@/api/user'

const route = useRoute()
const path = computed(() => route.path)
const userStore = useUserStore()

/**
 * 菜单项随角色变化：必须用 computed，否则登录/升级为管理员后菜单不会刷新。
 */
const menuItems = computed(() => {
  const all = routes[0].children || []
  return all.filter(
    (item: any) =>
      item.meta?.hide !== true &&
      !(item.meta?.auth === AUTH.ADMIN && userStore.userRole !== AUTH.ADMIN)
  )
})

const router = useRouter()
const onMenuChange = (key: string) => {
  router.push(key)
}

const doLogout = async () => {
  try {
    // 注销登录：失败也要清本地态，避免用户卡在"已登录但后端不认"的状态
    await getUserLogout()
  } catch {
    // 忽略：本地状态照常清理
  } finally {
    userStore.resetUser()
    router.push('/login')
  }
}

const doLogin = () => {
  router.push('/login')
}
</script>

<template>
  <div id="header">
    <div class="menu-demo">
      <a-menu :selected-keys="[path]" mode="horizontal">
        <a-menu-item key="0" :style="{ padding: 0, marginRight: '38px' }" disabled>
          <div class="logo_container">
            <a-space>
              <img alt="logo" class="logo" src="@/assets/logo.png" />
              <div class="title">Wang-OJ</div>
            </a-space>
          </div>
        </a-menu-item>
        <a-menu-item v-for="item in menuItems" :key="item.path" @click="onMenuChange(item.path)">
          {{ item.name }}
        </a-menu-item>
      </a-menu>
      <a-dropdown class="dropdown" position="br">
        <div class="avatar">
          <a-avatar :style="{ backgroundColor: '#00d0b6' }">
            <IconUser />
          </a-avatar>
          <div class="userAccount">{{ userStore.userAccount || '未登录' }}</div>
        </div>
        <template #content>
          <div v-if="userStore.userRole">
            <a-doption @click="doLogout()">
              <template #icon>
                <icon-export />
              </template>
              退出登录
            </a-doption>
          </div>
          <div v-else>
            <a-doption @click="doLogin()">
              <template #icon>
                <icon-export />
              </template>
              用户登录
            </a-doption>
          </div>
        </template>
      </a-dropdown>
    </div>
  </div>
</template>

<style scoped>
.logo {
  width: 35px;
  height: 35px;
}

.title {
  font-size: 20px;
  color: #000;
}

.logo_container {
  display: flex;
  justify-content: center;
  align-items: center;
}

.menu-demo {
  box-sizing: border-box;
  /*background-color: var(--color-neutral-2);*/
  width: 80%; /* 设置元素宽度为父元素宽度的80% */
  margin: 0 auto; /* 设置左右外边距为自动，实现水平居中 */
  display: flex; /* 将容器设置为 Flex 布局 */
  justify-content: flex-end; /* 将子元素推到容器的右侧 */
  align-items: center; /* 垂直方向上元素居中对齐 */
}

#header {
  border-bottom: 1px solid #ebebeb;
  background-color: #fff;
  width: 100%;
}

.dropdown {
  margin-left: auto; /* 将 a-dropdown 推到容器的右侧 */
  margin-right: 10px;
}

.avatar {
  cursor: pointer; /* 设置光标为手型 */
  display: flex;
  justify-content: center;
  align-items: center;
}

.userAccount {
  margin-left: 15px;
  min-width: 50px;
  font-size: 16px;
  color: #000;
}
</style>
