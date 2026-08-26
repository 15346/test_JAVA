import { describe, expect, it } from 'vitest'
import { usePermissionConfig } from './use-permission-config'

describe('usePermissionConfig', () => {
  it('返回新增/编辑/删除三个权限点', () => {
    const p = usePermissionConfig()
    expect(p).toEqual({ canAdd: true, canEdit: true, canDelete: true })
  })
})
