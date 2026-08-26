/** 固定值集中放这里：下拉选项、字典等 */

/** 列表页「状态」下拉筛选项（本地过滤用） */
export interface StatusOption {
  label: string
  value: string
}

export const STATUS_OPTIONS: StatusOption[] = [
  { label: '全部', value: 'all' },
  { label: '未完成', value: 'undone' },
  { label: '已完成', value: 'done' },
]
