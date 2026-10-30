<script lang="ts" setup>
import { ref } from 'vue'
import { IconLock, IconUser } from '@arco-design/web-vue/es/icon'
import { postUserLogin, type LoginForm, type TokenVO } from '@/api/user'
import { Message } from '@arco-design/web-vue'
import { useUserStore } from '@/stores/user'
import router from '@/router'
import { useRoute } from 'vue-router'

const form = ref<LoginForm>({
  userAccount: '',
  userPassword: ''
})
const userStore = useUserStore()
const route = useRoute()
const handleSubmit = async () => {
  if (!form.value.userAccount.trim() || !form.value.userPassword) {
    Message.warning('请输入用户名和密码')
    return
  }
  try {
    const res = await postUserLogin(form.value)
    // 保存 token 与用户信息
    const tokenVO: TokenVO = res.data.data
    userStore.setUser(tokenVO.userVO)
    userStore.setToken(tokenVO.token)
    Message.success('登录成功')
    const redirect = route?.query?.redirect
    router.push(typeof redirect === 'string' && redirect.startsWith('/') ? redirect : '/')
  } catch {
    // 错误提示已由 axios 拦截器统一处理
  }
}
</script>

<template>
  <div id="LoginContent">
    <a-form :model="form" :style="{ width: '100%' }" @submit="handleSubmit">
      <a-form-item field="name" label="用户名">
        <a-input v-model="form.userAccount" placeholder="请输入用户名">
          <template #prefix>
            <icon-user />
          </template>
        </a-input>
      </a-form-item>
      <a-form-item field="post" label="密码">
        <a-input-password v-model="form.userPassword" placeholder="请输入密码">
          <template #prefix>
            <icon-lock />
          </template>
        </a-input-password>
      </a-form-item>
      <a-form-item hide-label>
        <a-button html-type="submit" long shape="round" type="primary"> 登录</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<style scoped></style>
