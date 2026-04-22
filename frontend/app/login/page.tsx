'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import Link from 'next/link'
import { loginApi } from '@/api/auth'
import { useAuthStore } from '@/stores/authStore'

export default function LoginPage() {
  const router = useRouter()
  const { setAuth } = useAuthStore()
  const [userId, setUserId] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const data = await loginApi({ userId, password })
      setAuth(data.accessToken, data.userId, data.name)
      router.replace('/')
    } catch {
      setError('아이디 또는 비밀번호가 올바르지 않습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex h-screen w-screen overflow-hidden">
      {/* 왼쪽 브랜드 패널 */}
      <div
        className="hidden lg:flex lg:w-1/2 flex-col justify-between p-16 relative"
        style={{ backgroundColor: '#c64f4f' }}
      >
        {/* 배경 장식 원 */}
        <div
          className="absolute top-[-80px] right-[-80px] w-72 h-72 rounded-full opacity-20"
          style={{ backgroundColor: '#fff' }}
        />
        <div
          className="absolute bottom-[-60px] left-[-60px] w-96 h-96 rounded-full opacity-10"
          style={{ backgroundColor: '#fff' }}
        />
        <div
          className="absolute bottom-40 right-16 w-32 h-32 rounded-full opacity-15"
          style={{ backgroundColor: '#fff' }}
        />

        {/* 로고 */}
        <div className="relative z-10">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-white/20 flex items-center justify-center backdrop-blur-sm">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                <path
                  d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"
                  stroke="white"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
            </div>
            <span className="text-white font-bold text-xl tracking-tight">RAG GenAI</span>
          </div>
        </div>

        {/* 중앙 카피 */}
        <div className="relative z-10">
          <h2 className="text-white text-4xl font-bold leading-tight mb-4">
            지식을 연결하고
            <br />
            AI로 답을 찾다
          </h2>
          <p className="text-white/70 text-base leading-relaxed max-w-xs">
            문서 기반 AI 검색 플랫폼으로 더 빠르고 정확한 인사이트를 경험하세요.
          </p>
        </div>

        {/* 하단 장식 */}
        <div className="relative z-10 flex gap-2">
          <div className="w-8 h-1 rounded-full bg-white/60" />
          <div className="w-2 h-1 rounded-full bg-white/30" />
          <div className="w-2 h-1 rounded-full bg-white/30" />
        </div>
      </div>

      {/* 오른쪽 로그인 폼 */}
      <div className="flex-1 flex items-center justify-center px-8 bg-gray-50">
        <div className="w-full max-w-sm">
          {/* 모바일용 로고 */}
          <div className="lg:hidden flex items-center gap-2 mb-10">
            <div
              className="w-8 h-8 rounded-lg flex items-center justify-center"
              style={{ backgroundColor: '#c64f4f' }}
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                <path
                  d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"
                  stroke="white"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
            </div>
            <span className="font-bold text-lg text-gray-900">RAG GenAI</span>
          </div>

          <h1 className="text-2xl font-bold text-gray-900 mb-1">다시 오셨군요</h1>
          <p className="text-sm text-gray-500 mb-8">계정에 로그인하세요</p>

          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1.5">아이디</label>
              <input
                type="text"
                placeholder="아이디를 입력하세요"
                value={userId}
                onChange={(e) => setUserId(e.target.value)}
                required
                className="w-full rounded-xl border border-gray-200 bg-white px-4 py-3 text-sm text-gray-900 placeholder:text-gray-400 outline-none transition-all focus:border-[#c64f4f] focus:ring-3 focus:ring-[#c64f4f]/10"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1.5">비밀번호</label>
              <input
                type="password"
                placeholder="비밀번호를 입력하세요"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                className="w-full rounded-xl border border-gray-200 bg-white px-4 py-3 text-sm text-gray-900 placeholder:text-gray-400 outline-none transition-all focus:border-[#c64f4f] focus:ring-3 focus:ring-[#c64f4f]/10"
              />
            </div>

            {error && (
              <div className="flex items-center gap-2 rounded-lg bg-red-50 px-3 py-2.5">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" className="shrink-0">
                  <circle cx="12" cy="12" r="10" stroke="#ef4444" strokeWidth="2" />
                  <line x1="12" y1="8" x2="12" y2="12" stroke="#ef4444" strokeWidth="2" strokeLinecap="round" />
                  <circle cx="12" cy="16" r="1" fill="#ef4444" />
                </svg>
                <p className="text-xs text-red-600">{error}</p>
              </div>
            )}

            <button
              type="submit"
              disabled={loading}
              className="mt-2 w-full rounded-xl py-3 text-sm font-semibold text-white transition-all disabled:opacity-60 active:scale-[0.98]"
              style={{ backgroundColor: loading ? '#c64f4f99' : '#c64f4f' }}
              onMouseEnter={(e) => { if (!loading) e.currentTarget.style.backgroundColor = '#a54242' }}
              onMouseLeave={(e) => { if (!loading) e.currentTarget.style.backgroundColor = '#c64f4f' }}
            >
              {loading ? '로그인 중...' : '로그인'}
            </button>
          </form>

          <p className="mt-6 text-center text-sm text-gray-500">
            계정이 없으신가요?{' '}
            <Link
              href="/register"
              className="font-semibold transition-colors"
              style={{ color: '#c64f4f' }}
            >
              회원가입
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
