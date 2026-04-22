import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface AuthState {
  accessToken: string | null
  userId: string | null
  name: string | null
  setAuth: (accessToken: string, userId: string, name: string) => void
  clearAuth: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      userId: null,
      name: null,
      setAuth: (accessToken, userId, name) => set({ accessToken, userId, name }),
      clearAuth: () => set({ accessToken: null, userId: null, name: null }),
    }),
    { name: 'auth' },
  ),
)
