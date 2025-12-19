'use client'

import React, { useState, useEffect, Suspense } from 'react'
import { useRouter, useParams, useSearchParams } from 'next/navigation'
import {
  FolderOpen,
  Loader2,
  Save,
  FileText,
  AlignLeft,
  ListPlus,
  Eye, // Markdown 미리보기용 아이콘
  AlertCircle,
} from 'lucide-react'
import HtmlEditor from '@/components/editor/HtmlEditor'
import TurndownService from 'turndown'
// @ts-ignore
import { gfm } from 'turndown-plugin-gfm'
import { getChunkApi } from '@/api/chunk'

// ###################################################
// [타입 정의]
// ###################################################
interface ChunkFormData {
  chunkId?: number
  passageId?: number
  title: string
  subTitle: string
  thirdTitle: string
  content: string
  compactContent: string
  subContent: string
}

const updateChunkData = async (
  chunkId: number,
  data: ChunkFormData,
): Promise<void> => {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      if (Math.random() > 0.1) {
        console.log('API Request Payload:', { chunkId, ...data })
        resolve()
      } else {
        reject(new Error('저장에 실패했습니다. 잠시 후 다시 시도해주세요.'))
      }
    }, 1500)
  })
}

// ###################################################
// [Helpers] 텍스트 처리 및 유틸
// ###################################################
const normalizeTableForMarkdown = (htmlContent: string) => {
  if (typeof window === 'undefined') return htmlContent
  const parser = new DOMParser()
  const doc = parser.parseFromString(htmlContent, 'text/html')
  const tables = doc.querySelectorAll('table')
  tables.forEach((table) => {
    if (!table.querySelector('thead')) {
      const thead = doc.createElement('thead')
      const tbody = table.querySelector('tbody') || table
      const firstRow = tbody.querySelector('tr')
      if (firstRow) {
        thead.appendChild(firstRow)
        table.insertBefore(thead, table.firstChild)
        firstRow.querySelectorAll('td').forEach((cell) => {
          const th = doc.createElement('th')
          th.innerHTML = cell.innerHTML
          Array.from(cell.attributes).forEach((attr) =>
            th.setAttribute(attr.name, attr.value),
          )
          cell.replaceWith(th)
        })
      }
    }
  })
  return doc.body.innerHTML
}

const getContentLength = (html: string) => {
  if (!html) return 0
  let processed = html
  processed = processed.replace(/<p[^>]*>\s*<br\s*\/?>\s*<\/p>/gi, '\n')
  processed = processed.replace(
    /<\/p>|<\/div>|<\/h[1-6]>|<\/li>|<\/tr>/gi,
    '\n',
  )
  processed = processed.replace(/<br\s*\/?>/gi, '\n')
  processed = processed.replace(/&nbsp;/g, ' ')
  processed = processed.replace(
    /<\/?(?!(?:table|thead|tbody|tfoot|tr|th|td)\b)[^>]+>/gi,
    '',
  )
  return processed.trim().length
}

// ###################################################
// [Sub Components]
// ###################################################
const TokenBadge = ({ current, max }: { current: number; max: number }) => (
  <div
    className={`flex items-center gap-1 rounded border px-2 py-0.5 text-[10px] font-bold ${
      current > max
        ? 'border-red-200 bg-red-50 text-red-600'
        : 'border-gray-200 bg-gray-50 text-gray-500'
    }`}
  >
    <span>{current.toLocaleString()}</span>
    <span className="text-gray-300">/</span>
    <span>{max}</span>
  </div>
)

// ###################################################
// [Inner Component] 실제 로직
// ###################################################
function ChunkEditContent() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const chunkId = Number(searchParams.get('chunkId'))

  // --- 상태 관리 ---
  const [formData, setFormData] = useState<ChunkFormData>({
    title: '',
    subTitle: '',
    thirdTitle: '',
    content: '',
    compactContent: '',
    subContent: '',
  })
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const loadData = async () => {
    if (!chunkId || Number.isNaN(chunkId)) {
      setError('청크를 불러올 수 없습니다.')
      setIsLoading(false)
      return
    }

    setIsLoading(true)
    setError(null)

    try {
      await getChunkApi(chunkId).then((response) => {
        console.log(`📡 ${response.message}`)
        setFormData({
          title: response.result.title,
          subTitle: response.result.subTitle,
          thirdTitle: response.result.thirdTitle,
          content: response.result.content,
          compactContent: response.result.compactContent,
          subContent: response.result.subContent,
        })
      })
    } catch (err) {
      console.error(err)
      setError('청크를 불러올 수 없습니다.')
    } finally {
      setIsLoading(false)
    }

    setIsLoading(false)
  }

  /**
   * 화면 진입 시 데이터 로드
   */
  useEffect(() => {
    if (!chunkId || Number.isNaN(chunkId)) {
      setError('청크를 불러올 수 없습니다.')
      setIsLoading(false)
      return
    }

    loadData()
  }, [chunkId])

  // 2. 실시간 Markdown 변환 (Content -> CompactContent)
  useEffect(() => {
    if (formData.content) {
      const turndownService = new TurndownService({
        headingStyle: 'atx',
        codeBlockStyle: 'fenced',
        bulletListMarker: '-',
      })
      turndownService.use(gfm)
      const cleanHtml = normalizeTableForMarkdown(formData.content)
      const markdown = turndownService.turndown(cleanHtml)
      setFormData((prev) => {
        if (prev.compactContent === markdown) return prev
        return { ...prev, compactContent: markdown }
      })
    }
  }, [formData.content])

  // --- 핸들러 ---
  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target
    setFormData((prev) => ({ ...prev, [name]: value }))
  }

  const handleEditorChange =
    (fieldName: keyof ChunkFormData) => (html: string) => {
      setFormData((prev) => ({ ...prev, [fieldName]: html }))
    }

  const handleSave = async () => {
    // 유효성 검사
    if (getContentLength(formData.content) > 1200)
      return alert('본문이 1200자를 초과했습니다.')
    if (formData.compactContent.length > 1200)
      return alert('색인 본문이 1200자를 초과했습니다.')
    if (getContentLength(formData.subContent) > 1200)
      return alert('부가 본문이 1200자를 초과했습니다.')

    setIsSaving(true)
    try {
      await updateChunkData(chunkId, formData)
      router.back()
    } catch (e: any) {
      console.error(e)
      alert(e.message || '수정 중 오류가 발생했습니다.')
      setIsSaving(false)
    }
  }

  const handleCancel = () => {
    if (confirm('수정 중인 내용이 사라집니다. 취소하시겠습니까?')) {
      router.back()
    }
  }

  // --- 스타일 클래스 (CreatePage와 동일) ---
  const labelClass = 'mb-1.5 block text-xs font-bold text-gray-500'
  const inputClass =
    'w-full rounded-lg border border-gray-200 px-3 py-2 text-sm text-gray-800 outline-none transition-all placeholder:text-gray-400 focus:border-primary focus:ring-1 focus:ring-primary disabled:bg-gray-100 disabled:text-gray-500'

  // 로딩 상태
  if (isLoading) {
    return (
      <div className="flex h-full w-full flex-col items-center justify-center gap-3 bg-gray-50">
        <Loader2 className="text-primary h-10 w-10 animate-spin" />
        <p className="text-sm font-medium text-gray-500">
          데이터를 불러오는 중입니다...
        </p>
      </div>
    )
  }

  // 에러 상태
  if (error) {
    return (
      <div className="flex h-full w-full flex-col items-center justify-center gap-3 bg-gray-50">
        <AlertCircle className="h-10 w-10 text-red-500" />
        <p className="text-sm font-bold text-gray-700">{error}</p>
        <button
          onClick={() => router.back()}
          className="text-primary mt-2 text-xs font-bold hover:underline"
        >
          ← 목록으로 돌아가기
        </button>
      </div>
    )
  }

  return (
    <div className="flex h-full w-full flex-col overflow-y-auto bg-gray-50/50 p-6">
      {/* 1. 헤더 영역 */}
      <div className="mb-6 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div>
            <h2 className="flex items-center gap-2 text-2xl font-bold text-gray-800">
              <FolderOpen className="text-primary h-6 w-6" />
              청크 수정
            </h2>
            <p className="mt-1 text-xs text-gray-500">
              기존 청크 내용을 수정하고 저장합니다.
            </p>
          </div>
        </div>

        <div className="flex gap-2">
          <button
            onClick={handleCancel}
            disabled={isSaving}
            className="rounded-lg border border-gray-300 bg-white px-4 py-2 text-sm font-bold text-gray-600 shadow-sm hover:bg-gray-50 disabled:opacity-50"
          >
            취소
          </button>
          <button
            onClick={handleSave}
            disabled={isSaving}
            className="bg-primary hover:bg-primary-hover flex items-center gap-2 rounded-lg px-5 py-2 text-sm font-bold text-white shadow-md active:scale-95 disabled:cursor-not-allowed disabled:bg-gray-300"
          >
            {isSaving ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />
                저장 중...
              </>
            ) : (
              <>
                <Save className="h-4 w-4" />
                저장하기
              </>
            )}
          </button>
        </div>
      </div>

      {/* 2. 입력 폼 영역 */}
      <div className="flex w-full flex-col gap-6 pb-10">
        {/* 기본 정보 카드 */}
        <div className="flex w-full flex-col rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
          <h3 className="mb-4 flex items-center gap-2 border-b border-gray-100 pb-2 text-sm font-bold text-gray-800">
            <FileText className="text-primary h-4 w-4" /> 기본 정보
          </h3>

          <div className="flex flex-col gap-4">
            <div>
              <label className={labelClass}>
                제목 (Title) <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                name="title"
                value={formData.title}
                onChange={handleInputChange}
                className={`${inputClass} cursor-not-allowed bg-gray-50 text-gray-500`}
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className={labelClass}>중제목 (Sub Title)</label>
                <input
                  type="text"
                  name="subTitle"
                  value={formData.subTitle}
                  onChange={handleInputChange}
                  className={inputClass}
                />
              </div>
              <div>
                <label className={labelClass}>소제목 (Third Title)</label>
                <input
                  type="text"
                  name="thirdTitle"
                  value={formData.thirdTitle}
                  onChange={handleInputChange}
                  className={inputClass}
                />
              </div>
            </div>
          </div>
        </div>

        {/* [Main] 내용 작성 에디터 */}
        <div className="flex min-h-125 flex-col rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
          <div className="mb-4 flex items-center justify-between border-b border-gray-100 pb-2">
            <h3 className="flex items-center gap-2 text-sm font-bold text-gray-800">
              <AlignLeft className="text-primary h-4 w-4" /> 본문 (HTML)
            </h3>
            <TokenBadge
              current={getContentLength(formData.content)}
              max={1200}
            />
          </div>

          <div className="flex-1">
            <HtmlEditor
              value={formData.content}
              onChange={handleEditorChange('content')}
              placeholder="본문 내용을 입력해주세요."
              height={800}
            />
          </div>
        </div>

        {/* [Sub] 부가 정보 에디터 */}
        <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
          <div className="mb-4 flex items-center justify-between border-b border-gray-100 pb-2">
            <h3 className="flex items-center gap-2 text-sm font-bold text-gray-800">
              <ListPlus className="text-primary h-4 w-4" /> 부가 정보 (Sub
              Content)
            </h3>
            <TokenBadge
              current={getContentLength(formData.subContent)}
              max={1200}
            />
          </div>

          <HtmlEditor
            value={formData.subContent}
            onChange={handleEditorChange('subContent')}
            placeholder="추가적인 설명이나 주석을 입력하세요."
            height={400}
          />
        </div>
      </div>
    </div>
  )
}

// ###################################################
// [Main Component]
// ###################################################
export default function ChunkEditPage() {
  return (
    <Suspense
      fallback={
        <div className="flex h-full w-full items-center justify-center bg-gray-50">
          <div className="flex flex-col items-center gap-3">
            <Loader2 className="text-primary h-10 w-10 animate-spin" />
            <p className="text-sm font-bold text-gray-500">
              페이지를 불러오는 중입니다...
            </p>
          </div>
        </div>
      }
    >
      <ChunkEditContent />
    </Suspense>
  )
}
