'use client'

import React, { useRef, useEffect, useState } from 'react'
import { BookOpen, X } from 'lucide-react'
import QueryMessage from '@/components/chat/QueryMessage'
import AnswerMessage from '@/components/chat/AnswerMessage'
import InputBox from '@/components/chat/InputBox'
import { Document } from '@/types/domain'

export interface Message {
  role: 'user' | 'assistant'
  content: string
  inference?: string
  documents?: Document[]
}

interface ChatAreaProps {
  messages: Message[]
  onSendMessage: (query: string) => void
  onStop?: () => void
  isStreaming: boolean
  placeholder?: string
}

// #######################################################
// [내부 컴포넌트] 출처 확인 모달
// #######################################################
const SourceModal = ({
  isOpen,
  onClose,
  documents,
}: {
  isOpen: boolean
  onClose: () => void
  documents: Document[]
}) => {
  if (!isOpen) return null

  return (
    <div className="animate-in fade-in fixed inset-0 z-[100] flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm duration-200">
      <div className="flex h-[80vh] w-full max-w-7xl flex-col overflow-hidden rounded-2xl bg-white shadow-2xl">
        {/* 1. 모달 헤더 */}
        <div className="flex items-center justify-between border-b border-gray-100 px-6 py-4">
          <div className="flex items-center gap-2">
            <BookOpen className="text-primary h-5 w-5" />
            <h3 className="text-lg font-bold text-gray-800">참고 문서 출처</h3>
            <span className="rounded-full bg-blue-50 px-2 py-0.5 text-xs font-bold text-blue-600">
              {documents.length} 참고문서목록
            </span>
          </div>
          <button
            onClick={onClose}
            className="rounded-full p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-600"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* 2. 모달 내용 (스크롤 영역) */}
        <div className="flex-1 overflow-y-auto bg-gray-50 p-6">
          <div className="flex flex-col gap-4">
            {documents.map((document, idx) => (
              <div
                key={idx}
                className="hover:border-primary/30 flex flex-col rounded-xl border border-gray-200 bg-white p-6 shadow-sm transition-all hover:shadow-md"
              >
                {/* (1) 상단: 원본 파일명 (Badge 형태) */}
                <div className="mb-4 flex items-center justify-between">
                  <span className="inline-flex items-center rounded-md bg-gray-100 px-2.5 py-1 text-xs font-bold text-gray-600">
                    📄 {document.originFileName}
                  </span>
                </div>

                {/* (2) 중단: 제목 계층 구조 (Title > Sub > Third) */}
                <div className="mb-4 flex flex-col gap-1.5 border-l-2 border-gray-100 pl-4">
                  {/* 대제목 */}
                  {document.title && (
                    <h4 className="text-lg font-bold text-gray-900">
                      {document.title}
                    </h4>
                  )}

                  {/* 중제목 */}
                  {document.subTitle && (
                    <div className="flex items-center gap-1.5 text-sm font-semibold text-gray-700">
                      <span className="text-gray-300">↳</span>
                      {document.subTitle}
                    </div>
                  )}

                  {/* 소제목 */}
                  {document.thirdTitle && (
                    <div className="ml-4 flex items-center gap-1.5 text-xs font-medium text-gray-500">
                      <span className="text-gray-300">-</span>
                      {document.thirdTitle}
                    </div>
                  )}
                </div>

                {/* (3) 하단: 본문 (HTML 렌더링) */}
                <div
                  className="html-content overflow-x-auto rounded-lg border border-gray-100 bg-gray-50/50 p-4 text-sm leading-relaxed text-gray-700"
                  dangerouslySetInnerHTML={{ __html: document.content }}
                />
              </div>
            ))}
          </div>
        </div>

        {/* 3. 모달 하단 */}
        <div className="border-t border-gray-100 bg-white px-6 py-4 text-right">
          <button
            onClick={onClose}
            className="rounded-lg bg-gray-100 px-5 py-2.5 text-sm font-bold text-gray-600 hover:bg-gray-200"
          >
            닫기
          </button>
        </div>
      </div>
    </div>
  )
}

// #######################################################
// [메인 컴포넌트] ChatArea
// #######################################################
export default function ChatArea({
  messages,
  onSendMessage,
  onStop,
  isStreaming,
}: ChatAreaProps) {
  const [input, setInput] = useState('')
  const messagesEndRef = useRef<HTMLDivElement>(null)

  // 내부 모달 상태 관리
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [currentDocuments, setCurrnetDocuments] = useState<Document[]>([])

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages, isStreaming])

  const handleSend = () => {
    if (!input.trim() || isStreaming) return
    onSendMessage(input)
    setInput('')
  }

  // 내부 핸들러: 출처 클릭 시 모달 오픈
  const handleDocumentClick = (documents: Document[]) => {
    setCurrnetDocuments(documents)
    setIsModalOpen(true)
  }

  return (
    <>
      <div className="flex h-full flex-col overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
        {/* 1. 메시지 리스트 영역 */}
        <div className="scrollbar-thin scrollbar-thumb-gray-300 scrollbar-track-transparent min-h-0 flex-1 overflow-y-auto bg-gray-50 p-6">
          {messages.map((msg, idx) => (
            <React.Fragment key={idx}>
              {msg.role === 'user' ? (
                <QueryMessage content={msg.content} />
              ) : (
                <AnswerMessage
                  content={msg.content}
                  inference={msg.inference}
                  documents={msg.documents}
                  onDocumentClick={handleDocumentClick}
                />
              )}
            </React.Fragment>
          ))}
          <div ref={messagesEndRef} />
        </div>

        {/* 2. 입력 박스 영역 */}
        <InputBox
          value={input}
          onChange={setInput}
          onSend={handleSend}
          onStop={() => onStop && onStop()}
          isStreaming={isStreaming}
        />
      </div>

      {/* 모달 렌더링 */}
      <SourceModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        documents={currentDocuments}
      />
    </>
  )
}
