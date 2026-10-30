<template>
  <a-card :bordered="false">
    <a-space :size="54">
      <a-avatar :size="100" class="info-avatar">
        <icon-user />
      </a-avatar>
      <a-descriptions
        :data="renderData"
        :column="2"
        align="right"
        layout="inline-horizontal"
        :label-style="{
          width: '140px',
          fontWeight: 'normal',
          color: 'rgb(var(--gray-8))'
        }"
        :value-style="{
          width: '200px',
          paddingLeft: '8px',
          textAlign: 'left'
        }"
      >
      </a-descriptions>
    </a-space>
  </a-card>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { IconUser } from '@arco-design/web-vue/es/icon'

const userStore = useUserStore()

/**
 * 只展示后端真实存在的字段。
 * 头像/手机号等字段当前没有数据来源，不渲染占位假数据。
 */
const renderData = computed(() => [
  { label: '用户 ID', value: userStore.userId ?? '-' },
  { label: '账号', value: userStore.userAccount ?? '-' },
  { label: '角色', value: userStore.userRole === 'admin' ? '管理员' : '普通用户' }
])
</script>

<style scoped lang="less">
.arco-card {
  padding: 14px 0 4px 4px;
  border-radius: 4px;
}

:deep(.arco-avatar-trigger-icon-button) {
  width: 32px;
  height: 32px;
  line-height: 32px;
  background-color: #e8f3ff;

  .arco-icon-camera {
    margin-top: 8px;
    color: rgb(var(--arcoblue-6));
    font-size: 14px;
  }
}
</style>
