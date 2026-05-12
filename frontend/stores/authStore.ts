import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface AuthState {
  accessToken: string | null
  userId: string | null
  name: string | null
  menus: string[] | null
  setAuth: (
    accessToken: string,
    userId: string,
    name: string,
    menus: string[] | null,
  ) => void
  clearAuth: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      userId: null,
      name: null,
      menus: null,
      setAuth: (accessToken, userId, name, menus) =>
        set({ accessToken, userId, name, menus }),
      clearAuth: () =>
        set({ accessToken: null, userId: null, name: null, menus: null }),
    }),
    { name: 'auth' },
  ),
)
