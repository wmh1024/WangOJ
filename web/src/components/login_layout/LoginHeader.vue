<script lang="ts" setup></script>

<template>
  <div id="LoginHeader">
    <a-space class="logo_container">
      <img alt="logo" class="logo" src="@/assets/logo.png" />
      <div class="title">Wang-OJ</div>
    </a-space>
    <div class="des">Wang-OJ 是石家庄裕华区最很好看的在线判题平台</div>
  </div>
</template>

<style scoped>
.logo {
  width: 45px;
  height: 45px;
}

.title {
  font-size: 30px;
  color: #000;
}

.logo_container {
  margin-top: 65px;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 20px;
}

#LoginHeader {
  text-align: center;
  margin-bottom: 50px;
}

.des {
  color: rgba(0, 0, 0, 0.65);
  font-size: 16px;
}

/*:deep(.arco-menu-selected-label) {
  background-color: #00c1a6 !important;
}

:deep(.arco-menu-item) {
  color: #00c1a6;
}*/
</style>
