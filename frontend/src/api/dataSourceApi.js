import api from './axiosInstance'

export const dataSourceApi = {
  getAll:   (params) => api.get('/data-sources', { params }).then(r => r.data.data),
  getById:  (id)     => api.get(`/data-sources/${id}`).then(r => r.data.data),
  create:   (body)   => api.post('/data-sources', body).then(r => r.data.data),
  update:   (id, b)  => api.put(`/data-sources/${id}`, b).then(r => r.data.data),
  delete:   (id)     => api.delete(`/data-sources/${id}`).then(r => r.data),
  activate: (id)     => api.patch(`/data-sources/${id}/activate`).then(r => r.data.data),
  search:   (params) => api.get('/data-sources/search', { params }).then(r => r.data.data),
}
