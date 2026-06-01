import api from './api'

export const getInbox = () => api.get('/mail/inbox')
export const getSent = () => api.get('/mail/sent')
export const getTrash = () => api.get('/mail/trash')
export const searchInbox = (keyword) => api.get('/mail/search', { params: { q: keyword } })
export const getMailDetail = (mailId) => api.get(`/mail/${mailId}`)
export const sendMail = (payload) => api.post('/mail/send', payload)
export const markMailAsRead = (mailId) => api.post(`/mail/${mailId}/read`)
export const trashMail = (mailId) => api.delete(`/mail/${mailId}`)
export const restoreMail = (mailId) => api.put(`/mail/${mailId}/restore`)
