import api from './axiosInstance'

export const notificationsApi = {
  getAll: (params) => api.get('/notifications', { params }).then(res => res.data.data),
  getUnreadCount: () => api.get('/notifications/unread/count').then(res => res.data.data),
  markAsRead: (id) => api.patch(`/notifications/${id}/read`).then(res => res.data.data),
  markAllAsRead: () => api.patch('/notifications/read-all').then(res => res.data.data),
  delete: (id) => api.delete(`/notifications/${id}`).then(res => res.data.data),
  deleteAll: () => api.delete('/notifications/all').then(res => res.data.data),
}
