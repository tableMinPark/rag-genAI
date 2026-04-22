'use client'

import React, { useRef, useEffect, useState } from 'react'
import QueryMessage from '@/components/chat/QueryMessage'
import AnswerMessage from '@/components/chat/AnswerMessage'
import InputBox from '@/components/chat/InputBox'
import { Document } from '@/types/domain'
import { Message } from '@/types/chat'
import ModalChatDocument from '@/components/modal/ModalChatDocument'
import { useModalStore } from '@/stores/modalStore'

interface ChatAreaProps {
  messages: Message[]
  onSendMessage: (query: string) => void
  onStop?: () => void
  isStreaming: boolean
  isLoadingHistory?: boolean
  onLoadMoreHistory?: () => void
}

export default function ChatArea({
  messages,
  onSendMessage,
  onStop,
  isStreaming,
  isLoadingHistory = false,
  onLoadMoreHistory,
}: ChatAreaProps) {
  const modalStore = useModalStore()

  // ###################################################
  // 상태 관리
  // ###################################################
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const scrollContainerRef = useRef<HTMLDivElement>(null)
  const topSentinelRef = useRef<HTMLDivElement>(null)
  // 질의문 입력 상태
  const [query, setQuery] = useState('')
  // 참고 문서 상세 모달
  const [isModalOpen, setIsModalOpen] = useState(false)
  // 현재 선택된 참고 문서
  const [selectedDocuments, setSelectedDocuments] = useState<Document[]>([])
  // 추가 로드 전 스크롤 위치 복원용
  const prevScrollHeightRef = useRef<number>(0)

  // ###################################################
  // 랜더링 이펙트
  // ###################################################
  useEffect(() => {
    if (isStreaming) {
      handleScrollToBottom('smooth')
    }
  }, [isStreaming])

  useEffect(() => {
    if (!isLoadingHistory) {
      const container = scrollContainerRef.current
      if (!container) return
      // 추가 로드 후 스크롤 위치 복원 (위로 올릴 때)
      const newScrollHeight = container.scrollHeight
      const diff = newScrollHeight - prevScrollHeightRef.current
      if (diff > 0 && prevScrollHeightRef.current > 0) {
        container.scrollTop = diff
        prevScrollHeightRef.current = 0
        return
      }
    }
    // 이력 선택 시 DOM 렌더링 완료 후 즉시 맨 아래로
    requestAnimationFrame(() => {
      handleScrollToBottom('instant')
    })
  }, [messages])

  // 상단 sentinel IntersectionObserver — 추가 이력 로드
  useEffect(() => {
    if (!onLoadMoreHistory) return
    const sentinel = topSentinelRef.current
    if (!sentinel) return
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && !isLoadingHistory) {
          prevScrollHeightRef.current = scrollContainerRef.current?.scrollHeight ?? 0
          onLoadMoreHistory()
        }
      },
      { threshold: 0.1 },
    )
    observer.observe(sentinel)
    return () => observer.disconnect()
  }, [onLoadMoreHistory, isLoadingHistory])

  // ###################################################
  // 핸들러
  // ###################################################
  /**
   * 메시지 영역 스크롤 최하단 이동
   */
  const handleScrollToBottom = (behavior: ScrollBehavior = 'smooth') => {
    messagesEndRef.current?.scrollIntoView({ behavior })
  }

  /**
   * 질의 전송 핸들러
   */
  const handleSendQuery = () => {
    if (!query.trim()) {
      modalStore.setError(
        '필수 입력값 누락',
        '질의문 필수',
        '질의문을 입력해주세요.',
      )
      return
    } else if (isStreaming) {
      return
    }
    onSendMessage(query)
    setQuery('')
  }

  /**
   * 참고 문서 선택 핸들러
   * @param documents 참고문서 목록
   */
  const handleSelectDocument = (documents: Document[]) => {
    setSelectedDocuments(documents)
    setIsModalOpen(true)
  }

  // ###################################################
  // 렌더링 (Render)
  // ###################################################
  return (
    <>
      <div className="flex h-full flex-col overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
        {/* 1. 메시지 리스트 영역 */}
        <div
          ref={scrollContainerRef}
          className="scrollbar-thin scrollbar-thumb-gray-300 scrollbar-track-transparent relative min-h-0 flex-1 overflow-y-auto bg-gray-50 p-6"
        >
          {/* 상단 sentinel — 스크롤 올릴 때 추가 이력 로드 트리거 */}
          {onLoadMoreHistory && <div ref={topSentinelRef} className="h-1" />}

          {/* 이력 로딩 스피너 */}
          {isLoadingHistory && (
            <div className="sticky top-0 z-10 flex justify-center py-3">
              <div className="flex items-center gap-2 rounded-full bg-white px-4 py-2 shadow-md">
                <div className="h-4 w-4 animate-spin rounded-full border-2 border-gray-200 border-t-gray-500" />
                <span className="text-xs text-gray-500">이전 대화 불러오는 중</span>
              </div>
            </div>
          )}

          {messages.map((message, index) => (
            <React.Fragment key={index}>
              {message.role === 'user' ? (
                <QueryMessage content={message.content} timestamp={message.timestamp} />
              ) : (
                <AnswerMessage
                  isStreaming={isStreaming && index === messages.length - 1}
                  content={message.content}
                  inference={message.inference}
                  documents={message.documents}
                  timestamp={message.timestamp}
                  onSelectDocument={handleSelectDocument}
                />
              )}
            </React.Fragment>
          ))}
          <div ref={messagesEndRef} />
        </div>
        {/* 2. 입력 박스 영역 */}
        <InputBox
          value={query}
          onChange={setQuery}
          onSend={handleSendQuery}
          onStop={() => onStop && onStop()}
          isStreaming={isStreaming}
        />
      </div>
      {/* 모달 렌더링 */}
      {selectedDocuments && (
        <ModalChatDocument
          isOpen={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          documents={selectedDocuments}
        />
      )}
    </>
  )
}
