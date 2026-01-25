'use client'

import { Suspense, useEffect, useRef, useState } from 'react'
import ChatArea from '@/components/chat/ChatArea'
import { Bot } from 'lucide-react'
import { randomUUID, replaceEventDataToText } from '@/public/ts/commonUtil'
import { cancelStreamApi, streamApi } from '@/api/stream'
import { chatAiApi, getCategoriesApi } from '@/api/chat'
import { Category } from '@/types/domain'
import { useSearchParams } from 'next/navigation'
import { StreamEvent } from '@/types/streamEvent'
import { GreetingMessage } from '@/public/const/greeting'
import { createAnswerMessage, createQueryMessage, Message } from '@/types/chat'
import { useUiStore } from '@/stores/uiStore'
import NotFound from '@/components/common/NotFound'
import { useModalStore } from '@/stores/modalStore'

function AiContent() {
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
  // 카테고리 목록
  const [categories, setCategories] = useState<Category[]>([])
  // 선택한 카테고리 목록
  const [selectedCategories, setSelectedCategories] = useState<string[]>([])

  // ###################################################
  // 랜더링 이펙트
  // ###################################################
  const sentQueryRef = useRef<Set<string>>(new Set())
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
          await chatAiApi(query, sessionId, selectedCategories)
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
        onReference: (event) => {
          setMessages((prev) => {
            const messages = [...prev]
            const currentMessageIndex = messages.length - 1
            const currentMessage = messages[currentMessageIndex]
            messages[currentMessageIndex] = {
              ...currentMessage,
              documents: JSON.parse(event.data).documents,
            }
            return messages
          })
        },
      }),
    )
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
      .finally(() => setIsStreaming(false))
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
              <Bot className="text-primary h-6 w-6" />
              RAG Chat
            </h2>
            <p className="mt-1 text-xs text-gray-500">검색 기반 질문 & 답변</p>
          </div>
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

export default function AiPage() {
  return (
    <Suspense fallback={<NotFound />}>
      <AiContent />
    </Suspense>
  )
}
