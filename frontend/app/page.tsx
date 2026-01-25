'use client'

import React, { useState } from 'react'
import { useRouter } from 'next/navigation'
import { Sparkles, MessageSquare, SendHorizonal } from 'lucide-react'

const DEFAULT_RECOMMEND_QUERY = [
  '캐릭터 관련 규정',
  '인사규정',
  '프로젝트 관리 규정',
]

export default function ChatMainPage() {
  const router = useRouter()
  const [input, setInput] = useState('')

  /**
   * 질의 요청 핸들러
   * @param e 이벤트
   */
  const handleSendQuery = (e?: React.FormEvent) => {
    e?.preventDefault()
    if (!input.trim()) return
    const query = encodeURIComponent(input.trim())
    router.push(`/ai?query=${query}`)
  }

  /**
   * 키 다운 핸들러
   * @param e 이벤트
   */
  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && !e.nativeEvent.isComposing) {
      handleSendQuery()
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
            RAG 기반의 AI가 문서를 분석하여 정확한 답변을 제공합니다.
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
              onClick={() => handleSendQuery()}
              disabled={!input.trim()}
              className="bg-primary hover:bg-primary-hover absolute right-3 flex h-14 w-14 items-center justify-center rounded-full text-white shadow-md transition-all hover:scale-105 active:scale-95 disabled:cursor-not-allowed disabled:bg-gray-200 disabled:text-gray-400 disabled:hover:scale-100"
            >
              <SendHorizonal className="ml-0.5 h-6 w-6" />
            </button>
          </div>

          <div className="mt-4 flex justify-center gap-4 text-xs font-medium text-gray-400">
            {DEFAULT_RECOMMEND_QUERY.map((recommendQuery) => (
              <span
                onClick={() => router.push(`/ai?query=${recommendQuery}`)}
                className="hover:text-primary cursor-pointer transition-colors"
              >
                #{recommendQuery}
              </span>
            ))}
          </div>
        </div>
      </div>

      <div className="fixed bottom-6 text-xs text-gray-400">
        AI can make mistakes. Please check important information.
      </div>
    </div>
  )
}
