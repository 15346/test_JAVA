<script setup lang="ts">
import { ref, watch } from 'vue'
import type { Todo } from '../api/type'

const props = defineProps<{
  open: boolean
  /** 传入 = 编辑该条；null = 新增 */
  todo: Todo | null
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  save: [payload: { id?: number; title: string; done: boolean }]
}>()

const title = ref('')
const done = ref(false)

// 抽屉每次打开时，用传入的 todo 初始化表单
watch(
  () => props.open,
  (open) => {
    if (open) {
      title.value = props.todo?.title ?? ''
      done.value = props.todo?.done ?? false
    }
  },
)

function handleOk() {
  const text = title.value.trim()
  if (!text) return
  emit('save', { id: props.todo?.id, title: text, done: done.value })
  emit('update:open', false)
}

function handleCancel() {
  emit('update:open', false)
}
</script>

<template>
  <a-drawer :open="open" :title="todo ? '编辑待办' : '新增待办'" @close="handleCancel">
    <a-form layout="vertical">
      <a-form-item label="标题" required>
        <a-input
          v-model:value="title"
          placeholder="输入待办标题"
          @keydown.enter="handleOk"
        />
      </a-form-item>
      <a-form-item v-if="todo" label="完成状态">
        <a-switch v-model:checked="done" checked-children="完成" un-checked-children="未完" />
      </a-form-item>
      <a-form-item>
        <a-space>
          <a-button type="primary" @click="handleOk">保存</a-button>
          <a-button @click="handleCancel">取消</a-button>
        </a-space>
      </a-form-item>
    </a-form>
  </a-drawer>
</template>
