<template>
  <div class="register">
    <div class="auth-layout">
      <section class="auth-intro">
        <brand-logo />
        <span class="intro-kicker">START YOUR JOURNEY</span>
        <h1>建立你的实习培养档案</h1>
        <p>提交注册申请后，由部门管理员完成岗位与身份审核，审核通过即可进入培养流程。</p>
        <div class="register-steps"><span><b>01</b>填写资料</span><span><b>02</b>部门审核</span><span><b>03</b>开始学习</span></div>
      </section>
      <section class="auth-panel">
        <el-form ref="registerForm" :model="registerForm" :rules="registerRules" class="register-form">
          <div class="panel-heading"><span class="eyebrow">创建账号</span><h3 class="title">注册实习生账号</h3><p class="subtitle">提交后由部门管理员审核</p></div>
      <el-form-item prop="username"><el-input v-model="registerForm.username" placeholder="手机号（作为个人登录账号）" auto-complete="off" maxlength="11"><svg-icon slot="prefix" icon-class="phone" class="el-input__icon input-icon" /></el-input></el-form-item>
      <el-form-item prop="password"><el-input v-model="registerForm.password" type="password" show-password placeholder="登录密码" auto-complete="off"><svg-icon slot="prefix" icon-class="password" class="el-input__icon input-icon" /></el-input></el-form-item>
      <el-form-item prop="confirmPassword"><el-input v-model="registerForm.confirmPassword" type="password" show-password placeholder="确认密码" auto-complete="off" @keyup.enter.native="handleRegister"><svg-icon slot="prefix" icon-class="password" class="el-input__icon input-icon" /></el-input></el-form-item>
      <el-form-item prop="realName"><el-input v-model="registerForm.realName" placeholder="真实姓名" auto-complete="off"><svg-icon slot="prefix" icon-class="user" class="el-input__icon input-icon" /></el-input></el-form-item>
      <el-form-item prop="idCard"><el-input v-model="registerForm.idCard" placeholder="身份证号" auto-complete="off" @input="onIdCardInput"><svg-icon slot="prefix" icon-class="validCode" class="el-input__icon input-icon" /></el-input></el-form-item>
      <el-form-item prop="positionId"><el-select v-model="registerForm.positionId" placeholder="选择意向岗位" filterable class="full-width"><el-option v-for="position in positions" :key="position.id" :label="position.positionName" :value="position.id"><span>{{ position.positionName }}</span><small class="position-code">{{ position.positionCode }}</small></el-option></el-select></el-form-item>
      <div v-if="selectedPosition" class="dept-match"><span class="dept-match-label">自动匹配部门</span><strong>{{ selectedPosition.deptName || '待配置' }}</strong><span v-if="selectedPosition.deptName" class="dept-match-tip">提交后由该部门管理员审核</span></div>
      <el-form-item><el-date-picker v-model="registerForm.expectedEntryDate" type="date" value-format="yyyy-MM-dd" placeholder="预计入职日期（可选）" class="full-width" /></el-form-item>
      <el-form-item prop="code" v-if="captchaEnabled"><el-input v-model="registerForm.code" placeholder="验证码" style="width: 63%" /><div class="register-code"><img :src="codeUrl" @click="getCode" class="register-code-img" /></div></el-form-item>
      <el-form-item><el-button :loading="loading" type="primary" class="submit-button" @click.native.prevent="handleRegister">{{ loading ? '提交中...' : '提交注册申请' }}</el-button><div class="register-links"><el-link type="primary" :underline="false" @click="openStatusDialog()">查询申请进度</el-link><span class="link-split">|</span><router-link class="link-type" :to="'/login'">使用已有账号登录</router-link></div></el-form-item>
        </el-form>
      </section>
    </div>
    <div class="el-register-footer">实习生学习考核系统 · 本地开发环境</div>

    <!-- ★ 申请进度查询：由常驻区块改为弹窗；重复手机号提交时也会自动打开并给出结论 -->
    <el-dialog
      title="查询注册申请进度"
      :visible.sync="statusDialogVisible"
      width="470px"
      append-to-body
      custom-class="status-dialog"
      @closed="onStatusDialogClosed"
    >
      <p class="status-dialog-tip">输入注册申请时填写的手机号与密码，即可查看当前审核进度。无需登录。</p>
      <div v-if="statusDialogLead" class="status-dialog-lead">{{ statusDialogLead }}</div>

      <el-form class="status-dialog-form" @submit.native.prevent="queryRegisterStatus">
        <el-input
          v-model.trim="statusQueryPhone"
          maxlength="11"
          clearable
          placeholder="注册申请时填写的手机号"
          @keyup.enter.native="queryRegisterStatus"
        >
          <svg-icon slot="prefix" icon-class="phone" class="el-input__icon input-icon" />
        </el-input>
        <el-input
          v-model="statusQueryPassword"
          type="password"
          show-password
          maxlength="20"
          placeholder="注册申请时设置的密码"
          @keyup.enter.native="queryRegisterStatus"
        >
          <svg-icon slot="prefix" icon-class="password" class="el-input__icon input-icon" />
        </el-input>
        <el-button type="primary" class="status-dialog-submit" :loading="statusQuerying" @click="queryRegisterStatus">
          <i v-if="!statusQuerying" class="el-icon-search" /> 查询状态
        </el-button>
      </el-form>

      <div v-if="statusError" class="status-dialog-error">{{ statusError }}</div>

      <div v-if="statusResult" class="status-result">
        <div class="status-result-heading">
          <div>
            <strong>{{ statusResult.submitted ? '已提交注册申请' : '未找到注册申请' }}</strong>
            <span>{{ statusResult.submitted ? statusResult.applicationNo : '该手机号暂未提交申请' }}</span>
          </div>
          <i :class="statusResult.submitted ? 'el-icon-circle-check' : 'el-icon-info'" />
        </div>
        <div v-if="statusResult.submitted" class="status-conclusion" :class="conclusionTone">{{ conclusionText }}</div>
        <div class="status-steps">
          <div :class="statusStepClass('submit')">
            <span class="status-step-dot">1</span>
            <div><strong>{{ statusResult.submitted ? '已提交' : '未提交' }}</strong><small>申请资料</small></div>
          </div>
          <div :class="statusStepClass('review')">
            <span class="status-step-dot">2</span>
            <div><strong>{{ statusResult.submitted ? statusReviewLabel(statusResult.status) : '未审核' }}</strong><small>部门审核</small></div>
          </div>
          <div :class="statusStepClass('result')">
            <span class="status-step-dot">3</span>
            <div><strong>{{ statusResult.submitted ? statusResultLabel(statusResult.status) : '未处理' }}</strong><small>审核结果</small></div>
          </div>
        </div>
        <div v-if="statusResult.submitted" class="status-meta">
          <span v-if="statusResult.deptName">部门：{{ statusResult.deptName }}</span>
          <span v-if="statusResult.positionName">岗位：{{ statusResult.positionName }}</span>
          <span v-if="statusResult.createTime">提交时间：{{ formatStatusTime(statusResult.createTime) }}</span>
        </div>
        <el-alert v-if="statusResult.status === 'PASSED'" type="success" :closable="false" show-icon>
          审核已通过，可使用申请表填写的手机号和密码登录系统。
          <el-button type="text" class="status-login-link" @click="$router.push('/login')">去登录</el-button>
        </el-alert>
        <el-alert v-if="statusResult.status === 'REJECTED'" type="error" :closable="false" show-icon>
          <span>审核已驳回</span>
          <p class="reject-reason">驳回原因：{{ statusResult.rejectReason || '部门管理员未填写具体原因' }}</p>
        </el-alert>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getCodeImg, getRegisterPositions, register } from '@/api/login'
import { queryRegisterStatus } from '@/api/business/register'
import axios from 'axios'
import BrandLogo from '@/components/BrandLogo'

/**
 * 「该手机号已经提交过注册」的后端错误文案集合 —— 靠它判断要不要去探一次真实进度。
 * 后端按申请单状态走不同分支，文案并不统一：
 *   · 申请单存在且非驳回 → `注册账号已存在` / `该手机号已注册`
 *   · 上次申请被驳回、且这次密码填错 → `密码错误，重新提交需使用原申请密码`
 *   · 上次申请被驳回、但账号数据丢了 → `原申请账号数据不存在，请联系管理员`
 */
const DUPLICATE_REGISTER_HINT = /账号已存在|手机号已注册|已提交过注册|需使用原申请密码|原申请账号数据不存在/

/**
 * 「身份证号已被占用」的后端文案。
 * ⚠️ 刻意与 DUPLICATE_REGISTER_HINT 不重叠 —— 这种情况**绝不能**去探审核进度：
 * 该身份证挂在**别人的账号**下，把它的审核状态展示出来就是隐私泄露。
 * 命中后只做一件事：把文案落到「身份证号」字段的行内错误上。
 */
const IDCARD_TAKEN_HINT = /身份证号已被其他账号使用|身份证号已注册|身份证号已被使用/

export default {
  name: 'Register',
  components: { BrandLogo },
  data() {
    const equalToPassword = (rule, value, callback) => { if (this.registerForm.password !== value) callback(new Error('两次输入的密码不一致')); else callback() }
    const validateCode = (rule, value, callback) => { if (this.captchaEnabled && !value) callback(new Error('请输入验证码')); else callback() }
    /* 身份证号被占用：由提交后的后端文案回填，用户一改动该字段就自动撤销 */
    const validateIdCardConflict = (rule, value, callback) => { if (this.idCardConflict) callback(new Error(this.idCardConflict)); else callback() }
    return {
      codeUrl: '', positions: [], loading: false, captchaEnabled: false,
      idCardConflict: '',
      registerForm: { username: '', password: '', confirmPassword: '', realName: '', idCard: '', positionId: null, expectedEntryDate: '', code: '', uuid: '' },
      registerRules: {
        username: [{ required: true, trigger: 'blur', message: '请输入手机号作为个人登录账号' }, { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的11位手机号', trigger: 'blur' }],
        password: [{ required: true, trigger: 'blur', message: '请输入登录密码' }, { min: 5, max: 20, message: '密码长度必须介于 5 和 20 之间', trigger: 'blur' }],
        confirmPassword: [{ required: true, trigger: 'blur', message: '请再次输入密码' }, { validator: equalToPassword, trigger: 'blur' }],
        realName: [{ required: true, trigger: 'blur', message: '请输入真实姓名' }],
        idCard: [{ required: true, trigger: 'blur', message: '请输入身份证号' }, { pattern: /^(\d{15}|\d{17}[\dXx])$/, message: '请输入正确的身份证号', trigger: 'blur' }, { validator: validateIdCardConflict, trigger: 'change' }],
        positionId: [{ required: true, message: '请选择意向岗位', trigger: 'change' }],
        code: [{ validator: validateCode, trigger: 'change' }]
      },
      /* 申请进度弹窗 */
      statusDialogVisible: false,
      statusDialogLead: '',
      statusQueryPhone: '',
      statusQueryPassword: '',
      statusQuerying: false,
      statusResult: null,
      statusError: ''
    }
  },
  computed: {
    selectedPosition() { return this.positions.find(position => position.id === this.registerForm.positionId) },
    /** 状态结论：一句话说清「现在到哪一步、下一步干什么」 */
    conclusionText() {
      const status = this.statusResult && this.statusResult.status
      if (status === 'PASSED') return '申请已通过审批，可直接使用该手机号与密码登录系统'
      if (status === 'WAIT_AUDIT') return '已提交申请，正在等待部门管理员审批，可稍后再次查询'
      if (status === 'REJECTED') return '申请已被驳回，可按下方原因修改资料后重新提交'
      return '申请状态已更新，请查看下方进度'
    },
    conclusionTone() {
      const status = this.statusResult && this.statusResult.status
      if (status === 'PASSED') return 'is-success'
      if (status === 'WAIT_AUDIT') return 'is-waiting'
      if (status === 'REJECTED') return 'is-rejected'
      return 'is-info'
    }
  },
  created() { this.getCode(); this.getPositions() },
  methods: {
    getCode() { getCodeImg().then(res => { this.captchaEnabled = res.captchaEnabled === undefined ? true : res.captchaEnabled; if (this.captchaEnabled) { this.codeUrl = 'data:image/gif;base64,' + res.img; this.registerForm.uuid = res.uuid } }) },
    getPositions() { getRegisterPositions().then(res => { this.positions = res.data || [] }) },
    /** 用户一改身份证号就把「已被占用」的报错撤掉，免得改完了旧错误还挂着 */
    onIdCardInput() {
      if (!this.idCardConflict) return
      this.idCardConflict = ''
      this.$nextTick(() => { this.$refs.registerForm.clearValidate('idCard') })
    },
    handleRegister() {
      this.$refs.registerForm.validate(valid => {
        if (!valid) return
        this.loading = true
        const payload = {
          username: this.registerForm.username,
          password: this.registerForm.password,
          realName: this.registerForm.realName,
          idCard: this.registerForm.idCard,
          positionId: this.registerForm.positionId,
          expectedEntryDate: this.registerForm.expectedEntryDate || null,
          code: this.registerForm.code,
          uuid: this.registerForm.uuid
        }
        register(payload).then(res => { this.$alert('<p>注册申请已提交。</p><p>申请编号：<strong>' + res.data + '</strong></p><p>审核通过后即可使用手机号登录。</p>', '申请提交成功', { dangerouslyUseHTMLString: true, type: 'success' }).then(() => this.$router.push('/login')).catch(() => {})         }).catch(err => {
          this.loading = false
          if (this.captchaEnabled) this.getCode()
          const msg = (err && err.message) || ''
          // 身份证号被别的账号占用：只落到「身份证号」字段的行内错误上。
          // ⚠️ 这里**不能**走 explainDuplicateRegister —— 那会把别人账号的审核进度展示出来。
          if (IDCARD_TAKEN_HINT.test(msg)) {
            this.idCardConflict = msg
            this.$nextTick(() => { this.$refs.registerForm.validateField('idCard') })
            return
          }
          this.explainDuplicateRegister(payload.username, payload.password, err)
        })
      })
    },
    /**
     * 重复手机号提交注册时的友好引导。
     *
     * 后端对「已存在账号 / 已注册手机号」只回一句笼统错误（`注册账号已存在` / `该手机号已注册`），
     * 用户看不出自己到底卡在哪一步。这里用**表单里已有的手机号 + 密码**静默探一次真实状态：
     *   · 密码与申请时一致 → 直接展示「已提交申请，请等待审批 / 已通过 / 已驳回」；
     *   · 密码不一致（探不到）→ 引导用户改用申请时设置的密码在弹窗里查询。
     * 密码不一致时不透露任何状态，避免拿别人手机号乱试就能查到审核结果。
     */
    explainDuplicateRegister(phone, password, err) {
      const msg = (err && err.message) || ''
      if (!DUPLICATE_REGISTER_HINT.test(msg)) return
      if (!phone || !password) return
      const rejectedResubmit = /需使用原申请密码|原申请账号数据不存在/.test(msg)
      this.statusQueryPhone = phone
      this.statusQueryPassword = ''
      this.statusError = ''
      this.probeRegisterStatus(phone, password).then(res => {
        if (res.ok && res.data && res.data.submitted) {
          this.statusResult = res.data
          this.statusQueryPassword = password
          this.statusDialogLead = '该手机号已提交过注册申请，以下是当前审核进度。'
        } else if (res.ok && res.data) {
          this.statusResult = res.data
          this.statusQueryPassword = password
          this.statusDialogLead = '该手机号已有注册账号，但未查询到对应的申请单，请联系部门管理员核实。'
        } else {
          this.statusResult = null
          this.statusDialogLead = rejectedResubmit
            ? '该手机号已提交过注册申请，且上次申请已被驳回。请输入原申请密码，查看进度后重新提交。'
            : '该手机号已提交过注册申请。请输入申请时设置的密码，查看审核进度。'
        }
        this.statusDialogVisible = true
      })
    },
    /**
     * 静默查询申请状态（**不走全局 request 封装**）。
     * 全局拦截器会把后端的 `密码错误` 直接弹成 toast —— 在「重复手机号注册」这条链路里
     * 那会误导成「注册失败原因是密码错误」。所以这里用裸 axios 吞掉业务错误码，只回 { ok, data }。
     */
    probeRegisterStatus(phone, password) {
      if (!phone || !password) return Promise.resolve({ ok: false })
      return axios({
        url: process.env.VUE_APP_BASE_API + '/business/register/status',
        method: 'post',
        headers: { 'Content-Type': 'application/json' },
        data: { phone: phone, password: password }
      }).then(res => {
        const body = res.data || {}
        return body.code === 200 ? { ok: true, data: body.data || {} } : { ok: false }
      }).catch(() => ({ ok: false }))
    },
    openStatusDialog() {
      this.statusDialogLead = ''
      this.statusResult = null
      this.statusError = ''
      this.statusDialogVisible = true
    },
    onStatusDialogClosed() {
      this.statusDialogLead = ''
      this.statusResult = null
      this.statusError = ''
    },
    queryRegisterStatus() {
      const phone = (this.statusQueryPhone || '').trim()
      if (!/^1[3-9]\d{9}$/.test(phone)) {
        this.$modal.msgWarning('请输入申请时填写的有效手机号')
        return
      }
      if (!this.statusQueryPassword) {
        this.$modal.msgWarning('请输入注册时设置的密码')
        return
      }
      this.statusQuerying = true
      this.statusResult = null
      this.statusDialogLead = ''
      this.statusError = ''
      queryRegisterStatus(phone, this.statusQueryPassword).then(res => {
        this.statusResult = res.data || { submitted: false, status: 'NOT_SUBMITTED' }
      }).catch(err => {
        // 未找到账号 / 密码不符时后端回 `code:500`，拦截器已 toast；这里再做一份**常驻**的行内提示，
        // 免得 toast 3 秒消失后用户不知道刚才为什么没反应。（不加 catch 会产生 unhandled rejection）
        this.statusError = (err && err.message) || '查询失败，请稍后重试'
      }).finally(() => {
        this.statusQuerying = false
      })
    },
    statusReviewLabel(status) {
      return status === 'WAIT_AUDIT' ? '审核中' : '已审核'
    },
    statusResultLabel(status) {
      if (status === 'PASSED') return '已通过'
      if (status === 'REJECTED') return '已驳回'
      return '未处理'
    },
    statusStepClass(step) {
      const result = this.statusResult
      if (!result || !result.submitted) return step === 'submit' ? 'status-step is-current' : 'status-step'
      if (step === 'submit') return 'status-step is-done'
      if (step === 'review') return 'status-step ' + (result.status === 'WAIT_AUDIT' ? 'is-current' : 'is-done')
      if (result.status === 'PASSED') return 'status-step is-done is-passed'
      if (result.status === 'REJECTED') return 'status-step is-current is-rejected'
      return 'status-step'
    },
    formatStatusTime(value) {
      return value ? String(value).replace('T', ' ').slice(0, 16) : ''
    }
  }
}
</script>

<style rel="stylesheet/scss" lang="scss">
.register { display: flex; justify-content: center; align-items: center; min-height: 100%; padding: 24px 0 64px; box-sizing: border-box; background: #eef3f9; }
.register-form { width: 440px; padding: 30px; border-radius: 8px; background: #fff; box-sizing: border-box; box-shadow: 0 18px 45px rgba(35, 55, 86, .16); }
.title { margin: 0 auto 6px; color: #172033; font-size: 23px; text-align: center; }
.subtitle { margin: 0 0 22px; color: #8490a5; font-size: 13px; text-align: center; }
.full-width { width: 100%; }.position-code { float: right; margin-left: 18px; color: #98a2b3; }.input-icon { height: 39px; width: 14px; margin-left: 2px; }.register-code { float: right; width: 33%; height: 38px; }.register-code-img { height: 38px; cursor: pointer; }.submit-button { width: 100%; }.el-register-footer { position: fixed; bottom: 0; width: 100%; line-height: 40px; color: #98a2b3; font-size: 12px; text-align: center; }.dept-match { display: flex; align-items: center; gap: 8px; min-height: 32px; margin: -7px 0 17px; padding: 0 12px; border-left: 3px solid #1764f5; background: #f5f8ff; color: #1d2129; font-size: 13px; }.dept-match-label { color: #8490a5; }.dept-match-tip { margin-left: auto; color: #8490a5; font-size: 12px; }
@media screen and (max-width: 520px) { .register-form { width: calc(100vw - 32px); padding: 24px 20px; } }

.register { min-height: 100%; flex-direction: column; padding: 32px 20px; background: #f4f6f9; }
.register .auth-layout { display: grid; grid-template-columns: minmax(360px, 1fr) 480px; width: min(1040px, 100%); min-height: 650px; overflow: hidden; border: 1px solid #e7ecf3; border-radius: 10px; background: #fff; box-shadow: 0 16px 42px rgba(31, 49, 78, .08); }
.register .auth-intro { display: flex; flex-direction: column; justify-content: center; padding: 52px; background: #edf4ff; }
.register .auth-intro .brand { padding: 0 0 58px; }
.register .intro-kicker, .register .eyebrow { color: #1764f5; font-size: 12px; letter-spacing: 0; }
.register .auth-intro h1 { max-width: 380px; margin: 16px 0 14px; color: #1d2129; font-size: 30px; line-height: 1.35; }
.register .auth-intro p { max-width: 390px; margin: 0; color: #667085; line-height: 1.8; }
.register-steps { display: flex; gap: 24px; margin-top: 50px; color: #667085; font-size: 12px; }
.register-steps span { display: flex; flex-direction: column; gap: 5px; }
.register-steps b { color: #1764f5; font-size: 14px; }
.register .auth-panel { display: flex; flex-direction: column; justify-content: center; padding: 40px 42px; }
.register .panel-heading { margin-bottom: 24px; }
.register .panel-heading .title { margin: 8px 0 5px; color: #1d2129; font-size: 24px; }
.register .panel-heading .subtitle { margin: 0; color: #8490a5; font-size: 13px; }
.register-form { width: 100%; padding: 0; border-radius: 0; box-shadow: none; }
.register-form .el-form-item { margin-bottom: 17px; }
.register-form .el-button { height: 40px; border-radius: 6px; }
.el-register-footer { position: static; flex: 0 0 auto; margin-top: 16px; color: #98a2b3; }
@media screen and (max-width: 760px) { .register .auth-layout { grid-template-columns: 1fr; min-height: 0; }.register .auth-intro { padding: 28px; }.register .auth-intro .brand { padding-bottom: 26px; }.register .auth-intro h1 { margin-top: 12px; font-size: 24px; }.register-steps { margin-top: 26px; }.register .auth-panel { padding: 32px 28px; } }

/* 注册表单底部链接（原来的「进度查询区块」已收进弹窗） */
.register-links { display: flex; align-items: center; justify-content: flex-end; gap: 10px; margin-top: 12px; font-size: 13px; }
.register-links .link-split { color: #dcdfe6; }
.register-links .el-link { font-size: 13px; font-weight: 400; }
.register-links .el-link.el-link--primary, .register-links .el-link.el-link--primary:hover { color: #1764f5; }
.register-links .link-type, .register-links .link-type:hover, .register-links .link-type:focus { color: #1764f5; }

/* 申请进度弹窗 */
.status-dialog .el-dialog__header { padding: 22px 26px 0; }
.status-dialog .el-dialog__title { color: #1d2129; font-size: 17px; font-weight: 600; }
.status-dialog .el-dialog__headerbtn { top: 22px; right: 22px; }
.status-dialog .el-dialog__body { padding: 14px 26px 26px; }
.status-dialog-tip { margin: 0 0 16px; color: #8490a5; font-size: 12px; line-height: 1.7; }
.status-dialog-lead { margin: 0 0 16px; padding: 10px 12px; border-left: 3px solid #1764f5; background: #f5f8ff; color: #1d2129; font-size: 13px; line-height: 1.7; }
.status-dialog-form { display: flex; flex-direction: column; gap: 12px; }
.status-dialog-form .el-input { width: 100%; }
.status-dialog-form .input-icon { height: 39px; width: 14px; margin-left: 2px; }
.status-dialog-submit { width: 100%; height: 40px; border-radius: 6px; }
.status-dialog-error { margin-top: 14px; padding: 9px 12px; border: 1px solid #fee4e2; border-radius: 6px; background: #fef3f2; color: #d92d20; font-size: 13px; line-height: 1.6; }

.status-result { margin-top: 18px; padding: 16px; border: 1px solid #e5eaf1; border-radius: 6px; background: #fbfcfe; }
.status-result-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.status-result-heading strong, .status-result-heading span { display: block; }
.status-result-heading strong { color: #1d2939; font-size: 14px; }
.status-result-heading span { margin-top: 5px; color: #8490a5; font-size: 12px; }
.status-result-heading > i { color: #1764f5; font-size: 21px; }
.status-conclusion { margin-top: 12px; padding: 10px 12px; border: 1px solid #e5eaf1; border-radius: 6px; font-size: 13px; line-height: 1.6; }
.status-conclusion.is-waiting { border-color: #d6e4ff; background: #f0f6ff; color: #1764f5; }
.status-conclusion.is-success { border-color: #d1fadf; background: #ecfdf3; color: #039855; }
.status-conclusion.is-rejected { border-color: #fee4e2; background: #fef3f2; color: #d92d20; }
.status-conclusion.is-info { background: #f8fafc; color: #667085; }
.status-steps { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin: 16px 0 14px; }
.status-step { position: relative; display: flex; align-items: center; gap: 8px; min-width: 0; color: #98a2b3; }
.status-step:not(:last-child)::after { position: absolute; top: 14px; left: 34px; right: -5px; height: 1px; background: #e3e8ef; content: ''; }
.status-step-dot { z-index: 1; display: flex; flex: 0 0 28px; width: 28px; height: 28px; align-items: center; justify-content: center; border: 1px solid #d8e0eb; border-radius: 50%; background: #fff; color: #98a2b3; font-size: 12px; }
.status-step strong, .status-step small { position: relative; z-index: 1; display: block; white-space: nowrap; }
.status-step strong { color: #8490a5; font-size: 12px; font-weight: 600; }
.status-step small { margin-top: 3px; color: #98a2b3; font-size: 11px; }
.status-step.is-current .status-step-dot, .status-step.is-done .status-step-dot { border-color: #1764f5; background: #1764f5; color: #fff; }
.status-step.is-current strong, .status-step.is-done strong { color: #1764f5; }
.status-step.is-passed .status-step-dot { border-color: #12b76a; background: #12b76a; }
.status-step.is-passed strong { color: #039855; }
.status-step.is-rejected .status-step-dot { border-color: #f04438; background: #f04438; }
.status-step.is-rejected strong { color: #d92d20; }
.status-meta { display: flex; flex-wrap: wrap; gap: 6px 14px; margin-bottom: 14px; color: #667085; font-size: 12px; }
.status-result .el-alert { margin-top: 12px; }
.status-login-link { margin-left: 8px; padding: 0; }
.reject-reason { margin: 7px 0 0; line-height: 1.6; }
@media screen and (max-width: 760px) { .status-steps { gap: 4px; }.status-step { align-items: flex-start; flex-direction: column; gap: 5px; }.status-step:not(:last-child)::after { top: 14px; left: 32px; right: -7px; }.status-step strong, .status-step small { white-space: normal; } }
</style>
