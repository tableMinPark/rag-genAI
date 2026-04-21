'use client'

import { Suspense, useEffect, useRef, useState } from 'react'
import ChatArea from '@/components/chat/ChatArea'
import ChatHistoryPanel from '@/components/chat/ChatHistoryPanel'
import { randomUUID, replaceEventDataToText } from '@/public/ts/commonUtil'
import { cancelStreamApi, FetchEventSource, streamApi } from '@/api/stream'
import { chatLlmApi, getChatDetailsApi } from '@/api/chat'
import { Chat } from '@/types/domain'
import { useModalStore } from '@/stores/modalStore'
import { GreetingMessage } from '@/public/const/greeting'
import { createAnswerMessage, createQueryMessage, Message } from '@/types/chat'
import { StreamEvent } from '@/types/streamEvent'
import NotFound from '@/components/NotFound'
import { menuInfos } from '@/public/const/menu'

function LlmContent() {
  const menuInfo = menuInfos.llm
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
  const streamRef = useRef<FetchEventSource | null>(null)
  // 선택된 대화 이력
  const [selectedChatId, setSelectedChatId] = useState<number | null>(null)

  const handleSelectChat = async (chat: Chat) => {
    setSelectedChatId(chat.chatId)
    try {
      const res = await getChatDetailsApi(chat.chatId, 0, 100)
      const details = [...res.result].reverse()
      const loaded: Message[] = details.flatMap((d) => [
        createQueryMessage(d.query),
        createAnswerMessage(d.answer, ''),
      ])
      setMessages(loaded)
    } catch (e) {
      console.error(e)
    }
  }

  // ###################################################
  // 랜더링 이펙트
  // ###################################################
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

    return () => {
      clearInterval(greetingMessageInterval)
      streamRef.current?.close()
    }
  }, [])

  // ###################################################
  // 핸들러
  // ###################################################
  /**
   * 답변 요청 핸들러
   * @param query 사용자 질의
   */
  const handleSendQuery = async (query: string) => {
    // 기존 스트림 정리
    streamRef.current?.close()
    // 스트림 상태 체크
    if (isStreaming) return
    // 스트림 시작 상태 변경
    setIsStreaming(true)
    // 스트림 옵션 설정
    const streamEvent = new StreamEvent({
      onConnect: async (_) => {
        console.log(`📡 질의 요청 : ${query}`)
      },
      onDisconnect: (_) => {
        setIsStreaming(false)
        streamRef.current = null
      },
      onException: (event) => {
        modalStore.setError(
          '에러 발생',
          '답변 생성 실패',
          event.data || '답변 생성 중 에러가 발생했습니다.',
        )
        setIsStreaming(false)
        streamRef.current = null
      },
      onError: (_) => {
        modalStore.setError(
          '서버 통신 불가',
          '답변 생성 실패',
          '답변 생성에 실패했습니다.',
        )
        setIsStreaming(false)
        streamRef.current = null
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
    })
    // 스트림 이벤트 연결
    await chatLlmApi(query, sessionId, streamEvent)
      .then((stream) => {
        console.log(`📡 답변 요청 성공`)
        streamRef.current = stream
        // 답변 등록
        setMessages((prev) => [...prev, createQueryMessage(query)])
        setMessages((prev) => [...prev, createAnswerMessage('', '', [])])
      })
      .catch((reason) => {
        console.error(reason)
        modalStore.setError(
          '서버 통신 불가',
          '답변 생성 실패',
          '답변 생성에 실패했습니다.',
        )
        setIsStreaming(false)
        streamRef.current = null
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
      .finally(() => {
        setIsStreaming(false)
        streamRef.current = null
      })
  }

  // ###################################################
  // 렌더링 (Render)
  // ###################################################
  return (
    <div className="flex h-full w-full flex-col overflow-hidden">
      {/* 상단 헤더 영역 (전체 너비) */}
      <div className="flex shrink-0 items-center justify-between border-b border-gray-100 px-6 py-4">
        <div className="flex items-center gap-3">
          <h2 className="flex items-center gap-2 text-2xl font-bold text-gray-800">
            <menuInfo.icon className="text-primary h-6 w-6" />
            {menuInfo.name}
          </h2>
          <p className="text-xs text-gray-400">{menuInfo.description}</p>
        </div>
      </div>

      {/* 하단: 이력 패널(1) + 채팅 영역(3) */}
      <div className="flex min-h-0 flex-1 overflow-hidden p-6 gap-4">
        {/* 좌측: 대화 이력 패널 (1/4) */}
        <div className="w-1/4 shrink-0 min-h-0">
          <ChatHistoryPanel
            menuCode="MENU_LLM"
            selectedChatId={selectedChatId}
            onSelectChat={handleSelectChat}
          />
        </div>

        {/* 우측: 채팅 영역 (3/4) */}
        <div className="flex min-w-0 flex-1 flex-col min-h-0">
          <ChatArea
            messages={messages}
            onSendMessage={handleSendQuery}
            onStop={handleStop}
            isStreaming={isStreaming}
          />
        </div>
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
