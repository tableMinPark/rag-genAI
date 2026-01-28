'use client'

import { Suspense, useEffect, useRef, useState } from 'react'
import ChatArea from '@/components/chat/ChatArea'
import { randomUUID, replaceEventDataToText } from '@/public/ts/commonUtil'
import { cancelStreamApi, streamApi } from '@/api/stream'
import { chatMyAiApi } from '@/api/chat'
import { useSearchParams } from 'next/navigation'
import { menuInfos } from '@/public/const/menu'
import { createAnswerMessage, createQueryMessage, Message } from '@/types/chat'
import { GreetingMessage } from '@/public/const/greeting'
import { StreamEvent } from '@/types/streamEvent'
import { useModalStore } from '@/stores/modalStore'
import NotFound from '@/components/NotFound'
import { Project } from '@/types/domain'
import { useUiStore } from '@/stores/uiStore'
import { getProjectApi } from '@/api/myai'

function MyAiContent() {
  const menuInfo = menuInfos.myai
  const uiStore = useUiStore()
  const modalStore = useModalStore()
  const searchParams = useSearchParams()
  const projectId = Number(searchParams.get('projectId'))

  // ###################################################
  // 상태 관리
  // ###################################################
  // 세션 ID 상태
  const [sessionId] = useState<string>(randomUUID())
  // 대화 내역 목록 상태
  const [messages, setMessages] = useState<Message[]>([])
  // 스트리밍 여부 상태
  const [isStreaming, setIsStreaming] = useState(false)
  const streamRef = useRef<EventSource | null>(null)
  // 프로젝트 리스트 상태
  const [project, setProject] = useState<Project | null>(null)

  // ###################################################
  // 랜더링 이펙트
  // ###################################################
  useEffect(() => {
    if (project) {
      uiStore.reset()
      setMessages([createAnswerMessage('', '')])
      let greetingMessageIndex = 0
      const greetingMessage = GreetingMessage.myai.replace(
        '<PROJECT_NAME/>',
        project ? `"${project.projectName}" ` : '',
      )
      const greetingMessageInterval = setInterval(() => {
        setMessages((prev) => {
          if (prev.length === 0) return prev
          const messages = [...prev]
          const lastIndex = 0
          messages[lastIndex] = {
            ...messages[lastIndex],
            content: replaceEventDataToText(
              greetingMessage.substring(0, greetingMessageIndex),
            ),
          }
          return messages
        })
        if (greetingMessageIndex >= greetingMessage.length) {
          clearInterval(greetingMessageInterval)
        } else {
          greetingMessageIndex++
        }
      }, 10)
      return () => clearInterval(greetingMessageInterval)
    }
  }, [project])

  useEffect(() => {
    uiStore.setLoading('프로젝트를 로딩중입니다')
    handleGetProject()
    return () => {
      streamRef.current?.close()
    }
  }, [])

  // ###################################################
  // 핸들러
  // ###################################################
  /**
   * 프로젝트 단건 조회 핸들러
   */
  const handleGetProject = async () => {
    uiStore.setLoading('프로젝트를 불러오는 중입니다')
    await getProjectApi(projectId)
      .then((response) => {
        console.log(`📡 ${response.message}`)
        setProject({
          projectId: response.result.projectId,
          projectName: response.result.projectName,
          projectDesc: response.result.projectDesc,
          sysCreateDt: response.result.sysCreateDt,
          sysModifyDt: response.result.sysModifyDt,
          sourceCount: response.result.sourceCount,
          chunkCount: response.result.chunkCount,
        })
        uiStore.reset()
      })
      .catch((reason) => {
        console.error(reason)
        uiStore.setError('프로젝트를 조회할 수 없습니다.', handleGetProject)
      })
  }

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
    streamRef.current = streamApi(
      sessionId,
      new StreamEvent({
        onConnect: async (_) => {
          console.log(`📡 질의 요청 : ${query}`)
          // 질의 등록
          setMessages((prev) => [...prev, createQueryMessage(query)])
          await chatMyAiApi(query, sessionId, projectId)
            .then((response) => {
              console.log(`📡 ${response.message}`)
              // 답변 등록
              setMessages((prev) => [...prev, createAnswerMessage('', '', [])])
            })
            .catch((reason) => {
              console.error(reason)
              modalStore.setError('서버 통신 에러', '답변 생성에 실패했습니다.')
              setIsStreaming(false)
              streamRef.current = null
            })
        },
        onDisconnect: (_) => {
          setIsStreaming(false)
          streamRef.current = null
        },
        onException: (_) => {
          setIsStreaming(false)
          streamRef.current = null
        },
        onError: (_) => {
          modalStore.setError('서버 통신 에러', '답변 생성에 실패했습니다.')
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
      .finally(() => {
        setIsStreaming(false)
        streamRef.current = null
      })
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
              <menuInfo.icon className="text-primary h-6 w-6" />
              {project ? project.projectName : menuInfo.name} Chat
            </h2>
            <p className="mt-1 text-xs text-gray-500">
              {project && <>"{project.projectName}" </>}
              {menuInfo.description}
            </p>
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

export default function MyAiPage() {
  return (
    <Suspense fallback={<NotFound />}>
      <MyAiContent />
    </Suspense>
  )
}
