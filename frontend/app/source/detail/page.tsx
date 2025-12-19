'use client'

import { useState, useEffect, Suspense } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { FolderOpen, Loader2, AlertCircle } from 'lucide-react'
import { getSourceApi } from '@/api/source'
import { getPassagesApi } from '@/api/passage'
import { Passage, Source } from '@/types/domain'

// ###################################################
// 상수 및 타입 정의 (Constants & Types)
// ###################################################

function SourceDetailContent() {
  // ###################################################
  // 훅 및 파라미터 정의 (Hooks & Params)
  // ###################################################
  // 페이지당 항목 수
  const ITEMS_PER_PAGE = 10
  const router = useRouter()
  const searchParams = useSearchParams()
  const sourceId = Number(searchParams.get('sourceId'))

  // ###################################################
  // 상태 정의 (State)
  // ###################################################
  const [source, setSource] = useState<Source | null>(null)
  const [passageList, setPassageList] = useState<Passage[]>([])
  const [page, setPage] = useState(1)
  const [size, setSize] = useState(ITEMS_PER_PAGE)
  const [totalPages, setTotalPages] = useState(0)
  const [totalCounts, setTotalCounts] = useState(0)

  // 로딩 및 에러 상태
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadData = async () => {
    if (!sourceId || Number.isNaN(sourceId)) {
      setError('패시지를 불러올 수 없습니다.')
      setIsLoading(false)
      return
    }

    setIsLoading(true)
    setError(null)

    try {
      await getSourceApi(sourceId).then((response) => {
        console.log(`📡 ${response.message}`)
        setSource(response.result)
      })

      await getPassagesApi(page, size, sourceId).then((response) => {
        console.log(`📡 ${response.message}`)
        setPage(response.result.pageNo)
        setSize(response.result.pageSize)
        setTotalPages(response.result.totalPages)
        setTotalCounts(response.result.totalCount)
        setPassageList(response.result.content)
      })
    } catch (err) {
      console.error(err)
      setError('패시지를 불러올 수 없습니다.')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    if (!sourceId || Number.isNaN(sourceId)) {
      setError('패시지를 불러올 수 없습니다.')
      setIsLoading(false)
      return
    }

    loadData()
  }, [page, size])

  useEffect(() => {
    if (!sourceId || Number.isNaN(sourceId)) {
      setError('패시지를 불러올 수 없습니다.')
      setIsLoading(false)
      return
    }

    loadData()
  }, [sourceId])

  // ###################################################
  // 핸들러 (Handler)
  // ###################################################
  const startIndex = (page - 1) * ITEMS_PER_PAGE
  const endIndex = startIndex + ITEMS_PER_PAGE
  /**
   * 행 클릭 핸들러 (상세 화면 이동)
   */
  const handleRowClick = (passageId: number) => {
    router.push(`/passage/detail?passageId=${passageId}`)
  }

  const handlePrevPage = () => {
    if (page > 1) {
      setPage((prev) => prev - 1)
    }
  }

  const handleNextPage = () => {
    if (page < totalPages) {
      setPage((prev) => prev + 1)
    }
  }

  // ###################################################
  // 렌더링 (Render)
  // ###################################################
  function updateStatusBadge(updateState: string) {
    const status = updateState as
      | 'UPDATE-STATE-STAY'
      | 'UPDATE-STATE-INSERT'
      | 'UPDATE-STATE-CHANGE'
      | 'UPDATE-STATE-DELETE'

    // 1. 값에 따른 색상 정의
    const updateStatusColors = {
      'UPDATE-STATE-STAY': 'green',
      'UPDATE-STATE-INSERT': 'blue',
      'UPDATE-STATE-CHANGE': 'red',
      'UPDATE-STATE-DELETE': 'black',
    }

    const updateStatusNames = {
      'UPDATE-STATE-STAY': '변경없음',
      'UPDATE-STATE-INSERT': '추가',
      'UPDATE-STATE-CHANGE': '변경',
      'UPDATE-STATE-DELETE': '삭제',
    }

    // 2. 현재 status에 맞는 색상 선택 (없으면 기본값 black)
    const updateStatusColor = updateStatusColors[status] || 'black'
    const updateStatusName = updateStatusNames[status] || '오류'

    return (
      <span
        className={`inline-flex items-center rounded-full bg-${updateStatusColor}-50 px-2 py-0.5 text-xs font-medium text-${updateStatusColor}-700 ring-1 ring-${updateStatusColor}-700/10 ring-inset`}
      >
        {updateStatusName}
      </span>
    )
  }

  // 1. 로딩 상태
  if (isLoading) {
    return (
      <div className="flex h-full w-full flex-col items-center justify-center gap-3">
        <Loader2 className="text-primary h-10 w-10 animate-spin" />
        <p className="text-sm font-medium text-gray-500">
          문서 정보를 불러오는 중입니다...
        </p>
      </div>
    )
  }

  // 2. 에러 상태
  if (error || !source) {
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
    <div className="flex w-full flex-col p-6">
      {/* 1. 상단: 문서 정보 카드 & 뒤로가기 */}
      <div className="mb-4 flex flex-col gap-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div>
              <h2 className="flex items-center gap-2 text-2xl font-bold text-gray-800">
                <FolderOpen className="text-primary h-6 w-6" />
                패시지 목록
              </h2>
              <p className="mt-1 text-xs text-gray-500">
                대상 문서 상세 정보 & 패시지 목록
              </p>
            </div>
          </div>
          {/* 뒤로가기 버튼 */}
          <button
            onClick={() => router.back()}
            className="bg-primary hover:bg-primary-hover flex w-fit items-center gap-2 rounded-lg px-4 py-2 text-sm font-bold text-white shadow-sm transition-all active:scale-95"
          >
            <span>← 목록으로 돌아가기</span>
          </button>
        </div>

        <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="flex items-center gap-2 text-2xl font-bold text-gray-800">
              {source.name}
            </h2>
          </div>
          <div className="grid grid-cols-8 gap-4 text-sm text-gray-600">
            <div className="flex flex-col">
              <span className="pb-2 text-xs text-gray-400">문서 ID</span>
              <span className="font-bold text-gray-800">{source.sourceId}</span>
            </div>
            <div className="flex flex-col">
              <span className="pb-2 text-xs text-gray-400">버전</span>
              <span className="text-primary font-bold">v{source.version}</span>
            </div>
            <div className="flex flex-col">
              <span className="pb-2 text-xs text-gray-400">문서 타입</span>
              <span className="font-bold text-gray-800">
                {source.sourceTypeName}
              </span>
            </div>
            <div className="flex flex-col">
              <span className="pb-2 text-xs text-gray-400">문서 분류</span>
              <span className="font-bold text-gray-800">
                {source.categoryName}
              </span>
            </div>
            <div className="flex flex-col">
              <span className="pb-2 text-xs text-gray-400">전처리 타입</span>
              <span className="font-bold text-gray-800">
                {source.selectTypeName}
              </span>
            </div>
            <div className="flex flex-col">
              <span className="pb-2 text-xs text-gray-400">자동화여부</span>
              <div>
                {source.isAuto ? (
                  <span className="inline-flex items-center rounded-full bg-blue-50 px-2 py-0.5 text-xs font-medium text-blue-700 ring-1 ring-blue-700/10 ring-inset">
                    자동
                  </span>
                ) : (
                  <span className="inline-flex items-center rounded-full bg-yellow-50 px-2 py-0.5 text-xs font-medium text-yellow-800 ring-1 ring-yellow-600/20 ring-inset">
                    수동
                  </span>
                )}
              </div>
            </div>
            <div className="flex flex-col">
              <span className="pb-2 text-xs text-gray-400">배치여부</span>
              <div>
                {source.isBatch ? (
                  <span className="inline-flex items-center rounded-full bg-green-50 px-2 py-0.5 text-xs font-medium text-green-700 ring-1 ring-green-700/10 ring-inset">
                    활성화
                  </span>
                ) : (
                  <span className="inline-flex items-center rounded-full bg-red-50 px-2 py-0.5 text-xs font-medium text-red-800 ring-1 ring-red-600/20 ring-inset">
                    비활성화
                  </span>
                )}
              </div>
            </div>
            <div className="flex flex-col">
              <span className="pb-2 text-xs text-gray-400">등록일</span>
              <span className="font-bold text-gray-800">
                {source.sysCreateDt}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* 2. 하단: Passage 목록 테이블 */}
      <div className="flex flex-col overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
        {/* 테이블 영역 */}
        <div className="overflow-auto">
          <table className="w-full min-w-full text-left text-sm text-gray-600">
            <thead className="bg-gray-50 text-xs font-bold text-gray-500 uppercase shadow-sm">
              <tr>
                <th className="w-[80px] px-6 py-4 text-center">ID</th>
                <th className="w-[80px] px-6 py-4 text-center">버전</th>
                <th className="w-[150px] px-6 py-4">제목</th>
                <th className="px-6 py-4">본문</th>
                <th className="w-[80px] px-6 py-4 text-center">토큰</th>
                <th className="w-[110px] px-6 py-4 text-center">변경이력</th>
                <th className="w-[80px] px-6 py-4 text-center">순서</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 bg-white">
              {passageList.map((passage) => (
                <tr
                  key={passage.passageId}
                  onClick={() => handleRowClick(passage.passageId)}
                  className="group cursor-pointer transition-colors hover:bg-gray-50"
                >
                  <td className="px-6 py-4 text-center font-mono text-gray-400 group-hover:text-gray-600">
                    {passage.passageId}
                  </td>
                  <td className="px-6 py-4 text-center text-xs text-gray-400">
                    v{passage.version}
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex flex-col truncate">
                      <span className="group-hover:text-primary font-bold text-gray-800 transition-colors">
                        {passage.title}
                      </span>
                      {passage.subTitle && (
                        <span className="text-xs text-gray-500">
                          {passage.subTitle}
                        </span>
                      )}
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <div
                      className="max-w-2xl truncate text-gray-600 group-hover:text-gray-900"
                      title={passage.content}
                    >
                      {passage.content}
                    </div>
                    {passage.subContent && (
                      <div
                        className="mt-1 max-w-xl truncate text-xs text-gray-400"
                        title={passage.subContent}
                      >
                        ↳ {passage.subContent}
                      </div>
                    )}
                  </td>
                  <td className="px-6 py-4 text-center">
                    <span className="inline-flex items-center rounded-md bg-gray-100 px-2 py-1 text-xs font-medium text-gray-600">
                      {passage.contentTokenSize}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-center">
                    {updateStatusBadge(passage.updateState)}
                  </td>
                  <td className="px-6 py-4 text-center font-medium text-gray-600">
                    {passage.sortOrder}
                  </td>
                </tr>
              ))}
              {passageList.length === 0 && (
                <tr>
                  <td
                    colSpan={6}
                    className="px-6 py-12 text-center text-gray-500"
                  >
                    생성된 Passage가 없습니다.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* 페이지네이션 */}
        <div className="flex items-center justify-between border-t border-gray-100 bg-gray-50 px-6 py-3">
          <span className="text-xs text-gray-500">
            패시지 목록{' '}
            <span className="font-bold">
              {Math.min(startIndex + 1, totalCounts)}
            </span>
            ~{' '}
            <span className="font-bold">{Math.min(endIndex, totalCounts)}</span>{' '}
            {'(전체 '}
            <span className="font-bold">{totalCounts}</span> {' 개의 패시지)'}
          </span>

          <div className="flex items-center gap-2">
            <button
              onClick={handlePrevPage}
              disabled={page === 1}
              className="rounded border border-gray-300 bg-white px-3 py-1 text-xs font-medium text-gray-600 shadow-sm hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              이전
            </button>

            <span className="px-2 text-xs font-bold text-gray-700">
              {page} / {totalPages}
            </span>

            <button
              onClick={handleNextPage}
              disabled={page === totalPages}
              className="rounded border border-gray-300 bg-white px-3 py-1 text-xs font-medium text-gray-600 shadow-sm hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              다음
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

export default function SourceDetailPage() {
  return (
    <Suspense
      fallback={
        <div className="flex h-screen items-center justify-center">
          <Loader2 className="h-10 w-10 animate-spin text-blue-500" />
        </div>
      }
    >
      <SourceDetailContent />
    </Suspense>
  )
}
