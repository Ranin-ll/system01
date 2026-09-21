/**
 * 文件预览 / 下载 公共逻辑
 * ---------------------------------------------------------------------------
 * 【本文档是「合并冲突解决方案」，不是仓库里的文件】
 *
 * 冲突来源：`feature/exam-module`（同事）与 `main`（含超管 P1）**各自新增**了
 * `ruoyi-vue/ruoyi-ui/src/utils/filePreview.js`（git 报 add/add 冲突）。
 * 两边是**两套独立实现**，引用方完全不重叠 —— 所以正确做法是**取并集**，
 * 而不是二选一。
 *
 * ⚠️ 唯一的真实冲突点：两边都导出 `extOf`，但**契约不同**
 *    · main 侧：`extOf(fileName, url)` → 小写不带点（'pdf'）；取不到 → ''
 *    · 分支侧：`extOf(url)`        → 大写（'PDF'）；取不到 → '未知格式'
 *    ⇒ 既不能"都留"（duplicate export → 语法报错），
 *      也不能"留一个"（另一个的调用方会静默拿到语义不同的值）。
 *    ⇒ **解法：把分支侧那个改名为 `extLabel`** —— 它返回的其实是「给人看的格式标签」
 *      （取不到时给 '未知格式'），不是"用于匹配的扩展名"。改名后语义各自归位。
 *
 * 合并后需要同步改名的调用点（只有 2 处，都是分支侧新增页面）：
 *   1. ruoyi-vue/ruoyi-ui/src/views/assessment/guide/index.vue
 *   2. ruoyi-vue/ruoyi-ui/src/views/department/study/prep/index.vue
 *      —— 两处 import 形如：
 *         import { previewKindOf, extOf, fileUrlOf as resolveFileUrl,
 *                  PREVIEW_SUPPORT_TEXT, triggerDownload } from '@/utils/filePreview'
 *      把其中的 `extOf` 改成 `extLabel` 即可（`as resolveFileUrl` 的别名保持不变）。
 *      ⚠️ 页面模板里如果有 `extOf(x)` 的调用，也要一并改成 `extLabel(x)`。
 *
 * 另外注意：两边各有一套「预览类型」判定，名字不同但**分类体系不同**，暂时共存：
 *   · `previewKind(fileName, url)`  → 'inline' | 'docx' | 'pptx' | 'image' | 'none'
 *        —— 回答「用哪个渲染器」（main 侧 FilePreview 组件用）
 *   · `previewKindOf(url)`          → 'image' | 'video' | 'pdf' | 'text' | 'other'
 *        —— 回答「浏览器能不能直接看」（分支侧备考资料页用）
 *   ⇒ 合并期**先共存**（保留各自调用方），后续可统一为一种；但别在合并时顺手重构，
 *     那会把"合并冲突"和"重构"两件事混在一起，出问题难定位。
 */

/* =========================================================================
 * 第一部分：来自 main 侧（超管 P1）—— 供 FilePreview / TaskAttachments / review 用
 * ========================================================================= */

const IMAGE_EXTS = ['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp']
const INLINE_EXTS = ['pdf', 'txt']

/** 取扩展名（**小写、不带点**）。文件名优先，回退到 URL 的路径部分；取不到返回 '' */
export function extOf(fileName, url) {
  let source = String(fileName || '').trim()
  if (!source && url) {
    source = String(url).split('?')[0].split('#')[0]
  }
  const base = source.split(/[\\/]/).pop() || ''
  const dot = base.lastIndexOf('.')
  return dot > -1 ? base.slice(dot + 1).toLowerCase() : ''
}

/**
 * 预览方式（决定用哪个渲染器）：
 *  `inline` 浏览器内联（iframe） / `docx` / `pptx` / `image` / `none`（不支持预览，只能下载）
 */
export function previewKind(fileName, url) {
  const ext = extOf(fileName, url)
  if (!ext) return 'none'
  if (INLINE_EXTS.indexOf(ext) > -1) return 'inline'
  if (ext === 'docx') return 'docx'
  if (ext === 'pptx') return 'pptx'
  if (IMAGE_EXTS.indexOf(ext) > -1) return 'image'
  return 'none'
}

export function canPreview(fileName, url) {
  return previewKind(fileName, url) !== 'none'
}

/**
 * 浏览器能否**自己**在新标签页里显示这个文件（pdf / txt / 图片）。
 *
 * docx / pptx 虽然能在线预览，但那是前端库渲染的结果 —— 浏览器本身不会显示它们，
 * 所以对它们「在新窗口打开」实际效果就是**下载**。界面上据此区分按钮文案：
 * 能原生显示的叫「新窗口」，其余的叫「下载」，别让文案和行为不一致。
 */
export function nativeOpen(fileName, url) {
  const kind = previewKind(fileName, url)
  return kind === 'inline' || kind === 'image'
}

/** 给用户的提示：为什么不能预览 */
export function previewHint(fileName, url) {
  const ext = extOf(fileName, url)
  if (!ext) return '无法识别文件类型，请下载后查看'
  return `「${ext.toUpperCase()}」暂不支持在线预览，请下载后查看`
}

/* =========================================================================
 * 第二部分：来自 feature/exam-module（同事）—— 供备考资料 / 模拟备考管理 用
 * 实习生端「备考资料」(views/assessment/guide) 与管理端「模拟备考管理」
 * (views/department/study/prep) 共用同一套判断。
 *
 * 约定：
 *  - 列表项字段是 fileUrl（后端 material.file_url，形如 /profile/upload/xx.txt），
 *    预览对象里字段是 url —— 两处都要认，否则弹窗里的下载按钮会误报「暂无附件」。
 *  - 判断扩展名之前先去掉 ?query，否则带参数的文件地址永远匹配不到后缀。
 * ========================================================================= */

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

/**
 * 扩展名**标签**（大写，给人看）；取不到返回 '未知格式'。
 *
 * ⚠️ 本函数在合并前叫 `extOf`（与上方 main 侧的 `extOf` 同名但契约不同）——
 *    合并时已改名，**调用点需同步改为 `extLabel`**（见文件头「合并后需要同步改名的调用点」）。
 */
export function extLabel(url) {
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
Ln 3, Col 1
行 151
JavaScript
只读