'use client'

import React, { useState, useEffect } from 'react'
import Sidebar from '@/components/Sidebar'
import Header from '@/components/Header'
import { UiStatus, useUiStore } from '@/stores/uiStore'
import Loading from './common/Loading'
import Error from './common/Error'
import { usePathname } from 'next/navigation'
import { ModalType, useModalStore } from '@/stores/modalStore'
import ModalInfo from './modal/ModalInfo'
import ModalConfirm from './modal/ModalConfirm'
import NotFound from './NotFound'

export default function LayoutWrapper({
  children,
}: {
  children: React.ReactNode
}) {
  const pathname = usePathname()

  const uiStore = useUiStore()
  const uiStatus = useUiStore((s) => s.status)

  const modalStore = useModalStore()
  const modalIsOpen = useModalStore((m) => m.isOpen)
  const modalType = useModalStore((m) => m.type)

  // 사이드바 열림 상태
  const [isSidebarOpen, setIsSidebarOpen] = useState(true)

  /**
   * 경로 변경 시 UI 상태 초기화
   */
  useEffect(() => {
    uiStore.reset()
    modalStore.reset()
  }, [pathname])

  /**
   * 로컬 스토리지에서 상태 로드
   */
  useEffect(() => {
    const savedState = localStorage.getItem('sidebarOpen')
    if (savedState !== null) {
      setIsSidebarOpen(savedState === 'true')
    }
  }, [])

  /**
   * 사이드바 토글 핸들러
   */
  const handleToggleSidebar = () => {
    const newState = !isSidebarOpen
    setIsSidebarOpen(newState)
    localStorage.setItem('sidebarOpen', String(newState))
  }

  /**
   * UI 상태에 따른 화면 렌더링
   * @param uiStatus UI상태
   * @returns UI 상태에 따른 화면
   */
  function renderAlert(uiStatus: UiStatus) {
    switch (uiStatus) {
      case 'loading':
        return <Loading onCancel={uiStore.handleCancel} />

      case 'error':
        return <Error onRefresh={uiStore.handleRefresh} />

      default:
        return null
    }
  }

  /**
   * 모달 상태에 따른 화면 렌더링
   * @param modalType 모달 타입
   * @returns 모달 상태에 따른 화면
   */
  function renderModal(modalType: ModalType) {
    switch (modalType) {
      case 'info':
        return <ModalInfo />

      case 'confirm':
        return <ModalConfirm />

      default:
        return null
    }
  }

  return (
    <>
      <Sidebar isOpen={isSidebarOpen} onToggle={handleToggleSidebar} />
      <div className="flex h-full w-full flex-1 flex-col overflow-hidden">
        <Header />
        <main className="relative flex-1 overflow-y-auto bg-white">
          {/* 라우팅 컴포넌트 */}
          {children}
        </main>
      </div>
      {/* UI 상태에 따른 컴포넌트 */}
      {uiStatus !== 'idle' && (
        <div className="absolute z-50 h-full w-full">
          {renderAlert(uiStatus)}
        </div>
      )}
      {/* 모달 상태에 따른 컴포넌트 */}
      {modalIsOpen && (
        <div className="absolute z-100 h-full w-full">
          {renderModal(modalType)}
        </div>
      )}
    </>
  )
}
