<script lang="ts" setup>
import LoginHeader from '@/components/login_layout/LoginHeader.vue'
import LoginContent from '@/components/login_layout/LoginContent.vue'
import RegisterContent from '@/components/login_layout/RegisterContent.vue'
import CommonFooter from '@/components/common/CommonFooter.vue'
</script>

<template>
  <div id="CodeLayout">
    <a-layout style="height: 400px">
      <a-layout-header>
        <LoginHeader />
      </a-layout-header>
      <a-layout-content class="content">
        <a-tabs default-active-key="1" size="large">
          <a-tab-pane key="1" title="登录">
            <LoginContent />
          </a-tab-pane>
          <a-tab-pane key="2" title="注册">
            <RegisterContent />
          </a-tab-pane>
        </a-tabs>
      </a-layout-content>
      <a-layout-footer>
        <div class="footer">
          <CommonFooter />
        </div>
      </a-layout-footer>
    </a-layout>
  </div>
</template>

<style scoped>
#CodeLayout {
  display: flex;
  flex-direction: column; /* 方向为纵向，保持正常的从上到下文档流 */
  height: 100vh;
  background: url('@/assets/backgroud.webp');
  background-size: 100% 100%;
}

.content {
  width: 30%;
  margin: 0 auto;
}

:deep(.arco-tabs-nav-tab) {
  justify-content: center;
}

:deep(.arco-tabs-tab-title) {
  font-size: 16px;
}

.footer {
  margin-top: auto; /* 设置footer的上外边距为auto */
}
</style>
