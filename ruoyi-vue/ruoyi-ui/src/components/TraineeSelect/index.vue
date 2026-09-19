<template>
  <el-select
    :value="value"
    :size="size"
    :clearable="clearable"
    filterable
    :placeholder="placeholder"
    :style="{ width: width }"
    @input="onInput"
    @change="$emit('change', $event)"
  >
    <el-option
      v-for="u in trainees"
      :key="u.userId"
      :label="label(u)"
      :value="Number(u.userId)"
    />
  </el-select>
</template>

<script>
/**
 * 「选一个实习生」下拉（部门在培人员）
 *
 * 抽成共享组件的理由：部门端的「任务管理 / 通知管理 / 任务批阅」都要按人筛选，
 * 而「怎么算本部门的在培实习生」这件事（部门范围 + user_status 三态 + 昵称兜底）
 * 只应该有一处实现 —— 各页各写一遍迟早分叉。
 *
 * ⚠️ 数据源是 `/business/task/trainees`（业务侧、按 token 的部门收范围），
 * **不是** `/system/user/list` —— 后者要 `system:user:list` 权限，部门管理员是 403，
 * 前端会静默拿到空列表（「指定人员」选择器一度就是空的）。
 */
import { listTrainees } from '@/api/business/task'
import { mapGetters } from 'vuex'

/** 在培（含待转正）都算「实习生」 */
const TRAINEE_STATUS = ['PRE_TRAINEE', 'FORMAL_TRAINEE', 'PENDING_PROMOTE']

export default {
  name: 'TraineeSelect',
  props: {
    value: { type: [Number, String], default: null },
    placeholder: { type: String, default: '按实习生筛选' },
    size: { type: String, default: 'small' },
    width: { type: String, default: '160px' },
    clearable: { type: Boolean, default: true }
  },
  data() {
    return { users: [] }
  },
  computed: {
    ...mapGetters(['deptId']),
    trainees() {
      const dept = Number(this.deptId)
      return this.users.filter(u =>
        TRAINEE_STATUS.indexOf(String(u.userStatus)) > -1 &&
        (!dept || Number(u.deptId) === dept)
      )
    }
  },
  created() {
    this.reload()
  },
  methods: {
    reload() {
      listTrainees().then(res => {
        this.users = res.data || []
        // 抛给父组件：父组件常用它把 userId 反解成姓名（如筛选提示里显示「只看 张三 的任务」）
        this.$emit('loaded', this.trainees)
      }).catch(() => { this.users = [] })
    },
    label(u) {
      const name = u.nickName || u.userName
      return name + (String(u.userStatus) === 'FORMAL_TRAINEE' ? '（正式）' : '（预备）')
    },
    onInput(v) {
      this.$emit('input', v)
    }
  }
}
</script>
