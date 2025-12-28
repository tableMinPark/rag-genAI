'use client'

import { Suspense, useEffect, useState } from 'react'
import ChatArea, { Message } from '@/components/ChatArea'
import { AlertCircle, Bot, Loader2, RefreshCw } from 'lucide-react'
import { randomUUID, replaceEventDataToText } from '@/public/ts/commonUtil'
import { cancelStreamApi, streamApi } from '@/api/stream'
import { chatAiApi, chatMyAiApi, getCategoriesApi } from '@/api/chat'
import { Category, Document } from '@/types/domain'
import { useSearchParams } from 'next/navigation'

function MyAiContent() {
  const searchParams = useSearchParams()
  const projectId = Number(searchParams.get('projectId'))
  // ###################################################
  // 상태 정의 (State)
  // ###################################################
  // 세션 ID 상태
  const [projectName, setProjectName] = useState<string>('')
  const [documents, setDocuments] = useState<string[]>([])
  const [sessionId] = useState<string>(randomUUID())
  // 대화 내역 목록 상태
  const [messages, setMessages] = useState<Message[]>([
    {
      role: 'assistant',
      content:
        '안녕하세요. **나만의 AI** 입니다.\n\n질의를 작성해주시면 **직접 등록하신 문서**를 기반으로 답변 드리겠습니다.',
    },
  ])
  // 프로세스 상태
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  // 스트리밍 여부 상태
  const [isStreaming, setIsStreaming] = useState(false)

  const loadData = async () => {
    setIsLoading(true)
    setError(null)
    try {
      // TODO: 프로젝트 문서 목록 조회
      setProjectName('테스트 프로젝트명')
      setDocuments(['테스트 문서 1', '테스트 문서 2', '테스트 문서 3'])
    } catch (err) {
      console.error(err)
      setError('프로젝트 정보가 없습니다.')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [projectId])

  useEffect(() => {
    loadData()
  }, [])

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
    let documents: Document[] | undefined
    // 세션 기반 SSE 연결
    const eventSource = streamApi(sessionId)
    // SSE 연결 이벤트
    eventSource.addEventListener('connect', async (event) => {
      console.log(`📡 스트림 연결`)
      console.log(`📡 질의 등록 : ${query}`)

      console.log(`📡 질의 요청 : ${query}`)
      await chatMyAiApi(query, sessionId, projectId)
        .then((response) => {
          console.log(`📡 ${response.message}`)
          documents = response.result.documents
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

  const handleRefresh = () => {
    loadData()
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
              나만의 AI Chat
            </h2>
            <p className="mt-1 text-xs text-gray-500">"{projectName}" 채팅</p>
          </div>
        </div>
      </div>

      {/* 채팅 영역 */}
      <div className="min-h-0 flex-1">
        {isLoading && (
          <div className="flex flex-1 flex-col items-center justify-center gap-3">
            <Loader2 className="text-primary h-8 w-8 animate-spin" />
            <p className="text-sm font-medium text-gray-500">
              목록을 불러오는 중입니다...
            </p>
          </div>
        )}

        {!isLoading && error && (
          <div className="flex flex-1 flex-col items-center justify-center gap-3">
            <AlertCircle className="h-8 w-8 text-red-500" />
            <p className="text-sm font-bold text-gray-700">{error}</p>
            <button
              onClick={handleRefresh}
              className="flex items-center gap-2 rounded-md bg-gray-100 px-3 py-1.5 text-xs font-bold text-gray-600 hover:bg-gray-200"
            >
              <RefreshCw className="h-3 w-3" />
              다시 시도
            </button>
          </div>
        )}

        {!isLoading && !error && (
          <ChatArea
            messages={messages}
            onSendMessage={handleSendMessage}
            onStop={handleStop}
            isStreaming={isStreaming}
          />
        )}
      </div>
    </div>
  )
}

export default function MyAiPage() {
  return (
    <Suspense
      fallback={
        <div className="flex h-screen items-center justify-center">
          <Loader2 className="h-10 w-10 animate-spin text-blue-500" />
        </div>
      }
    >
      <MyAiContent />
    </Suspense>
  )
}
