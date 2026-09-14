<template>
  <el-dialog
    :visible="visible"
    width="920px"
    class="agreement-gate"
    append-to-body
    :show-close="false"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    @opened="initSignatureCanvas"
  >
    <div slot="title" class="gate-title">
      <span class="gate-icon"><i class="el-icon-lock" /></span>
      <div>
        <h2>首次登录保密协议签署</h2>
        <p>签署后方可进入实习学习考核系统</p>
      </div>
      <el-tag type="warning" effect="plain">强制签署</el-tag>
    </div>

    <el-alert
      v-if="loadError"
      :title="loadError"
      type="error"
      :closable="false"
      show-icon
      class="gate-alert"
    />
    <template v-else>
      <div class="agreement-meta">
        <div><span>协议名称</span><b>{{ agreement.agreementName || '实习生保密协议' }}</b></div>
        <div><span>当前版本</span><b>{{ agreement.versionNo || 'V1.2' }}</b></div>
        <div><span>签署账号</span><b>{{ name }}</b></div>
      </div>
      <div class="agreement-content">
        <h3>{{ agreement.agreementName || '实习生保密协议' }}</h3>
        <p v-for="(paragraph, index) in paragraphs" :key="index">{{ paragraph }}</p>
      </div>
      <el-form label-position="top" class="sign-form">
        <el-form-item label="签署人">
          <el-input :value="nickName || name" disabled />
        </el-form-item>
        <el-form-item label="本人签名" class="signature-item">
          <div class="signature-pad">
            <canvas
              ref="signatureCanvas"
              class="signature-canvas"
              width="760"
              height="180"
              @mousedown="startSignature"
              @mousemove="drawSignature"
              @mouseup="endSignature"
              @mouseleave="endSignature"
              @touchstart.prevent="startSignature"
              @touchmove.prevent="drawSignature"
              @touchend="endSignature"
            />
            <div class="signature-pad-toolbar">
              <span>请使用鼠标或触控设备在框内签名</span>
              <el-button type="text" icon="el-icon-delete" @click="clearSignature">清空签名</el-button>
            </div>
          </div>
        </el-form-item>
        <el-checkbox v-model="form.confirmed">我已完整阅读、理解并同意遵守上述保密协议</el-checkbox>
      </el-form>
    </template>

    <span slot="footer" class="gate-footer">
      <span>未签署前无法访问工作台及其他业务页面</span>
      <div>
        <el-button :disabled="submitting" @click="decline">暂不签署并退出</el-button>
        <el-button type="primary" :loading="submitting" :disabled="!!loadError" @click="submit">确认签署</el-button>
      </div>
    </span>
  </el-dialog>
</template>

<script>
import { getCurrentAgreement, signCurrentAgreement } from '@/api/business/agreement'
import { mapGetters } from 'vuex'

export default {
  name: 'AgreementGate',
  data() {
    return {
      agreement: {},
      loadError: '',
      loading: false,
      submitting: false,
      form: { signature: '', confirmed: false },
      signatureCanvasContext: null,
      signatureCanvasRect: null,
      isDrawing: false
    }
  },
  computed: {
    ...mapGetters(['roles', 'name', 'nickName', 'protocolStatus']),
    isIntern() {
      return this.roles.indexOf('PRE_TRAINEE') > -1 || this.roles.indexOf('FORMAL_TRAINEE') > -1
    },
    visible() {
      return this.isIntern && Number(this.protocolStatus) !== 1
    },
    paragraphs() {
      const content = this.agreement.content || '请阅读并遵守公司实习期间的信息安全与保密要求。'
      return content.split(/\n+/).filter(Boolean)
    }
  },
  watch: {
    visible: {
      immediate: true,
      handler(value) {
        if (value) {
          this.loadAgreement()
          this.$nextTick(() => this.initSignatureCanvas())
        }
      }
    }
  },
  methods: {
    initSignatureCanvas() {
      const canvas = this.$refs.signatureCanvas
      if (!canvas) return
      const rect = canvas.getBoundingClientRect()
      const ratio = window.devicePixelRatio || 1
      canvas.width = Math.max(1, Math.floor(rect.width * ratio))
      canvas.height = Math.max(1, Math.floor(rect.height * ratio))
      const context = canvas.getContext('2d')
      context.scale(ratio, ratio)
      context.strokeStyle = '#172033'
      context.lineWidth = 2.4
      context.lineCap = 'round'
      context.lineJoin = 'round'
      this.signatureCanvasContext = context
      this.signatureCanvasRect = rect
    },
    getSignaturePoint(event) {
      const source = event.touches && event.touches.length ? event.touches[0] : event
      const rect = this.$refs.signatureCanvas.getBoundingClientRect()
      return { x: source.clientX - rect.left, y: source.clientY - rect.top }
    },
    startSignature(event) {
      if (!this.signatureCanvasContext) this.initSignatureCanvas()
      if (!this.signatureCanvasContext) return
      const point = this.getSignaturePoint(event)
      this.isDrawing = true
      this.signatureCanvasContext.beginPath()
      this.signatureCanvasContext.moveTo(point.x, point.y)
    },
    drawSignature(event) {
      if (!this.isDrawing || !this.signatureCanvasContext) return
      const point = this.getSignaturePoint(event)
      this.signatureCanvasContext.lineTo(point.x, point.y)
      this.signatureCanvasContext.stroke()
    },
    endSignature() {
      if (!this.isDrawing) return
      this.isDrawing = false
      this.form.signature = this.$refs.signatureCanvas.toDataURL('image/png')
    },
    clearSignature() {
      const canvas = this.$refs.signatureCanvas
      if (this.signatureCanvasContext && this.signatureCanvasRect) {
        this.signatureCanvasContext.clearRect(0, 0, this.signatureCanvasRect.width, this.signatureCanvasRect.height)
      } else if (canvas) {
        canvas.getContext('2d').clearRect(0, 0, canvas.width, canvas.height)
      }
      this.form.signature = ''
      this.isDrawing = false
    },
    loadAgreement() {
      if (this.loading || this.agreement.id) return
      this.loading = true
      this.loadError = ''
      getCurrentAgreement().then(res => {
        this.agreement = res.data || {}
        if (this.agreement.signed) this.$store.commit('SET_PROTOCOL_STATUS', 1)
      }).catch(error => {
        this.loadError = (error && error.msg) || '保密协议加载失败，请重新登录后再试'
      }).finally(() => {
        this.loading = false
      })
    },
    submit() {
      if (!this.form.signature || !this.form.confirmed) {
        this.$modal.msgWarning('请完成本人签名并勾选协议确认项')
        return
      }
      this.submitting = true
      signCurrentAgreement(this.form).then(res => {
        this.agreement = res.data || this.agreement
        this.$store.commit('SET_PROTOCOL_STATUS', 1)
        this.$modal.msgSuccess('保密协议已签署，可以开始使用系统')
      }).finally(() => {
        this.submitting = false
      })
    },
    decline() {
      this.$store.dispatch('LogOut').finally(() => {
        location.href = '/login'
      })
    }
  }
}
</script>

<style lang="scss">
.agreement-gate .el-dialog { width: min(920px, calc(100vw - 28px)); max-width: calc(100vw - 28px); border-radius: 6px; }
.agreement-gate .el-dialog__header { padding: 22px 24px 16px; border-bottom: 1px solid #e7ebf0; }
.agreement-gate .el-dialog__body { max-height: calc(100vh - 230px); overflow-y: auto; padding: 20px 24px; }
.agreement-gate .el-dialog__footer { padding: 16px 24px 20px; border-top: 1px solid #e7ebf0; }
.gate-title { display: flex; align-items: center; gap: 12px; }
.gate-title h2 { margin: 0 0 4px; color: #1d2939; font-size: 19px; letter-spacing: 0; }
.gate-title p { margin: 0; color: #667085; font-size: 13px; }
.gate-title .el-tag { margin-left: auto; }
.gate-icon { display: flex; width: 42px; height: 42px; align-items: center; justify-content: center; color: #fff; border-radius: 6px; background: #1764f5; font-size: 20px; }
.gate-alert { margin-bottom: 16px; }
.agreement-meta { display: grid; grid-template-columns: repeat(3, 1fr); gap: 1px; margin-bottom: 16px; overflow: hidden; border: 1px solid #e6ebf1; background: #e6ebf1; }
.agreement-meta div { padding: 11px 13px; background: #f8fafc; }
.agreement-meta span, .agreement-meta b { display: block; }
.agreement-meta span { margin-bottom: 5px; color: #7a8694; font-size: 12px; }
.agreement-meta b { color: #344054; font-size: 13px; }
.agreement-content { max-height: 340px; overflow-y: auto; padding: 18px 20px; border: 1px solid #e2e8f0; background: #fff; }
.agreement-content h3 { margin: 0 0 14px; text-align: center; font-size: 17px; }
.agreement-content p { margin: 0 0 12px; color: #475467; line-height: 1.8; }
.sign-form { display: grid; grid-template-columns: 1fr 1fr; gap: 0 14px; margin-top: 16px; }
.sign-form .el-checkbox { grid-column: 1 / -1; }
.gate-footer { display: flex; align-items: center; justify-content: space-between; gap: 16px; color: #b54708; font-size: 12px; }
.signature-item { grid-column: 1 / -1; }
.signature-pad { overflow: hidden; border: 1px solid #d8e0eb; border-radius: 4px; background: #fff; }
.signature-canvas { display: block; width: 100%; height: 180px; cursor: crosshair; touch-action: none; background-color: #fff; background-image: linear-gradient(#fff 95%, #eef2f7 95%); background-size: 100% 34px; }
.signature-pad-toolbar { display: flex; align-items: center; justify-content: space-between; min-height: 36px; padding: 0 12px; border-top: 1px solid #e7ebf0; color: #8490a5; font-size: 12px; }
.signature-pad-toolbar .el-button { margin: 0; }
@media (max-width: 640px) { .agreement-meta, .sign-form { grid-template-columns: 1fr; }.gate-footer { align-items: stretch; flex-direction: column; }.gate-footer > div { display: flex; }.gate-footer .el-button { flex: 1; }.gate-title p { display: none; } }
</style>
