import api from './axiosInstance'

export const transformationApi = {
  getAll:    (params) => api.get('/transformation-rules', { params }).then(r => r.data.data),
  getById:   (id)     => api.get(`/transformation-rules/${id}`).then(r => r.data.data),
  create:    (body)   => api.post('/transformation-rules', body).then(r => r.data.data),
  update:    (id, b)  => api.put(`/transformation-rules/${id}`, b).then(r => r.data.data),
  delete:    (id)     => api.delete(`/transformation-rules/${id}`).then(r => r.data),
  applyToJob:(jobId)  => api.post(`/transformation-rules/apply/${jobId}`).then(r => r.data.data),
}
