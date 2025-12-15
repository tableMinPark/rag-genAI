'use client'

import React, { useState, useEffect } from 'react'
import Sidebar from '@/components/Sidebar'
import Header from '@/components/Header'

export default function LayoutWrapper({
  children,
}: {
  children: React.ReactNode
}) {
  // 기본값은 true로 시작하되, 마운트 후 로컬스토리지 값을 반영
  const [isSidebarOpen, setIsSidebarOpen] = useState(true)

  useEffect(() => {
    // 로컬 스토리지에서 상태 불러오기
    const savedState = localStorage.getItem('sidebarOpen')
    if (savedState !== null) {
      setIsSidebarOpen(savedState === 'true')
    }
  }, [])

  const toggleSidebar = () => {
    const newState = !isSidebarOpen
    setIsSidebarOpen(newState)
    // 상태 변경 시 로컬 스토리지에 저장
    localStorage.setItem('sidebarOpen', String(newState))
  }

  return (
    <>
      <Sidebar isOpen={isSidebarOpen} onToggle={toggleSidebar} />
      <div className="flex h-full w-full flex-1 flex-col overflow-hidden">
        <Header />
        <main className="relative flex-1 overflow-y-auto bg-white p-4">
          {children}
        </main>
      </div>
    </>
  )
}
