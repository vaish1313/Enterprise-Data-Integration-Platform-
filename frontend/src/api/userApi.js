import api from './axiosInstance'

export const userApi = {
  getAll:   (params) => api.get('/users', { params }).then(r => r.data.data),
  getById:  (id)     => api.get(`/users/${id}`).then(r => r.data.data),
  create:   (body)   => api.post('/users', body).then(r => r.data.data),
  update:   (id, b)  => api.put(`/users/${id}`, b).then(r => r.data.data),
  delete:   (id)     => api.delete(`/users/${id}`).then(r => r.data),
  getMe:    ()       => api.get('/users/me').then(r => r.data.data),
}
