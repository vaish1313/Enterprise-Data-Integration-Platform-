import api from './axiosInstance'

export const authApi = {
  login:   (body)  => api.post('/auth/login',   body).then(r => r.data.data),
  register:(body)  => api.post('/auth/register', body).then(r => r.data.data),
  refresh: (token) => api.post('/auth/refresh',  { refreshToken: token }).then(r => r.data.data),
  logout:  ()      => api.post('/auth/logout').catch(() => {}),
}
