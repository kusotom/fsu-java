import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

export default request
