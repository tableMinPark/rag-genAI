'use client'

import { useAuthStore } from '@/stores/authStore'
import { useRouter } from 'next/navigation'
import { logoutApi } from '@/api/auth'
import { LogOut } from 'lucide-react'

export default function Header() {
  const { name, clearAuth } = useAuthStore()
  const router = useRouter()

  const handleLogout = async () => {
    try {
      await logoutApi()
    } catch {
      // 쿠키 삭제 실패해도 클라이언트 상태는 초기화
    }
    clearAuth()
    router.replace('/login')
  }

  return (
    <header className="bg-primary z-10 flex h-10 items-center justify-between px-4 text-white shadow-md">
      <div className="flex items-center" />
      {name && (
        <div className="flex items-center gap-3">
          <span className="text-sm font-medium">{name}</span>
          <button
            onClick={handleLogout}
            className="flex items-center gap-1 rounded-md px-2 py-1 text-xs text-white/80 hover:bg-white/20 transition"
          >
            <LogOut className="h-3.5 w-3.5" />
            로그아웃
          </button>
        </div>
      )}
    </header>
  )
}
