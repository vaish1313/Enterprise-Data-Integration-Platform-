import api from './axiosInstance'

export const auditApi = {
  getAll:       (params) => api.get('/audit', { params }).then(r => r.data.data),
  getByUser:    (u, p)   => api.get(`/audit/user/${u}`, { params: p }).then(r => r.data.data),
  getByAction:  (a, p)   => api.get(`/audit/action/${a}`, { params: p }).then(r => r.data.data),
  exportCsv:    ()       => api.get('/audit/export', { responseType: 'blob' }).then(r => r.data),
  deleteById:   (id)     => api.delete(`/audit/${id}`).then(r => r.data),
  purge:        (days)   => api.delete(`/audit/purge?days=${days}`).then(r => r.data),
}
