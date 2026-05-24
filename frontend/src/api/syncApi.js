import api from './axiosInstance'

export const syncApi = {
  run:        (dataSourceId) => api.post('/sync/run', { dataSourceId }).then(r => r.data.data),
  getAll:     (params)       => api.get('/sync/jobs', { params }).then(r => r.data.data),
  getById:    (id)           => api.get(`/sync/jobs/${id}`).then(r => r.data.data),
  getStats:   ()             => api.get('/sync/statistics').then(r => r.data.data),
  getRecent:  (limit = 10)   => api.get('/sync/jobs/recent', { params: { limit } }).then(r => r.data.data),
  getBySource:(dsId, params) => api.get(`/sync/jobs/source/${dsId}`, { params }).then(r => r.data.data),
}
