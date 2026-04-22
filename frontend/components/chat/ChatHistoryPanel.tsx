'use client'

import { useEffect, useRef, useState } from 'react'
import { Chat } from '@/types/domain'
import { getChatsApi } from '@/api/chat'
import { Clock, MessageSquareDashed, SquarePen } from 'lucide-react'

interface ChatHistoryPanelProps {
  menuCode: string
  selectedChatId: number | null
  onSelectChat: (chat: Chat) => void
  onNewChat?: () => void
  refreshTrigger?: number
}

const PAGE_SIZE = 20

function formatDate(dateStr: string) {
  const d = new Date(dateStr)
  const now = new Date()
  const diffMs = now.getTime() - d.getTime()
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))

  if (diffDays === 0) {
    return d.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
  } else if (diffDays === 1) {
    return '어제'
  } else if (diffDays < 7) {
    return `${diffDays}일 전`
  } else {
    return d.toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' })
  }
}

export default function ChatHistoryPanel({
  menuCode,
  selectedChatId,
  onSelectChat,
  onNewChat,
  refreshTrigger,
}: ChatHistoryPanelProps) {
  const [chats, setChats] = useState<Chat[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const observerRef = useRef<HTMLDivElement>(null)
  // ref로 최신 상태 유지 — observer 클로저에서 stale 값 참조 방지
  const pageRef = useRef(0)
  const isLastRef = useRef(false)
  const isLoadingRef = useRef(false)

  const fetchChats = async (pageNo: number) => {
    if (isLoadingRef.current) return
    if (pageNo > 0 && isLastRef.current) return

    isLoadingRef.current = true
    setIsLoading(true)
    try {
      const res = await getChatsApi(menuCode, pageNo, PAGE_SIZE)
      const { content, isLast: last } = res.result
      setChats((prev) => (pageNo === 0 ? content : [...prev, ...content]))
      isLastRef.current = last
      pageRef.current = pageNo
    } catch (e) {
      console.error(e)
    } finally {
      isLoadingRef.current = false
      setIsLoading(false)
    }
  }

  useEffect(() => {
    pageRef.current = 0
    isLastRef.current = false
    isLoadingRef.current = false
    setChats([])
    fetchChats(0)
  }, [menuCode])

  useEffect(() => {
    if (!refreshTrigger) return
    pageRef.current = 0
    isLastRef.current = false
    isLoadingRef.current = false
    fetchChats(0)
  }, [refreshTrigger])

  useEffect(() => {
    const el = observerRef.current
    if (!el) return
    const observer = new IntersectionObserver(
      (entries) => {
        if (
          entries[0].isIntersecting &&
          !isLastRef.current &&
          !isLoadingRef.current
        ) {
          fetchChats(pageRef.current + 1)
        }
      },
      { threshold: 0.5 },
    )
    observer.observe(el)
    return () => observer.disconnect()
  }, [menuCode])

  return (
    <div className="flex h-full flex-col overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
      {/* 헤더 */}
      <div className="flex shrink-0 items-center justify-between border-b border-gray-200 bg-gray-50 px-4 py-3">
        <div className="flex items-center gap-1.5">
          <Clock className="h-3.5 w-3.5 text-gray-400" />
          <p className="text-xs font-semibold tracking-wide text-gray-400 uppercase">
            대화 이력
          </p>
        </div>
        {onNewChat && (
          <button
            onClick={onNewChat}
            title="새 채팅"
            className="text-primary hover:bg-primary/10 rounded-md p-1 transition-colors"
          >
            <SquarePen className="h-4 w-4" />
          </button>
        )}
      </div>

      {/* 목록 */}
      <div className="scrollbar-thin scrollbar-thumb-gray-300 scrollbar-track-transparent flex-1 overflow-y-auto bg-gray-50">
        {chats.length === 0 && !isLoading ? (
          <div className="flex flex-col items-center justify-center gap-2 py-16 text-center">
            <MessageSquareDashed className="h-8 w-8 text-gray-300" />
            <p className="text-xs text-gray-400">대화 이력이 없습니다</p>
          </div>
        ) : (
          <ul className="p-2 flex flex-col gap-1">
            {chats.map((chat) => {
              const isSelected = chat.chatId === selectedChatId
              return (
                <li key={chat.chatId}>
                  <button
                    onClick={() => onSelectChat(chat)}
                    className={`w-full cursor-pointer rounded-lg px-3 py-2.5 text-left transition-all ${
                      isSelected
                        ? 'bg-primary/10 ring-primary/30 ring-1'
                        : 'hover:bg-white hover:shadow-sm'
                    }`}
                  >
                    <p
                      className={`truncate text-xs font-medium leading-snug ${
                        isSelected ? 'text-primary' : 'text-gray-700'
                      }`}
                    >
                      {chat.title}
                    </p>
                    <p className="mt-0.5 text-[10px] text-gray-400">
                      {formatDate(chat.sysCreateDt)}
                    </p>
                  </button>
                </li>
              )
            })}
          </ul>
        )}

        {/* 무한 스크롤 트리거 */}
        <div ref={observerRef} className="h-4" />

        {/* 로딩 인디케이터 */}
        {isLoading && (
          <div className="flex justify-center py-3">
            <div className="h-4 w-4 animate-spin rounded-full border-2 border-gray-200 border-t-gray-400" />
          </div>
        )}
      </div>
    </div>
  )
}
