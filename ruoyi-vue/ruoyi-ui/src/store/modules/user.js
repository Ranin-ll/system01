import { login, logout, getInfo } from '@/api/login'
import { getToken, setToken, removeToken } from '@/utils/auth'

const user = {
  state: {
    token: getToken(),
    id: '',
    name: '',
    nickName: '',
    avatar: '',
    roles: [],
    permissions: [],
    userStatus: '',
    protocolStatus: null,
    mentorName: '',
    mentorPhone: '',
    deptName: '',
    /** 当前用户部门ID（发通知选「本部门」范围时要用；部门管理员必填） */
    deptId: null
  },

  mutations: {
    SET_TOKEN: (state, token) => {
      state.token = token
    },
    SET_ID: (state, id) => {
      state.id = id
    },
    SET_NAME: (state, name) => {
      state.name = name
    },
    SET_NICK_NAME: (state, nickName) => {
      state.nickName = nickName
    },
    SET_AVATAR: (state, avatar) => {
      state.avatar = avatar
    },
    SET_ROLES: (state, roles) => {
      state.roles = roles
    },
    SET_PERMISSIONS: (state, permissions) => {
      state.permissions = permissions
    },
    SET_USER_STATUS: (state, userStatus) => {
      state.userStatus = userStatus
    },
    SET_PROTOCOL_STATUS: (state, protocolStatus) => {
      state.protocolStatus = protocolStatus
    },
    SET_MENTOR: (state, mentor) => {
      state.mentorName = mentor.name || ''
      state.mentorPhone = mentor.phone || ''
    },
    SET_DEPT_NAME: (state, deptName) => {
      state.deptName = deptName
    },
    SET_DEPT_ID: (state, deptId) => {
      state.deptId = deptId === undefined || deptId === null || deptId === '' ? null : deptId
    }
  },

  actions: {
    // 登录
    Login({ commit }, userInfo) {
      const username = userInfo.username.trim()
      const password = userInfo.password
      const code = userInfo.code
      const uuid = userInfo.uuid
      return new Promise((resolve, reject) => {
        login(username, password, code, uuid).then(res => {
          setToken(res.token)
          commit('SET_TOKEN', res.token)
          resolve()
        }).catch(error => {
          reject(error)
        })
      })
    },

    // 获取用户信息
    GetInfo({ commit, state }) {
      return new Promise((resolve, reject) => {
        getInfo().then(res => {
          const user = res.user
          const avatar = (user.avatar == "" || user.avatar == null) ? require("@/assets/images/profile.jpg") : process.env.VUE_APP_BASE_API + user.avatar;
          if (res.roles && res.roles.length > 0) { // 验证返回的roles是否是一个非空数组
            commit('SET_ROLES', res.roles)
            commit('SET_PERMISSIONS', res.permissions)
          } else {
            commit('SET_ROLES', ['ROLE_DEFAULT'])
          }
          commit('SET_ID', user.userId)
          commit('SET_NAME', user.userName)
          commit('SET_NICK_NAME', user.nickName || user.userName)
          commit('SET_AVATAR', avatar)
          commit('SET_USER_STATUS', user.userStatus || '')
          commit('SET_PROTOCOL_STATUS', Number(user.protocolStatus || 0))
          commit('SET_MENTOR', { name: user.mentorName, phone: user.mentorPhone })
          commit('SET_DEPT_NAME', user.dept && user.dept.deptName ? user.dept.deptName : '')
          commit('SET_DEPT_ID', user.deptId)
          resolve(res)
        }).catch(error => {
          reject(error)
        })
      })
    },

    // 退出系统
    LogOut({ commit, state }) {
      return new Promise((resolve, reject) => {
        logout(state.token).then(() => {
          commit('SET_TOKEN', '')
          commit('SET_ROLES', [])
          commit('SET_PERMISSIONS', [])
          commit('SET_USER_STATUS', '')
          commit('SET_PROTOCOL_STATUS', null)
          removeToken()
          resolve()
        }).catch(error => {
          reject(error)
        })
      })
    },

    // 前端 登出
    FedLogOut({ commit }) {
      return new Promise(resolve => {
        commit('SET_TOKEN', '')
        commit('SET_USER_STATUS', '')
        commit('SET_PROTOCOL_STATUS', null)
        removeToken()
        resolve()
      })
    }
  }
}

export default user
