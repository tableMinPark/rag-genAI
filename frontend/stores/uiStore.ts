import { create } from 'zustand'

export type UiStatus = 'idle' | 'loading' | 'error'

type UiState = {
  status: UiStatus
  message?: string
  handleCancel?: () => void
  handleRefresh?: () => void

  setLoading: (message: string, handleCancel?: () => void) => void
  setError: (message: string, handleRefresh: () => void) => void
  reset: () => void
}

/**
 * UI 상태 관리 스토어
 */
export const useUiStore = create<UiState>((set, get) => ({
  status: 'idle',
  message: undefined,

  setLoading: (message, handleCancel) =>
    set({
      status: 'loading',
      message,
      handleCancel: () => {
        if (handleCancel) {
          handleCancel()
        }
        set({
          status: 'idle',
          message: undefined,
          handleCancel: undefined,
          handleRefresh: undefined,
        })
      },
      handleRefresh: undefined,
    }),
  setError: (message, handleRefresh) =>
    set({
      status: 'error',
      message,
      handleCancel: undefined,
      handleRefresh: handleRefresh,
    }),
  reset: () =>
    set({
      status: 'idle',
      message: undefined,
      handleCancel: undefined,
      handleRefresh: undefined,
    }),
}))
