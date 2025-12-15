'use client'

import { useState } from 'react'
import ChatArea, { Message } from '@/components/ChatArea'
import { Brain } from 'lucide-react'
import { randomUUID, replaceEventDataToText } from '@/public/js/util.js'
import { cancelStreamApi, streamApi } from '@/api/stream'
import { chatLlmApi } from '@/api/chat'

export default function LlmPage() {
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
        '안녕하세요. **LLM 기반 AI** 입니다.\n\n궁금한 내용을 물어보시면 자유롭게 답변해 드립니다.',
    },
  ])
  // 스트리밍 여부 상태
  const [isStreaming, setIsStreaming] = useState(false)

  // ###################################################
  // 핸들러 (Handler)
  // ###################################################
  /**
   * 답변 요청 핸들러
   *
   * @param query 사용자 질의
   */
  const handleSendMessage = async (query: string) => {
    // 질의 등록
    const userMessage: Message = { role: 'user', content: query }
    setMessages((prev) => [...prev, userMessage])

    // 스트림 시작 상태 변경
    setIsStreaming(true)

    let content = ''
    let inference = ''
    // 세션 기반 SSE 연결
    const eventSource = streamApi(sessionId)
    // SSE 연결 이벤트
    eventSource.addEventListener('connect', async (event) => {
      console.log(`📡 스트림 연결`)
      console.log(`📡 질의 등록 : ${query}`)
      setIsStreaming(true)

      console.log(`📡 질의 요청 : ${query}`)
      await chatLlmApi(query, sessionId)
        .then((response) => {
          console.log(`📡 ${response.message}`)
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
   * 답변 중단 핸들러
   */
  const handleStop = async () => {
    await cancelStreamApi(sessionId)
      .then((response) => {
        console.log(`📡 ${response.message}`)
      })
      .catch((reason) => console.error(reason))
      .finally(() => setIsStreaming(false))
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
              <Brain className="text-primary h-6 w-6" />
              LLM Chat
            </h2>
            <p className="mt-1 text-xs text-gray-500">일반 질문 & 답변</p>
          </div>
        </div>
      </div>

      {/* 채팅 영역 컨테이너 */}
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
