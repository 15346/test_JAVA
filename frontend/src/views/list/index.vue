<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { createTodo, deleteTodo, listTodos, updateTodo } from './api'
import type { Todo } from './api/type'
import { usePermissionConfig } from '../../shared/use-permission-config'
import { useSearch } from './hooks/use-search'
import { useTableColumns } from './hooks/use-table-columns'
import TodoFormDrawer from './components/todo-form-drawer.vue'

// ---------- 权限 ----------
const permission = usePermissionConfig()

// ---------- 搜索（模型 + 字段配置，本地过滤） ----------
const { model: searchModel, fields: searchFields, reset: resetSearch } = useSearch()

// ---------- 数据 ----------
const todos = ref<Todo[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    todos.value = await listTodos()
  } finally {
    loading.value = false
  }
}

onMounted(load)

// 后端只有全量 GET，关键词/状态过滤都在前端做
const filtered = computed(() =>
  todos.value.filter((t) => {
    const hitKeyword = t.title.includes(searchModel.keyword.trim())
    const hitStatus =
      searchModel.status === 'all'
        ? true
        : searchModel.status === 'done'
          ? t.done
          : !t.done
    return hitKeyword && hitStatus
  }),
)

// ---------- 表格事件 ----------
async function handleToggle(todo: Todo) {
  await updateTodo({ ...todo, done: !todo.done })
  await load()
}

async function handleDelete(todo: Todo) {
  await deleteTodo(todo.id)
  await load()
}

const columns = useTableColumns({
  onEdit: (todo) => openDrawer(todo),
  onDelete: handleDelete,
  onToggle: handleToggle,
  permission,
})

// ---------- 抽屉（新增/编辑共用） ----------
const drawerOpen = ref(false)
const editing = ref<Todo | null>(null)

function openDrawer(todo: Todo | null) {
  editing.value = todo
  drawerOpen.value = true
}

async function handleSave(payload: { id?: number; title: string; done: boolean }) {
  if (payload.id === undefined) {
    await createTodo(payload.title)
  } else {
    await updateTodo({ id: payload.id, title: payload.title, done: payload.done })
  }
  await load()
}
</script>

<template>
  <a-card>
    <!-- 搜索栏：按 use-search 的 fields 配置渲染 -->
    <a-form layout="inline" style="margin-bottom: 16px">
      <a-form-item v-for="field in searchFields" :key="field.key" :label="field.label">
        <a-input
          v-if="field.component === 'input'"
          v-model:value="searchModel[field.key]"
          :placeholder="field.placeholder"
          allow-clear
          style="width: 200px"
        />
        <a-select
          v-else
          v-model:value="searchModel[field.key]"
          :options="field.options"
          style="width: 140px"
        />
      </a-form-item>
      <a-form-item>
        <a-space>
          <a-button type="primary" @click="load">查询</a-button>
          <a-button @click="resetSearch">重置</a-button>
        </a-space>
      </a-form-item>
    </a-form>

    <!-- 工具栏：新增（受权限点控制） -->
    <a-button
      v-if="permission.canAdd"
      type="primary"
      style="margin-bottom: 16px"
      @click="openDrawer(null)"
    >
      新增待办
    </a-button>

    <!-- 表格：antd 自带前端分页 -->
    <a-table
      row-key="id"
      :columns="columns"
      :data-source="filtered"
      :loading="loading"
      :pagination="{ pageSize: 5, showSizeChanger: false }"
    />

    <!-- 新增/编辑抽屉 -->
    <TodoFormDrawer v-model:open="drawerOpen" :todo="editing" @save="handleSave" />
  </a-card>
</template>
