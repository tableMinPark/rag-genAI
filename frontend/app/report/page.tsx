'use client'

import { useState, useRef, useEffect } from 'react'
import MarkdownIt from 'markdown-it'
import { FileText, Loader2, Play, X } from 'lucide-react'
import styles from '@/public/css/markdown.module.css'
import { randomUUID, replaceEventDataToText } from '@/public/ts/commonUtil'
import { generateReportFileApi, generateReportTextApi } from '@/api/report'
import { getMenuInfo } from '@/public/const/menu'
import { useModalStore } from '@/stores/modalStore'
import { FetchEventSource, streamApi } from '@/api/stream'
import { Prepare, StreamEvent } from '@/types/streamEvent'

const ALLOW_EXT = ['pdf', 'hwp', 'hwpx']

const md = new MarkdownIt({
  html: true,
  breaks: true,
  linkify: true,
})

export default function ReportPage() {
  const menuInfo = getMenuInfo('report')
  const modalStore = useModalStore()

  // ###################################################
  // 상태 관리
  // ###################################################
  // 세션 ID 상태
  const [sessionId] = useState<string>(randomUUID())
  // 입력 텍스트
  const [title, setTitle] = useState('')
  // 시스템 프롬프트 입력 상태 (보고서 양식)
  const [requestContent, setRequestContent] = useState('')
  // 참고 문서 입력 상태
  const [context, setContext] = useState('')
  // 파일 상태
  const [selectedFile, setSelectedFile] = useState<File[]>([])
  // 스트리밍 상태
  const [isStreaming, setIsStreaming] = useState(false)
  const streamRef = useRef<FetchEventSource | null>(null)
  // 스트림 상태
  const [prepare, setPrepare] = useState<Prepare | null>(null)
  // 출력 텍스트
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
   * 파일 업로드 핸들러
   */
  const handleSelectFiles = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      const files = Array.from(e.target.files)
      setSelectedFile((prev) => [...prev, ...files])
    }
  }

  /**
   * 새로운 파일 삭제 핸들러
   * @param index 파일 인덱스
   */
  const handleRemoveFile = (index: number) => {
    setSelectedFile((prev) => {
      const now = prev.filter((_, i) => i !== index)
      if (now.length == 0) {
        setContext('')
      }
      return now
    })
  }

  /**
   * 보고서 생성 핸들러
   */
  const handleGenerateReport = async () => {
    // 스트림 상태 체크
    if (isStreaming) return
    // 스트림 시작 상태 변경
    setIsStreaming(true)
    // 결과 값 초기화
    setOutput('')
    // 스트림 옵션 설정
    const streamEvent = new StreamEvent({
      onConnect: async (_) => {
        console.log(`📡 보고서 생성 요청 : ${title}`)
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
          '보고서 생성 실패',
          '보고서 생성에 실패했습니다.',
        )
        setIsStreaming(false)
        streamRef.current = null
      },
      onInference: (event) => {
        setOutput((prev) => replaceEventDataToText(prev + event.data))
      },
      onAnswerStart: (_) => {
        setOutput('')
      },
      onAnswer: (event) => {
        setOutput((prev) => replaceEventDataToText(prev + event.data))
      },
      onPrepare: (event) => {
        setPrepare(JSON.parse(event.data))
      },
    })
    // 세션 기반 SSE 연결
    if (selectedFile.length == 0) {
      await generateReportTextApi(
        sessionId,
        requestContent,
        title,
        context,
        streamEvent,
      )
        .then((stream) => {
          console.log(`📡 답변 요청 성공`)
          streamRef.current = stream
        })
        .catch((reason) => {
          console.error(reason)
          modalStore.setError(
            '서버 통신 에러',
            '보고서 생성 실패',
            '보고서 생성에 실패했습니다.',
          )
          setIsStreaming(false)
          streamRef.current = null
        })
    } else {
      await generateReportFileApi(
        sessionId,
        requestContent,
        title,
        selectedFile,
        streamEvent,
      )
        .then((stream) => {
          console.log(`📡 답변 요청 성공`)
          streamRef.current = stream
        })
        .catch((reason) => {
          console.error(reason)
          modalStore.setError(
            '서버 통신 에러',
            '보고서 생성 실패',
            '보고서 생성에 실패했습니다.',
          )
          setIsStreaming(false)
          streamRef.current = null
        })
    }
  }

  // ###################################################
  // 렌더링 (Render)
  // ###################################################
  const size = 48
  const strokeWidth = 2
  const radius = (size - strokeWidth) / 2
  const circumference = 2 * Math.PI * radius
  return (
    <div className="flex h-full w-full flex-col p-6">
      {/* 헤더 영역 */}
      <div className="mb-6 flex items-center justify-between">
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
      {/* 메인 영역: 좌우 분할 */}
      <div className="flex min-h-0 flex-1 gap-4">
        {/* [왼쪽] 입력 영역 컨테이너 (위/아래 2개 박스) */}
        <div className="flex flex-1 flex-col gap-4">
          <div className="shrink-0 rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
            <label className="mb-2 block text-sm font-bold text-gray-700">
              보고서 제목 (Report title) <span className="text-red-500">*</span>
            </label>
            <input
              disabled={isStreaming}
              type="text"
              className="focus:border-primary focus:ring-primary w-full rounded-lg border border-gray-300 bg-gray-50 px-4 py-2.5 text-sm focus:ring-1 focus:outline-none"
              placeholder="보고서 제목을 입력하세요."
              value={title}
              onChange={(e) => setTitle(e.target.value)}
            />
          </div>
          {/* 1. 보고서 양식/프롬프트 입력 (상단) */}
          <div className="shrink-0 rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
            <label className="mb-2 block text-sm font-bold text-gray-700">
              작성 요청 사항 <span className="text-red-500">*</span>
            </label>
            <textarea
              disabled={isStreaming}
              className="focus:border-primary focus:ring-primary h-24 w-full resize-none rounded-lg border border-gray-300 bg-gray-50 px-4 py-2.5 text-sm leading-relaxed focus:ring-1 focus:outline-none"
              placeholder="작성할 보고서의 목차, 스타일, 필수 포함 사항 등을 입력하세요."
              value={requestContent}
              onChange={(e) => setRequestContent(e.target.value)}
            />
          </div>
          <div className="flex min-h-37.5 flex-1 flex-col rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
            <label className="mb-2 block text-sm font-bold text-gray-700">
              참고 자료 (Context) <span className="text-red-500">*</span>
            </label>
            {selectedFile.length == 0 ? (
              /* 참고 자료 텍스트 */
              <textarea
                disabled={isStreaming}
                className="focus:border-primary focus:ring-primary mb-4 w-full flex-1 resize-none rounded-lg border border-gray-300 bg-gray-50 px-4 py-2.5 text-sm leading-relaxed focus:ring-1 focus:outline-none"
                placeholder="보고서 작성에 참고할 내용을 입력하거나, 아래에서 파일을 업로드하세요."
                value={context}
                onChange={(e) => setContext(e.target.value)}
              />
            ) : (
              /* 추가된 파일 목록 */
              <div className="flex-1 py-2.5 focus:outline-none">
                <div className="flex max-h-80 flex-col gap-2 overflow-y-auto pr-2">
                  {selectedFile.map((file, index) => (
                    <div
                      key={index}
                      className="flex items-center justify-between rounded-lg border border-gray-200 bg-gray-50 px-3 py-2 text-sm"
                    >
                      <div className="flex items-center gap-2 overflow-hidden">
                        <FileText className="h-4 w-4 shrink-0 text-blue-500" />
                        <span className="truncate text-gray-700">
                          {file.name.substring(0, 55) +
                            (file.name.length > 50 ? '...' : '')}
                        </span>
                      </div>
                      {!isStreaming && (
                        <button
                          onClick={() => handleRemoveFile(index)}
                          className="text-gray-400 hover:text-red-500"
                        >
                          <X className="h-4 w-4" />
                        </button>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            )}
            {/* 파일 업로드 영역 */}
            {!isStreaming && (
              <div className="border-t border-gray-100 bg-gray-50">
                <label className="hover:border-primary group flex w-full cursor-pointer items-center justify-center rounded-lg border-2 border-dashed border-gray-300 p-3 transition-colors hover:bg-red-50">
                  <div className="group-hover:text-primary flex items-center gap-2 text-gray-500">
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      width="20"
                      height="20"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="2"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    >
                      <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                      <polyline points="17 8 12 3 7 8"></polyline>
                      <line x1="12" y1="3" x2="12" y2="15"></line>
                    </svg>
                    <span className="text-sm font-medium">
                      {selectedFile.length > 0
                        ? `참고 파일 추가 (${ALLOW_EXT.map((ext) => ext.toUpperCase()).join(', ')})`
                        : `참고 파일 등록 (${ALLOW_EXT.map((ext) => ext.toUpperCase()).join(', ')})`}
                    </span>
                  </div>
                  <input
                    type="file"
                    className="hidden"
                    accept={ALLOW_EXT.map((ext) => `.${ext}`).join(', ')}
                    multiple
                    onChange={handleSelectFiles}
                  />
                </label>
              </div>
            )}
          </div>
        </div>
        {/* [중앙] 생성 버튼 영역 */}
        <div className="flex flex-col items-center justify-center">
          <div className="relative flex items-center justify-center">
            {/* 1. Progress Ring SVG */}
            {/* 버튼이 로딩 중(isStreaming)이거나 준비 중일 때만 보여주거나, 항상 보여주되 색상만 제어할 수 있습니다. */}
            <svg
              className="absolute top-0 left-0 z-20 -rotate-90 transform"
              width={size}
              height={size}
              viewBox={`0 0 ${size} ${size}`}
              fill="none"
              style={{ pointerEvents: 'none' }} // 클릭은 버튼이 받도록 통과시킴
            >
              {/* (옵션) 배경 트랙: 로딩 중일 때만 연한 회색으로 표시 */}
              {isStreaming && prepare && (
                <circle
                  cx={size / 2}
                  cy={size / 2}
                  r={radius}
                  stroke="#e5e7eb" // gray-200
                  strokeWidth={strokeWidth}
                  fill="none"
                />
              )}
              {/* 실제 진행 바 (Progress) */}
              {prepare && prepare.progress < 1 && (
                <circle
                  cx={size / 2}
                  cy={size / 2}
                  r={radius}
                  stroke="currentColor"
                  strokeWidth={strokeWidth}
                  fill="none"
                  strokeLinecap="round"
                  className={`text-primary transition-all duration-300 ease-out ${
                    isStreaming ? 'opacity-100' : 'opacity-0'
                  }`}
                  style={{
                    strokeDasharray: circumference,
                    strokeDashoffset: isStreaming
                      ? circumference - prepare.progress * circumference
                      : circumference,
                  }}
                />
              )}
            </svg>
            {/* 2. 중앙 버튼 */}
            <button
              onClick={handleGenerateReport}
              disabled={
                isStreaming ||
                !title ||
                !requestContent ||
                (!context && selectedFile.length == 0)
              }
              className={`group relative z-10 flex h-12 w-12 items-center justify-center rounded-full shadow-md transition-all ${!isStreaming ? 'hover:scale-110' : ''} active:scale-95 disabled:cursor-not-allowed disabled:bg-gray-300 ${
                isStreaming
                  ? 'text-primary bg-white'
                  : 'bg-primary hover:bg-primary-hover text-white'
              }`}
              title="보고서 생성하기"
            >
              {isStreaming ? (
                // 로딩 중: 진행률 텍스트 혹은 정지 아이콘 표시
                // <span className="text-xs font-bold">
                //   {Math.round(prepare.progress * 100)}%
                // </span>
                <div className="h-5 w-5 animate-spin rounded-full border-2 border-white border-t-transparent"></div>
              ) : (
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  width="24"
                  height="24"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z" />
                  <polyline points="14 2 14 8 20 8" />
                  <line x1="12" x2="12" y1="18" y2="12" />
                  <line x1="9" x2="15" y1="15" y2="15" />
                </svg>
              )}
            </button>
          </div>
        </div>
        {/* [오른쪽] 생성 결과 영역 */}
        <div className="flex flex-1 flex-col overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
          {/* 헤더 */}
          <div className="flex h-13 items-center justify-between border-b border-gray-100 bg-gray-50 px-4 py-3">
            <span className="text-sm font-bold text-gray-700">
              생성 결과 (Result)
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
                {!isStreaming ? (
                  <>
                    <div className="rounded-full bg-gray-100 p-4">
                      <Play className="h-8 w-8 text-gray-300" />
                    </div>
                    <p className="mt-4 text-sm">
                      왼쪽 폼을 입력하고 버튼을 눌러보세요.
                    </p>
                  </>
                ) : (
                  <>
                    <div className="rounded-full bg-gray-100 p-4">
                      <Loader2 className="h-8 w-8 animate-spin text-gray-300" />
                    </div>
                    <p className="mt-4 text-sm">
                      보고서 초안을 생성중입니다...
                      {prepare && prepare.progress
                        ? `(${Math.round(prepare.progress * 100)}%)`
                        : ''}
                    </p>
                  </>
                )}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
