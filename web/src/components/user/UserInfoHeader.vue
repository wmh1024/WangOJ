<template>
  <div class="header">
    <a-space :size="12" direction="vertical" align="center">
      <a-avatar :size="64">
        <icon-user />
      </a-avatar>
      <a-typography-title :heading="6" style="margin: 0">
        {{ userInfo.userAccount }}
        <a-tag color="red" v-if="userInfo['userRole'] === 'admin'">管理员</a-tag>
      </a-typography-title>
    </a-space>
  </div>
</template>

<script lang="ts" setup>
import { useUserStore } from '@/stores/user'
import { IconUser } from '@arco-design/web-vue/es/icon'

const userInfo = useUserStore()
</script>

<style scoped lang="less">
.header {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 204px;
  color: var(--gray-10);
  background: #f2f3f5;
  background-size: cover;
  border-radius: 4px;

  :deep(.arco-avatar-trigger-icon-button) {
    color: rgb(var(--arcoblue-6));

    :deep(.arco-icon) {
      vertical-align: -1px;
    }
  }

  .user-msg {
    .arco-icon {
      color: rgb(var(--gray-10));
    }

    .arco-typography {
      margin-left: 6px;
    }
  }
}
</style>
