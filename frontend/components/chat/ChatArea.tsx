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
}

export default function ChatArea({
  messages,
  onSendMessage,
  onStop,
  isStreaming,
}: ChatAreaProps) {
  const modalStore = useModalStore()

  // ###################################################
  // 상태 관리
  // ###################################################
  const messagesEndRef = useRef<HTMLDivElement>(null)
  // 질의문 입력 상태
  const [query, setQuery] = useState('')
  // 참고 문서 상세 모달
  const [isModalOpen, setIsModalOpen] = useState(false)
  // 현재 선택된 참고 문서
  const [selectedDocuments, setSelectedDocuments] = useState<Document[]>([])

  // ###################################################
  // 랜더링 이펙트
  // ###################################################
  useEffect(() => {
    if (isStreaming) {
      handleScrollToBottom()
    }
  }, [isStreaming])

  // ###################################################
  // 핸들러
  // ###################################################
  /**
   * 메시지 영역 스크롤 최하단 이동
   */
  const handleScrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
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
        <div className="scrollbar-thin scrollbar-thumb-gray-300 scrollbar-track-transparent min-h-0 flex-1 overflow-y-auto bg-gray-50 p-6">
          {messages.map((message, index) => (
            <React.Fragment key={index}>
              {message.role === 'user' ? (
                <QueryMessage content={message.content} />
              ) : (
                <AnswerMessage
                  isStreaming={isStreaming}
                  content={message.content}
                  inference={message.inference}
                  documents={message.documents}
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
