import api from './api'
import { getToken } from '@/stores/auth'

export const getInbox = () => api.get('/mail/inbox')
export const getSent = () => api.get('/mail/sent')
export const getTrash = () => api.get('/mail/trash')
export const getDrafts = () => api.get('/mail/drafts')
export const searchInbox = (keyword) => api.get('/mail/search', { params: { q: keyword } })
export const getMailDetail = (mailId) => api.get(`/mail/${mailId}`)
export const sendMail = (payload) => api.post('/mail/send', payload)
export const saveDraft = (payload, draftId) => api.post('/mail/draft', payload, { params: draftId ? { draftId } : {} })
export const sendDraft = (draftId, payload) => api.post(`/mail/draft/${draftId}/send`, payload)
export const deleteDraft = (draftId) => api.delete(`/mail/draft/${draftId}`)
export const markMailAsRead = (mailId) => api.post(`/mail/${mailId}/read`)
export const trashMail = (mailId) => api.delete(`/mail/${mailId}`)
export const restoreMail = (mailId) => api.put(`/mail/${mailId}/restore`)
export const permanentDelete = (mailId) => api.delete(`/mail/${mailId}/permanent`)

/** 带附件发送邮件（multipart/form-data） */
export const sendMailWithAttachments = (payload, files) => {
  const formData = new FormData()
  formData.append('payload', new Blob([JSON.stringify(payload)], { type: 'application/json' }))
  for (const file of files) {
    formData.append('files', file)
  }
  // 不手动设置 Content-Type，让浏览器自动附加 boundary 参数
  return api.post('/mail/send', formData)
}

/** 下载附件（返回 Blob + 自动触发下载） */
export const downloadAttachment = async (attachmentId, fileName) => {
  try {
    const blob = await api.get(`/attachment/download/${attachmentId}`, { responseType: 'blob' })
    // 检查返回的是否是 JSON 错误（部分后端错误可能返回 JSON 而非 blob）
    if (blob.type === 'application/json') {
      const text = await blob.text()
      let errMsg = '下载失败'
      try {
        const parsed = JSON.parse(text)
        errMsg = parsed.msg || parsed.message || errMsg
      } catch { /* ignore parse error */ }
      throw new Error(errMsg)
    }
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = fileName || 'attachment'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch (error) {
    // 尝试从 blob 类型的错误响应中提取消息
    if (error.response?.data instanceof Blob) {
      try {
        const text = await error.response.data.text()
        const parsed = JSON.parse(text)
        throw new Error(parsed.msg || parsed.message || '下载失败')
      } catch (parseErr) {
        if (parseErr.message && parseErr.message !== '下载失败') throw parseErr
      }
    }
    throw error
  }
}
