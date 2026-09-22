<template>
  <div class="intern-agreement">
    <div class="ia-head">
      <div>
        <span class="ia-eyebrow">SIGN & ARCHIVE</span>
        <h1>保密协议与签署凭证</h1>
        <p>查看当前生效的协议全文，以及你本人签署时留存的手写签名与凭证信息。</p>
      </div>
      <el-tag :type="signed ? 'success' : 'warning'" effect="plain" size="medium">{{ signed ? '已签署' : '待签署' }}</el-tag>
    </div>

    <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon class="ia-alert" />

    <div class="ia-grid">
      <!-- 协议全文 -->
      <section class="ia-card ia-doc">
        <div class="ia-card-h">
          <span class="ia-idx">01</span>
          <h2>{{ agreement.agreementName || '实习生保密协议' }}</h2>
          <span class="ia-ver" v-if="agreement.versionNo">版本 {{ agreement.versionNo }}</span>
        </div>
        <div class="ia-doc-body">
          <p v-for="(paragraph, index) in paragraphs" :key="index">{{ paragraph }}</p>
          <div v-if="!paragraphs.length" class="ia-empty">当前没有生效中的协议正文，请联系部门管理员。</div>
        </div>
      </section>

      <!-- 签署凭证 -->
      <section class="ia-card ia-receipt">
        <div class="ia-card-h">
          <span class="ia-idx">02</span>
          <h2>签署凭证</h2>
          <span class="ia-ver" v-if="receipt">本次签署留档</span>
        </div>

        <div v-if="receipt" class="ia-receipt-body">
          <div class="ia-sign-frame">
            <img v-if="signatureImage" :src="signatureImage" class="ia-sign-img" alt="本人签名" />
            <div v-else class="ia-sign-text">
              <i class="el-icon-warning-outline" />
              <span>该记录为历史文字式签署，未留存手写签名图</span>
              <code>{{ signatureText || '（空）' }}</code>
            </div>
          </div>

          <dl class="ia-meta">
            <div><dt>凭证编号</dt><dd class="ia-mono">{{ receipt.certificateNo || '—' }}</dd></div>
            <div><dt>签署人</dt><dd>{{ nickName || name || '—' }}</dd></div>
            <div><dt>协议版本</dt><dd>{{ receipt.versionNo || agreement.versionNo || '—' }}</dd></div>
            <div><dt>签署方式</dt><dd>{{ signModeLabel }}</dd></div>
            <div><dt>签署时间</dt><dd>{{ formatTime(receipt.signTime) }}</dd></div>
            <div><dt>签署 IP</dt><dd class="ia-mono">{{ receipt.signIp || '—' }}</dd></div>
            <div class="ia-meta-wide"><dt>签署终端</dt><dd class="ia-terminal">{{ receipt.terminal || '—' }}</dd></div>
          </dl>

          <p class="ia-note">
            <i class="el-icon-info" />
            签名图为签署当次鼠标（或触控）手写笔迹的原样留档，仅本人在此查看；管理员可在人员档案中调阅同一条记录。
          </p>
        </div>

        <div v-else class="ia-empty-block">
          <i class="el-icon-edit-outline" />
          <b>{{ loadError ? '凭证加载失败' : '尚未签署' }}</b>
          <span>首次登录时会自动弹出签署窗口；完成后签名图与凭证信息会显示在这里。</span>
        </div>
      </section>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { getCurrentAgreement, getMyAgreementSignature } from '@/api/business/agreement'

export default {
  name: 'InternAgreement',
  data() {
    return {
      agreement: {},
      receipt: null,
      loadError: ''
    }
  },
  computed: {
    ...mapGetters(['name', 'nickName', 'protocolStatus']),
    signed() {
      return Number(this.protocolStatus) === 1
    },
    paragraphs() {
      const content = this.agreement.content || ''
      return content.split(/\n+/).filter(Boolean)
    },
    signatureImage() {
      const data = this.receipt && this.receipt.signatureData
      return data && /^data:image\//.test(data) ? data : ''
    },
    signatureText() {
      const data = this.receipt && this.receipt.signatureData
      return data && !/^data:image\//.test(data) ? data : ''
    },
    signModeLabel() {
      const mode = this.receipt && this.receipt.signMode
      if (mode === 'MOUSE') return '鼠标 / 触控手写'
      if (mode === 'TEXT') return '文字式（历史记录）'
      return mode || '—'
    }
  },
  created() {
    this.loadAll()
  },
  methods: {
    loadAll() {
      this.loadError = ''
      getCurrentAgreement().then(res => {
        this.agreement = res.data || {}
      }).catch(error => {
        this.loadError = (error && error.msg) || '协议加载失败，请稍后重试'
      })
      getMyAgreementSignature().then(res => {
        this.receipt = res.data || null
      }).catch(() => {
        this.receipt = null
      })
    },
    formatTime(value) {
      return value ? String(value).replace('T', ' ').slice(0, 19) : '—'
    }
  }
}
</script>

<style lang="scss" scoped>
.intern-agreement { padding: 20px 24px 40px; }
.ia-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; padding: 20px 22px; border: 1px solid #e7ecf3; border-radius: 8px; background: #fff; }
.ia-eyebrow { color: #1764f5; font-size: 12px; letter-spacing: 0; }
.ia-head h1 { margin: 8px 0 6px; color: #1d2129; font-size: 22px; }
.ia-head p { margin: 0; color: #667085; font-size: 13px; line-height: 1.7; }
.ia-alert { margin-top: 14px; }

.ia-grid { display: grid; grid-template-columns: minmax(0, 7fr) minmax(0, 5fr); gap: 16px; margin-top: 16px; align-items: start; }
.ia-card { border: 1px solid #e7ecf3; border-radius: 8px; background: #fff; }
.ia-card-h { display: flex; align-items: center; gap: 10px; padding: 14px 18px; border-bottom: 1px solid #eef2f7; }
.ia-idx { display: flex; width: 22px; height: 22px; align-items: center; justify-content: center; border-radius: 4px; background: #eef4ff; color: #1764f5; font-size: 12px; }
.ia-card-h h2 { flex: 1; margin: 0; color: #1d2129; font-size: 15px; font-weight: 600; }
.ia-ver { color: #8490a5; font-size: 12px; }

.ia-doc-body { max-height: 560px; overflow-y: auto; padding: 18px 20px; }
.ia-doc-body p { margin: 0 0 12px; color: #475467; font-size: 13px; line-height: 1.9; }

.ia-receipt-body { padding: 16px 18px 18px; }
.ia-sign-frame { display: flex; min-height: 132px; align-items: center; justify-content: center; padding: 10px; border: 1px dashed #d8e0eb; border-radius: 6px; background: #fcfdff; }
.ia-sign-img { max-width: 100%; max-height: 200px; object-fit: contain; }
.ia-sign-text { display: flex; flex-direction: column; align-items: center; gap: 8px; color: #b54708; font-size: 12px; text-align: center; }
.ia-sign-text i { font-size: 22px; }
.ia-sign-text code { padding: 3px 10px; border-radius: 4px; background: #f4f6f9; color: #344054; font-size: 12px; }

.ia-meta { display: grid; grid-template-columns: 1fr 1fr; gap: 1px; margin: 16px 0 0; overflow: hidden; border: 1px solid #e6ebf1; border-radius: 6px; background: #e6ebf1; }
.ia-meta > div { padding: 10px 12px; background: #f8fafc; }
.ia-meta-wide { grid-column: 1 / -1; }
.ia-meta dt { margin-bottom: 4px; color: #7a8694; font-size: 12px; }
.ia-meta dd { margin: 0; color: #344054; font-size: 13px; }
.ia-mono { font-family: Consolas, Monaco, monospace; font-size: 12px; }
.ia-terminal { word-break: break-all; font-size: 12px; line-height: 1.6; }

.ia-note { display: flex; gap: 7px; margin: 14px 0 0; color: #8490a5; font-size: 12px; line-height: 1.7; }
.ia-note i { margin-top: 2px; }

.ia-empty-block { display: flex; min-height: 240px; flex-direction: column; align-items: center; justify-content: center; gap: 8px; padding: 24px; color: #98a2b3; text-align: center; }
.ia-empty-block i { font-size: 30px; color: #cbd5e1; }
.ia-empty-block b { color: #475467; font-size: 14px; }
.ia-empty-block span { max-width: 280px; font-size: 12px; line-height: 1.7; }
.ia-empty { color: #98a2b3; font-size: 13px; }

@media screen and (max-width: 1100px) {
  .ia-grid { grid-template-columns: 1fr; }
  .ia-doc-body { max-height: 340px; }
}
@media screen and (max-width: 640px) {
  .intern-agreement { padding: 14px 14px 32px; }
  .ia-head { flex-direction: column; }
  .ia-meta { grid-template-columns: 1fr; }
}
</style>
