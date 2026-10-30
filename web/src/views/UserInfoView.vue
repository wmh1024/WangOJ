<template>
  <div id="UserInfoView">
    <div class="container">
      <UserInfoHeader />
      <a-divider />
      <LatestActivity />
    </div>
  </div>
</template>

<script lang="ts" setup>
import UserInfoHeader from '@/components/user/UserInfoHeader.vue'
import LatestActivity from '@/components/user/LatestActivity.vue'
</script>

<style scoped lang="less">
.container {
  padding: 20px 20px 20px 20px;
}

.content {
  display: flex;
  margin-top: 12px;

  &-left {
    flex: 1;
    margin-right: 16px;
    overflow: hidden;

    :deep(.arco-tabs-nav-tab) {
      margin-left: 16px;
    }
  }

  &-right {
    width: 332px;
  }

  .tab-pane-wrapper {
    padding: 0 16px 16px 16px;
  }
}
</style>

<style lang="less" scoped>
#UserInfoView {
  width: 80%;
  margin: 0 auto;
}

.mobile {
  .content {
    display: block;

    &-left {
      margin-right: 0;
      margin-bottom: 16px;
    }

    &-right {
      width: 100%;
    }
  }
}
</style>
