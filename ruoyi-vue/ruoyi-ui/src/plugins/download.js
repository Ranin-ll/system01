import axios from 'axios'
import {Loading, Message} from 'element-ui'
import { saveAs } from 'file-saver'
import { getToken } from '@/utils/auth'
import errorCode from '@/utils/errorCode'
import { blobValidate } from "@/utils/ruoyi";

const baseURL = process.env.VUE_APP_BASE_API
let downloadLoadingInstance;

export default {
  name(name, isDelete = true) {
    var url = baseURL + "/common/download?fileName=" + encodeURIComponent(name) + "&delete=" + isDelete
    axios({
      method: 'get',
      url: url,
      responseType: 'blob',
      headers: { 'Authorization': 'Bearer ' + getToken() }
    }).then((res) => {
      const isBlob = blobValidate(res.data);
      if (isBlob) {
        const blob = new Blob([res.data])
        this.saveAs(blob, decodeURIComponent(res.headers['download-filename']))
      } else {
        this.printErrMsg(res.data);
      }
    })
  },
  /**
   * 下载数据库里登记的资源（走 /common/download/resource）。
   *
   * @param resource     资源地址（content_url，形如 /profile/upload/...）
   * @param fallbackName 兜底文件名：响应头缺失时用它，避免存出名叫 undefined 的空文件
   */
  resource(resource, fallbackName) {
    var url = baseURL + "/common/download/resource?resource=" + encodeURIComponent(resource);
    axios({
      method: 'get',
      url: url,
      responseType: 'blob',
      headers: { 'Authorization': 'Bearer ' + getToken() }
    }).then((res) => {
      const isBlob = blobValidate(res.data);
      if (!isBlob) return this.printErrMsg(res.data);
      // 后端下载失败时可能回「200 + 空 body」（旧实现会把异常吞掉），
      // 不拦的话 blobValidate 会当成文件保存 → 用户得到一个 0 字节、无扩展名的空文件。
      if (!res.data || res.data.size === 0) {
        Message.error('下载失败：服务端没有返回文件内容（文件可能已丢失，或该格式不允许下载）');
        return;
      }
      const headerName = res.headers && res.headers['download-filename'];
      let name = fallbackName || '下载文件';
      if (headerName) {
        // 文件名是 percent-encoded 的，含中文/空格时格外要注意解码失败的情况
        try { name = decodeURIComponent(headerName) } catch (e) { name = headerName }
      }
      this.saveAs(new Blob([res.data], { type: 'application/octet-stream' }), name);
    }).catch((error) => {
      console.error(error);
      Message.error('下载文件出现错误，请联系管理员！');
    })
  },
  zip(url, name) {
    var url = baseURL + url
    downloadLoadingInstance = Loading.service({ text: "正在下载数据，请稍候", spinner: "el-icon-loading", background: "rgba(0, 0, 0, 0.7)", })
    axios({
      method: 'get',
      url: url,
      responseType: 'blob',
      headers: { 'Authorization': 'Bearer ' + getToken() }
    }).then((res) => {
      const isBlob = blobValidate(res.data);
      if (isBlob) {
        const blob = new Blob([res.data], { type: 'application/zip' })
        this.saveAs(blob, name)
      } else {
        this.printErrMsg(res.data);
      }
      downloadLoadingInstance.close();
    }).catch((r) => {
      console.error(r)
      Message.error('下载文件出现错误，请联系管理员！')
      downloadLoadingInstance.close();
    })
  },
  saveAs(text, name, opts) {
    saveAs(text, name, opts);
  },
  async printErrMsg(data) {
    const resText = await data.text();
    const rspObj = JSON.parse(resText);
    const errMsg = errorCode[rspObj.code] || rspObj.msg || errorCode['default']
    Message.error(errMsg);
  }
}

