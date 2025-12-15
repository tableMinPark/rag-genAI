'use client'

import { useState, useEffect } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { FolderOpen, Edit, Loader2, AlertCircle } from 'lucide-react'

// ###################################################
// 상수 및 타입 정의 (Constants & Types)
// ###################################################

// API 응답 데이터 타입 정의
interface ChunkDetailType {
  chunkId: number
  passageId: number
  version: number
  title: string
  subTitle: string | null
  thirdTitle: string | null
  content: string
  compactContent: string | null
  subContent: string | null
  tokenSize: number
  compactTokenSize: number
  sysCreateDt: string
  sysModifyDt: string
}

// [API Mock] 청크 상세 조회 API (실제로는 src/api/chunk.ts 등에서 import)
const fetchChunkDetail = async (chunkId: number): Promise<ChunkDetailType> => {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      // 90% 성공, 10% 에러 시뮬레이션
      if (Math.random() > 0.1) {
        resolve({
          chunkId: chunkId,
          passageId: 101,
          version: 1,
          title: '제1장 총칙',
          subTitle: '제1조(목적)',
          thirdTitle: '제1항',
          content: `이 법은 국민의 질병ㆍ부상에 대한 예방ㆍ진단ㆍ치료ㆍ재활과 출산ㆍ사망 및 건강증진에 대하여 보험급여를 실시함으로써 국민보건 향상과 사회보장 증진에 이바지함을 목적으로 한다. (API에서 로드된 데이터입니다.)`,
          compactContent: `국민건강보험법 목적: 국민의 질병, 부상, 예방, 진단, 치료, 재활, 출산, 사망, 건강증진에 대한 보험급여 실시. 국민보건 향상 및 사회보장 증진 기여.`,
          subContent: `[전문개정 2011. 12. 31.]`,
          tokenSize: 156,
          compactTokenSize: 85,
          sysCreateDt: '2023-12-13 14:30:00',
          sysModifyDt: '2023-12-13 14:30:00',
        })
      } else {
        reject(new Error('데이터를 불러오는데 실패했습니다.'))
      }
    }, 800) // 0.8초 딜레이
  })
}

export default function ChunkDetailPage() {
  // ###################################################
  // 훅 및 파라미터 정의 (Hooks & Params)
  // ###################################################
  const params = useParams()
  const router = useRouter()
  const chunkId = Number(params.chunkId)

  // ###################################################
  // 상태 정의 (State)
  // ###################################################
  const [chunk, setChunk] = useState<ChunkDetailType | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // ###################################################
  // 이펙트 및 로직 (Effects & Logic)
  // ###################################################
  /**
   * 화면 진입 시 데이터 로드
   */
  useEffect(() => {
    if (!chunkId) return

    const loadData = async () => {
      setIsLoading(true)
      setError(null)
      try {
        const data = await fetchChunkDetail(chunkId)
        setChunk(data)
      } catch (err) {
        console.error(err)
        setError('청크 정보를 불러올 수 없습니다.')
      } finally {
        setIsLoading(false)
      }
    }

    loadData()
  }, [chunkId])

  // ###################################################
  // 핸들러 (Handler)
  // ###################################################
  /**
   * 수정 화면 이동 핸들러
   */
  const handleEdit = () => {
    if (chunk) {
      router.push(`/chunk/edit/${chunk.chunkId}`)
    }
  }

  // ###################################################
  // 렌더링 (Render)
  // ###################################################

  // 1. 로딩 중일 때
  if (isLoading) {
    return (
      <div className="flex h-full w-full flex-col items-center justify-center gap-3">
        <Loader2 className="text-primary h-10 w-10 animate-spin" />
        <p className="text-sm font-medium text-gray-500">
          데이터를 불러오는 중입니다...
        </p>
      </div>
    )
  }

  // 2. 에러 발생 시
  if (error || !chunk) {
    return (
      <div className="flex h-full w-full flex-col items-center justify-center gap-3">
        <AlertCircle className="h-10 w-10 text-red-500" />
        <p className="text-sm font-bold text-gray-700">
          {error || '데이터가 존재하지 않습니다.'}
        </p>
        <button
          onClick={() => router.back()}
          className="text-primary mt-2 text-xs font-bold hover:underline"
        >
          ← 목록으로 돌아가기
        </button>
      </div>
    )
  }

  // 3. 정상 렌더링
  return (
    <div className="flex h-full w-full flex-col p-6">
      {/* 1. 상단 네비게이션 */}
      <div className="mb-4 flex items-center justify-between">
        <div className="flex shrink-0 items-center gap-2">
          <FolderOpen className="text-primary h-6 w-6" />
          <h2 className="text-2xl font-bold text-gray-800">청크 상세</h2>
        </div>

        {/* 버튼 그룹 */}
        <div className="flex items-center gap-3">
          {/* 뒤로가기 버튼 */}
          <button
            onClick={() => router.back()}
            className="rounded-lg border border-gray-300 bg-white px-4 py-2 text-sm font-bold text-gray-600 shadow-sm transition-colors hover:bg-gray-50"
          >
            ← 뒤로가기
          </button>

          {/* 수정 버튼 */}
          <button
            onClick={handleEdit}
            className="bg-primary hover:bg-primary-hover flex items-center gap-2 rounded-lg px-4 py-2 text-sm font-bold text-white shadow-sm transition-all active:scale-95"
          >
            <Edit className="h-4 w-4" />
            수정
          </button>
        </div>
      </div>

      {/* 2. 메인 컨텐츠 영역 */}
      <div className="flex min-h-0 flex-1 flex-col">
        {/* 카드 컨테이너 */}
        <div className="flex w-full flex-1 flex-col overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
          {/* 카드 헤더 */}
          <div className="flex items-center justify-between border-b border-gray-100 bg-gray-50 px-6 py-4">
            <div className="flex items-center gap-3">
              <h2 className="text-lg font-bold text-gray-800">
                청크 원문 데이터
              </h2>
              <span className="rounded border border-gray-200 bg-white px-2 py-0.5 text-[10px] font-bold text-gray-500">
                v{chunk.version}
              </span>
            </div>
            <span className="font-mono text-xs text-gray-400">
              최종수정: {chunk.sysModifyDt}
            </span>
          </div>

          {/* 본문 및 메타데이터 (스크롤 가능) */}
          <div className="flex flex-col gap-8 overflow-y-auto p-8">
            {/* (5) 메타데이터 그리드 */}
            <div className="rounded-xl border border-gray-100 bg-gray-50 p-6">
              <label className="mb-4 block text-xs font-bold text-gray-400 uppercase">
                청크 메타 데이터 (Chunk Metadata)
              </label>
              <div className="grid grid-cols-2 gap-6 sm:grid-cols-5">
                <div className="flex flex-col gap-1">
                  <span className="text-xs text-gray-500">청크 ID</span>
                  <span className="font-bold text-gray-800">
                    {chunk.chunkId}
                  </span>
                </div>
                <div className="flex flex-col gap-1">
                  <span className="text-xs text-gray-500">본문 토큰 수</span>
                  <span className="font-bold text-gray-800">
                    {chunk.tokenSize}
                  </span>
                </div>
                <div className="flex flex-col gap-1">
                  <span className="text-xs text-gray-500">
                    색인 본문 토큰 수
                  </span>
                  <span className="text-primary font-bold">
                    {chunk.compactTokenSize}
                  </span>
                </div>
                <div className="flex flex-col gap-1">
                  <span className="text-xs text-gray-500">등록일</span>
                  <span className="text-xs font-bold text-gray-800">
                    {chunk.sysCreateDt}
                  </span>
                </div>
                <div className="flex flex-col gap-1">
                  <span className="text-xs text-gray-500">수정일</span>
                  <span className="text-xs font-bold text-gray-800">
                    {chunk.sysModifyDt}
                  </span>
                </div>
              </div>
            </div>

            {/* (1) 제목 영역 */}
            <div className="flex flex-col gap-2 border-b border-gray-100 pb-6">
              {chunk.title && (
                <h1 className="text-xl font-bold text-gray-900">
                  {chunk.title}
                </h1>
              )}
              {chunk.subTitle && (
                <h2 className="text-md flex items-center gap-2 font-semibold text-gray-700">
                  <span className="bg-primary h-3 w-1 rounded-full"></span>
                  {chunk.subTitle}
                </h2>
              )}
              {chunk.thirdTitle && (
                <h3 className="border-l-2 border-gray-200 pl-3 text-sm font-medium text-gray-600">
                  {chunk.thirdTitle}
                </h3>
              )}

              {!chunk.title && !chunk.subTitle && !chunk.thirdTitle && (
                <span className="text-gray-400 italic">(제목 없음)</span>
              )}
            </div>

            {/* (3) 색인 대상 본문 (Compact Content) */}
            {chunk.compactContent && (
              <div>
                <label className="mb-2 block text-xs font-bold text-blue-500 uppercase">
                  색인 본문 (Compact Content)
                </label>
                <div className="rounded-lg border border-blue-100 bg-blue-50 p-5 text-sm leading-relaxed whitespace-pre-wrap text-gray-700">
                  {chunk.compactContent}
                </div>
              </div>
            )}

            {/* (2) 본문 영역 (Content) */}
            <div>
              <label className="mb-2 block text-xs font-bold text-gray-400 uppercase">
                본문 (Content)
              </label>
              <div className="rounded-lg border border-gray-100 bg-gray-50 p-6 text-base leading-8 whitespace-pre-wrap text-gray-800">
                {chunk.content}
              </div>
            </div>

            {/* (4) 부가 본문 (Sub Content) */}
            {chunk.subContent && (
              <div>
                <label className="mb-2 block text-xs font-bold text-gray-400 uppercase">
                  부가 본문 (Sub Content)
                </label>
                <div className="border-l-2 border-gray-200 pl-2 text-sm leading-relaxed whitespace-pre-wrap text-gray-500">
                  {chunk.subContent}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
