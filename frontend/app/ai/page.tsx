'use client'

import { Suspense, useEffect, useRef, useState } from 'react'
import ChatArea from '@/components/chat/ChatArea'
import ChatHistoryPanel from '@/components/chat/ChatHistoryPanel'
import { randomUUID, replaceEventDataToText } from '@/public/ts/commonUtil'
import { cancelStreamApi, FetchEventSource, streamApi } from '@/api/stream'
import { chatAiApi, getCategoriesApi, getChatDetailsApi } from '@/api/chat'
import { Category, Chat } from '@/types/domain'
import { useSearchParams } from 'next/navigation'
import { StreamEvent } from '@/types/streamEvent'
import { GreetingMessage } from '@/public/const/greeting'
import { createAnswerMessage, createQueryMessage, Message } from '@/types/chat'
import { useUiStore } from '@/stores/uiStore'
import NotFound from '@/components/NotFound'
import { useModalStore } from '@/stores/modalStore'
import { menuInfos } from '@/public/const/menu'

function AiContent() {
  const menuInfo = menuInfos.ai
  const uiStore = useUiStore()
  const modalStore = useModalStore()
  const searchParams = useSearchParams()
  const outerQuery = searchParams.get('query')

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
  // 카테고리 목록
  const [categories, setCategories] = useState<Category[]>([])
  // 선택한 카테고리 목록
  const [selectedCategories, setSelectedCategories] = useState<string[]>([])
  const sentQueryRef = useRef<Set<string>>(new Set())
  // 선택된 대화 이력
  const [selectedChatId, setSelectedChatId] = useState<number | null>(null)

  // ###################################################
  // 랜더링 이펙트
  // ###################################################
  useEffect(() => {
    if (!outerQuery) return
    const queryKey = JSON.stringify({
      query: outerQuery,
      initializeQuery: true,
    })
    if (sentQueryRef.current.has(queryKey)) return
    sentQueryRef.current.add(queryKey)
    handleSendQuery(outerQuery)
  }, [outerQuery])

  useEffect(() => {
    handleGetCategories()
    if (outerQuery) return
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
            GreetingMessage.ai.substring(0, greetingMessageIndex),
          ),
        }
        return messages
      })
      if (greetingMessageIndex >= GreetingMessage.ai.length) {
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
      onReference: (event) => {
        setMessages((prev) => {
          const messages = [...prev]
          const currentMessageIndex = messages.length - 1
          const currentMessage = messages[currentMessageIndex]
          messages[currentMessageIndex] = {
            ...currentMessage,
            documents: JSON.parse(event.data),
          }
          return messages
        })
      },
    })
    // 세션 기반 SSE 연결
    await chatAiApi(query, sessionId, selectedCategories, streamEvent)
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
   * 카테고리 목록 조회
   */
  const handleGetCategories = async () => {
    uiStore.setLoading('카테고리 목록을 불러오는 중입니다')
    await getCategoriesApi()
      .then((response) => {
        console.log(`📡 ${response.message}`)
        setCategories(() => response.result)
        uiStore.reset()
      })
      .catch((reason) => {
        console.error(reason)
        uiStore.setError(
          '질문 가능한 카테고리가 없습니다.',
          handleGetCategories,
        )
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

  /**
   * 카테고리 토글 핸들러
   * @param code 카테고리 코드
   */
  const handleToggleCategory = (code: string = 'ALL') => {
    if (code === 'ALL') {
      setSelectedCategories([])
    } else {
      setSelectedCategories((prev) =>
        prev.includes(code) ? prev.filter((c) => c !== code) : [...prev, code],
      )
    }
  }

  /**
   * 대화 이력 선택 핸들러 — 해당 채팅의 Q&A를 불러와 대화창에 반영
   */
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
        {categories.length > 0 && (
          <div className="flex items-center gap-2 rounded-lg border border-gray-200 bg-white px-4 py-2 shadow-sm">
            <span className="mr-2 text-xs font-bold text-gray-500">
              검색 범위:
            </span>
            <label
              key="ALL"
              className={`flex cursor-pointer items-center gap-1.5 rounded-md px-2.5 py-1.5 text-xs font-bold transition-all ${
                selectedCategories.length == 0
                  ? 'bg-primary hover:bg-primary-hover text-white shadow-sm'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              }`}
            >
              <input
                type="checkbox"
                className="hidden"
                checked={selectedCategories.length == 0}
                onChange={() => handleToggleCategory()}
              />
              {selectedCategories.length == 0 && (
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
              전체
            </label>
            {categories.map((category) => (
              <label
                key={category.code}
                className={`flex cursor-pointer items-center gap-1.5 rounded-md px-2.5 py-1.5 text-xs font-bold transition-all ${
                  selectedCategories.includes(category.code)
                    ? 'bg-primary hover:bg-primary-hover text-white shadow-sm'
                    : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                }`}
              >
                <input
                  type="checkbox"
                  className="hidden"
                  checked={selectedCategories.includes(category.code)}
                  onChange={() => handleToggleCategory(category.code)}
                />
                {selectedCategories.includes(category.code) && (
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
                {category.name}
              </label>
            ))}
          </div>
        )}
      </div>

      {/* 하단: 이력 패널(1) + 채팅 영역(3) */}
      <div className="flex min-h-0 flex-1 overflow-hidden p-6 gap-4">
        {/* 좌측: 대화 이력 패널 (1/4) */}
        <div className="w-1/4 shrink-0 min-h-0">
          <ChatHistoryPanel
            menuCode="MENU_AI"
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

export default function AiPage() {
  return (
    <Suspense fallback={<NotFound />}>
      <AiContent />
    </Suspense>
  )
}
