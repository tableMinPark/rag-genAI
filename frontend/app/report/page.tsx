'use client'

import { useState, useRef } from 'react'
import { FileText } from 'lucide-react'

// ###################################################
// 상수 정의 (Const)
// ###################################################
// (현재는 상수가 없지만, 추후 확장 시 여기에 추가)

export default function ReportPage() {
  // ###################################################
  // 상태 정의 (State)
  // ###################################################
  // 입력 텍스트
  const [promptText, setPromptText] = useState('')
  const [contextText, setContextText] = useState('')
  // 출력 텍스트
  const [outputText, setOutputText] = useState('')
  // 파일 관련
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)
  // 프로세스 상태
  const [isGenerating, setIsGenerating] = useState(false)

  // ###################################################
  // 핸들러 (Handler)
  // ###################################################
  /**
   * 프롬프트(양식) 입력 핸들러
   */
  const handlePromptChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setPromptText(e.target.value)
  }

  /**
   * 참고 자료(Context) 입력 핸들러
   */
  const handleContextChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setContextText(e.target.value)
  }

  /**
   * 파일 업로드 핸들러
   */
  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) {
      setSelectedFile(file)
      setContextText(`📄 ${file.name}`)
      setOutputText('')
    }
  }

  /**
   * 파일 선택 초기화 핸들러
   */
  const clearFile = () => {
    setSelectedFile(null)
    setContextText('')
    setOutputText('')
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
  }

  /**
   * 보고서 생성 핸들러 (Mock)
   */
  const handleGenerate = () => {
    if (!promptText || (!contextText && !selectedFile)) {
      alert('보고서 양식과 참고 자료를 모두 입력해주세요.')
      return
    }

    setIsGenerating(true)

    // API 호출 시뮬레이션
    setTimeout(() => {
      const result = `# [보고서] ${selectedFile ? selectedFile.name : '참고 자료'} 기반 분석\n\n## 1. 개요\n사용자가 요청한 양식에 따라 작성된 보고서입니다. ${promptText.substring(0, 20)}...\n\n## 2. 주요 내용\n- 분석 결과 1\n- 분석 결과 2\n\n## 3. 결론\n성공적으로 보고서가 생성되었습니다.`
      setOutputText(result)
      setIsGenerating(false)
    }, 2000)
  }

  // ###################################################
  // 렌더링 (Render)
  // ###################################################
  return (
    <div className="flex h-full w-full flex-col p-6">
      {/* 헤더 */}
      <div className="mb-4 flex shrink-0 items-center gap-2">
        <FileText className="text-primary h-6 w-6" />
        <h2 className="text-2xl font-bold text-gray-800">보고서 생성</h2>
      </div>

      {/* 메인 영역: 좌우 분할 */}
      <div className="flex min-h-0 flex-1 gap-4">
        {/* [왼쪽] 입력 영역 컨테이너 (위/아래 2개 박스) */}
        <div className="flex flex-1 flex-col gap-4">
          {/* 1. 보고서 양식/프롬프트 입력 (상단) */}
          <div className="flex flex-1 flex-col overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
            <div className="border-b border-gray-100 bg-gray-50 px-4 py-3">
              <span className="text-sm font-bold text-gray-700">
                보고서 양식 (Prompt)
              </span>
            </div>
            <textarea
              className="flex-1 resize-none p-4 text-sm leading-relaxed text-gray-800 focus:outline-none"
              placeholder="작성할 보고서의 목차, 스타일, 필수 포함 사항 등을 입력하세요."
              value={promptText}
              onChange={handlePromptChange}
            />
          </div>

          {/* 2. 참고 자료/문맥 입력 + 파일 업로드 (하단) */}
          <div className="flex flex-1 flex-col overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
            <div className="flex items-center justify-between border-b border-gray-100 bg-gray-50 px-4 py-3">
              <span className="text-sm font-bold text-gray-700">
                참고 자료 (Context)
              </span>
              {selectedFile && (
                <button
                  onClick={clearFile}
                  className="text-xs font-medium text-red-500 hover:text-red-700 hover:underline"
                >
                  파일 취소
                </button>
              )}
            </div>

            <textarea
              className={`flex-1 resize-none p-4 text-sm leading-relaxed focus:outline-none ${
                selectedFile
                  ? 'cursor-not-allowed bg-gray-50 text-gray-500'
                  : 'bg-white text-gray-800'
              }`}
              placeholder="보고서 작성에 참고할 내용을 입력하거나, 아래에서 파일을 업로드하세요."
              value={contextText}
              onChange={handleContextChange}
              disabled={!!selectedFile}
            />

            {/* 파일 업로드 영역 */}
            <div className="border-t border-gray-100 bg-gray-50 p-4">
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
                    {selectedFile ? '파일 변경하기' : '참고 파일 업로드'}
                  </span>
                </div>
                <input
                  type="file"
                  className="hidden"
                  ref={fileInputRef}
                  onChange={handleFileUpload}
                />
              </label>
            </div>
          </div>
        </div>

        {/* [중앙] 생성 버튼 */}
        <div className="flex flex-col items-center justify-center">
          <button
            onClick={handleGenerate}
            disabled={
              isGenerating || !promptText || (!contextText && !selectedFile)
            }
            className="bg-primary hover:bg-primary-hover group relative flex h-12 w-12 items-center justify-center rounded-full text-white shadow-md transition-transform hover:scale-110 active:scale-95 disabled:cursor-not-allowed disabled:bg-gray-300"
            title="보고서 생성하기"
          >
            {isGenerating ? (
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

        {/* [오른쪽] 생성 결과 영역 */}
        <div className="flex flex-1 flex-col overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
          {/* 헤더 */}
          <div className="flex h-[52px] items-center justify-between border-b border-gray-100 bg-gray-50 px-4 py-3">
            <span className="text-sm font-bold text-gray-700">
              생성 결과 (Result)
            </span>
          </div>

          <div className="relative flex-1 bg-gray-50/30">
            <textarea
              readOnly
              className="h-full w-full resize-none bg-transparent p-4 text-sm leading-relaxed text-gray-800 focus:outline-none"
              placeholder="여기에 생성된 보고서가 표시됩니다."
              value={outputText}
            />

            {/* 복사 버튼 */}
            {outputText && (
              <button
                className="hover:text-primary absolute top-2 right-2 rounded-md border border-gray-200 bg-white p-2 text-gray-400 shadow-sm transition-colors"
                onClick={() => navigator.clipboard.writeText(outputText)}
                title="복사하기"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  width="16"
                  height="16"
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
        </div>
      </div>
    </div>
  )
}
