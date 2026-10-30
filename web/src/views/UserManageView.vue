<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { getUserList, putUserUpdate } from '@/api/user'
import { Message } from '@arco-design/web-vue'

// 列名
const columns = [
  {
    title: '编号',
    dataIndex: 'id',
    width: 100
  },
  {
    title: '账号',
    dataIndex: 'userAccount'
  },
  {
    title: '用户权限',
    slotName: 'userRole'
  },
  {
    title: '上次登录时间',
    dataIndex: 'lastLoginTime'
  },
  {
    title: '注册时间',
    dataIndex: 'createdTime'
  },
  {
    title: '操作',
    slotName: 'optional',
    width: 200
  }
]

// 表格数据
const data = ref([])

// 表格相关变量
const loading = ref(true)
const current = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 获取数据列表
const getData = async () => {
  loading.value = true
  const res = await getUserList(current.value, pageSize.value)
  total.value = res.data.data.total
  current.value = res.data.data.current
  pageSize.value = res.data.data.size
  data.value = res.data.data.t
  loading.value = false
}

// 钩子
onMounted(() => {
  getData()
})

// 修改表单
const updateForm = reactive({
  id: '',
  userAccount: '',
  userPassword: ''
})

// 更新用户对话框
const visible = ref(false)

// 更新用户
const updateUser = async () => {
  await putUserUpdate(+updateForm.id, {
    userAccount: updateForm.userAccount,
    userPassword: updateForm.userPassword
  })
  Message.success('修改成功')
  updateForm.id = ''
  updateForm.userAccount = ''
  updateForm.userPassword = ''
}

// 删除用户
const delUser = async (id: number) => {
  await delUser(id)
  Message.success('删除成功')
  getData()
}
</script>

<template>
  <div id="UserManagementView">
    <h1 class="title">用户列表</h1>
    <a-table :columns="columns" :data="data" :loading="loading" :pagination="false">
      <template #userRole="{ record }">
        <a-tag color="red" v-if="record['userRole'] === 'admin'">管理员</a-tag>
        <a-tag color="blue" v-else>普通用户</a-tag>
      </template>

      <template #optional="{ record }">
        <a-space>
          <a-button
            type="primary"
            status="warning"
            @click="
              (updateForm.userAccount = record['userAccount']),
                (visible = true),
                (updateForm.id = record['id'])
            "
            >编辑
          </a-button>
          <a-popconfirm content="确定要删除吗？" type="error" @ok="() => delUser(record['id'])">
            <a-button type="primary" status="danger">删除</a-button>
          </a-popconfirm>
        </a-space>
      </template>
    </a-table>
    <div class="pagination">
      <a-space direction="vertical" size="large" style="margin-top: 30px">
        <a-pagination
          :current="current"
          :page-size="pageSize"
          :total="total"
          show-jumper
          show-total
          size="medium"
          @change="(page: number) => {
            current = page
            getData()
          }"
        />
      </a-space>
    </div>
    <a-modal v-model:visible="visible" @ok="updateUser()" unmountOnClose>
      <template #title>编辑用户</template>
      <div>
        <a-form :model="updateForm" layout="vertical">
          <a-form-item field="userAccount" label="账号">
            <a-input v-model="updateForm.userAccount" disabled placeholder="请输入账号" />
          </a-form-item>
          <a-form-item field="userPassword" label="密码">
            <a-input-password v-model="updateForm.userPassword" placeholder="请输入密码" />
          </a-form-item>
        </a-form>
      </div>
    </a-modal>
  </div>
</template>

<style scoped>
#UserManagementView {
  width: 80%;
  margin: 0 auto;
}

.title {
  text-align: center;
  margin: 30px 0;
}

.pagination {
  display: flex;
  justify-content: center;
}
</style>
