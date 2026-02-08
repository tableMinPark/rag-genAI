import { create } from 'zustand'

export type ModalType = 'none' | 'info' | 'confirm'

type ModalState = {
  type: ModalType
  title?: string
  message?: string
  description?: string
  isOpen: boolean
  isLoading: boolean
  handleConfirm?: () => void
  handleCancel?: () => void

  closeModal: () => void
  setInfo: (
    title: string,
    message: string,
    description: string,
    handleConfirm?: () => void,
  ) => void
  setError: (
    title: string,
    message: string,
    description: string,
    handleConfirm?: () => void,
  ) => void
  setConfirm: (
    title: string,
    message: string,
    description: string,
    handleConfirm?: () => void,
    handleCancel?: () => void,
  ) => void
  reset: () => void
}

/**
 * Modal 상태 관리 스토어
 */
export const useModalStore = create<ModalState>((set) => ({
  type: 'none',
  title: undefined,
  message: undefined,
  description: undefined,
  isOpen: false,
  isLoading: false,
  // 모달 닫기
  closeModal: () => set({ isOpen: false }),
  // 정보 모달 설정
  setInfo: (title, message, description, handleConfirm = () => {}) =>
    set({
      type: 'info',
      title,
      message,
      description,
      isOpen: true,
      handleConfirm: async () => {
        set({ isLoading: true })
        await handleConfirm()
        set({ isLoading: false, isOpen: false })
      },
      handleCancel: () => set({ isOpen: false }),
    }),
  // 정보 모달 설정
  setError: (title, message, description, handleConfirm = () => {}) =>
    set({
      type: 'info',
      title,
      message,
      description,
      isOpen: true,
      handleConfirm: async () => {
        set({ isLoading: true })
        await handleConfirm()
        set({ isLoading: false, isOpen: false })
      },
      handleCancel: () => set({ isOpen: false }),
    }),
  // 확인 모달 설정
  setConfirm: (
    title,
    message,
    description,
    handleConfirm = () => {},
    handleCancel = () => {},
  ) =>
    set({
      type: 'confirm',
      description,
      title,
      message,
      isOpen: true,
      handleConfirm: async () => {
        set({ isLoading: true })
        await handleConfirm()
        set({ isLoading: false, isOpen: false })
      },
      handleCancel: async () => {
        await handleCancel()
        set({ isOpen: false })
      },
    }),
  // 모달 상태 초기화
  reset: () =>
    set({
      type: 'none',
      title: undefined,
      message: undefined,
      description: undefined,
      isOpen: false,
      isLoading: false,
      handleConfirm: undefined,
      handleCancel: undefined,
    }),
}))
