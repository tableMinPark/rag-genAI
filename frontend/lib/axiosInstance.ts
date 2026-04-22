import axios, { AxiosInstance } from 'axios'
import { useAuthStore } from '@/stores/authStore'
import { BASE_URL, reissueToken, redirectToLogin } from '@/lib/authCore'

export const axiosInstance: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  withCredentials: true,
})

axiosInstance.interceptors.request.use((cfg) => {
  const token = useAuthStore.getState().accessToken
  if (token) {
    cfg.headers.Authorization = `Bearer ${token}`
  }
  return cfg
})

axiosInstance.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config

    if (error.response?.status !== 401) {
      return Promise.reject(error)
    }

    if (originalRequest._retry) {
      useAuthStore.getState().clearAuth()
      redirectToLogin()
      return Promise.reject(error)
    }

    originalRequest._retry = true

    try {
      const newToken = await reissueToken()
      originalRequest.headers.Authorization = `Bearer ${newToken}`
      return axiosInstance(originalRequest)
    } catch (reissueError) {
      return Promise.reject(reissueError)
    }
  },
)
