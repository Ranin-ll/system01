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
      </section>
    </div>
    <div class="el-register-footer">实习生学习考核系统 · 本地开发环境</div>
  </div>
</template>

<script>
import { getCodeImg, getRegisterPositions, register } from '@/api/login'
import BrandLogo from '@/components/BrandLogo'

export default {
  name: 'Register',
  components: { BrandLogo },
  data() {
    const equalToPassword = (rule, value, callback) => { if (this.registerForm.password !== value) callback(new Error('两次输入的密码不一致')); else callback() }
    const validateCode = (rule, value, callback) => { if (this.captchaEnabled && !value) callback(new Error('请输入验证码')); else callback() }
    return {
      codeUrl: '', positions: [],
      registerForm: { username: '', password: '', confirmPassword: '', realName: '', idCard: '', positionId: null, expectedEntryDate: '', code: '', uuid: '' },
      registerRules: {
        username: [{ required: true, trigger: 'blur', message: '请输入手机号作为个人登录账号' }, { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的11位手机号', trigger: 'blur' }],
        password: [{ required: true, trigger: 'blur', message: '请输入登录密码' }, { min: 5, max: 20, message: '密码长度必须介于 5 和 20 之间', trigger: 'blur' }],
        confirmPassword: [{ required: true, trigger: 'blur', message: '请再次输入密码' }, { validator: equalToPassword, trigger: 'blur' }],
        realName: [{ required: true, trigger: 'blur', message: '请输入真实姓名' }],
        idCard: [{ required: true, trigger: 'blur', message: '请输入身份证号' }, { pattern: /^(\d{15}|\d{17}[\dXx])$/, message: '请输入正确的身份证号', trigger: 'blur' }],
        positionId: [{ required: true, message: '请选择意向岗位', trigger: 'change' }],
        code: [{ validator: validateCode, trigger: 'change' }]
      }, loading: false, captchaEnabled: true
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
        const payload = { username: this.registerForm.username, password: this.registerForm.password, realName: this.registerForm.realName, idCard: this.registerForm.idCard, positionId: this.registerForm.positionId, expectedEntryDate: this.registerForm.expectedEntryDate || null }
        register(payload).then(res => { this.$alert('<p>注册申请已提交。</p><p>申请编号：<strong>' + res.data + '</strong></p><p>审核通过后即可使用手机号登录。</p>', '申请提交成功', { dangerouslyUseHTMLString: true, type: 'success' }).then(() => this.$router.push('/login')).catch(() => {}) }).catch(() => { this.loading = false; if (this.captchaEnabled) this.getCode() })
      })
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
</style>
