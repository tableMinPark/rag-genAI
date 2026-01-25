'use client'

import { Suspense, useEffect, useState } from 'react'
import ChatArea from '@/components/chat/ChatArea'
import { Brain } from 'lucide-react'
import { randomUUID, replaceEventDataToText } from '@/public/ts/commonUtil'
import { cancelStreamApi, streamApi } from '@/api/stream'
import { chatLlmApi } from '@/api/chat'
import { useUiStore } from '@/stores/uiStore'
import { useModalStore } from '@/stores/modalStore'
import { GreetingMessage } from '@/public/const/greeting'
import { createAnswerMessage, createQueryMessage, Message } from '@/types/chat'
import { StreamEvent } from '@/types/streamEvent'
import NotFound from '@/components/common/NotFound'

function LlmContent() {
  const uiStore = useUiStore()
  const modalStore = useModalStore()

  // ###################################################
  // 상태 관리
  // ###################################################
  // 세션 ID 상태
  const [sessionId] = useState<string>(randomUUID())
  // 대화 내역 목록 상태
  const [messages, setMessages] = useState<Message[]>([])
  // 스트리밍 여부 상태
  const [isStreaming, setIsStreaming] = useState(false)

  useEffect(() => {
    setMessages([createAnswerMessage('', '')])
    let greetingMessageIndex = 0
    const greetingMessageInterval = setInterval(() => {
      setMessages((prev) => {
        if (prev.length === 0) return prev
        const messages = [...prev]
        const lastIndex = 0
        messages[lastIndex] = {
          ...messages[lastIndex],
          content: replaceEventDataToText(
            GreetingMessage.llm.substring(0, greetingMessageIndex),
          ),
        }
        return messages
      })
      if (greetingMessageIndex >= GreetingMessage.llm.length) {
        clearInterval(greetingMessageInterval)
      } else {
        greetingMessageIndex++
      }
    }, 10)

    return () => clearInterval(greetingMessageInterval)
  }, [])

  // ###################################################
  // 핸들러
  // ###################################################
  /**
   * 답변 요청 핸들러
   * @param query 사용자 질의
   */
  const handleSendQuery = async (query: string) => {
    // 스트림 상태 체크
    if (isStreaming) return
    // 스트림 시작 상태 변경
    setIsStreaming(true)
    // 세션 기반 SSE 연결
    await streamApi(
      sessionId,
      new StreamEvent({
        onConnect: async (_) => {
          console.log(`📡 질의 요청 : ${query}`)
          // 질의 등록
          setMessages((prev) => [...prev, createQueryMessage(query)])
          await chatLlmApi(query, sessionId)
            .then((response) => {
              console.log(`📡 ${response.message}`)
              // 답변 등록
              setMessages((prev) => [...prev, createAnswerMessage('', '', [])])
            })
            .catch((reason) => {
              console.error(reason)
              modalStore.setInfo('서버 통신 에러', '답변 생성에 실패했습니다.')
              setIsStreaming(false)
            })
        },
        onDisconnect: (_) => {
          setIsStreaming(false)
        },
        onException: (_) => {
          setIsStreaming(false)
        },
        onError: (_) => {
          modalStore.setInfo('서버 통신 에러', '답변 생성에 실패했습니다.')
          setIsStreaming(false)
        },
        onInference: (event) => {
          setMessages((prev) => {
            const messages = [...prev]
            const currentMessageIndex = messages.length - 1
            const currentMessage = messages[currentMessageIndex]
            messages[currentMessageIndex] = {
              ...currentMessage,
              inference: replaceEventDataToText(
                currentMessage.inference + event.data,
              ),
            }
            return messages
          })
        },
        onAnswer: (event) => {
          setMessages((prev) => {
            const messages = [...prev]
            const currentMessageIndex = messages.length - 1
            const currentMessage = messages[currentMessageIndex]
            messages[currentMessageIndex] = {
              ...currentMessage,
              content: replaceEventDataToText(
                currentMessage.content + event.data,
              ),
            }
            return messages
          })
        },
      }),
    )
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

  // ###################################################
  // 렌더링 (Render)
  // ###################################################
  return (
    <div className="flex h-full w-full flex-col p-6">
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
      {/* 채팅 영역 */}
      <div className="min-h-0 flex-1">
        <ChatArea
          messages={messages}
          onSendMessage={handleSendQuery}
          onStop={handleStop}
          isStreaming={isStreaming}
        />
      </div>
    </div>
  )
}

export default function LlmPage() {
  return (
    <Suspense fallback={<NotFound />}>
      <LlmContent />
    </Suspense>
  )
}
