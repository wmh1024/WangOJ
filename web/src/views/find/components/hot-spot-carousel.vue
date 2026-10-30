<!--
  本文件改编自 Arco Design Pro
  （https://github.com/arco-design/arco-design-pro-vue，
  src/views/dashboard/workplace/components/carousel.vue）。
  Copyright (c) 2021 Bytedance, Inc. and its affiliates，MIT License；
  许可证全文见 web/LICENSE-ARCO-PRO，归属说明见仓库根目录 NOTICE。
-->
<template>
  <a-carousel
    class="carousel"
    :auto-play="true"
    :move-speed="1000"
    animation-name="fade"
    show-arrow="never"
  >
    <a-carousel-item v-for="(image, idx) in images" :key="idx">
      <img
        :src="image"
        :style="{
                display: 'block',
                margin: 'auto',
                maxWidth: '100%',
                minWidth: '100%',
                position: 'absolute',
                top: '50%',
                transform: 'translateY(-50%)',
        }"
      />
    </a-carousel-item>
  </a-carousel>
</template>

<script>
import { HOME_BANNERS } from '@/utils/constants'

export default {
  setup() {
    const images = HOME_BANNERS;
    return {
      images
    };
  }
};
</script>

<style lang="less" scoped>
.carousel {
  width: 100%;
  height: 450px;
}
</style>
