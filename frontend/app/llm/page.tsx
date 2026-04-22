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
import { menuInfos } from '@/public/const/menu'

function LlmContent() {
  const menuInfo = menuInfos.llm
  const modalStore = useModalStore()

  // ###################################################
  // 상태 관리
  // ###################################################
  // 세션 ID 상태
  const sessionIdRef = useRef<string>(randomUUID())
  // 대화 내역 목록 상태
  const [messages, setMessages] = useState<Message[]>([])
  // 스트리밍 여부 상태
  const [isStreaming, setIsStreaming] = useState(false)
  const streamRef = useRef<FetchEventSource | null>(null)
  // 선택된 대화 이력
  const [selectedChatId, setSelectedChatId] = useState<number | null>(null)
  // 채팅 이력 패널 리셋 키
  const [historyKey, setHistoryKey] = useState(0)
  // 채팅 이력 패널 새로고침 트리거
  const [historyRefreshTrigger, setHistoryRefreshTrigger] = useState(0)
  // 대화 이력 무한스크롤 상태
  const [isLoadingHistory, setIsLoadingHistory] = useState(false)
  const historyPageRef = useRef(0)
  const historyIsLastRef = useRef(true)
  const HISTORY_PAGE_SIZE = 5

  const handleSelectChat = async (chat: Chat) => {
    setSelectedChatId(chat.chatId)
    historyPageRef.current = 0
    historyIsLastRef.current = false
    setIsLoadingHistory(true)
    try {
      const res = await getChatDetailsApi(chat.chatId, 0, HISTORY_PAGE_SIZE)
      historyIsLastRef.current = res.result.length < HISTORY_PAGE_SIZE
      const details = [...res.result].reverse()
      const loaded: Message[] = details.flatMap((d) => [
        createQueryMessage(d.query, d.sysCreateDt),
        createAnswerMessage(d.answer, '', undefined, d.sysModifyDt),
      ])
      setMessages(loaded)
    } catch (e) {
      console.error(e)
    } finally {
      setIsLoadingHistory(false)
    }
  }

  const handleLoadMoreHistory = async () => {
    if (isLoadingHistory || historyIsLastRef.current || selectedChatId === null) return
    setIsLoadingHistory(true)
    const nextPage = historyPageRef.current + 1
    try {
      const res = await getChatDetailsApi(selectedChatId, nextPage, HISTORY_PAGE_SIZE)
      if (res.result.length === 0) {
        historyIsLastRef.current = true
        return
      }
      historyPageRef.current = nextPage
      historyIsLastRef.current = res.result.length < HISTORY_PAGE_SIZE
      const details = [...res.result].reverse()
      const prepend: Message[] = details.flatMap((d) => [
        createQueryMessage(d.query, d.sysCreateDt),
        createAnswerMessage(d.answer, '', undefined, d.sysModifyDt),
      ])
      setMessages((prev) => [...prepend, ...prev])
    } catch (e) {
      console.error(e)
    } finally {
      setIsLoadingHistory(false)
    }
  }

  // ###################################################
  // 랜더링 이펙트
  // ###################################################
  const greetingIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null)

  const startGreeting = () => {
    if (greetingIntervalRef.current) clearInterval(greetingIntervalRef.current)
    setMessages([createAnswerMessage('', '')])
    let idx = 0
    greetingIntervalRef.current = setInterval(() => {
      setMessages((prev) => {
        if (prev.length === 0) return prev
        const messages = [...prev]
        messages[0] = {
          ...messages[0],
          content: replaceEventDataToText(GreetingMessage.llm.substring(0, idx)),
        }
        return messages
      })
      if (idx >= GreetingMessage.llm.length) {
        clearInterval(greetingIntervalRef.current!)
        greetingIntervalRef.current = null
      } else {
        idx++
      }
    }, 10)
  }

  useEffect(() => {
    startGreeting()
    return () => {
      if (greetingIntervalRef.current) clearInterval(greetingIntervalRef.current)
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
      onConnect: async (event) => {
        const receivedChatId = Number(event.data)
        if (!isNaN(receivedChatId) && receivedChatId > 0) {
          setSelectedChatId((prev) => {
            if (prev === null) setHistoryRefreshTrigger((t) => t + 1)
            return receivedChatId
          })
        }
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
    await chatLlmApi(query, sessionIdRef.current, streamEvent, selectedChatId)
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
   * 새 채팅 시작 핸들러
   */
  const handleNewChat = () => {
    streamRef.current?.close()
    streamRef.current = null
    setIsStreaming(false)
    sessionIdRef.current = randomUUID()
    setSelectedChatId(null)
    setHistoryKey((k) => k + 1)
    historyPageRef.current = 0
    historyIsLastRef.current = true
    startGreeting()
  }

  /**
   * 스트림 중단 핸들러
   */
  const handleStop = async () => {
    await cancelStreamApi(sessionIdRef.current)
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
            key={historyKey}
            menuCode="MENU_LLM"
            selectedChatId={selectedChatId}
            onSelectChat={handleSelectChat}
            onNewChat={handleNewChat}
            refreshTrigger={historyRefreshTrigger}
          />
        </div>

        {/* 우측: 채팅 영역 (3/4) */}
        <div className="flex min-w-0 flex-1 flex-col min-h-0">
          <ChatArea
            messages={messages}
            onSendMessage={handleSendQuery}
            onStop={handleStop}
            isStreaming={isStreaming}
            isLoadingHistory={isLoadingHistory}
            onLoadMoreHistory={selectedChatId !== null && !historyIsLastRef.current ? handleLoadMoreHistory : undefined}
          />
        </div>
      </div>
    </div>
  )
}

export default function LlmPage() {
  return (
    <Suspense fallback={null}>
      <LlmContent />
    </Suspense>
  )
}
