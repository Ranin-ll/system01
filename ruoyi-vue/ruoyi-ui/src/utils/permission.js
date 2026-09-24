import store from '@/store'

/**
 * 字符权限校验
 * @param {Array} value 校验值
 * @returns {Boolean}
 */
export function checkPermi(value) {
  if (value && value instanceof Array && value.length > 0) {
    const permissions = store.getters && store.getters.permissions
    const permissionDatas = value
    const all_permission = "*:*:*";

    const hasPermission = permissions.some(permission => {
      return all_permission === permission || permissionDatas.includes(permission)
    })

    return hasPermission;

  } else {
    console.error(`need roles! Like checkPermi="['system:user:add','system:user:edit']"`)
    return false
  }
}

/**
 * 角色权限校验
 * @param {Array} value 校验值
 * @returns {Boolean}
 */
export function checkRole(value) {
  if (value && value instanceof Array && value.length > 0) {
    const roles = store.getters && store.getters.roles
    const permissionRoles = value
    const super_admin = "admin";

    const hasRole = roles.some(role => {
      return super_admin === role || permissionRoles.includes(role)
    })

    return hasRole;

  } else {
    console.error(`need roles! Like checkRole="['admin','editor']"`)
    return false
  }
}

/**
 * 是否超级管理员。
 * ★ 必须同时认两种标识：内置 `admin`（role_key='admin'，后端一律按超管放行）
 *   与业务超管角色 `SUPER_ADMIN`。只判 SUPER_ADMIN 会让 admin 账号在页面上
 *   被当成部门管理员（页头文案 / 部门列 / 跳转前缀全错），与后端判权不一致。
 * @param {Array} roles 角色标识数组，缺省取 store 里的 roles
 * @returns {Boolean}
 */
export function isSuperAdminRole(roles) {
  const list = roles || (store.getters && store.getters.roles) || []
  return list.indexOf('SUPER_ADMIN') > -1 || list.indexOf('admin') > -1
}

/**
 * 是否部门管理员（本部门范围）
 * @param {Array} roles 角色标识数组，缺省取 store 里的 roles
 * @returns {Boolean}
 */
export function isDeptAdminRole(roles) {
  const list = roles || (store.getters && store.getters.roles) || []
  return list.indexOf('DEPT_ADMIN') > -1
}