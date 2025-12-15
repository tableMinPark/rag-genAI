'use client'

import { useState, useEffect } from 'react'
import Link from 'next/link'
import { useParams, useRouter } from 'next/navigation'
import { FolderOpen, Plus, Loader2, AlertCircle } from 'lucide-react'

// ###################################################
// 상수 및 타입 정의 (Constants & Types)
// ###################################################

interface PassageDetailType {
  passageId: number
  sourceId: number
  sourceName: string
  title: string
  subTitle: string | null
  thirdTitle: string | null
  content: string
  subContent: string | null
  version: number
}

interface ChunkType {
  id: number
  content: string
  type: 'TABLE' | 'TEXT'
  tokenSize: number
}

// [API Mock] 패시지 상세 및 청크 목록 조회 API
const fetchPassageData = async (
  passageId: number,
): Promise<{ passage: PassageDetailType; chunks: ChunkType[] }> => {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      if (Math.random() > 0.1) {
        // 90% 성공
        const passage = {
          passageId: passageId,
          sourceId: 1,
          sourceName: '국민건강보험법.pdf',
          title: '제1장 총칙',
          subTitle: '제1조(목적)',
          thirdTitle: '제1항',
          content: `이 법은 국민의 질병ㆍ부상에 대한 예방ㆍ진단ㆍ치료ㆍ재활과 출산ㆍ사망 및 건강증진에 대하여 보험급여를 실시함으로써 국민보건 향상과 사회보장 증진에 이바지함을 목적으로 한다. (API Load)`,
          subContent: `[전문개정 2011. 12. 31.]\n(이 부분은 부가적인 설명이나 개정 이력 등이 들어가는 공간입니다.)`,
          version: 1,
        }

        // Mock Chunks (30개)
        const chunks = Array.from({ length: 30 }).map((_, i) => {
          const isTable = i % 10 === 5
          const id = 1001 + i
          let content = ''
          if (isTable) {
            content = `[표 ${Math.floor(i / 10) + 1}] 보험료율 현황\n| 구분 | 요율 |\n|---|---|\n| 직장 | 7.09% |`
          } else {
            content = `제${Math.floor(i / 3) + 1}조 내용... (청크 ID: ${id})`
          }
          return {
            id: id,
            content: content,
            type: (isTable ? 'TABLE' : 'TEXT') as 'TABLE' | 'TEXT',
            tokenSize: Math.floor(Math.random() * 300) + 50,
          }
        })

        resolve({ passage, chunks })
      } else {
        reject(new Error('패시지 정보를 불러오는데 실패했습니다.'))
      }
    }, 800)
  })
}

export default function PassageDetailPage() {
  // ###################################################
  // 훅 및 파라미터 정의 (Hooks & Params)
  // ###################################################
  const params = useParams()
  const router = useRouter()
  const passageId = Number(params.passageId)

  // ###################################################
  // 상태 정의 (State)
  // ###################################################
  const [passage, setPassage] = useState<PassageDetailType | null>(null)
  const [chunkList, setChunkList] = useState<ChunkType[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // ###################################################
  // 이펙트 및 로직 (Effects & Logic)
  // ###################################################
  /**
   * 화면 진입 시 데이터 로드
   */
  useEffect(() => {
    if (!passageId) return

    const loadData = async () => {
      setIsLoading(true)
      setError(null)
      try {
        const { passage, chunks } = await fetchPassageData(passageId)
        setPassage(passage)
        setChunkList(chunks)
      } catch (err) {
        console.error(err)
        setError('데이터를 불러올 수 없습니다.')
      } finally {
        setIsLoading(false)
      }
    }

    loadData()
  }, [passageId])

  // ###################################################
  // 핸들러 (Handler)
  // ###################################################
  /**
   * 청크 등록 화면으로 이동 핸들러
   */
  const handleCreateChunk = () => {
    if (passage) {
      router.push(`/chunk/create?passageId=${passage.passageId}`)
    }
  }

  // ###################################################
  // 렌더링 (Render)
  // ###################################################

  // 1. 로딩 상태
  if (isLoading) {
    return (
      <div className="flex h-full w-full flex-col items-center justify-center gap-3">
        <Loader2 className="text-primary h-10 w-10 animate-spin" />
        <p className="text-sm font-medium text-gray-500">
          패시지 정보를 불러오는 중입니다...
        </p>
      </div>
    )
  }

  // 2. 에러 상태
  if (error || !passage) {
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
          ← 뒤로가기
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
          <h2 className="text-2xl font-bold text-gray-800">패시지 상세</h2>
        </div>
        {/* 뒤로가기 버튼 */}
        <button
          onClick={() => router.back()}
          className="bg-primary hover:bg-primary-hover flex items-center gap-2 rounded-lg px-4 py-2 text-sm font-bold text-white shadow-sm transition-all active:scale-95"
        >
          <span>← 뒤로가기</span>
        </button>
      </div>

      {/* 2. 메인 컨텐츠 영역 (좌우 분할) */}
      <div className="flex min-h-0 flex-1 gap-6">
        {/* [왼쪽] Passage 상세 내용 */}
        <div className="flex flex-[2] flex-col overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
          <div className="border-b border-gray-100 bg-gray-50 px-6 py-4">
            <h3 className="font-bold text-gray-800">패시지 원문 데이터</h3>
          </div>

          <div className="flex-1 overflow-auto p-8">
            <div className="mx-auto flex max-w-3xl flex-col gap-8">
              {/* (1) 제목 영역 */}
              <div className="flex flex-col gap-2 border-b border-gray-100 pb-6">
                {passage.title && (
                  <h1 className="text-2xl font-bold text-gray-900">
                    {passage.title}
                  </h1>
                )}
                {passage.subTitle && (
                  <h2 className="flex items-center gap-2 text-lg font-semibold text-gray-700">
                    <span className="bg-primary h-4 w-1 rounded-full"></span>
                    {passage.subTitle}
                  </h2>
                )}
                {passage.thirdTitle && (
                  <h3 className="text-md border-l-2 border-gray-200 pl-3 font-medium text-gray-600">
                    {passage.thirdTitle}
                  </h3>
                )}
                {!passage.title && !passage.subTitle && !passage.thirdTitle && (
                  <span className="text-gray-400 italic">(제목 없음)</span>
                )}
              </div>

              {/* (2) 본문 영역 */}
              <div>
                <label className="mb-2 block text-xs font-bold text-gray-400 uppercase">
                  본문 (Content)
                </label>
                <div className="text-base leading-8 whitespace-pre-wrap text-gray-800">
                  {passage.content}
                </div>
              </div>

              {/* (3) 부가 본문 영역 */}
              {passage.subContent && (
                <div className="rounded-lg border border-gray-100 bg-gray-50 p-5">
                  <label className="mb-2 block text-xs font-bold text-gray-400 uppercase">
                    부가 본문 (Sub Content)
                  </label>
                  <div className="text-sm leading-relaxed whitespace-pre-wrap text-gray-600">
                    {passage.subContent}
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* [오른쪽] Chunk 목록 */}
        <div className="flex flex-1 flex-col overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
          {/* 헤더: 카운트 & 등록 버튼 */}
          <div className="flex items-center justify-between border-b border-gray-100 bg-gray-50 px-6 py-4">
            <div className="flex items-center gap-2">
              <h3 className="font-bold text-gray-800">청크 목록</h3>
              <span className="rounded-full bg-blue-100 px-2 py-0.5 text-xs font-bold text-blue-700">
                {chunkList.length}
              </span>
            </div>

            {/* 청크 등록 버튼 */}
            <button
              onClick={handleCreateChunk}
              className="hover:border-primary hover:text-primary flex items-center gap-1.5 rounded-lg border border-gray-200 bg-white px-3 py-1.5 text-xs font-bold text-gray-600 shadow-sm transition-all active:scale-95"
            >
              <Plus className="h-3.5 w-3.5" />
              청크 등록
            </button>
          </div>

          <div className="flex-1 overflow-auto bg-gray-50/50 p-4">
            <div className="flex flex-col gap-3">
              {chunkList.map((chunk) => (
                <Link
                  key={chunk.id}
                  href={`/chunk/${chunk.id}`}
                  className="hover:border-primary group relative block overflow-hidden rounded-xl border border-gray-200 bg-white p-5 transition-all hover:shadow-md"
                >
                  {/* 카드 헤더 */}
                  <div className="mb-3 flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-bold text-gray-500">
                        #{chunk.id}
                      </span>
                      <span className="rounded border border-gray-200 bg-gray-100 px-1.5 py-0.5 text-[10px] font-bold text-gray-600">
                        {chunk.type}
                      </span>
                    </div>
                    <span className="text-[10px] font-medium text-gray-400">
                      {chunk.tokenSize} Tokens
                    </span>
                  </div>

                  {/* 카드 본문 미리보기 */}
                  <p className="line-clamp-3 text-sm leading-relaxed text-gray-600 group-hover:text-gray-900">
                    {chunk.content}
                  </p>

                  {/* 호버 화살표 */}
                  <div className="text-primary absolute right-4 bottom-4 -translate-x-2 opacity-0 transition-all group-hover:translate-x-0 group-hover:opacity-100">
                    →
                  </div>
                </Link>
              ))}

              {chunkList.length === 0 && (
                <div className="flex h-40 flex-col items-center justify-center text-gray-400">
                  <span className="mb-2 text-2xl">📭</span>
                  <span className="text-sm">생성된 청크가 없습니다.</span>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

