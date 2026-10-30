<script lang="ts" setup>
import { IconLock, IconUser } from '@arco-design/web-vue/es/icon'
import { ref } from 'vue'
import { postUserRegister, type LoginForm, type TokenVO } from '@/api/user'
import { Message } from '@arco-design/web-vue'
import { useUserStore } from '@/stores/user'
import router from '@/router'

const optionForm = ref({
  reUserPassword: '',
  isRead: false
})

const userForm = ref<LoginForm>({
  userAccount: '',
  userPassword: ''
})

const userStore = useUserStore()

// 与后端校验规则保持一致：字母开头，字母/数字/下划线，1-64 位
const ACCOUNT_PATTERN = /^[A-Za-z][A-Za-z0-9_]{0,63}$/
// 至少 8 位，必须同时包含字母和数字
const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*[0-9])[A-Za-z0-9~!@#$%^&*_.-]{8,64}$/

const handleSubmit = async () => {
  const userAccount = userForm.value.userAccount.trim()
  if (!ACCOUNT_PATTERN.test(userAccount)) {
    Message.warning('用户名需以字母开头，只能包含字母、数字、下划线（1-64 位）')
    return
  }
  if (!PASSWORD_PATTERN.test(userForm.value.userPassword)) {
    Message.warning('密码需 8-64 位，且同时包含字母和数字')
    return
  }
  if (optionForm.value.isRead === false) {
    // 未勾选
    Message.warning('请阅读并勾选协议条款！')
    return
  }
  if (optionForm.value.reUserPassword !== userForm.value.userPassword) {
    Message.warning('两次输入的密码不一致！')
    userForm.value.userPassword = ''
    optionForm.value.reUserPassword = ''
    return
  }
  userForm.value.userAccount = userAccount
  try {
    const res = await postUserRegister(userForm.value)
    // 保存 token 与用户信息
    const tokenVO: TokenVO = res.data.data
    userStore.setUser(tokenVO.userVO)
    userStore.setToken(tokenVO.token)
    Message.success('注册成功')
    router.push('/')
  } catch {
    // 错误提示已由 axios 拦截器统一处理
  }
}
</script>

<template>
  <div id="RegisterContent">
    <a-form :model="userForm" :style="{ width: '100%' }" @submit="handleSubmit">
      <a-form-item field="name" label="用户名">
        <a-input v-model="userForm.userAccount" placeholder="请输入用户名">
          <template #prefix>
            <icon-user />
          </template>
        </a-input>
      </a-form-item>
      <a-form-item field="post" label="密码">
        <a-input-password v-model="userForm.userPassword" placeholder="请输入密码">
          <template #prefix>
            <icon-lock />
          </template>
        </a-input-password>
      </a-form-item>
      <a-form-item field="post" label="确认密码">
        <a-input-password v-model="optionForm.reUserPassword" placeholder="请再次输入密码">
          <template #prefix>
            <icon-lock />
          </template>
        </a-input-password>
      </a-form-item>
      <a-form-item field="isRead" hide-label>
        <a-checkbox v-model="optionForm.isRead">我已阅读并清楚 Wang-OJ 相关协议条款</a-checkbox>
      </a-form-item>
      <a-form-item hide-label>
        <a-button html-type="submit" long shape="round" type="primary">注册</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<style scoped></style>
