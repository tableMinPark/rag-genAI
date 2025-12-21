'use client'

import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { FileQuestion, Home, ArrowLeft } from 'lucide-react'

export default function NotFound() {
  const router = useRouter()

  return (
    <div className="flex min-h-screen w-full flex-col items-center justify-center bg-gray-50 p-6">
      {/* 메인 컨텐츠 */}
      <div className="animate-in fade-in zoom-in-95 flex w-full max-w-md flex-col items-center text-center duration-300">
        {/* 아이콘 영역 */}
        <div className="mb-6 flex h-24 w-24 items-center justify-center rounded-full bg-blue-50 shadow-sm ring-1 ring-blue-100">
          <FileQuestion className="text-primary h-10 w-10" />
        </div>

        {/* 텍스트 영역 */}
        <h1 className="mb-2 text-6xl font-extrabold tracking-tight text-gray-900">
          404
        </h1>
        <h2 className="mb-4 text-2xl font-bold text-gray-800">
          페이지를 찾을 수 없습니다.
        </h2>
        <p className="mb-8 text-sm leading-relaxed text-gray-500">
          요청하신 페이지가 삭제되었거나, 이름이 변경되었거나,
          <br />
          일시적으로 사용할 수 없습니다.
        </p>

        {/* 버튼 영역 */}
        <div className="flex w-full flex-col gap-3 sm:flex-row sm:justify-center">
          <button
            onClick={() => router.back()}
            className="flex items-center justify-center gap-2 rounded-lg border border-gray-300 bg-white px-5 py-3 text-sm font-bold text-gray-700 shadow-sm transition-all hover:bg-gray-50 active:scale-95"
          >
            <ArrowLeft className="h-4 w-4" />
            이전 페이지
          </button>

          <Link
            href="/"
            className="bg-primary hover:bg-primary-hover flex items-center justify-center gap-2 rounded-lg px-5 py-3 text-sm font-bold text-white shadow-md transition-all hover:shadow-lg active:scale-95"
          >
            <Home className="h-4 w-4" />
            홈으로 가기
          </Link>
        </div>
      </div>
    </div>
  )
}
