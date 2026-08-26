import { describe, expect, it, vi } from 'vitest'
import { useTableColumns } from './use-table-columns'
import type { PermissionConfig } from '../../../use-permission-config'

const permission: PermissionConfig = { canAdd: true, canEdit: true, canDelete: true }

function makeColumns(p: PermissionConfig = permission) {
  return useTableColumns({
    onEdit: vi.fn(),
    onDelete: vi.fn(),
    onToggle: vi.fn(),
    permission: p,
  })
}

describe('useTableColumns', () => {
  it('生成 ID/标题/状态/操作 四列', () => {
    const keys = makeColumns().map((c) => String(c.key ?? c.dataIndex))
    expect(keys).toEqual(['id', 'title', 'done', 'action'])
  })

  it('状态列和操作列提供 customRender（JSX 渲染）', () => {
    const columns = makeColumns()
    expect(typeof columns[2].customRender).toBe('function')
    expect(typeof columns[3].customRender).toBe('function')
  })

  it('权限关闭时列结构不变（按钮显隐在渲染层处理）', () => {
    const columns = makeColumns({ canAdd: true, canEdit: false, canDelete: false })
    expect(columns).toHaveLength(4)
  })
})
