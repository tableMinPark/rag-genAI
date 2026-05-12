import axios from 'axios'
import { config } from '@/public/ts/config'

const BASE_URL = `http://${config.apiHost}:${config.apiPort}${config.apiBasePath}`

export interface LoginRequest {
  userId: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  userId: string
  name: string
  menus: string[]
}

export interface RegisterRequest {
  userId: string
  password: string
  name: string
  email: string
}

export const loginApi = async (
  request: LoginRequest,
): Promise<LoginResponse> => {
  const response = await axios.post<LoginResponse>(
    `${BASE_URL}/auth/login`,
    request,
    {
      withCredentials: true,
    },
  )
  return response.data
}

export const registerApi = async (request: RegisterRequest): Promise<void> => {
  await axios.post(`${BASE_URL}/auth/register`, request)
}

export const logoutApi = async (): Promise<void> => {
  await axios.post(`${BASE_URL}/auth/logout`, {}, { withCredentials: true })
}
