'use client'

import React, { useState, useRef } from 'react'
import {
  Upload,
  FileText,
  Code,
  FileType,
  RefreshCw,
  CheckCircle2,
  X,
  Copy,
  Check,
} from 'lucide-react'
import { extractFileApi, extractFileTextApi } from '@/api/extract'
import { measureRequest, replaceEventDataToText } from '@/public/ts/commonUtil'

// ###################################################
// 상수 및 타입 정의 (Constants & Types)
// ###################################################
// 추출 옵션 타입
type ExtractType = 'html' | 'markdown' | 'text'

export default function ExtractPage() {
  // ###################################################
  // 상태 정의 (State)
  // ###################################################
  // 파일 관련 상태
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)

  // 추출 설정 및 결과 상태
  const [extractType, setExtractType] = useState<ExtractType>('markdown')
  const [isProcessing, setIsProcessing] = useState(false)
  const [extractedLines, setExtractedLines] = useState<string[]>([])
  const [processTime, setProcessTime] = useState<string | null>(null)

  // UI 상호작용 상태 (복사 피드백)
  const [copiedIndex, setCopiedIndex] = useState<number | null>(null)

  // ###################################################
  // 핸들러 (Handler)
  // ###################################################
  /**
   * 파일 선택 핸들러
   */
  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) {
      setSelectedFile(file)
      setExtractedLines([])
      setProcessTime(null)
    }
  }

  /**
   * 파일 선택 초기화 핸들러
   */
  const clearFile = () => {
    setSelectedFile(null)
    setExtractedLines([])
    setProcessTime(null)
    if (fileInputRef.current) fileInputRef.current.value = ''
  }

  /**
   * 텍스트 추출 실행 핸들러
   */
  const handleExtract = async () => {
    if (!selectedFile) return alert('파일을 업로드해주세요.')

    setIsProcessing(true)
    setExtractedLines([])

    if (extractType === 'text') {
      await measureRequest(() => extractFileTextApi(selectedFile))
        .then(({ response, duration }) => {
          setExtractedLines([response.result])
          setProcessTime(`${duration.toFixed(2)}ms`)
        })
        .catch((reason) => {
          console.error(reason)
          alert(
            '서버와 통신이 원할하지 않습니다.\n\n잠시후 다시 시도 해주세요.',
          )
        })
    } else {
      await measureRequest(() => extractFileApi(extractType, selectedFile))
        .then(({ response, duration }) => {
          setExtractedLines(response.result.lines.map((line) => line.content))
          setProcessTime(`${duration.toFixed(2)}ms`)
        })
        .catch((reason) => {
          console.error(reason)
          alert(
            '서버와 통신이 원할하지 않습니다.\n\n잠시후 다시 시도 해주세요.',
          )
        })
    }
    setIsProcessing(false)
  }

  /**
   * 라인별 텍스트 복사 핸들러
   */
  const handleCopyLine = (text: string, index: number) => {
    navigator.clipboard.writeText(text)
    setCopiedIndex(index)
    // 2초 후 아이콘 초기화
    setTimeout(() => setCopiedIndex(null), 2000)
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
              <FileText className="text-primary h-6 w-6" />
              문서 추출
            </h2>
            <p className="mt-1 text-xs text-gray-500">파일 문서 텍스트 추출</p>
          </div>
        </div>
      </div>

      {/* 메인 컨텐츠 (좌우 분할) */}
      <div className="flex min-h-0 flex-1 gap-6">
        {/* [왼쪽] 설정 및 업로드 패널 */}
        <div className="flex w-[400px] flex-col gap-6">
          {/* 1. 파일 업로드 카드 */}
          <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
            <h3 className="mb-4 text-sm font-bold text-gray-700">
              대상 파일 업로드
            </h3>
            {!selectedFile ? (
              <label className="group hover:border-primary flex h-40 w-full cursor-pointer flex-col items-center justify-center rounded-lg border-2 border-dashed border-gray-300 bg-gray-50 transition-colors hover:bg-red-50">
                <div className="flex flex-col items-center justify-center pt-5 pb-6">
                  <Upload className="group-hover:text-primary mb-3 h-8 w-8 text-gray-400" />
                  <p className="mb-2 text-sm text-gray-500 group-hover:text-gray-700">
                    <span className="font-semibold">클릭하여 업로드</span> 또는
                    드래그
                  </p>
                  <p className="text-xs text-gray-400">
                    PDF, DOCX, HWP (MAX. 10MB)
                  </p>
                </div>
                <input
                  type="file"
                  className="hidden"
                  ref={fileInputRef}
                  onChange={handleFileUpload}
                  accept=".pdf,.docx,.hwp,.txt"
                />
              </label>
            ) : (
              <div className="border-primary/20 relative flex items-center gap-4 rounded-lg border bg-red-50 p-4">
                <div className="text-primary flex h-10 w-10 items-center justify-center rounded-full bg-white shadow-sm">
                  <FileText className="h-5 w-5" />
                </div>
                <div className="flex-1 overflow-hidden">
                  <p className="truncate text-sm font-bold text-gray-800">
                    {selectedFile.name}
                  </p>
                  <p className="text-xs text-gray-500">
                    {(selectedFile.size / 1024).toFixed(1)} KB
                  </p>
                </div>
                <button
                  onClick={clearFile}
                  className="rounded-full p-1 text-gray-400 hover:bg-white hover:text-red-500 hover:shadow-sm"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>
            )}
          </div>

          {/* 2. 추출 옵션 선택 카드 */}
          <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
            <h3 className="mb-4 text-sm font-bold text-gray-700">
              추출 포맷 설정
            </h3>
            <div className="grid grid-cols-3 gap-3">
              <button
                onClick={() => setExtractType('text')}
                className={`flex flex-col items-center justify-center gap-2 rounded-lg border p-4 transition-all ${
                  extractType === 'text'
                    ? 'border-primary text-primary ring-primary bg-red-50 ring-1'
                    : 'hover:border-primary/50 border-gray-200 bg-white text-gray-600'
                }`}
              >
                <FileText className="h-6 w-6" />
                <span className="text-sm font-bold">PlainText</span>
              </button>
              <button
                onClick={() => setExtractType('markdown')}
                className={`flex flex-col items-center justify-center gap-2 rounded-lg border p-4 transition-all ${
                  extractType === 'markdown'
                    ? 'border-primary text-primary ring-primary bg-red-50 ring-1'
                    : 'hover:border-primary/50 border-gray-200 bg-white text-gray-600'
                }`}
              >
                <FileType className="h-6 w-6" />
                <span className="text-sm font-bold">Markdown</span>
              </button>
              <button
                onClick={() => setExtractType('html')}
                className={`flex flex-col items-center justify-center gap-2 rounded-lg border p-4 transition-all ${
                  extractType === 'html'
                    ? 'border-primary text-primary ring-primary bg-red-50 ring-1'
                    : 'hover:border-primary/50 border-gray-200 bg-white text-gray-600'
                }`}
              >
                <Code className="h-6 w-6" />
                <span className="text-sm font-bold">HTML</span>
              </button>
            </div>
            <div className="mt-4 rounded bg-gray-50 p-3 text-xs text-gray-500">
              <p>
                • <b>PlainText</b>: 표 및 이미지를 제외한 텍스트만을 추출합니다.
              </p>
              <p className="mt-1">
                • <b>Markdown</b>: LLM 학습 및 색인에 최적화된 경량화된
                포맷입니다.
              </p>
              <p className="mt-1">
                • <b>HTML</b>: 원본 문서의 스타일과 구조(표, 이미지 위치 등)를
                최대한 유지합니다.
              </p>
            </div>
          </div>

          {/* 실행 버튼 */}
          <button
            onClick={handleExtract}
            disabled={!selectedFile || isProcessing}
            className="bg-primary hover:bg-primary-hover flex w-full items-center justify-center gap-2 rounded-xl py-4 text-base font-bold text-white shadow-md transition-all active:scale-95 disabled:cursor-not-allowed disabled:bg-gray-300"
          >
            {isProcessing ? (
              <>
                <RefreshCw className="h-5 w-5 animate-spin" />
                추출 진행 중...
              </>
            ) : (
              <>
                <CheckCircle2 className="h-5 w-5" />
                텍스트 추출 시작
              </>
            )}
          </button>
        </div>

        {/* [오른쪽] 결과 출력 패널 */}
        <div className="flex flex-1 flex-col overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
          {/* 헤더 */}
          <div className="flex items-center justify-between border-b border-gray-100 bg-gray-50 px-6 py-4">
            <div className="flex items-center gap-2">
              <span className="text-sm font-bold text-gray-700">
                추출 결과 (Line Segments)
              </span>
              {extractedLines.length > 0 && (
                <span className="rounded-full bg-blue-100 px-2 py-0.5 text-xs font-bold text-blue-700">
                  Total {extractedLines.length} lines
                </span>
              )}
            </div>
            {processTime && (
              <span className="font-mono text-xs text-gray-400">
                Time: {processTime}
              </span>
            )}
          </div>

          {/* 결과 리스트 (스크롤 영역) */}
          <div className="flex-1 overflow-y-auto bg-gray-50/30 p-6">
            {extractedLines.length > 0 ? (
              <div className="flex flex-col gap-2">
                {extractedLines.map((line, index) => (
                  <div
                    key={index}
                    className="group hover:border-primary/30 relative flex gap-4 rounded-lg border border-gray-100 bg-white p-3 shadow-sm transition-colors"
                  >
                    {/* 라인 번호 */}
                    <span className="group-hover:text-primary/50 w-8 shrink-0 pt-1 text-right font-mono text-xs text-gray-300 select-none">
                      {index + 1}
                    </span>

                    {/* 텍스트 내용 */}
                    <div className="flex-1 overflow-hidden pr-8">
                      <p className="font-mono text-sm leading-relaxed whitespace-pre-wrap text-gray-700">
                        {line}
                      </p>
                    </div>

                    {/* 복사 버튼 (Hover 시 등장) */}
                    <button
                      onClick={() => handleCopyLine(line, index)}
                      className="hover:text-primary absolute top-2 right-2 rounded p-1.5 text-gray-400 opacity-0 transition-all group-hover:opacity-100 hover:bg-gray-100"
                      title="이 라인 복사하기"
                    >
                      {copiedIndex === index ? (
                        <Check className="h-4 w-4 text-green-500" />
                      ) : (
                        <Copy className="h-4 w-4" />
                      )}
                    </button>
                  </div>
                ))}
              </div>
            ) : (
              // 빈 상태 (Placeholder)
              <div className="flex h-full flex-col items-center justify-center gap-4 text-gray-400">
                <div className="rounded-full bg-gray-100 p-6">
                  <FileText className="h-10 w-10 text-gray-300" />
                </div>
                <div className="text-center">
                  <p className="text-sm font-bold text-gray-500">
                    추출된 데이터가 없습니다.
                  </p>
                  <p className="mt-1 text-xs">
                    파일을 업로드하고 추출을 시작해보세요.
                  </p>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
