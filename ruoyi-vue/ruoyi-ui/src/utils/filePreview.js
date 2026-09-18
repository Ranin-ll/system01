/**
 * 文件预览判定 —— 组件与页面共用，避免「哪些格式能预览」在两处各写一份。
 *
 * 浏览器能直接内联渲染 pdf / txt / 图片；docx 与 pptx 靠前端库渲染
 * （依赖 `docx-preview` / `pptx-preview`，见 package.json）。
 * 其余格式（zip/xls/csv/md…）前端不解析，只能下载。
 */
const IMAGE_EXTS = ['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp']
const INLINE_EXTS = ['pdf', 'txt']

/** 取扩展名（小写，不带点）。文件名优先，回退到 URL 的路径部分 */
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
 * 预览方式：
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
 * <p>docx / pptx 虽然能在线预览，但那是前端库渲染的结果 —— 浏览器本身不会显示它们，
 * 所以对它们「在新窗口打开」实际效果就是**下载**。界面上据此区分按钮文案：
 * 能原生显示的叫「新窗口」，其余的叫「下载」，别让文案和行为不一致。</p>
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
