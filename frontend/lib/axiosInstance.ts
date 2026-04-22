import axios, { AxiosInstance } from 'axios'
import { config } from '@/public/ts/config'
import { useAuthStore } from '@/stores/authStore'

const BASE_URL = `http://${config.apiHost}:${config.apiPort}${config.apiBasePath}`

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

let isRefreshing = false
let pendingQueue: Array<{
  resolve: (token: string) => void
  reject: (err: unknown) => void
}> = []

const processPendingQueue = (token: string | null, error: unknown = null) => {
  pendingQueue.forEach(({ resolve, reject }) => {
    if (token) resolve(token)
    else reject(error)
  })
  pendingQueue = []
}

axiosInstance.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config

    if (error.response?.status !== 401 || originalRequest._retry) {
      return Promise.reject(error)
    }

    if (originalRequest._retry) {
      useAuthStore.getState().clearAuth()
      window.location.href = `${config.basePath}/login`
      return Promise.reject(error)
    }

    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        pendingQueue.push({ resolve, reject })
      }).then((token) => {
        originalRequest.headers.Authorization = `Bearer ${token}`
        return axiosInstance(originalRequest)
      })
    }

    originalRequest._retry = true
    isRefreshing = true

    try {
      const response = await axios.post<{ accessToken: string }>(
        `${BASE_URL}/auth/reissue`,
        {},
        { withCredentials: true },
      )
      const newToken = response.data.accessToken
      useAuthStore
        .getState()
        .setAuth(
          newToken,
          useAuthStore.getState().userId ?? '',
          useAuthStore.getState().name ?? '',
        )
      processPendingQueue(newToken)
      originalRequest.headers.Authorization = `Bearer ${newToken}`
      return axiosInstance(originalRequest)
    } catch (reissueError) {
      processPendingQueue(null, reissueError)
      useAuthStore.getState().clearAuth()
      if (typeof window !== 'undefined') {
        window.location.href = '/login'
      }
      window.location.href = `${config.basePath}/login`
      return Promise.reject(reissueError)
    } finally {
      isRefreshing = false
    }
  },
)
