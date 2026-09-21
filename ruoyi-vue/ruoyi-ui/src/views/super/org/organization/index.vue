<template>
  <div class="s-page">
    <header class="s-head">
      <div>
        <span class="eyebrow">SUPER ADMIN · ORGANIZATION</span>
        <h1>组织与岗位管理</h1>
        <p><b>部门、岗位、绑定关系都在这一页</b>：「组织总览」看对应关系与人数、并维护绑定；「部门管理 / 岗位管理」页签可直接新增与修改。<b>一个部门可以绑多个岗位，一个岗位只能属于一个部门。</b></p>
      </div>
      <div class="s-head-actions">
        <el-button size="small" type="primary" icon="el-icon-plus" @click="openBind">新增绑定</el-button>
        <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="loadAll">刷新</el-button>
      </div>
    </header>

    <div class="s-tabs">
      <span :class="{ on: tab === 'overview' }" @click="tab = 'overview'">组织总览</span>
      <span :class="{ on: tab === 'dept' }" @click="tab = 'dept'">部门管理</span>
      <span :class="{ on: tab === 'position' }" @click="tab = 'position'">岗位管理</span>
    </div>

    <div class="s-grid" v-show="tab === 'overview'">
      <section class="s-card s-c8">
        <div class="s-card-h">
          <div class="tt"><span class="s-idx">组</span><h3>部门 · 绑定岗位 · 在培人数</h3></div>
          <span class="hint">真实数据：sys_dept + position + dept_position + sys_user + register_application</span>
        </div>
        <div v-loading="loading">
          <table v-if="rows.length" class="s-tbl">
            <thead>
              <tr><th>部门</th><th style="width:150px">绑定岗位</th><th style="width:100px">在培人数</th><th style="width:90px">待审核</th><th style="width:90px">状态</th><th style="width:90px">操作</th></tr>
            </thead>
            <tbody>
              <tr v-for="r in rows" :key="r.key">
                <td class="nm">{{ r.deptName }}</td>
                <td>{{ r.positionName || '未绑定' }}</td>
                <td class="num">{{ r.training === null ? '—' : r.training }}</td>
                <td>{{ r.waiting }}</td>
                <td>
                  <span class="s-badge" :class="r.bound ? 'ok' : 'warn'" :title="'绑定来源：' + r.sourceLabel">{{ r.bound ? '已绑定' : '待配置' }}</span>
                </td>
                <td>
                  <el-button v-if="r.bindingId" type="text" size="mini" class="danger-text" @click="unbind(r)">解绑</el-button>
                  <span v-else class="hint">—</span>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-else class="s-empty"><i class="el-icon-office-building" /><span>暂无部门数据</span></div>
        </div>
        <p class="s-note">
          部门取 <code>sys_dept</code> 中公司根节点（<code>parentId = 0</code>）下的 5 个培养部门。
          <b>绑定岗位 · 在培人数 · 待审核</b> 均取自接口 <code>/business/position/dept-bindings</code>：
          绑定以权威表 <code>dept_position</code> 为准（后端课程可见性用的就是它），两个人数由该接口用 SQL 直接计算
          （在培 = <code>user_status</code> 处于预备 / 正式 / 待转正；待审核 = <code>register_application.status</code> 为 WAIT_AUDIT）。
          取不到时退回报名记录，此时「在培人数」显示 <b>—</b> 而非 0。悬停状态徽标可看本条实际来源。
        </p>
      </section>

      <section class="s-card s-c4">
        <div class="s-card-h">
          <div class="tt"><span class="s-idx g">岗</span><h3>岗位清单</h3></div>
          <span class="hint">{{ positions.length }} 个</span>
        </div>
        <table v-if="positions.length" class="s-tbl">
          <thead><tr><th>岗位</th><th style="width:86px">编码</th></tr></thead>
          <tbody>
            <tr v-for="p in positions" :key="p.id">
              <td class="nm">{{ p.positionName }}</td>
              <td>{{ p.positionCode }}</td>
            </tr>
          </tbody>
        </table>
        <div v-else class="s-empty"><i class="el-icon-s-custom" /><span>暂无岗位</span></div>
        <div class="s-callout" style="margin-top:12px">
          <i class="el-icon-info" />
          <span>岗位决定<b>课程可见性</b>与<b>考核配置范围</b>：课程 / 题库 / 考核都按 <code>position_id</code> 归属。实习生注册时<b>只选岗位</b>，系统由该岗位<b>反查</b>所属部门（<code>selectDeptIdByPositionId</code>）—— 这正是「一个岗位只能属于一个部门」的原因。</span>
        </div>
      </section>

      <section class="s-card s-c12">
        <div class="s-card-h">
          <div class="tt"><span class="s-idx p">规</span><h3>绑定规则与边界</h3></div>
        </div>
        <div class="s-grid">
          <div class="s-c6">
            <div class="s-steps">
              <div class="s-step done"><span class="mark">✓</span><div class="txt"><b>一个部门可以绑定多个岗位</b><span>由 <code>dept_position</code> 维护 —— 如「开发部门」可同时挂「开发实习生」与「初级开发实习生」</span></div></div>
              <div class="s-step done"><span class="mark">✓</span><div class="txt"><b>一个岗位只能属于一个部门</b><span>唯一索引 <code>uk_position</code> 兜底 —— 注册只选岗位、由岗位反查部门，跨部门会让归属不确定</span></div></div>
              <div class="s-step done"><span class="mark">✓</span><div class="txt"><b>岗位决定培养内容范围</b><span>课程、题库、模拟与正式考核均按岗位归属</span></div></div>
            </div>
          </div>
          <div class="s-c6">
            <div class="s-callout ok">
              <i class="el-icon-success" />
              <span><b>一个部门可以绑定多个岗位</b>（如「开发部门」可同时绑「开发实习生」与「初级开发实习生」）；但<b>同一个岗位只能属于一个部门</b> —— 注册流程只选岗位、由岗位反查部门，岗位跨部门会让归属不确定。数据库对 <code>position_id</code> 加了唯一索引兜底。</span>
              <span>部门与岗位本身的<b>新增 / 改名 / 停用</b>已整合到本页：切到上方「<b>部门管理</b>」「<b>岗位管理</b>」页签即可维护，无需再去「系统管理」；本卡只讲两者的<b>绑定规则</b>。</span>
            </div>
          </div>
        </div>
      </section>
    </div>

    <!-- ② 部门管理（RuoYi 原生 sys_dept） -->
    <div v-show="tab === 'dept'">
      <section class="s-card">
        <div class="s-card-h">
          <div class="tt"><span class="s-idx g">部</span><h3>部门管理</h3></div>
          <el-button size="mini" type="primary" icon="el-icon-plus" @click="openDept()">新增部门</el-button>
        </div>
        <table v-if="depts.length" class="s-tbl">
          <thead>
            <tr><th style="width:90px">ID</th><th>部门名称</th><th style="width:130px">上级部门</th><th style="width:80px">排序</th><th style="width:86px">状态</th><th style="width:120px">操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="d in depts" :key="d.deptId">
              <td>{{ d.deptId }}</td>
              <td class="nm">{{ d.deptName }}</td>
              <td>{{ deptNameOf(d.parentId) }}</td>
              <td>{{ d.orderNum }}</td>
              <td><span class="s-badge" :class="d.status === '0' ? 'ok' : 'warn'">{{ d.status === '0' ? '正常' : '停用' }}</span></td>
              <td>
                <el-button type="text" size="mini" @click="openDept(d)">修改</el-button>
                <el-button type="text" size="mini" class="danger-text" @click="removeDept(d)">删除</el-button>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-else class="s-empty"><i class="el-icon-office-building" /><span>暂无部门</span></div>
        <p class="s-note">
          部门取自 <code>sys_dept</code>（RuoYi 原生表）—— 与「系统管理 › 部门管理」是<b>同一份数据</b>，改哪边都一样、本页改完即时生效。
          删除前请确认该部门下没有账号、也没有绑定岗位 —— 有的话后端会拒绝。
          <b>新建部门后记得点右上角「新增绑定」给它配上岗位</b> —— 注册页没有部门字段，申请人选的是<b>岗位</b>；
          没有岗位归属的部门，等于没人能注册进来。
        </p>
      </section>
    </div>

    <!-- ③ 岗位管理（业务表 position，不是 RuoYi 的 sys_post） -->
    <div v-show="tab === 'position'">
      <section class="s-card">
        <div class="s-card-h">
          <div class="tt"><span class="s-idx o">岗</span><h3>岗位管理</h3></div>
          <el-button size="mini" type="primary" icon="el-icon-plus" @click="openPos()">新增岗位</el-button>
        </div>
        <table v-if="positions.length" class="s-tbl">
          <thead>
            <tr><th style="width:70px">ID</th><th style="width:180px">岗位编码</th><th>岗位名称</th><th style="width:80px">排序</th><th style="width:86px">状态</th><th style="width:120px">操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="p in positions" :key="p.id">
              <td>{{ p.id }}</td>
              <td>{{ p.positionCode }}</td>
              <td class="nm">{{ p.positionName }}</td>
              <td>{{ p.sortNo }}</td>
              <td><span class="s-badge" :class="p.status === 1 ? 'ok' : 'warn'">{{ p.status === 1 ? '启用' : '停用' }}</span></td>
              <td>
                <el-button type="text" size="mini" @click="openPos(p)">修改</el-button>
                <el-button type="text" size="mini" class="danger-text" @click="removePos(p)">删除</el-button>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-else class="s-empty"><i class="el-icon-collection-tag" /><span>暂无岗位</span></div>
        <p class="s-note">
          岗位取自业务表 <code>position</code>（<b>不是</b> RuoYi 自带的 <code>sys_post</code>，两者互不影响）。
          新增岗位后<b>务必先绑定部门</b>（点右上角「新增绑定」，在任何页签都能点）—— 未绑定的岗位<b>仍会出现在注册页的岗位下拉里</b>
          （注册接口按 <code>LEFT JOIN</code> 取岗位），申请人要提交时才被拦下并提示「所选岗位尚未配置归属部门」。
          绑定完成后，实习生注册与课程可见性才会真正用到这个岗位。
        </p>
      </section>
    </div>
      <!-- 新增「部门 ↔ 岗位」绑定（超管） -->
    <el-dialog title="新增「部门 ↔ 岗位」绑定" :visible.sync="bindVisible" width="480px" append-to-body>
      <el-form label-width="70px">
        <el-form-item label="部门">
          <el-select v-model="bindForm.deptId" placeholder="请选择部门" style="width:100%">
            <el-option v-for="d in trainDepts" :key="d.deptId" :label="d.deptName" :value="Number(d.deptId)" />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位">
          <el-select v-model="bindForm.positionId" placeholder="请选择岗位" style="width:100%">
            <el-option v-for="p in positions" :key="p.id" :label="p.positionName" :value="Number(p.id)" />
          </el-select>
        </el-form-item>
      </el-form>
      <el-alert type="info" :closable="false" show-icon
                title="一个部门可以绑定多个岗位；但同一个岗位只能属于一个部门（否则注册时无法自动判断归属部门）。若本部门需要同类岗位，请先切到「岗位管理」页签新建一个，再回到这里绑定。" />
      <span slot="footer">
        <el-button @click="bindVisible = false">取消</el-button>
        <el-button type="primary" :loading="bindSubmitting" @click="submitBind">确定</el-button>
      </span>
    </el-dialog>

    <!-- 部门 新增/修改 -->
    <el-dialog :title="deptForm.id ? '修改部门' : '新增部门'" :visible.sync="deptVisible" width="520px" append-to-body>
      <el-form ref="deptForm" :model="deptForm" :rules="deptRules" label-width="90px">
        <el-form-item label="上级部门" prop="parentId">
          <el-select v-model="deptForm.parentId" placeholder="请选择上级部门" style="width:100%">
            <el-option v-for="d in depts" :key="d.deptId" :label="d.deptName" :value="Number(d.deptId)"
                       :disabled="Number(d.deptId) === Number(deptForm.id)" />
          </el-select>
        </el-form-item>
        <el-form-item label="部门名称" prop="deptName"><el-input v-model="deptForm.deptName" placeholder="如「测试部门」" maxlength="30" /></el-form-item>
        <el-form-item label="显示排序" prop="orderNum"><el-input-number v-model="deptForm.orderNum" :min="0" controls-position="right" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="deptForm.status">
            <el-radio label="0">正常</el-radio><el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="deptVisible = false">取消</el-button>
        <el-button type="primary" :loading="deptSubmitting" @click="submitDept">确定</el-button>
      </span>
    </el-dialog>

    <!-- 岗位 新增/修改 -->
    <el-dialog :title="posForm.id ? '修改岗位' : '新增岗位'" :visible.sync="posVisible" width="520px" append-to-body>
      <el-form ref="posForm" :model="posForm" :rules="posRules" label-width="90px">
        <el-form-item label="岗位编码" prop="positionCode">
          <el-input v-model="posForm.positionCode" placeholder="英文大写，如 JUNIOR_DEV" maxlength="32" />
        </el-form-item>
        <el-form-item label="岗位名称" prop="positionName">
          <el-input v-model="posForm.positionName" placeholder="如「初级开发实习生」" maxlength="32" />
        </el-form-item>
        <el-form-item label="显示排序" prop="sortNo"><el-input-number v-model="posForm.sortNo" :min="0" controls-position="right" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="posForm.status">
            <el-radio :label="1">启用</el-radio><el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-alert type="warning" :closable="false" show-icon
                  title="同一岗位只能绑定一个部门；若本部门需要同类方向，请另建一个岗位（如「初级开发实习生」）。" />
      </el-form>
      <span slot="footer">
        <el-button @click="posVisible = false">取消</el-button>
        <el-button type="primary" :loading="posSubmitting" @click="submitPos">确定</el-button>
      </span>
    </el-dialog>
</div>



</template>

<script>
import { listDept, addDept, updateDept, delDept } from '@/api/system/dept'
import {
  listPosition, addPosition, updatePosition, delPosition,
  getDeptBindings, addDeptBinding, removeDeptBinding
} from '@/api/business/position'
import { listRegister } from '@/api/business/register'

/** 绑定来源的可读名（用于状态徽标的 title 提示） */
const SOURCE_LABEL = {
  dept_position: 'dept_position（配置表 · 权威，人数由后端 SQL 计算）',
  register: 'register_application（报名记录 · 仅退路）',
  none: '无来源'
}

/**
 * 超管「组织岗位」页（只读核对）
 *
 * 真实数据：/system/dept/list、/business/position/list、
 *          /business/position/dept-bindings（部门↔岗位绑定 + 在培/待审核人数）、
 *          /business/register/list（退路）
 *
 * 推导规则（2026-09-17 三轮修正，每轮都踩了一个坑）：
 *   ① 绑定岗位：以权威表 `dept_position` 为准（接口 dept-bindings），拿不到时退回报名记录。
 *      —— 旧实现只看报名记录，遇到「账号直接创建、未走注册审核」的部门会显示未绑定（建模 107）。
 *   ② 在培人数 / 待审核：**由后端 SQL 直接算**。
 *      —— 曾试图用 `/system/user/list` 在前端数，但该接口虽然返回 `userStatus` 字段、值却是 null
 *         （RuoYi 的 selectUserList 没 select 这个业务扩展列，也不返回 positionId），结果五行全是 0。
 *   ③ 取不到时显示 `—`，**不伪装成 0**（沿用项目既有口径）。
 */
export default {
  name: 'SuperOrganization',
  data() {
    return {
      loading: false,
      depts: [],
      positions: [],
      interns: [],
      bindings: [],
      /* 新增绑定 */
      bindVisible: false,
      bindSubmitting: false,
      bindForm: { deptId: null, positionId: null },
      /* 页签：组织总览 / 部门管理 / 岗位管理（整合，2026-09-20） */
      tab: 'overview',
      /* 部门 新增/修改 */
      deptVisible: false,
      deptSubmitting: false,
      deptForm: {},
      deptRules: {
        parentId: [{ required: true, message: '请选择上级部门', trigger: 'change' }],
        deptName: [{ required: true, message: '部门名称不能为空', trigger: 'blur' }],
        orderNum: [{ required: true, message: '显示排序不能为空', trigger: 'blur' }]
      },
      /* 岗位 新增/修改 */
      posVisible: false,
      posSubmitting: false,
      posForm: {},
      posRules: {
        positionCode: [{ required: true, message: '岗位编码不能为空', trigger: 'blur' }],
        positionName: [{ required: true, message: '岗位名称不能为空', trigger: 'blur' }],
        sortNo: [{ required: true, message: '显示排序不能为空', trigger: 'blur' }]
      }
    }
  },
  computed: {
    /** 公司根节点：sys_dept 里 parentId=0 的那一行（当前为「融谷」） */
    companyRootId() {
      const root = this.depts.find(d => Number(d.parentId) === 0)
      return root ? Number(root.deptId) : 100
    },
    /** 培养部门 = 公司根节点的直属子部门（交付 / 开发 / 设计 / 质检 / 建模） */
    trainDepts() {
      const root = this.companyRootId
      return this.depts
        .filter(d => Number(d.parentId) === root && Number(d.deptId) !== root)
        .sort((a, b) => a.deptId - b.deptId)
    },
    /**
     * 一行 = 一个「部门 · 岗位」绑定（**支持一部门多岗位**，2026-09-20）。
     *
     * <p>原来是「一行一个部门、只取第一个绑定」—— 一部门多岗位后那样会丢掉多余的岗位。
     * 没有绑定的部门仍出一行、岗位列显示「未绑定」，便于一眼发现漏配。</p>
     */
    rows() {
      const out = []
      this.trainDepts.forEach(d => {
        const deptId = Number(d.deptId)
        const mine = this.interns.filter(r => Number(r.deptId) === deptId)
        const bound = this.bindings.filter(b => Number(b.deptId) === deptId)
        if (bound.length) {
          bound.forEach(b => {
            out.push({
              key: deptId + '-' + b.positionId,
              bindingId: b.id,
              deptId: deptId,
              deptName: d.deptName,
              positionId: b.positionId,
              positionName: b.positionName,
              sourceLabel: SOURCE_LABEL.dept_position,
              // 在培 / 待审核 是**部门级**指标（后端按 dept_id 算），
              // 一个部门绑多个岗位时，这几行会显示同一个数（见表格下方的说明）
              training: Number(b.trainingCount),
              waiting: Number(b.waitAuditCount),
              bound: true
            })
          })
        } else {
          out.push({
            key: deptId + '-none',
            bindingId: null,
            deptId: deptId,
            deptName: d.deptName,
            positionId: null,
            positionName: '',
            sourceLabel: mine.length ? SOURCE_LABEL.register : SOURCE_LABEL.none,
            training: null,
            waiting: mine.filter(r => r.status === 'WAIT_AUDIT').length,
            bound: false
          })
        }
      })
      return out
    }
  },
  created() {
    // 页签可从 URL 指定（?tab=dept|position|overview）—— 便于分享 / 刷新后回到原页签
    const t = this.$route.query.tab
    if (t === 'dept' || t === 'position' || t === 'overview') {
      this.tab = t
    }
    this.loadAll()
  },
  methods: {
    loadAll() {
      this.loading = true
      Promise.all([
        listDept({ status: '0' }).then(res => { this.depts = res.data || [] }).catch(() => { this.depts = [] }),
        listPosition({ pageNum: 1, pageSize: 100 }).then(res => { this.positions = res.rows || [] }).catch(() => { this.positions = [] }),
        getDeptBindings().then(res => { this.bindings = res.data || [] }).catch(() => { this.bindings = [] }),
        listRegister({ pageNum: 1, pageSize: 500 }).then(res => { this.interns = res.rows || [] }).catch(() => { this.interns = [] })
      ]).finally(() => { this.loading = false })
    },

    /* ---------------- 部门 ↔ 岗位 绑定维护（超管专属） ---------------- */

    openBind() {
      this.bindForm = { deptId: null, positionId: null }
      this.bindVisible = true
    },
    submitBind() {
      const f = this.bindForm
      if (!f.deptId || !f.positionId) {
        this.$message.warning('请选择部门与岗位')
        return
      }
      const existed = this.bindings.some(b =>
        Number(b.deptId) === Number(f.deptId) && Number(b.positionId) === Number(f.positionId))
      if (existed) {
        this.$message.warning('该部门已绑定此岗位')
        return
      }
      this.bindSubmitting = true
      addDeptBinding({ deptId: f.deptId, positionId: f.positionId }).then(() => {
        this.$message.success('绑定成功')
        this.bindVisible = false
        this.loadAll()
      }).catch(err => {
        this.$message.error((err && err.message) ? err.message : '绑定失败')
      }).finally(() => { this.bindSubmitting = false })
    },
    unbind(row) {
      this.$confirm('解绑后该部门不再包含「' + row.positionName + '」岗位：按岗位归属的课程可见性会相应变化。确定解绑？',
        '解绑 · ' + row.deptName + ' / ' + row.positionName, { type: 'warning' }).then(() => {
        return removeDeptBinding(row.bindingId).then(res => {
          this.$message.success(res.msg || '已解绑')
          this.loadAll()
        })
      }).catch(err => {
        // 'cancel' 是用户点了取消；其余是后端拒绝（如「最后一个岗位不能解绑」）
        if (err && err !== 'cancel' && err !== 'close') {
          this.$message.error((err.message) ? err.message : '解绑失败')
        }
      })
    },

    /* ---------------- 部门管理（sys_dept，与原生「部门管理」同一份数据） ---------------- */

    deptNameOf(parentId) {
      if (!parentId || Number(parentId) === 0) {
        return '—（根节点）'
      }
      const d = this.depts.find(x => Number(x.deptId) === Number(parentId))
      return d ? d.deptName : parentId
    },
    openDept(row) {
      this.deptForm = row
        ? { id: row.deptId, parentId: Number(row.parentId), deptName: row.deptName, orderNum: row.orderNum, status: row.status }
        : { id: null, parentId: Number(this.companyRootId), deptName: '', orderNum: 99, status: '0' }
      this.deptVisible = true
    },
    submitDept() {
      this.$refs.deptForm.validate(valid => {
        if (!valid) return
        const f = this.deptForm
        const body = { deptId: f.id, parentId: f.parentId, deptName: f.deptName, orderNum: f.orderNum, status: f.status }
        this.deptSubmitting = true
        const req = f.id ? updateDept(body) : addDept(body)
        req.then(res => {
          this.$message.success(res.msg || '保存成功')
          this.deptVisible = false
          this.loadAll()
        }).catch(err => {
          this.$message.error((err && err.message) ? err.message : '保存失败')
        }).finally(() => { this.deptSubmitting = false })
      })
    },
    removeDept(row) {
      this.$confirm('删除部门「' + row.deptName + '」？若该部门下还有账号或绑定岗位，后端会拒绝。', '提示', { type: 'warning' })
        .then(() => delDept(row.deptId).then(res => {
          this.$message.success(res.msg || '已删除')
          this.loadAll()
        })).catch(err => {
        if (err && err !== 'cancel' && err !== 'close') {
          this.$message.error((err.message) ? err.message : '删除失败')
        }
      })
    },

    /* ---------------- 岗位管理（业务表 position） ---------------- */

    openPos(row) {
      this.posForm = row
        ? { id: row.id, positionCode: row.positionCode, positionName: row.positionName, sortNo: row.sortNo, status: row.status }
        : { id: null, positionCode: '', positionName: '', sortNo: 99, status: 1 }
      this.posVisible = true
    },
    submitPos() {
      this.$refs.posForm.validate(valid => {
        if (!valid) return
        const f = this.posForm
        this.posSubmitting = true
        const req = f.id ? updatePosition(f) : addPosition(f)
        req.then(res => {
          this.$message.success(res.msg || '保存成功')
          this.posVisible = false
          this.loadAll()
        }).catch(err => {
          this.$message.error((err && err.message) ? err.message : '保存失败')
        }).finally(() => { this.posSubmitting = false })
      })
    },
    removePos(row) {
      this.$confirm('删除岗位「' + row.positionName + '」？已绑定到部门的岗位请先在「组织总览」解绑。', '提示', { type: 'warning' })
        .then(() => delPosition(row.id).then(res => {
          this.$message.success(res.msg || '已删除')
          this.loadAll()
        })).catch(err => {
        if (err && err !== 'cancel' && err !== 'close') {
          this.$message.error((err.message) ? err.message : '删除失败')
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
@import '~@/assets/styles/super-module.scss';

/* 页签（2026-09-20 把部门/岗位管理整合进本页）—— 共享基线里没有 tabs，这里自包含。
   注意：@import 进 scoped 时不能用 :root，令牌一律走 SCSS 变量（本项目既有铁律）。 */
.s-tabs {
  display: flex; gap: 6px; margin-bottom: 14px;
  span {
    height: 32px; line-height: 32px; padding: 0 16px; border-radius: 8px;
    background: #fff; border: 1px solid #e4e9f0; color: #667085;
    font-size: 13px; cursor: pointer; user-select: none;
    &:hover { color: #1764f5; }
    &.on { background: #1764f5; border-color: #1764f5; color: #fff; }
  }
}
.danger-text { color: #f04438; }
</style>
