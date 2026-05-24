import api from './axiosInstance'

export const dashboardApi = {
  overview:        () => api.get('/dashboard/overview').then(r => r.data.data),
  ingestion:       () => api.get('/dashboard/ingestion').then(r => r.data.data),
  synchronization: () => api.get('/dashboard/synchronization').then(r => r.data.data),
  audit:           () => api.get('/dashboard/audit').then(r => r.data.data),
}
