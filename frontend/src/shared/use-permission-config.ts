/**
 * 按钮权限配置。
 * 真实项目里这里通常读当前用户角色或接口下发的权限点；
 * 本示例没有登录体系，静态全开，演示「视图层按权限点控制按钮显隐」的写法。
 */
export interface PermissionConfig {
  canAdd: boolean
  canEdit: boolean
  canDelete: boolean
}

export function usePermissionConfig(): PermissionConfig {
  return {
    canAdd: true,
    canEdit: true,
    canDelete: true,
  }
}
