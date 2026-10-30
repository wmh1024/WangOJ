<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue'
import MarkdownEditor from '@/components/editor/MarkdownEditor.vue'
import type { ProblemVO } from '@/api/problem'
import { addProblem, changeProblem, getProblemVOById } from '@/api/problem'
import { Message } from '@arco-design/web-vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const id = ref()

// 与后端允许范围保持一致：时间 200-5000ms，内存 32-512MB
const DEFAULT_TIMEOUT = 1000
const DEFAULT_MEMORY = 256

const form = reactive({
  title: '',
  timeoutLimit: DEFAULT_TIMEOUT,
  memoryLimit: DEFAULT_MEMORY,
  testCase: [
    {
      input: '',
      output: ''
    }
  ]
})

/**
 * 提交前的表单校验。
 */
const validate = (): boolean => {
  if (!form.title.trim()) {
    Message.warning('请填写题目标题')
    return false
  }
  if (!content.value.trim()) {
    Message.warning('请填写题面内容')
    return false
  }
  if (!(form.timeoutLimit >= 200 && form.timeoutLimit <= 5000)) {
    Message.warning('时间限制需在 200-5000 ms 之间')
    return false
  }
  if (!(form.memoryLimit >= 32 && form.memoryLimit <= 512)) {
    Message.warning('内存限制需在 32-512 MB 之间')
    return false
  }
  const cases = form.testCase.filter((c) => c && (c.input?.trim() || c.output?.trim()))
  if (cases.length === 0) {
    Message.warning('至少需要一个测试用例')
    return false
  }
  form.testCase = cases
  return true
}
const tags = ref([])
const content = ref('')

const doSubmit = async () => {
  if (!validate()) return
  if (route?.query?.id) {
    const problem: ProblemVO = {
      testCase: form.testCase,
      title: form.title,
      tags: tags.value,
      limitOption: {
        timeoutLimit: form.timeoutLimit,
        memoryLimit: form.memoryLimit
      },
      content: content.value,
      id: id.value
    }
    const res = await changeProblem(problem)
    if (res) {
      Message.success('修改成功！')
      // 重置变量
      router.back()
    }
  } else {
    const problem: ProblemVO = {
      testCase: form.testCase,
      title: form.title,
      tags: tags.value,
      limitOption: {
        timeoutLimit: form.timeoutLimit,
        memoryLimit: form.memoryLimit
      },
      content: content.value
    }
    const res = await addProblem(problem)
    if (res) {
      Message.success('添加成功！')
      // 重置变量
      content.value = ''
      form.title = ''
      tags.value = []
      form.testCase = [
        {
          input: '',
          output: ''
        }
      ]
      form.timeoutLimit = DEFAULT_TIMEOUT
      form.memoryLimit = DEFAULT_MEMORY
    }
  }
}

const handleAdd = () => {
  form.testCase.push({
    input: '',
    output: ''
  })
}
const handleDelete = (index: any) => {
  form.testCase.splice(index, 1)
}

const getContent = (v: string) => {
  content.value = v
}

const change = async () => {
  const res = await getProblemVOById(id.value)
  const problem = res.data?.data
  if (!problem) {
    Message.error('题目不存在或无权访问')
    return
  }
  form.title = problem.title || ''
  form.testCase = problem.testCase?.length
    ? problem.testCase
    : [{ input: '', output: '' }]
  form.timeoutLimit = problem.limitOption?.timeoutLimit || DEFAULT_TIMEOUT
  form.memoryLimit = problem.limitOption?.memoryLimit || DEFAULT_MEMORY
  tags.value = problem.tags || []
  content.value = problem.content || ''
}

if (route?.query?.id) {
  id.value = route.query.id
  change()
}
onMounted(async () => {})
</script>

<template>
  <div id="CreateView">
    <h1 class="title">{{ id ? '修改题目' : '创建题目' }}</h1>
    <a-form :model="form" :style="{ width: '90%' }">
      <a-form-item field="name" label="标题">
        <a-input v-model="form.title" placeholder="请填写题目标题" />
      </a-form-item>
      <a-form-item field="post" label="题目内容">
        <MarkdownEditor @get-content="getContent" />
      </a-form-item>
      <a-form-item field="post" label="标签">
        <a-input-tag v-model="tags" allow-clear placeholder="请填写题目标签" />
      </a-form-item>
      <a-form-item label="题目限制">
        <a-space :size="16" direction="vertical" style="display: block; width: 100%">
          <a-row :gutter="24">
            <a-col :span="12">
              <a-form-item field="post" label="时间限制">
                <a-input-number
                  v-model="form.timeoutLimit"
                  :min="200"
                  :max="5000"
                  :step="100"
                  placeholder="请填写时间限制"
                >
                  <template #append>ms</template>
                </a-input-number>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item field="post" label="空间限制">
                <a-input-number
                  v-model="form.memoryLimit"
                  :min="32"
                  :max="512"
                  :step="32"
                  placeholder="请填写空间限制"
                >
                  <template #append>MB</template>
                </a-input-number>
              </a-form-item>
            </a-col>
          </a-row>
        </a-space>
      </a-form-item>
      <a-form-item
        v-for="(post, index) of form.testCase"
        :key="index"
        :field="`posts[${index}].value`"
        :label="`测试用例-${index + 1}`"
      >
        <a-space :size="16" direction="vertical" style="display: block; width: 100%">
          <a-row :gutter="24">
            <a-col :span="11">
              <a-form-item field="posts.post1" label="输入样例" style="width: 100%">
                <a-textarea v-model="post.input" placeholder="请输入输入样例" />
              </a-form-item>
            </a-col>
            <a-col :span="11">
              <a-form-item field="posts.post2" label="输出样例">
                <a-textarea v-model="post.output" placeholder="请输入输出样例" />
              </a-form-item>
            </a-col>
            <a-col :span="2">
              <a-button
                :style="{ marginLeft: '10px' }"
                status="danger"
                @click="handleDelete(index)"
              >
                删除
              </a-button>
            </a-col>
          </a-row>
        </a-space>
      </a-form-item>
    </a-form>
    <div class="commit">
      <a-button long status="success" style="width: 30%" type="primary" @click="handleAdd">
        添加测试用例
      </a-button>
      <div class="spacer"></div>
      <a-button html-type="submit" long style="width: 30%" type="primary" @click="doSubmit">
        {{ id ? '修改题目' : '创建题目' }}
      </a-button>
    </div>
  </div>
</template>

<style scoped>
#CreateView {
  width: 90%;
  margin: 0 auto;
}

.title {
  text-align: center;
  margin: 30px 0;
}

.commit {
  display: flex;
  justify-content: center;
}

.spacer {
  flex-grow: 0.2; /* 占位元素会占据剩余空间 */
}
</style>
