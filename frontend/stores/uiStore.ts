import { create } from 'zustand'

export type UiStatus = 'idle' | 'loading' | 'error'

type UiState = {
  status: UiStatus
  message?: string
  handleRefresh?: () => void

  setLoading: (message?: string) => void
  setError: (message: string, handleRefresh: () => void) => void
  reset: () => void
}

/**
 * UI 상태 관리 스토어
 */
export const useUiStore = create<UiState>((set) => ({
  status: 'idle',
  message: undefined,

  setLoading: (message) =>
    set({ status: 'loading', message, handleRefresh: undefined }),
  setError: (message, handleRefresh) =>
    set({ status: 'error', message, handleRefresh: handleRefresh }),
  reset: () =>
    set({ status: 'idle', message: undefined, handleRefresh: undefined }),
}))
