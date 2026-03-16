'use client'

import { useEffect, useRef, useState } from 'react'
import MarkdownIt from 'markdown-it'
import { Play, Square, RotateCcw } from 'lucide-react'
import styles from '@/public/css/markdown.module.css'
import { randomUUID, replaceEventDataToText } from '@/public/ts/commonUtil'
import { cancelStreamApi, streamApi } from '@/api/stream'
import { chatSimulateionApi } from '@/api/chat'
import { StreamEvent } from '@/types/streamEvent'
import { useModalStore } from '@/stores/modalStore'
import { menuInfos } from '@/public/const/menu'

const md = new MarkdownIt({
  html: true,
  breaks: true,
  linkify: true,
})

// 기본 설정값
const DEFAULT_SYSTEM_PROMPT = ''
const DEFAULT_TEMPERATURE = 0.7
const DEFAULT_TOP_P = 0.95
const DEFAULT_MAX_TOKENS = 1200

export default function SimulationPage() {
  const menuInfo = menuInfos.simulation
  const modalStore = useModalStore()

  // ###################################################
  // 상태 관리
  // ###################################################
  // 세션 ID 상태
  const [sessionId] = useState<string>(randomUUID())
  // 질의문 입력 상태
  const [query, setQuery] = useState('')
  // 시스템 프롬프트 입력 상태
  const [prompt, setPrompt] = useState(DEFAULT_SYSTEM_PROMPT)
  // 참고 문서 입력 상태
  const [context, setContext] = useState('')
  // Temperature 입력 상태
  const [temperature, setTemperature] = useState(DEFAULT_TEMPERATURE)
  // TopP 입력 상태
  const [topP, setTopP] = useState(DEFAULT_TOP_P)
  // MaxTokens
  const [maxTokens, setMaxTokens] = useState(DEFAULT_MAX_TOKENS)
  // 스트리밍 상태
  const [isStreaming, setIsStreaming] = useState(false)
  const streamRef = useRef<EventSource | null>(null)
  // 실행 및 결과 상태
  const [output, setOutput] = useState('')

  // ###################################################
  // 랜더링 이펙트
  // ###################################################
  useEffect(() => {
    return () => {
      streamRef.current?.close()
    }
  }, [])

  // ###################################################
  // 핸들러
  // ###################################################
  /**
   * 입력값 초기화 핸들러
   */
  const handleReset = () => {
    setQuery('')
    setPrompt(DEFAULT_SYSTEM_PROMPT)
    setContext('')
    setTemperature(DEFAULT_TEMPERATURE)
    setTopP(DEFAULT_TOP_P)
    setMaxTokens(DEFAULT_MAX_TOKENS)
    setOutput('')
  }

  /**
   * 시뮬레이션 실행 핸들러
   */
  const handleSendQuery = async (
    query: string,
    prompt: string,
    context: string,
    temperature: number,
    topP: number,
    maxTokens: number,
  ) => {
    // 스트림 상태 체크
    if (isStreaming) return
    // 스트림 시작 상태 변경
    setIsStreaming(true)
    // 결과 값 초기화
    setOutput(
      `**설정된 파라미터:**\n- Temperature: \`${temperature}\`\n- Top P: \`${topP}\`\n- Max Tokens: \`${maxTokens}\`\n\n**System Prompt:**\n\`\`\`text\n${prompt.trim()}\n\`\`\`\n\n**Reference Context:**\n${context ? `\`\`\`text\n${context.trim()}\n\`\`\`` : '(참고 문서 없음)'}`,
    )
    // 세션 기반 SSE 연결
    streamRef.current = streamApi(
      sessionId,
      new StreamEvent({
        onConnect: async (_) => {
          console.log(`📡 질의 요청 : ${query}`)
          await chatSimulateionApi(
            query,
            sessionId,
            context,
            prompt,
            maxTokens,
            temperature,
            topP,
          )
            .then((response) => {
              console.log(`📡 ${response.message}`)
            })
            .catch((reason) => {
              console.error(reason)
              modalStore.setError(
                '서버 통신 에러',
                '시뮬레이션 실패',
                '시뮬레이션 실행에 실패했습니다.',
              )
              setIsStreaming(false)
              streamRef.current = null
            })
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
            '서버 통신 에러',
            '시뮬레이션 실패',
            '시뮬레이션 실행에 실패했습니다.',
          )
          setIsStreaming(false)
          streamRef.current = null
        },
        onInferenceStart: (_) => {
          setOutput((prev) =>
            replaceEventDataToText(prev + `\n\n---\n\n**추론:**\n`),
          )
        },
        onInference: (event) => {
          setOutput((prev) => replaceEventDataToText(prev + event.data))
        },
        onAnswerStart: (_) => {
          setOutput((prev) =>
            replaceEventDataToText(prev + `\n\n---\n\n**답변:**\n`),
          )
        },
        onAnswer: (event) => {
          setOutput((prev) => replaceEventDataToText(prev + event.data))
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
      {/* 헤더 */}
      <div className="mb-4 flex shrink-0 items-center gap-2">
        <div className="flex items-center gap-3">
          <div>
            <h2 className="flex items-center gap-2 text-2xl font-bold text-gray-800">
              <menuInfo.icon className="text-primary h-6 w-6" />
              {menuInfo.name}
            </h2>
            <p className="mt-1 text-xs text-gray-500">{menuInfo.description}</p>
          </div>
        </div>
      </div>
      {/* 메인 컨텐츠 (좌우 분할) */}
      <div className="flex min-h-0 flex-1 gap-6">
        {/* [왼쪽] 입력 폼 영역 */}
        <div className="flex h-full flex-1 flex-col gap-4">
          {/* 1. 질의문 (Input) */}
          <div className="shrink-0 rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
            <label className="mb-2 block text-sm font-bold text-gray-700">
              사용자 질의 (Query)
            </label>
            <input
              type="text"
              className="focus:border-primary focus:ring-primary w-full rounded-lg border border-gray-300 bg-gray-50 px-4 py-2.5 text-sm focus:ring-1 focus:outline-none"
              placeholder="LLM에게 던질 질문을 입력하세요."
              value={query}
              onChange={(e) => setQuery(e.target.value)}
            />
          </div>
          {/* 2. 시스템 프롬프트 (Textarea) */}
          <div className="shrink-0 rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
            <label className="mb-2 block text-sm font-bold text-gray-700">
              시스템 프롬프트 (Prompt) <span className="text-red-500">*</span>
            </label>
            <textarea
              className="focus:border-primary focus:ring-primary h-24 w-full resize-none rounded-lg border border-gray-300 bg-gray-50 px-4 py-2.5 text-sm leading-relaxed focus:ring-1 focus:outline-none"
              placeholder="AI의 페르소나나 지시사항을 입력하세요."
              value={prompt}
              onChange={(e) => setPrompt(e.target.value)}
            />
          </div>
          {/* 3. 참고 문서 (Textarea) */}
          <div className="flex min-h-37.5 flex-1 flex-col rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
            <label className="mb-2 block text-sm font-bold text-gray-700">
              참고 문서 (Context)
            </label>
            <textarea
              className="focus:border-primary focus:ring-primary w-full flex-1 resize-none rounded-lg border border-gray-300 bg-gray-50 px-4 py-2.5 text-sm leading-relaxed focus:ring-1 focus:outline-none"
              placeholder="LLM이 답변 생성 시 참고할 문맥 정보를 직접 입력하세요."
              value={context}
              onChange={(e) => setContext(e.target.value)}
            />
          </div>
          {/* 4. 파라미터 설정 */}
          <div className="grid shrink-0 grid-cols-3 gap-6 rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
            {/* Temperature */}
            <div className="flex flex-col gap-2">
              <div className="flex justify-between">
                <label className="text-xs font-bold text-gray-600">
                  Temperature <span className="text-red-500">*</span>
                </label>
                <span className="text-primary text-xs font-bold">
                  {temperature}
                </span>
              </div>
              <input
                type="range"
                min="0"
                max="2"
                step="0.1"
                value={temperature}
                onChange={(e) => setTemperature(parseFloat(e.target.value))}
                className="accent-primary h-2 w-full cursor-pointer appearance-none rounded-lg bg-gray-200"
              />
            </div>
            {/* Top P */}
            <div className="flex flex-col gap-2">
              <div className="flex justify-between">
                <label className="text-xs font-bold text-gray-600">
                  Top P <span className="text-red-500">*</span>
                </label>
                <span className="text-primary text-xs font-bold">{topP}</span>
              </div>
              <input
                type="range"
                min="0"
                max="1"
                step="0.05"
                value={topP}
                onChange={(e) => setTopP(parseFloat(e.target.value))}
                className="accent-primary h-2 w-full cursor-pointer appearance-none rounded-lg bg-gray-200"
              />
            </div>
            {/* Max Tokens */}
            <div className="flex flex-col gap-2">
              <div className="flex justify-between">
                <label className="text-xs font-bold text-gray-600">
                  Max Tokens <span className="text-red-500">*</span>
                </label>
                <span className="text-primary text-xs font-bold">
                  {maxTokens}
                </span>
              </div>
              <input
                type="range"
                min="10"
                max="4096"
                step="1"
                value={maxTokens}
                onChange={(e) => setMaxTokens(parseFloat(e.target.value))}
                className="accent-primary h-2 w-full cursor-pointer appearance-none rounded-lg bg-gray-200"
              />
            </div>
          </div>
          {/* 5. 버튼 그룹 */}
          <div className="flex shrink-0 gap-3 pt-2">
            <button
              onClick={handleReset}
              disabled={isStreaming}
              className="flex items-center gap-2 rounded-lg border border-gray-300 bg-white px-5 py-3 text-sm font-bold text-gray-700 transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <RotateCcw className="h-4 w-4" />
              초기화
            </button>
            {isStreaming ? (
              <button
                onClick={handleStop}
                className="flex flex-1 items-center justify-center gap-2 rounded-lg bg-gray-600 px-5 py-3 text-sm font-bold text-white shadow-md transition-colors hover:bg-gray-700 active:scale-95"
              >
                <Square className="h-4 w-4 fill-current" />
                생성 중단
              </button>
            ) : (
              <button
                disabled={!prompt}
                onClick={(e) =>
                  handleSendQuery(
                    query,
                    prompt,
                    context,
                    temperature,
                    topP,
                    maxTokens,
                  )
                }
                className="bg-primary hover:bg-primary-hover flex flex-1 items-center justify-center gap-2 rounded-lg px-5 py-3 text-sm font-bold text-white shadow-md transition-all active:scale-95 disabled:cursor-not-allowed disabled:bg-gray-300"
              >
                <Play className="h-4 w-4 fill-current" />
                테스트 실행
              </button>
            )}
          </div>
        </div>
        {/* [오른쪽] 답변 출력 영역 */}
        <div className="flex flex-1 flex-col overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
          {/* 헤더 */}
          <div className="flex h-14 items-center justify-between border-b border-gray-100 bg-gray-50 px-6 py-3">
            <span className="flex items-center gap-2 text-sm font-bold text-gray-700">
              시뮬레이션 결과 (Output)
              {isStreaming && (
                <span className="relative flex h-2 w-2">
                  <span className="bg-primary absolute inline-flex h-full w-full animate-ping rounded-full opacity-75"></span>
                  <span className="bg-primary relative inline-flex h-2 w-2 rounded-full"></span>
                </span>
              )}
            </span>
            {output && (
              <button
                className="hover:text-primary text-gray-400 transition-colors"
                onClick={() => navigator.clipboard.writeText(output)}
                title="결과 복사"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  width="18"
                  height="18"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <rect width="14" height="14" x="8" y="8" rx="2" ry="2" />
                  <path d="M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2" />
                </svg>
              </button>
            )}
          </div>
          {/* 결과 뷰어 */}
          <div className="flex-1 overflow-y-auto bg-gray-50/30 p-6">
            {output ? (
              <div
                className={`${styles.markdown} wrap-break-words text-sm leading-relaxed`}
                dangerouslySetInnerHTML={{ __html: md.render(output) }}
              />
            ) : (
              <div className="flex h-full flex-col items-center justify-center gap-3 text-gray-400">
                <div className="rounded-full bg-gray-100 p-4">
                  <Play className="h-8 w-8 text-gray-300" />
                </div>
                <p className="mt-4 text-sm">
                  왼쪽 폼을 입력하고 [ 테스트 실행 ] 을 눌러보세요.
                </p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
