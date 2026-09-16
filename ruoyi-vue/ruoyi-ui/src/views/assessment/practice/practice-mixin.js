import { parseTime } from '@/utils/ruoyi'

/**
 * 模拟考核模块公共逻辑（理论自测页 / 回顾页 / 实操页共用）
 *
 * 统一选项解析、作答对比、题型映射、时间格式化与附件解析，
 * 避免各独立页面重复实现同一套方法导致行为不一致。
 */
export default {
  computed: {
    /** 附件下载需要拼后端前缀 */
    baseApi() {
      return process.env.VUE_APP_BASE_API || ''
    }
  },
  methods: {
    /** 解析 options_json → [{ key, content }]，容错脏数据 */
    parseOptions(json) {
      try {
        const arr = JSON.parse(json || '[]')
        return Array.isArray(arr) ? arr : []
      } catch (e) {
        return []
      }
    },
    /** 解析实操题附件 [{ name, url }]，容错脏数据 */
    parseAttachments(json) {
      if (!json) return []
      try {
        const arr = JSON.parse(json)
        return Array.isArray(arr) ? arr : []
      } catch (e) {
        return []
      }
    },
    /** 作答/答案串 → 选项键数组，如 "A,C" → ['A','C'] */
    splitAnswer(val) {
      if (!val) return []
      return String(val).toUpperCase().split(',').map(s => s.trim()).filter(s => !!s)
    },
    /** 该选项是否为正确答案 */
    isCorrectOpt(item, key) {
      return this.splitAnswer(item.correctAnswer).indexOf(key) > -1
    },
    /** 该选项是否被本人选择 */
    isPicked(item, key) {
      return this.splitAnswer(item.userAnswer).indexOf(key) > -1
    },
    /** 后端明细 → 回顾项（is_correct 的 1/0 统一成布尔） */
    mapRecordItem(it) {
      return {
        questionId: it.questionId,
        qtype: it.qtype,
        stem: it.stem,
        optionsJson: it.optionsJson,
        userAnswer: it.userAnswer,
        correctAnswer: it.correctAnswer,
        analysis: it.analysis,
        correct: it.isCorrect === 1 || it.isCorrect === true
      }
    },
    /** 时间格式化，避免直接展示 ISO 串 */
    fmtTime(val) {
      return val ? parseTime(val, '{y}-{m}-{d} {h}:{i}') : '-'
    },
    typeLabel(qtype) {
      return { SINGLE: '单选', MULTI: '多选', JUDGE: '判断' }[qtype] || qtype
    },
    typeTag(qtype) {
      return { SINGLE: 'success', MULTI: 'warning', JUDGE: 'primary' }[qtype] || 'info'
    }
  }
}
