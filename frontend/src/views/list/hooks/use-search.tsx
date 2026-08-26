import { reactive } from 'vue'
import { STATUS_OPTIONS } from '../../../constant'

/**
 * 搜索栏配置。index.vue 按 fields 渲染表单项；
 * .tsx 后缀：将来某个搜索项需要自定义渲染时直接写 JSX。
 */
export interface SearchModel {
  keyword: string
  status: string
}

export interface SearchField {
  label: string
  key: keyof SearchModel
  component: 'input' | 'select'
  placeholder?: string
  options?: { label: string; value: string }[]
}

export function useSearch() {
  const model = reactive<SearchModel>({ keyword: '', status: 'all' })

  const fields: SearchField[] = [
    {
      label: '关键词',
      key: 'keyword',
      component: 'input',
      placeholder: '按标题搜索',
    },
    {
      label: '状态',
      key: 'status',
      component: 'select',
      options: STATUS_OPTIONS,
    },
  ]

  function reset() {
    model.keyword = ''
    model.status = 'all'
  }

  return { model, fields, reset }
}
