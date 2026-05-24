import api from './axiosInstance'

export const ingestionApi = {
  getAll:     (params) => api.get('/ingestion/jobs', { params }).then(r => r.data.data),
  getById:    (id)     => api.get(`/ingestion/jobs/${id}`).then(r => r.data.data),
  getRecords: (id, p)  => api.get(`/ingestion/jobs/${id}/records`, { params: p }).then(r => r.data.data),
  uploadCsv:  (dsId, file) => {
    const form = new FormData()
    form.append('file', file)
    return api.post(`/ingestion/upload/${dsId}`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then(r => r.data.data)
  },
  triggerApi: (dsId)   => api.post(`/ingestion/trigger/${dsId}`).then(r => r.data.data),
}
