import { Message, MessageBox, Notification, Loading } from 'element-ui'

let loadingInstance;

export default {
  // 消息提示
  msg(content) {
    Message.info(content)
  },
  // 错误消息
  msgError(content) {
    Message.error(content)
  },
  // 成功消息
  msgSuccess(content) {
    Message.success(content)
  },
  // 警告消息
  msgWarning(content) {
    Message.warning(content)
  },
  // 弹出提示
  // ⚠ 必须 return：MessageBox.alert 返回 Promise，调用方常写 `$modal.alert(x).then/catch(...)`。
  // 不 return 会得到 undefined，链式调用直接抛
  // `TypeError: Cannot read properties of undefined (reading 'catch')`，
  // 且异常发生在异步回调里会被静默吞掉，后续代码全部不执行（倒计时自动交卷就这样失效过）。
  alert(content) {
    return MessageBox.alert(content, "系统提示")
  },
  // 错误提示
  alertError(content) {
    return MessageBox.alert(content, "系统提示", { type: 'error' })
  },
  // 成功提示
  alertSuccess(content) {
    return MessageBox.alert(content, "系统提示", { type: 'success' })
  },
  // 警告提示
  alertWarning(content) {
    return MessageBox.alert(content, "系统提示", { type: 'warning' })
  },
  /**
   * 只有一个「确认」按钮的提示框（返回 Promise，点确认后 resolve）。
   * 用于「倒计时到点 → 告知将自动提交 → 确认后继续下一步」这类不需要取消的流程。
   * showClose:false + 屏蔽遮罩点击 / Esc —— 保证「确认」是唯一出口：
   * 否则用户点掉 X 会 reject，拒绝被 catch 吞掉后停在 0 秒答题页上无法自愈。
   */
  confirmOnly(content, confirmButtonText, options) {
    return MessageBox.confirm(content, "系统提示", Object.assign({
      confirmButtonText: confirmButtonText || '确认',
      showCancelButton: false,
      showClose: false,
      closeOnClickModal: false,
      closeOnPressEscape: false,
      type: 'warning'
    }, options || {}))
  },
  // 通知提示
  notify(content) {
    Notification.info(content)
  },
  // 错误通知
  notifyError(content) {
    Notification.error(content);
  },
  // 成功通知
  notifySuccess(content) {
    Notification.success(content)
  },
  // 警告通知
  notifyWarning(content) {
    Notification.warning(content)
  },
  // 确认窗体
  confirm(content) {
    return MessageBox.confirm(content, "系统提示", {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: "warning",
    })
  },
  // 提交内容
  prompt(content) {
    return MessageBox.prompt(content, "系统提示", {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: "warning",
    })
  },
  // 打开遮罩层
  loading(content) {
    loadingInstance = Loading.service({
      lock: true,
      text: content,
      spinner: "el-icon-loading",
      background: "rgba(0, 0, 0, 0.7)",
    })
  },
  // 关闭遮罩层
  closeLoading() {
    loadingInstance.close();
  }
}
