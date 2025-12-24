import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 5000
})

request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`
  }
  return config
}, error => {
  return Promise.reject(error)
})

request.interceptors.response.use(response => {
  // If it's a blob, return the response directly
  if (response.config.responseType === 'blob') {
    return response
  }

  const res = response.data
  // Backend result structure: { code: 200, message: "Success", data: ... }
  if (res.code && res.code !== 200) {
    return Promise.reject(new Error(res.message || 'Error'))
  }
  return res
}, error => {
  return Promise.reject(error)
})

export default request
