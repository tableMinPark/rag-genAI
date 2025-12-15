'use client'

import React, { useState, Suspense } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import {
  FolderOpen,
  Loader2,
  Save,
  FileText,
  AlignLeft,
  ListPlus,
} from 'lucide-react'
import HtmlEditor from '@/components/editor/HtmlEditor'
import TurndownService from 'turndown'
// @ts-ignore
import { gfm } from 'turndown-plugin-gfm'

// ###################################################
// [타입 정의]
// ###################################################
interface ChunkFormData {
  passageId: string
  title: string
  subTitle: string
  thirdTitle: string
  content: string // Main Content (HTML)
  compactContent: string // Markdown for Vector DB
  subContent: string // Sub Content (HTML)
}

// ###################################################
// [API Mock]
// ###################################################
const createChunkData = async (data: ChunkFormData): Promise<void> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      console.log('[API] Chunk Created:', data)
      resolve()
    }, 1000)
  })
}

// ###################################################
// [Helpers]
// ###################################################
const convertHtmlToMarkdown = (html: string): string => {
  if (!html) return ''
  const turndownService = new TurndownService({
    headingStyle: 'atx',
    codeBlockStyle: 'fenced',
  })
  turndownService.use(gfm)
  return turndownService.turndown(html)
}

const getContentLength = (html: string) => {
  if (!html) return 0
  let processed = html
  processed = processed.replace(/<[^>]*>/g, '')
  processed = processed.replace(/&nbsp;/g, ' ')
  return processed.trim().length
}

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
// [Inner Component]
// ###################################################
function ChunkCreateContent() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const passageIdParam = searchParams.get('passageId') || ''

  // --- 상태 관리 ---
  const [formData, setFormData] = useState<ChunkFormData>({
    passageId: passageIdParam,
    title: '',
    subTitle: '',
    thirdTitle: '',
    content: '',
    compactContent: '',
    subContent: '',
  })

  const [isSaving, setIsSaving] = useState(false)

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
    if (!formData.title.trim()) return alert('제목은 필수입니다.')
    if (!formData.content.trim()) return alert('내용을 입력해주세요.')

    setIsSaving(true)
    try {
      const markdown = convertHtmlToMarkdown(formData.content)
      const submitData: ChunkFormData = {
        ...formData,
        compactContent: markdown,
      }
      await createChunkData(submitData)
      alert('등록되었습니다.')
      router.back()
    } catch (error) {
      console.error(error)
      alert('오류가 발생했습니다.')
    } finally {
      setIsSaving(false)
    }
  }

  const handleCancel = () => {
    if (confirm('작성 중인 내용이 사라집니다. 취소하시겠습니까?')) {
      router.back()
    }
  }

  // --- 스타일 클래스 ---
  const labelClass = 'mb-1.5 block text-xs font-bold text-gray-500'
  const inputClass =
    'w-full rounded-lg border border-gray-200 px-3 py-2 text-sm text-gray-800 outline-none transition-all placeholder:text-gray-400 focus:border-primary focus:ring-1 focus:ring-primary disabled:bg-gray-100 disabled:text-gray-500'

  return (
    <div className="flex h-full w-full flex-col overflow-y-auto bg-gray-50/50 p-6">
      {/* 1. 헤더 */}
      <div className="mb-6 flex shrink-0 items-center justify-between">
        <div className="flex items-center gap-3">
          <div>
            <h2 className="flex items-center gap-2 text-2xl font-bold text-gray-800">
              <FolderOpen className="text-primary h-6 w-6" />
              청크 등록
            </h2>
            <p className="mt-1 text-xs text-gray-500">
              새로운 지식 청크를 생성하고 내용을 작성합니다.
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
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Save className="h-4 w-4" />
            )}{' '}
            저장하기
          </button>
        </div>
      </div>

      {/* 2. 메인 컨텐츠 (수직 배치, 너비 제한 없음) */}
      <div className="flex w-full flex-col gap-6 pb-10">
        {/* [1] 기본 정보 */}
        <div className="flex w-full flex-col rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
          <h3 className="mb-4 flex items-center gap-2 border-b border-gray-100 pb-2 text-sm font-bold text-gray-800">
            <FileText className="text-primary h-4 w-4" /> 기본 정보
          </h3>

          <div className="flex flex-col gap-4">
            <div>
              <label className={labelClass}>Passage ID</label>
              <input
                type="text"
                value={formData.passageId}
                readOnly
                className={`${inputClass} cursor-not-allowed bg-gray-50 text-gray-500`}
              />
            </div>

            <div>
              <label className={labelClass}>
                제목 (Title) <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                name="title"
                value={formData.title}
                onChange={handleInputChange}
                placeholder="청크의 제목을 입력하세요"
                className={inputClass}
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
// [Main Component] Suspense 적용
// ###################################################
export default function ChunkCreatePage() {
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
      <ChunkCreateContent />
    </Suspense>
  )
}
