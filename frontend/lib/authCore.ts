import axios from 'axios'
import { config } from '@/public/ts/config'
import { useAuthStore } from '@/stores/authStore'

export const BASE_URL = `http://${config.apiHost}:${config.apiPort}${config.apiBasePath}`

export interface ReissueResponse {
  accessToken: string
  userId: string
  name: string
  menus: string[]
}

export const redirectToLogin = () => {
  if (typeof window !== 'undefined') {
    window.location.href = `${config.basePath}/login`
  }
}

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

export const reissueToken = async (): Promise<string> => {
  if (isRefreshing) {
    return new Promise<string>((resolve, reject) => {
      pendingQueue.push({ resolve, reject })
    })
  }

  isRefreshing = true
  try {
    const response = await axios.post<ReissueResponse>(
      `${BASE_URL}/auth/reissue`,
      {},
      { withCredentials: true },
    )
    const newToken = response.data.accessToken
    useAuthStore
      .getState()
      .setAuth(
        newToken,
        response.data.userId,
        response.data.name,
        response.data.menus,
      )
    processPendingQueue(newToken)
    return newToken
  } catch (err) {
    processPendingQueue(null, err)
    useAuthStore.getState().clearAuth()
    redirectToLogin()
    throw err
  } finally {
    isRefreshing = false
  }
}

export const fetchWithAuth = async (
  url: string,
  options: RequestInit = {},
): Promise<Response> => {
  const token = useAuthStore.getState().accessToken
  const headers = new Headers(options.headers)
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const res = await fetch(url, { ...options, headers })

  if (res.status === 401) {
    const newToken = await reissueToken()
    headers.set('Authorization', `Bearer ${newToken}`)
    return fetch(url, { ...options, headers })
  }

  return res
}
