import { describe, expect, it } from 'vitest'
import { useSearch } from './use-search'

describe('useSearch', () => {
  it('初始模型为空关键词 + 全部状态', () => {
    const { model } = useSearch()
    expect(model.keyword).toBe('')
    expect(model.status).toBe('all')
  })

  it('字段配置覆盖关键词与状态', () => {
    const { fields } = useSearch()
    expect(fields.map((f) => f.key)).toEqual(['keyword', 'status'])
  })

  it('reset 清空搜索条件', () => {
    const { model, reset } = useSearch()
    model.keyword = '牛奶'
    model.status = 'done'
    reset()
    expect(model.keyword).toBe('')
    expect(model.status).toBe('all')
  })
})
