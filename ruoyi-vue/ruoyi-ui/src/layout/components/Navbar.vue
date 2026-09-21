<template>
  <div class="navbar">
    <hamburger id="hamburger-container" :is-active="sidebar.opened" class="hamburger-container" @toggleClick="toggleSideBar" />

    <breadcrumb id="breadcrumb-container" class="breadcrumb-container" v-if="!topNav"/>
    <top-nav id="topmenu-container" class="topmenu-container" v-if="topNav"/>

    <div class="right-menu">
      <template v-if="device !== 'mobile'">
        <el-tooltip content="全屏" effect="dark" placement="bottom">
          <screenfull id="screenfull" class="right-menu-item hover-effect" />
        </el-tooltip>
      </template>

      <!-- 消息中心：所有角色都可见（未读数来自送达谓词，超管看督办回执、部门管理员看催办/待批） -->
      <el-tooltip :content="unread ? ('消息中心 · ' + unread + ' 条未读') : '消息中心'" effect="dark" placement="bottom">
        <router-link to="/messages" class="right-menu-item hover-effect bell-item">
          <i class="el-icon-bell" />
          <i v-if="unread" class="bell-dot" :class="{ large: unread > 9 }">{{ unread > 99 ? '99+' : unread }}</i>
        </router-link>
      </el-tooltip>

      <!-- 用户信息入口：头像（无头像时用姓名首字色块）+ 姓名 + 角色·部门 -->
      <el-dropdown class="user-entry" trigger="click" placement="bottom-end" @command="onCommand">
        <div class="user-pill hover-effect">
          <span class="up-avatar" :style="{ background: avatarBg }">
            <img v-if="hasAvatar" :src="avatar" alt="" />
            <em v-else>{{ initial }}</em>
          </span>
          <span v-if="device !== 'mobile'" class="up-meta">
            <b>{{ displayName }}</b>
            <small>{{ roleLabel }}<template v-if="deptName"> · {{ deptName }}</template></small>
          </span>
          <i v-if="device !== 'mobile'" class="el-icon-arrow-down up-caret" />
        </div>

        <el-dropdown-menu slot="dropdown" class="user-menu">
          <el-dropdown-item command="profile">
            <i class="el-icon-user" /> 个人中心
          </el-dropdown-item>
          <el-dropdown-item command="password">
            <i class="el-icon-lock" /> 修改密码
          </el-dropdown-item>
          <el-dropdown-item command="messages">
            <i class="el-icon-bell" /> 消息中心<span v-if="unread">（{{ unread }}）</span>
          </el-dropdown-item>
          <el-dropdown-item command="logout" divided>
            <i class="el-icon-switch-button" /> 退出登录
          </el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import Breadcrumb from '@/components/Breadcrumb'
import TopNav from '@/components/TopNav'
import Hamburger from '@/components/Hamburger'
import Screenfull from '@/components/Screenfull'
import { getUnreadCount } from '@/api/business/message'

/** RuoYi 的默认头像图；等于它说明用户没设过头像 → 改用姓名首字色块 */
const DEFAULT_AVATAR = require('@/assets/images/profile.jpg')
/** 首字头像的取色盘（取自系统状态色，浅色背景下对比度足够） */
const AVATAR_COLORS = ['#1764f5', '#12b76a', '#7a5af8', '#f79009', '#0e7490']
/** 角色 → 中文标签 */
const ROLE_LABELS = {
  SUPER_ADMIN: '超级管理员',
  admin: '超级管理员',
  DEPT_ADMIN: '部门管理员',
  PRE_TRAINEE: '预备实习生',
  FORMAL_TRAINEE: '正式实习生'
}

/**
 * 顶栏组件
 *
 * 2026-09-17 按本系统风格精简：
 *  - **去掉**「搜索（搜菜单）」「布局大小」「布局设置抽屉入口」—— 都是若依演示功能，与业务无关
 *  - **保留** 全屏；实习生保留铃铛
 *  - 头像区由「40×40 图片 + 箭头」改为**信息胶囊**：首字色块头像（未设头像时）+ 姓名 + 角色·部门
 *  - 下拉改为 个人中心 / 修改密码 /（实习生）消息中心 / 退出登录
 */
export default {
  components: {
    Breadcrumb,
    TopNav,
    Hamburger,
    Screenfull
  },
  data() {
    return {
      /** 未读消息数（来自 /business/message/unread-count，与消息列表共用同一份送达谓词） */
      unread: 0,
      unreadTimer: null
    }
  },
  watch: {
    // 路由变化即刷新：用户在消息中心读完，切走时红点立刻消失
    $route() {
      this.loadUnread()
    }
  },
  created() {
    this.loadUnread()
    // 60s 轮询；页面隐藏时跳过（省后端、避免后台标签狂刷）
    this.unreadTimer = setInterval(this.loadUnread, 60000)
  },
  beforeDestroy() {
    if (this.unreadTimer) {
      clearInterval(this.unreadTimer)
      this.unreadTimer = null
    }
  },
  computed: {
    ...mapGetters([
      'sidebar',
      'avatar',
      'name',
      'nickName',
      'device',
      'roles',
      'deptName'
    ]),
    isIntern() {
      const roles = this.roles || []
      return roles.indexOf('PRE_TRAINEE') > -1 || roles.indexOf('FORMAL_TRAINEE') > -1
    },
    displayName() {
      return this.nickName || this.name || '用户'
    },
    /** 未设置真实头像时用姓名首字 */
    hasAvatar() {
      return !!this.avatar && this.avatar !== DEFAULT_AVATAR
    },
    initial() {
      const n = (this.displayName || '用').trim()
      return n.charAt(0).toUpperCase()
    },
    /** 按姓名稳定取色，同一人每次都是同一个颜色 */
    avatarBg() {
      const n = this.displayName || ''
      let sum = 0
      for (let i = 0; i < n.length; i++) {
        sum += n.charCodeAt(i)
      }
      return AVATAR_COLORS[sum % AVATAR_COLORS.length]
    },
    roleLabel() {
      const roles = this.roles || []
      for (let i = 0; i < roles.length; i++) {
        if (ROLE_LABELS[roles[i]]) {
          return ROLE_LABELS[roles[i]]
        }
      }
      return '用户'
    },
    topNav: {
      get() {
        return this.$store.state.settings.topNav
      }
    }
  },
  methods: {
    toggleSideBar() {
      this.$store.dispatch('app/toggleSideBar')
    },
    /** 拉未读数；页面隐藏时跳过（失败静默，不打扰用户） */
    loadUnread() {
      if (typeof document !== 'undefined' && document.hidden) {
        return
      }
      getUnreadCount().then(res => {
        const d = res.data || {}
        this.unread = Number(d.total || 0)
      }).catch(() => {})
    },
    onCommand(command) {
      if (command === 'profile') {
        this.$router.push('/user/profile')
      } else if (command === 'password') {
        // 直达个人中心的「修改密码」页签
        this.$router.push({ path: '/user/profile', query: { tab: 'resetPwd' } })
      } else if (command === 'messages') {
        this.$router.push('/messages')
      } else if (command === 'logout') {
        this.logout()
      }
    },
    async logout() {
      this.$confirm('确定注销并退出系统吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('LogOut').then(() => {
          // 用根路径而非 /index：登出后由路由守卫按角色决定落点
          // （部门管理员 → 部门工作台；超管/实习生 → 通用工作台）
          location.href = '/'
        })
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.navbar {
  height: 50px;
  overflow: hidden;
  position: relative;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, .06);

  .hamburger-container {
    line-height: 46px;
    height: 100%;
    float: left;
    cursor: pointer;
    transition: background .3s;
    -webkit-tap-highlight-color: transparent;

    &:hover {
      background: rgba(0, 0, 0, .025);
    }
  }

  .breadcrumb-container {
    float: left;
  }

  .topmenu-container {
    position: absolute;
    left: 50px;
  }

  .right-menu {
    float: right;
    height: 100%;
    display: flex;
    align-items: center;
    padding-right: 16px;

    &:focus {
      outline: none;
    }

    .right-menu-item {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 34px;
      height: 34px;
      margin-right: 4px;
      border-radius: 8px;
      font-size: 17px;
      color: #5f6b7a;
      transition: background .3s, color .3s;

      &.hover-effect {
        cursor: pointer;

        &:hover {
          background: #f2f5f9;
          color: #1764f5;
        }
      }
    }

    /* 铃铛未读红点：0 条不显示；>=100 折成 99+ */
    .bell-item {
      position: relative;

      .bell-dot {
        position: absolute;
        top: 1px;
        right: 0;
        min-width: 16px;
        height: 16px;
        padding: 0 4px;
        border-radius: 8px;
        background: #f04438;
        color: #fff;
        font-size: 10px;
        font-style: normal;
        line-height: 16px;
        text-align: center;
        box-shadow: 0 0 0 2px #fff;

        &.large { padding: 0 5px; }
      }
    }

    /* ---- 用户信息胶囊 ---- */
    .user-entry {
      margin-left: 6px;
      line-height: normal;
    }

    .user-pill {
      display: flex;
      align-items: center;
      gap: 9px;
      height: 38px;
      padding: 0 10px 0 6px;
      border: 1px solid #e4e9f0;
      border-radius: 10px;
      background: #fff;
      transition: border-color .3s, background .3s;

      &.hover-effect {
        cursor: pointer;
      }

      &:hover {
        border-color: #bcd4fb;
        background: #f7faff;
      }

      .up-avatar {
        flex: none;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 28px;
        height: 28px;
        border-radius: 8px;
        overflow: hidden;
        color: #fff;
        font-size: 14px;
        font-weight: 500;
        letter-spacing: 0;

        img {
          width: 100%;
          height: 100%;
          object-fit: cover;
        }

        em {
          font-style: normal;
          line-height: 1;
        }
      }

      .up-meta {
        display: flex;
        flex-direction: column;
        justify-content: center;
        max-width: 168px;
        line-height: 1.25;

        b {
          color: #1d2939;
          font-size: 13px;
          font-weight: 500;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        small {
          color: #98a2b3;
          font-size: 11px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }

      .up-caret {
        flex: none;
        color: #98a2b3;
        font-size: 12px;
      }
    }
  }
}
</style>
