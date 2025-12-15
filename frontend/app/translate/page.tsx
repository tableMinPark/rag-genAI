'use client'

import { useState, useRef } from 'react'
import { Languages } from 'lucide-react'

// ###################################################
// 상수 정의 (Const)
// ###################################################
const LANGUAGES = [
  { code: 'KO', name: '한국어 (Korean)' },
  { code: 'EN', name: '영어 (English)' },
  { code: 'ZH', name: '중국어 (Chinese)' },
]

export default function TranslatePage() {
  // ###################################################
  // 상태 정의 (State)
  // ###################################################
  // 입력/출력 텍스트
  const [inputText, setInputText] = useState('')
  const [outputText, setOutputText] = useState('')
  // 파일 관련
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)
  // 프로세스 상태
  const [isTranslating, setIsTranslating] = useState(false)
  // 언어 선택 상태
  const [sourceLang, setSourceLang] = useState('EN') // 기본값: 영어
  const [targetLang, setTargetLang] = useState('KO') // 기본값: 한국어

  // ###################################################
  // 핸들러 (Handler)
  // ###################################################
  /**
   * 텍스트 입력 핸들러
   */
  const handleTextChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setInputText(e.target.value)
  }

  /**
   * 파일 업로드 핸들러
   */
  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) {
      setSelectedFile(file)
      setInputText(`📄 ${file.name}`)
      setOutputText('')
    }
  }

  /**
   * 파일 선택 초기화 핸들러
   */
  const clearFile = () => {
    setSelectedFile(null)
    setInputText('')
    setOutputText('')
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
  }

  /**
   * 번역 실행 핸들러 (Mock)
   */
  const handleTranslate = () => {
    if (!inputText && !selectedFile) return

    setIsTranslating(true)

    // API 호출 시뮬레이션 로그
    console.log(`Translate: ${sourceLang} -> ${targetLang}`)

    setTimeout(() => {
      const targetLangName = LANGUAGES.find(
        (l) => l.code === targetLang,
      )?.name.split(' ')[0]

      setOutputText(
        `[${targetLangName} 번역 결과]\n${inputText}\n\n(실제 번역 결과가 여기에 표시됩니다.)`,
      )
      setIsTranslating(false)
    }, 1500)
  }

  // ###################################################
  // 렌더링 (Render)
  // ###################################################
  return (
    <div className="flex h-full w-full flex-col p-6">
      {/* 헤더 */}
      <div className="mb-4 flex shrink-0 items-center gap-2">
        <Languages className="text-primary h-6 w-6" />
        <h2 className="text-2xl font-bold text-gray-800">번역</h2>
      </div>

      {/* 메인 영역: 좌우 분할 */}
      <div className="flex min-h-0 flex-1 gap-4">
        {/* [왼쪽] 원문 입력 영역 */}
        <div className="flex flex-1 flex-col overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
          {/* 헤더: 언어 선택 */}
          <div className="flex h-[52px] items-center justify-between border-b border-gray-100 bg-gray-50 px-4 py-3">
            <div className="flex items-center gap-2">
              <span className="text-xs font-bold text-gray-500">FROM</span>
              <select
                value={sourceLang}
                onChange={(e) => setSourceLang(e.target.value)}
                className="hover:text-primary cursor-pointer bg-transparent text-sm font-bold text-gray-800 transition-colors focus:outline-none"
              >
                {LANGUAGES.map((lang) => (
                  <option key={lang.code} value={lang.code}>
                    {lang.name}
                  </option>
                ))}
              </select>
            </div>

            {selectedFile && (
              <button
                onClick={clearFile}
                className="text-xs font-medium text-red-500 underline hover:text-red-700"
              >
                파일 취소
              </button>
            )}
          </div>

          <div className="relative flex-1">
            <textarea
              className={`h-full w-full resize-none p-4 leading-relaxed text-gray-800 focus:outline-none ${
                selectedFile
                  ? 'cursor-not-allowed bg-gray-100 text-gray-500'
                  : 'bg-white'
              }`}
              placeholder="번역할 텍스트를 입력하거나, 아래에서 파일을 업로드하세요."
              value={inputText}
              onChange={handleTextChange}
              disabled={!!selectedFile}
            />
          </div>

          {/* 하단 파일 업로드 영역 */}
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
                  {selectedFile ? '파일 변경하기' : '파일 업로드'}
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

        {/* [중앙] 번역 버튼 */}
        <div className="flex flex-col items-center justify-center">
          <button
            onClick={handleTranslate}
            disabled={isTranslating || (!inputText && !selectedFile)}
            className="bg-primary hover:bg-primary-hover group relative flex h-12 w-12 items-center justify-center rounded-full text-white shadow-md transition-transform hover:scale-110 active:scale-95 disabled:cursor-not-allowed disabled:bg-gray-300"
            title="번역하기"
          >
            {isTranslating ? (
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
                <path d="M5 12h14"></path>
                <path d="m12 5 7 7-7 7"></path>
              </svg>
            )}
          </button>
        </div>

        {/* [오른쪽] 번역 결과 영역 */}
        <div className="flex flex-1 flex-col overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
          {/* 헤더: 언어 선택 */}
          <div className="flex h-[52px] items-center justify-between border-b border-gray-100 bg-gray-50 px-4 py-3">
            <div className="flex items-center gap-2">
              <span className="text-xs font-bold text-gray-500">TO</span>
              <select
                value={targetLang}
                onChange={(e) => setTargetLang(e.target.value)}
                className="text-primary hover:text-primary-hover cursor-pointer bg-transparent text-sm font-bold transition-colors focus:outline-none"
              >
                {LANGUAGES.map((lang) => (
                  <option key={lang.code} value={lang.code}>
                    {lang.name}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* 결과 텍스트 */}
          <div className="relative flex-1 bg-gray-50/30">
            <textarea
              readOnly
              className="h-full w-full resize-none bg-transparent p-4 leading-relaxed text-gray-800 focus:outline-none"
              placeholder="번역 결과가 여기에 표시됩니다."
              value={outputText}
            />
            {outputText && (
              <button
                className="hover:text-primary absolute top-2 right-2 rounded-md border border-gray-200 bg-white p-2 text-gray-400 shadow-sm"
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
