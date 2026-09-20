/**
 * 附件预览 / 下载公共逻辑
 *
 * 实习生端「备考资料」（views/assessment/guide）与管理端「模拟备考管理」
 * （views/department/study/prep）共用同一套判断，避免两边各写一份导致
 * 一边支持 txt 预览、另一边提示「暂不支持」。
 *
 * 约定：
 *  - 列表项字段是 fileUrl（后端 material.file_url，形如 /profile/upload/xx.txt），
 *    预览对象里字段是 url —— 两处都要认，否则弹窗里的下载按钮会误报「暂无附件」。
 *  - 判断扩展名之前先去掉 ?query，否则带参数的文件地址永远匹配不到后缀。
 */

const IMAGE_EXT = /\.(png|jpe?g|gif|webp|bmp|svg)$/
const VIDEO_EXT = /\.(mp4|webm|ogg|ogv|mov|avi|m4v)$/
const TEXT_EXT = /\.(txt|md|markdown|json|csv|log|xml|yml|yaml|ini|conf)$/

/** 去掉 query / hash，并转小写，便于做扩展名匹配 */
export function normalizeUrl(url) {
  return String(url || '').toLowerCase().split('?')[0].split('#')[0]
}

/** 预览类型：image / video / pdf / text / other（other = 只能下载后查看） */
export function previewKindOf(url) {
  const u = normalizeUrl(url)
  if (IMAGE_EXT.test(u)) return 'image'
  if (VIDEO_EXT.test(u)) return 'video'
  if (/\.pdf$/.test(u)) return 'pdf'
  if (TEXT_EXT.test(u)) return 'text'
  return 'other'
}

/** 扩展名（大写）；取不到返回 '未知格式' */
export function extOf(url) {
  const m = normalizeUrl(url).match(/\.([a-z0-9]+)$/)
  return m ? m[1].toUpperCase() : '未知格式'
}

/** 取附件地址：列表项认 fileUrl，预览对象认 url */
export function fileUrlOf(item) {
  return (item && (item.fileUrl || item.url)) || ''
}

/** 在线预览支持清单（提示文案用） */
export const PREVIEW_SUPPORT_TEXT = '可在线预览：PDF、图片、视频、TXT / MD 等纯文本'

/** 触发浏览器下载（用 baseApi 拼前缀，走前端代理避免跨域） */
export function triggerDownload(baseApi, url, filename) {
  if (!url) return false
  const a = document.createElement('a')
  a.href = (baseApi || '') + url
  a.download = filename || ''
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  return true
}
