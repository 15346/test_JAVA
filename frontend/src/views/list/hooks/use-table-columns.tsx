import { Button, Popconfirm, Switch } from 'ant-design-vue'
import type { TableColumnType } from 'ant-design-vue'
import type { Todo } from '../api/type'
import type { PermissionConfig } from '../../../shared/use-permission-config'

/**
 * 表格列配置。.tsx：状态列/操作列的 customRender 用 JSX 写最顺手。
 * 注意：JSX 不经过模板编译，antd 组件必须显式 import（按需插件只作用于模板）。
 */
export interface UseTableColumnsOptions {
  onEdit: (todo: Todo) => void
  onDelete: (todo: Todo) => void
  /** Switch 切换完成状态 */
  onToggle: (todo: Todo) => void
  permission: PermissionConfig
}

export function useTableColumns({ onEdit, onDelete, onToggle, permission }: UseTableColumnsOptions) {
  const columns: TableColumnType<Todo>[] = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '标题', dataIndex: 'title' },
    {
      title: '状态',
      dataIndex: 'done',
      width: 140,
      customRender: ({ record }) => (
        <Switch
          checked={record.done}
          checkedChildren="完成"
          unCheckedChildren="未完"
          disabled={!permission.canEdit}
          onChange={() => onToggle(record as Todo)}
        />
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 160,
      customRender: ({ record }) => (
        <>
          {permission.canEdit && (
            <Button type="link" size="small" onClick={() => onEdit(record as Todo)}>
              编辑
            </Button>
          )}
          {permission.canDelete && (
            <Popconfirm title="确认删除这条待办？" onConfirm={() => onDelete(record as Todo)}>
              <Button type="link" size="small" danger>
                删除
              </Button>
            </Popconfirm>
          )}
        </>
      ),
    },
  ]

  return columns
}
