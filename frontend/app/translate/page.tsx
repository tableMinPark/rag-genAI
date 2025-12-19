'use client'

import { useState, useRef, useEffect } from 'react'
import MarkdownIt from 'markdown-it'
import { Languages, Play } from 'lucide-react'
import styles from '@/public/css/markdown.module.css'
import { randomUUID, replaceEventDataToText } from '@/public/ts/commonUtil'
import {
  getTranslateLanguagesApi,
  translateFileApi,
  translateTextApi,
} from '@/api/translate'
import { TranslateLanguage } from '@/types/domain'

// ###################################################
// 상수 정의 (Const)
// ###################################################
// Markdown 파서 설정
const md = new MarkdownIt({
  html: true,
  breaks: true,
  linkify: true,
})

export default function TranslatePage() {
  // ###################################################
  // 상태 정의 (State)
  // ###################################################
  // 세션 ID 상태
  const [sessionId] = useState<string>(randomUUID())
  // 입력/출력 텍스트
  const [inputText, setInputText] = useState('')
  const [outputText, setOutputText] = useState('')
  // 파일 관련
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)
  // 프로세스 상태
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [isTranslating, setIsTranslating] = useState(false)
  // 언어 선택 상태
  const [languages, setLanguages] = useState<TranslateLanguage[]>([])
  const [sourceLang, setSourceLang] = useState('EN') // 기본값: 영어
  const [targetLang, setTargetLang] = useState('KR') // 기본값: 한국어
  const [containDictionary, setContainDictionary] = useState(false)

  const loadData = async () => {
    setIsLoading(true)
    setError(null)
    try {
      await getTranslateLanguagesApi().then((response) => {
        console.log(`📡 ${response.message}`)
        setLanguages(response.data)

        if (response.data.length == 0) {
          setError('번역 가능 언어가 없습니다.')
        } else {
          setSourceLang(response.data[0].code)
          setTargetLang(
            response.data.length > 1
              ? response.data[1].code
              : response.data[0].code,
          )
        }
      })
    } catch (err) {
      console.error(err)
      setError('번역 언어 목록을 불러올 수 없습니다.')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [])

  // ###################################################
  // 핸들러 (Handler)
  // ###################################################
  const handleRefresh = () => {
    loadData()
  }

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
  const handleTranslate = async () => {
    if (!inputText && !selectedFile) {
      alert('번역할 텍스트 또는 파일를 입력해주세요.')
      return
    }

    setIsTranslating(true)

    if (!selectedFile) {
      await translateTextApi(
        sessionId,
        sourceLang,
        targetLang,
        containDictionary,
        inputText,
      )
        .then((response) => {
          console.log(`📡 ${response.message}`)
          setOutputText(replaceEventDataToText(response.data.content))
        })
        .catch((reason) => {
          console.error(reason)
          setOutputText(
            '서버와 통신이 원할하지 않습니다.\n\n잠시후 다시 시도 해주세요.',
          )
          setIsTranslating(false)
        })
    } else {
      await translateFileApi(
        sessionId,
        sourceLang,
        targetLang,
        containDictionary,
        selectedFile,
      )
        .then((response) => {
          console.log(`📡 ${response.message}`)
          setOutputText(replaceEventDataToText(response.data.content))
        })
        .catch((reason) => {
          console.error(reason)
          setOutputText(
            '서버와 통신이 원할하지 않습니다.\n\n잠시후 다시 시도 해주세요.',
          )
          setIsTranslating(false)
        })
    }

    setIsTranslating(false)
  }

  // ###################################################
  // 렌더링 (Render)
  // ###################################################
  return (
    <div className="flex h-full w-full flex-col p-6">
      {/* 헤더 */}
      <div className="mb-6 flex shrink-0 items-center justify-between">
        {/* 왼쪽: 타이틀 */}
        <div className="flex items-center gap-3">
          <div>
            <h2 className="flex items-center gap-2 text-2xl font-bold text-gray-800">
              <Languages className="text-primary h-6 w-6" />
              번역
            </h2>
            <p className="mt-1 text-xs text-gray-500">텍스트 및 파일 번역</p>
          </div>
        </div>

        {/* 오른쪽: 사전 적용 토글 버튼 [추가됨] */}
        <div className="flex items-center gap-3">
          <span className="text-sm font-bold text-gray-600">
            사전 적용 여부
          </span>
          <label className="relative inline-flex cursor-pointer items-center">
            <input
              type="checkbox"
              checked={containDictionary}
              onChange={(e) => setContainDictionary(e.target.checked)}
              className="peer sr-only"
            />
            <div className="peer peer-checked:bg-primary peer-focus:ring-primary/20 h-6 w-11 rounded-full bg-gray-200 peer-focus:ring-2 peer-focus:outline-none after:absolute after:top-[2px] after:left-[2px] after:h-5 after:w-5 after:rounded-full after:border after:border-gray-300 after:bg-white after:transition-all after:content-[''] peer-checked:after:translate-x-full peer-checked:after:border-white"></div>
          </label>
        </div>
      </div>

      {/* 메인 영역: 좌우 분할 */}
      <div className="flex min-h-0 flex-1 gap-4">
        {!isLoading && !error && (
          <>
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
                    {languages.map((lang) => (
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
                    {languages.map((lang) => (
                      <option key={lang.code} value={lang.code}>
                        {lang.name}
                      </option>
                    ))}
                  </select>
                </div>

                {outputText && (
                  <button
                    className="hover:text-primary text-gray-400 transition-colors"
                    onClick={() => navigator.clipboard.writeText(outputText)}
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
                {outputText ? (
                  <div
                    className={`${styles.markdown} wrap-break-words text-sm leading-relaxed`}
                    dangerouslySetInnerHTML={{ __html: md.render(outputText) }}
                  />
                ) : (
                  <div className="flex h-full flex-col items-center justify-center gap-3 text-gray-400">
                    <div className="rounded-full bg-gray-100 p-4">
                      <Play className="ml-1 h-8 w-8 text-gray-300" />
                    </div>
                    <p className="text-sm">
                      왼쪽 폼을 입력하고 버튼을 눌러보세요.
                    </p>
                  </div>
                )}
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  )
}
