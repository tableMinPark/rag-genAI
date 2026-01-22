import { create } from 'zustand'

export type UIStatus = 'idle' | 'loading' | 'error' | 'success'

type UIState = {
  status: UIStatus
  message?: string
  modalOpen: boolean

  setLoading: (message?: string) => void
  setError: (message: string) => void
  setSuccess: (message?: string) => void
  reset: () => void
}

export const useUIStore = create<UIState>((set) => ({
  status: 'idle',
  message: undefined,
  modalOpen: false,

  setLoading: (message) => set({ status: 'loading', message, modalOpen: true }),

  setError: (message) => set({ status: 'error', message, modalOpen: true }),

  setSuccess: (message) => set({ status: 'success', message, modalOpen: true }),

  reset: () => set({ status: 'idle', message: undefined, modalOpen: false }),
}))
