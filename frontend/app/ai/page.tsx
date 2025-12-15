'use client'

import { useState } from 'react'
import ChatArea, { Message } from '@/components/ChatArea'
import { Bot } from 'lucide-react'
import { randomUUID, replaceEventDataToText } from '@/public/js/util.js'
import { cancelStreamApi, streamApi } from '@/api/stream'
import { chatAiApi } from '@/api/chat'
import { Document } from '@/types/domain'

// ###################################################
// 상수 정의 (Const)
// ###################################################
const CATEGORIES = [
  { code: 'TRAIN-LAW', name: '법률' },
  { code: 'TRAIN-GUIDE', name: '지침' },
  { code: 'TRAIN-MANUAL', name: '매뉴얼' },
  { code: 'TRAIN-EDU', name: '교육자료' },
]

export default function AiPage() {
  // ###################################################
  // 상태 정의 (State)
  // ###################################################
  // 세션 ID 상태
  const [sessionId] = useState<string>(randomUUID())
  // 대화 내역 목록 상태
  const [messages, setMessages] = useState<Message[]>([
    {
      role: 'assistant',
      content:
        '안녕하세요. **AI MATE** 입니다.\n\n질의를 작성해주시면 문서를 기반으로 답변 드리겠습니다.',
    },
  ])
  // 스트리밍 여부 상태
  const [isStreaming, setIsStreaming] = useState(false)
  // 선택 카테고리 목록 상태
  const [selectedCategories, setSelectedCategories] = useState<string[]>(
    CATEGORIES.map((c) => c.code),
  )

  // ###################################################
  // 핸들러 (Handler)
  // ###################################################
  /**
   * 답변 요청 핸들러
   *
   * @param query 사용자 질의
   */
  const handleSendMessage = async (query: string) => {
    // 입력 값 체크
    if (selectedCategories.length === 0) {
      alert('최소 하나의 카테고리를 선택해주세요.')
      return
    }

    // 질의 등록
    const userMessage: Message = { role: 'user', content: query }
    setMessages((prev) => [...prev, userMessage])

    // 스트림 시작 상태 변경
    setIsStreaming(true)

    let content = ''
    let inference = ''
    let documents: Document[] | undefined
    // 세션 기반 SSE 연결
    const eventSource = streamApi(sessionId)
    // SSE 연결 이벤트
    eventSource.addEventListener('connect', async (event) => {
      console.log(`📡 스트림 연결`)
      console.log(`📡 질의 등록 : ${query}`)

      console.log(`📡 질의 요청 : ${query}`)
      await chatAiApi(query, sessionId, selectedCategories)
        .then((response) => {
          console.log(`📡 ${response.message}`)
          documents = response.data.documents
          setMessages((prev) => [
            ...prev,
            {
              role: 'assistant',
              content: content,
              inference: inference,
            },
          ])
        })
        .catch((reason) => {
          console.error(reason)
          setMessages((prev) => [
            ...prev,
            {
              role: 'assistant',
              content:
                '서버와 통신이 원할하지 않습니다.\n\n잠시후 다시 시도 해주세요.',
              inference: '',
            },
          ])
          setIsStreaming(false)
        })
    })
    // SSE 추론 시작 이벤트
    eventSource.addEventListener('inference-start', (_) => {
      console.log('📋 추론 과정 표출 시작')
    })
    // SSE 추론 이벤트
    eventSource.addEventListener('inference', (event) => {
      setMessages((prev) => {
        const newMsgs = [...prev]
        const lastMsgIndex = newMsgs.length - 1

        const updatedLastMsg = {
          ...newMsgs[lastMsgIndex],
          inference: replaceEventDataToText(
            newMsgs[lastMsgIndex].inference + event.data,
          ),
        }

        newMsgs[lastMsgIndex] = updatedLastMsg
        return newMsgs
      })
    })
    // SSE 추론 종료 이벤트
    eventSource.addEventListener('inference-done', (_) => {
      console.log('📋 추론 과정 표출 종료')
    })
    // SSE 답변 시작 이벤트
    eventSource.addEventListener('answer-start', (_) => {
      console.log('📋 답변 시작')
    })
    // SSE 답변 이벤트
    eventSource.addEventListener('answer', (event) => {
      setMessages((prev) => {
        const newMsgs = [...prev]
        const lastMsgIndex = newMsgs.length - 1

        const updatedLastMsg = {
          ...newMsgs[lastMsgIndex],
          content: replaceEventDataToText(
            newMsgs[lastMsgIndex].content + event.data,
          ),
        }

        newMsgs[lastMsgIndex] = updatedLastMsg
        return newMsgs
      })
    })
    // SSE 답변 종료 이벤트
    eventSource.addEventListener('answer-done', (_) => {
      console.log(`📋 답변 종료`)
      setMessages((prev) => {
        const newMsgs = [...prev]
        newMsgs[newMsgs.length - 1].documents = documents ? documents : []
        return newMsgs
      })
    })
    // SSE 연결 종료 이벤트
    eventSource.addEventListener('disconnect', (_) => {
      eventSource.close()
      console.log(`❌ 스트림 닫힘`)
      setIsStreaming(false)
    })
    // SSE 예외 이벤트
    eventSource.addEventListener('exception', (_) => {
      eventSource.close()
      console.log(`❌ 예외 발생`)
      setIsStreaming(false)
    })
  }

  /**
   * 스트림 중단 핸들러
   */
  const handleStop = async () => {
    await cancelStreamApi(sessionId)
      .then((response) => {
        console.log(`📡 ${response.message}`)
      })
      .catch((reason) => console.error(reason))
      .finally(() => setIsStreaming(false))
  }

  /**
   * 카테고리 토글 핸들러
   * @param code 카테고리 코드
   */
  const toggleCategory = (code: string) => {
    setSelectedCategories((prev) =>
      prev.includes(code) ? prev.filter((c) => c !== code) : [...prev, code],
    )
  }

  // ###################################################
  // 렌더링 (Render)
  // ###################################################
  return (
    <div className="flex h-full w-full flex-col p-2">
      {/* 헤더 영역 */}
      <div className="mb-6 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div>
            <h2 className="flex items-center gap-2 text-2xl font-bold text-gray-800">
              <Bot className="text-primary h-6 w-6" />
              RAG Chat
            </h2>
            <p className="mt-1 text-xs text-gray-500">검색 기반 질문 & 답변</p>
          </div>
        </div>
        <div className="flex items-center gap-2 rounded-lg border border-gray-200 bg-white px-4 py-2 shadow-sm">
          <span className="mr-2 text-xs font-bold text-gray-500">
            검색 범위:
          </span>
          {CATEGORIES.map((cat) => (
            <label
              key={cat.code}
              className={`flex cursor-pointer items-center gap-1.5 rounded-md px-2.5 py-1.5 text-xs font-bold transition-all ${
                selectedCategories.includes(cat.code)
                  ? 'bg-primary hover:bg-primary-hover text-white shadow-sm'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              }`}
            >
              <input
                type="checkbox"
                className="hidden"
                checked={selectedCategories.includes(cat.code)}
                onChange={() => toggleCategory(cat.code)}
              />
              {selectedCategories.includes(cat.code) && (
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  viewBox="0 0 20 20"
                  fill="currentColor"
                  className="h-3 w-3"
                >
                  <path
                    fillRule="evenodd"
                    d="M16.704 4.153a.75.75 0 01.143 1.052l-8 10.5a.75.75 0 01-1.127.075l-4.5-4.5a.75.75 0 011.06-1.06l3.894 3.893 7.48-9.817a.75.75 0 011.05-.143z"
                    clipRule="evenodd"
                  />
                </svg>
              )}
              {cat.name}
            </label>
          ))}
        </div>
      </div>

      {/* 채팅 영역 */}
      <div className="min-h-0 flex-1">
        <ChatArea
          messages={messages}
          onSendMessage={handleSendMessage}
          onStop={handleStop}
          isStreaming={isStreaming}
        />
      </div>
    </div>
  )
}
