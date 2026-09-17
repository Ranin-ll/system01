<template>
  <div class="guide-page">
    <div class="guide-breadcrumb">
      <span>学习与考核</span>
      <span>/</span>
      <b>备考资料</b>
    </div>

    <header class="guide-heading">
      <div>
        <span class="eyebrow">MATERIAL CENTER</span>
        <h1>备考资料</h1>
        <p>部门管理员发布的考试指南与考核规则，支持在线预览与下载。</p>
      </div>
      <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="loadMaterials">刷新</el-button>
    </header>

    <section class="guide-panel">
      <div class="panel-heading">
        <div>
          <span class="section-index">01</span>
          <div>
            <h2>考试指南</h2>
            <p>由所属部门管理员在管理端上传，发布后在此处生效。</p>
          </div>
        </div>
        <span class="panel-count">
          <el-tag v-if="demoMode" size="mini" type="warning" effect="plain">示例数据</el-tag>
          <span class="count-text">{{ materials.length }} 份资料</span>
        </span>
      </div>

      <div v-if="!materials.length" class="empty-state">
        <i class="el-icon-notebook-2" />
        <strong>暂无可下载的备考资料</strong>
        <span>部门管理员上传考试指南（PDF）后，会出现在这里。</span>
      </div>

      <ul v-else class="material-list">
        <li v-for="item in materials" :key="item.id" class="material-row">
          <span class="material-icon"><i class="el-icon-document" /></span>
          <div class="material-info">
            <h3>{{ item.materialName }}</h3>
            <p>{{ item.summary || '考试指南' }}</p>
            <span class="material-meta">版本 {{ item.versionNo || 'v1.0' }} · 更新 {{ fmtTime(item.updateTime) }}</span>
          </div>
          <div class="material-action">
            <el-button size="mini" plain @click="preview(item)">文件预览</el-button>
            <el-button size="mini" type="primary" plain @click="download(item)">下载</el-button>
          </div>
        </li>
      </ul>
    </section>

    <section class="guide-hint">
      <span class="hint-icon"><i class="el-icon-info" /></span>
      <div>
        <strong>备考资料包含什么</strong>
        <p>考试指南 PDF（考核范围与评分说明）、考核规则与注意事项、必要的参考资料。资料由部门管理员统一发布，你无需自行上传。</p>
      </div>
    </section>
  </div>
</template>

<script>
import { parseTime } from '@/utils/ruoyi'

/**
 * 备考资料页（实习生端只读）
 *
 * 数据来源：`material` 表已存在（material_name / material_type / position_id /
 * summary / file_url / version_no / is_public / status / valid_from / valid_to），
 * 但当前 `ruoyi-business` **没有对应的 Controller**。
 *
 * 因此当前用 `DEMO_MATERIALS` **模拟静态数据**撑起版面（页面上打「示例数据」标记），
 * 待后端补只读接口后，把 loadMaterials() 里的取数换成真实请求即可 —— 模板无需改动。
 *
 * 接入方式：
 *   listPublishedMaterials({ positionId: this.$store.getters.positionId })
 *     .then(res => { this.materials = res.data || []; this.demoMode = false })
 */
const DEMO_MATERIALS = [
  {
    id: 'demo-1',
    materialName: '开发实习生转正考核指南',
    summary: '考核范围、评分标准、考试纪律与注意事项说明',
    versionNo: 'v1.0',
    updateTime: '2026-09-10 10:20:00'
  },
  {
    id: 'demo-2',
    materialName: '开发工具与项目环境 · 考试大纲',
    summary: '开发环境搭建、版本管理、项目结构等考核要点',
    versionNo: 'v1.1',
    updateTime: '2026-09-12 15:40:00'
  },
  {
    id: 'demo-3',
    materialName: '代码规范与分支协作 · 参考资料',
    summary: 'Git 工作流、Code Review 与分支管理规范汇编',
    versionNo: 'v1.0',
    updateTime: '2026-09-14 09:05:00'
  },
  {
    id: 'demo-4',
    materialName: 'MySQL 与数据访问 · 复习提纲',
    summary: 'SQL 编写规范、索引优化与常见面试考点',
    versionNo: 'v1.0',
    updateTime: '2026-09-15 14:30:00'
  }
]

export default {
  name: 'InternGuide',
  data() {
    return {
      loading: false,
      demoMode: true,
      materials: []
    }
  },
  created() {
    this.loadMaterials()
  },
  methods: {
    loadMaterials() {
      this.loading = true
      // TODO(后端接口就绪后替换)：改为真实请求并置 demoMode = false
      this.materials = DEMO_MATERIALS
      this.demoMode = true
      this.loading = false
    },
    fmtTime(val) {
      return val ? parseTime(val, '{y}-{m}-{d}') : '--'
    },
    preview() {
      this.$modal.msgWarning('当前为示例数据，后端接口接入后可在线预览')
    },
    download() {
      this.$modal.msgWarning('当前为示例数据，后端接口接入后可下载')
    }
  }
}
</script>

<style lang="scss" scoped>
.guide-page { min-height: 100%; padding: 0 0 40px; color: #283544; }
.guide-breadcrumb { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; color: #98a2b3; font-size: 12px; }
.guide-breadcrumb b { color: #475467; font-weight: 500; }
.guide-heading { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 18px; }
.eyebrow { color: #2878c7; font-size: 11px; letter-spacing: .08em; }
.guide-heading h1 { margin: 5px 0 7px; color: #1d2939; font-size: 28px; font-weight: 600; }
.guide-heading p { margin: 0; color: #667085; font-size: 13px; }
.guide-panel { padding: 22px 24px 25px; background: #fff; box-shadow: 0 1px 3px rgba(16, 24, 40, .04); }
.panel-heading { display: flex; align-items: center; justify-content: space-between; padding-bottom: 18px; border-bottom: 1px solid #edf0f4; }
.panel-heading > div { display: flex; align-items: flex-start; gap: 12px; }
.section-index { color: #1764f5; font-size: 11px; font-weight: 700; }
.panel-heading h2 { margin: 0 0 5px; color: #1d2939; font-size: 18px; font-weight: 600; }
.panel-heading p { margin: 0; color: #8490a0; font-size: 12px; }
.panel-count { display: flex; align-items: center; gap: 8px; color: #667085; font-size: 12px; }
.empty-state { display: flex; min-height: 220px; align-items: center; justify-content: center; flex-direction: column; gap: 9px; color: #98a2b3; }
.empty-state i { color: #b7c1cc; font-size: 34px; }
.empty-state strong { color: #667085; font-size: 14px; font-weight: 500; }
.empty-state span { font-size: 12px; }
.material-list { margin: 0; padding: 0; list-style: none; }
.material-row { display: flex; align-items: center; gap: 18px; padding: 20px 8px; border-bottom: 1px solid #edf0f4; }
.material-row:last-child { border-bottom: 0; }
.material-icon { display: flex; width: 48px; height: 48px; flex: 0 0 48px; align-items: center; justify-content: center; color: #fff; background: #23966f; font-size: 22px; }
.material-info { min-width: 0; flex: 1; }
.material-info h3 { margin: 0 0 6px; color: #1d2939; font-size: 16px; font-weight: 600; }
.material-info p { margin: 0 0 7px; overflow: hidden; color: #667085; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.material-meta { color: #98a2b3; font-size: 11px; }
.material-action { display: flex; min-width: 150px; gap: 8px; justify-content: flex-end; }
.guide-hint { display: flex; align-items: flex-start; gap: 12px; margin-top: 14px; padding: 13px 16px; border: 1px solid #dbeafe; background: #f5f9ff; }
.hint-icon { display: flex; width: 25px; height: 25px; flex: 0 0 25px; align-items: center; justify-content: center; color: #2878c7; font-size: 16px; }
.guide-hint strong { color: #344054; font-size: 12px; }
.guide-hint p { margin: 4px 0 0; color: #667085; font-size: 11px; line-height: 1.5; }
@media (max-width: 700px) {
  .guide-page { padding-bottom: 28px; }
  .guide-heading { align-items: flex-start; flex-direction: column; gap: 12px; }
  .guide-heading h1 { font-size: 24px; }
  .guide-panel { padding: 18px 14px; }
  .panel-heading { align-items: flex-start; gap: 10px; }
  .panel-heading p { max-width: 240px; line-height: 1.5; }
  .material-row { align-items: flex-start; flex-wrap: wrap; gap: 12px; }
  .material-info { width: calc(100% - 60px); flex: none; }
  .material-action { width: 100%; min-width: 0; justify-content: flex-end; }
}
</style>
