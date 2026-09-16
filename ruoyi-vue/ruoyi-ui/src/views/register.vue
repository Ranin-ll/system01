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
      <el-form-item prop="idCard"><el-input v-model="registerForm.idCard" placeholder="身份证号" auto-complete="off"><svg-icon slot="prefix" icon-class="validCode" class="el-input__icon input-icon" /></el-input></el-form-item>
      <el-form-item prop="positionId"><el-select v-model="registerForm.positionId" placeholder="选择意向岗位" filterable class="full-width"><el-option v-for="position in positions" :key="position.id" :label="position.positionName" :value="position.id"><span>{{ position.positionName }}</span><small class="position-code">{{ position.positionCode }}</small></el-option></el-select></el-form-item>
      <div v-if="selectedPosition" class="dept-match"><span class="dept-match-label">自动匹配部门</span><strong>{{ selectedPosition.deptName || '待配置' }}</strong><span v-if="selectedPosition.deptName" class="dept-match-tip">提交后由该部门管理员审核</span></div>
      <el-form-item><el-date-picker v-model="registerForm.expectedEntryDate" type="date" value-format="yyyy-MM-dd" placeholder="预计入职日期（可选）" class="full-width" /></el-form-item>
      <el-form-item prop="code" v-if="captchaEnabled"><el-input v-model="registerForm.code" placeholder="验证码" style="width: 63%" /><div class="register-code"><img :src="codeUrl" @click="getCode" class="register-code-img" /></div></el-form-item>
      <el-form-item><el-button :loading="loading" type="primary" class="submit-button" @click.native.prevent="handleRegister">{{ loading ? '提交中...' : '提交注册申请' }}</el-button><div class="register-links"><router-link class="link-type" :to="'/login'">使用已有账号登录</router-link></div></el-form-item>
        </el-form>

        <section class="status-query" aria-labelledby="status-query-title">
          <div class="status-query-heading">
            <div>
              <span class="eyebrow">申请进度</span>
              <h3 id="status-query-title">查询注册申请状态</h3>
            </div>
            <span class="status-query-note">无需登录</span>
          </div>
          <el-form class="status-query-form" @submit.native.prevent="queryRegisterStatus">
            <el-input
              v-model.trim="statusQueryPhone"
              maxlength="11"
              clearable
              placeholder="请输入注册申请时填写的手机号"
              @keyup.enter.native="queryRegisterStatus"
            >
              <svg-icon slot="prefix" icon-class="phone" class="el-input__icon input-icon" />
            </el-input>
            <el-input
              v-model="statusQueryPassword"
              type="password"
              show-password
              maxlength="20"
              placeholder="请输入注册申请时设置的密码"
              @keyup.enter.native="queryRegisterStatus"
            >
              <svg-icon slot="prefix" icon-class="password" class="el-input__icon input-icon" />
            </el-input>
            <el-button type="primary" :loading="statusQuerying" @click="queryRegisterStatus">
              <i class="el-icon-search" /> 查询状态
            </el-button>
          </el-form>

          <div v-if="statusResult" class="status-result">
            <div class="status-result-heading">
              <div>
                <strong>{{ statusResult.submitted ? '已提交注册申请' : '未找到注册申请' }}</strong>
                <span>{{ statusResult.submitted ? statusResult.applicationNo : '该手机号暂未提交申请' }}</span>
              </div>
              <i :class="statusResult.submitted ? 'el-icon-circle-check' : 'el-icon-info'" />
            </div>
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
            <el-alert v-if="statusResult.status === 'WAIT_AUDIT'" title="申请已提交，部门管理员尚未完成审核，请稍后再次查询。" type="info" :closable="false" show-icon />
            <el-alert v-if="statusResult.status === 'PASSED'" type="success" :closable="false" show-icon>
              审核已通过，可使用申请表填写的手机号和密码登录系统。
              <el-button type="text" class="status-login-link" @click="$router.push('/login')">去登录</el-button>
            </el-alert>
            <el-alert v-if="statusResult.status === 'REJECTED'" type="error" :closable="false" show-icon>
              <span>审核已驳回</span>
              <p class="reject-reason">驳回原因：{{ statusResult.rejectReason || '部门管理员未填写具体原因' }}</p>
            </el-alert>
          </div>
        </section>
      </section>
    </div>
    <div class="el-register-footer">实习生学习考核系统 · 本地开发环境</div>
  </div>
</template>

<script>
import { getCodeImg, getRegisterPositions, register } from '@/api/login'
import { queryRegisterStatus } from '@/api/business/register'
import BrandLogo from '@/components/BrandLogo'

export default {
  name: 'Register',
  components: { BrandLogo },
  data() {
    const equalToPassword = (rule, value, callback) => { if (this.registerForm.password !== value) callback(new Error('两次输入的密码不一致')); else callback() }
    const validateCode = (rule, value, callback) => { if (this.captchaEnabled && !value) callback(new Error('请输入验证码')); else callback() }
    return {
      codeUrl: '', positions: [], statusQueryPhone: '', statusQueryPassword: '', statusQuerying: false, statusResult: null,
      registerForm: { username: '', password: '', confirmPassword: '', realName: '', idCard: '', positionId: null, expectedEntryDate: '', code: '', uuid: '' },
      registerRules: {
        username: [{ required: true, trigger: 'blur', message: '请输入手机号作为个人登录账号' }, { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的11位手机号', trigger: 'blur' }],
        password: [{ required: true, trigger: 'blur', message: '请输入登录密码' }, { min: 5, max: 20, message: '密码长度必须介于 5 和 20 之间', trigger: 'blur' }],
        confirmPassword: [{ required: true, trigger: 'blur', message: '请再次输入密码' }, { validator: equalToPassword, trigger: 'blur' }],
        realName: [{ required: true, trigger: 'blur', message: '请输入真实姓名' }],
        idCard: [{ required: true, trigger: 'blur', message: '请输入身份证号' }, { pattern: /^(\d{15}|\d{17}[\dXx])$/, message: '请输入正确的身份证号', trigger: 'blur' }],
        positionId: [{ required: true, message: '请选择意向岗位', trigger: 'change' }],
        code: [{ validator: validateCode, trigger: 'change' }]
      }, loading: false, captchaEnabled: false
    }
  },
  computed: {
    selectedPosition() { return this.positions.find(position => position.id === this.registerForm.positionId) }
  },
  created() { this.getCode(); this.getPositions() },
  methods: {
    getCode() { getCodeImg().then(res => { this.captchaEnabled = res.captchaEnabled === undefined ? true : res.captchaEnabled; if (this.captchaEnabled) { this.codeUrl = 'data:image/gif;base64,' + res.img; this.registerForm.uuid = res.uuid } }) },
    getPositions() { getRegisterPositions().then(res => { this.positions = res.data || [] }) },
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
        register(payload).then(res => { this.$alert('<p>注册申请已提交。</p><p>申请编号：<strong>' + res.data + '</strong></p><p>审核通过后即可使用手机号登录。</p>', '申请提交成功', { dangerouslyUseHTMLString: true, type: 'success' }).then(() => this.$router.push('/login')).catch(() => {}) }).catch(() => { this.loading = false; if (this.captchaEnabled) this.getCode() })
      })
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
      queryRegisterStatus(phone, this.statusQueryPassword).then(res => {
        this.statusResult = res.data || { submitted: false, status: 'NOT_SUBMITTED' }
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
.full-width { width: 100%; }.position-code { float: right; margin-left: 18px; color: #98a2b3; }.input-icon { height: 39px; width: 14px; margin-left: 2px; }.register-code { float: right; width: 33%; height: 38px; }.register-code-img { height: 38px; cursor: pointer; }.submit-button { width: 100%; }.register-links { margin-top: 12px; text-align: right; }.el-register-footer { position: fixed; bottom: 0; width: 100%; line-height: 40px; color: #98a2b3; font-size: 12px; text-align: center; }.dept-match { display: flex; align-items: center; gap: 8px; min-height: 32px; margin: -7px 0 17px; padding: 0 12px; border-left: 3px solid #1764f5; background: #f5f8ff; color: #1d2129; font-size: 13px; }.dept-match-label { color: #8490a5; }.dept-match-tip { margin-left: auto; color: #8490a5; font-size: 12px; }
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
.status-query { margin-top: 22px; padding-top: 20px; border-top: 1px solid #edf0f4; }
.status-query-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.status-query-heading h3 { margin: 6px 0 0; color: #1d2129; font-size: 17px; font-weight: 600; }
.status-query-note { padding-top: 3px; color: #98a2b3; font-size: 12px; }
.status-query-form { display: flex; flex-direction: column; gap: 10px; margin-top: 14px; }
.status-query-form .el-input { flex: 1; min-width: 0; }
.status-query-form .input-icon { position: relative; top: 10px; }
.status-query-form .el-button { width: 100%; }
.status-result { margin-top: 16px; padding: 16px; border: 1px solid #e5eaf1; background: #fbfcfe; }
.status-result-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.status-result-heading strong, .status-result-heading span { display: block; }
.status-result-heading strong { color: #1d2939; font-size: 14px; }
.status-result-heading span { margin-top: 5px; color: #8490a5; font-size: 12px; }
.status-result-heading > i { color: #1764f5; font-size: 21px; }
.status-steps { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin: 18px 0 14px; }
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
@media screen and (max-width: 760px) { .status-query-form { flex-direction: column; }.status-query-form .el-button { width: 100%; }.status-steps { gap: 4px; }.status-step { align-items: flex-start; flex-direction: column; gap: 5px; }.status-step:not(:last-child)::after { top: 14px; left: 32px; right: -7px; }.status-step strong, .status-step small { white-space: normal; } }</style>
