<!--
  本文件的左右分栏布局参考自 Arco Design Pro
  （https://github.com/arco-design/arco-design-pro-vue，src/views/dashboard/workplace/index.vue）。
  Copyright (c) 2021 Bytedance, Inc. and its affiliates，MIT License；
  许可证全文见 web/LICENSE-ARCO-PRO，归属说明见仓库根目录 NOTICE。
-->
<template>
  <div id="findIndex">
    <div class="container-f">
      <div class="layout">
        <div class="layout-left-side">
          <a-row :gutter="[24, 12]">
            <a-col :span="23">
              <a-row>
                <a-col :span="24">
                  <hot-spot-carousel />
                </a-col>
              </a-row>
            </a-col>
            <a-col :span="23">
              <problem-table />
            </a-col>
          </a-row>
        </div>
        <div class="layout-right-side">
          <a-space size="medium" direction="vertical">
            <announcement />
            <user-list />
          </a-space>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import HotSpotCarousel from '@/views/find/components/hot-spot-carousel.vue'
import ProblemTable from '@/views/find/components/problem-table.vue'
import Announcement from '@/views/find/components/announcement.vue'
import UserList from '@/views/find/components/user-list.vue'
</script>

<style scoped lang="less">
.container-f {
  padding: 0 20px 20px;
}

.layout {
  display: flex;
  justify-content: space-between;

  &-left-side {
    flex-basis: 75%;
  }

  &-right-side {
    flex-basis: 25%;
  }
}
</style>

<style lang="less" scoped>
#findIndex {
  padding-top: 20px;
  width: 80%;
  margin: 0 auto;
}

// responsive
@media (max-width: @screen-lg) {
  .layout {
    flex-wrap: wrap;

    &-left-side {
      flex: 1;
      flex-basis: 100%;
      margin-bottom: 16px;
    }

    &-right-side {
      flex-basis: 100%;
    }
  }
}
</style>

