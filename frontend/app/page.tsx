'use client'

import React, { useState } from 'react'
import { useRouter } from 'next/navigation' // 라우터 훅
import { Send, Sparkles, MessageSquare } from 'lucide-react'

export default function ChatMainPage() {
  const router = useRouter()
  const [input, setInput] = useState('')

  // [수정됨] 검색/채팅 시작 핸들러
  const handleSearch = (e?: React.FormEvent) => {
    e?.preventDefault()
    if (!input.trim()) return

    // 입력값을 URL 인코딩하여 쿼리 스트링으로 전달
    const query = encodeURIComponent(input.trim())

    // /ai 경로로 이동 (예: /ai?q=안녕하세요)
    router.push(`/ai?query=${query}`)
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    // 한글 입력 중 조합(Composing) 이슈 방지 및 엔터키 처리
    if (e.key === 'Enter' && !e.nativeEvent.isComposing) {
      handleSearch()
    }
  }

  return (
    <div className="flex min-h-screen w-full flex-col items-center justify-center bg-gray-50 p-6">
      {/* 메인 컨텐츠 래퍼 */}
      <div className="animate-in fade-in slide-in-from-bottom-4 flex w-full max-w-3xl flex-col items-center gap-10 duration-700">
        {/* 타이틀 영역 */}
        <div className="flex flex-col items-center gap-4 text-center">
          <div className="flex h-16 w-16 items-center justify-center rounded-3xl bg-white shadow-lg ring-1 ring-gray-100">
            <Sparkles className="text-primary fill-primary/20 h-8 w-8" />
          </div>
          <h1 className="text-4xl font-extrabold tracking-tight text-gray-900 sm:text-5xl">
            무엇을 도와드릴까요?
          </h1>
          <p className="text-lg text-gray-500">
            RAG 기반의 AI가 사내 문서를 분석하여 정확한 답변을 제공합니다.
          </p>
        </div>

        {/* 검색/입력바 영역 */}
        <div className="w-full">
          <div className="group relative flex items-center">
            <div className="absolute left-6 text-gray-400">
              <MessageSquare className="h-6 w-6" />
            </div>

            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="궁금한 내용을 입력하거나 문서를 검색해 보세요..."
              className="focus:border-primary/50 focus:ring-primary/10 h-20 w-full rounded-full border border-gray-200 bg-white pr-16 pl-16 text-lg text-gray-800 shadow-xl shadow-gray-200/60 transition-all outline-none placeholder:text-gray-400 hover:border-gray-300 hover:shadow-2xl focus:ring-4"
              autoFocus
            />

            <button
              onClick={() => handleSearch()}
              disabled={!input.trim()}
              className="bg-primary hover:bg-primary-hover absolute right-3 flex h-14 w-14 items-center justify-center rounded-full text-white shadow-md transition-all hover:scale-105 active:scale-95 disabled:cursor-not-allowed disabled:bg-gray-200 disabled:text-gray-400 disabled:hover:scale-100"
            >
              <Send className="ml-0.5 h-6 w-6" />
            </button>
          </div>

          <div className="mt-4 flex justify-center gap-4 text-xs font-medium text-gray-400">
            {/* 추천 검색어 클릭 시 바로 이동하도록 처리 가능 */}
            <span
              onClick={() => router.push('/ai?query=인사규정')}
              className="hover:text-primary cursor-pointer transition-colors"
            >
              #인사규정
            </span>
            <span
              onClick={() => router.push('/ai?query=IT지원')}
              className="hover:text-primary cursor-pointer transition-colors"
            >
              #IT지원
            </span>
            <span
              onClick={() => router.push('/ai?query=프로젝트관리')}
              className="hover:text-primary cursor-pointer transition-colors"
            >
              #프로젝트관리
            </span>
          </div>
        </div>
      </div>

      <div className="fixed bottom-6 text-xs text-gray-400">
        AI can make mistakes. Please check important information.
      </div>
    </div>
  )
}
