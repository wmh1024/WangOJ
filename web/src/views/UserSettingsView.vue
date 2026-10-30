<template>
  <div id="UserSettingsView">
    <div class="container">
      <a-row style="margin-bottom: 16px">
        <a-col :span="24">
          <UserPanel />
        </a-col>
      </a-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import UserPanel from '@/components/user/UserPanel.vue'
</script>

<style scoped>
#UserSettingsView {
  width: 75%;
  margin: 0 auto;
}
</style>
