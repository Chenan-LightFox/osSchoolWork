import api from './api'

/** 上传附件到指定邮件（草稿或已发送） */
export const uploadAttachment = (mailId, file) => {
  const formData = new FormData()
  formData.append('mailId', mailId)
  formData.append('file', file)
  return api.post('/attachment/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** 下载附件，返回下载 URL 可直接用于 <a> 或 window.open */
export const getDownloadUrl = (attachmentId) => {
  const token = api.defaults.headers.Authorization
    || api.defaults.headers.common?.Authorization
  // 通过 axios 拦截器自动带 token，直接返回 api baseURL + 路径
  return `/api/attachment/download/${attachmentId}`
}

/** 删除单个附件 */
export const deleteAttachment = (attachmentId) =>
  api.delete(`/attachment/${attachmentId}`)

/** 列出某邮件的所有附件 */
export const listAttachments = (mailId) =>
  api.get(`/attachment/list/${mailId}`)
