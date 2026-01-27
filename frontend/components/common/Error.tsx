import { useUiStore } from '@/stores/uiStore'
import { useRouter } from 'next/navigation'
import { AlertCircle, Home, RefreshCw } from 'lucide-react'

type ErrorProps = {
  onRefresh?: () => void
}

/**
 * 에러 컴포넌트
 * @param handleRefresh 재시도 핸들러
 * @returns 에러 화면
 */
export default function Error({ onRefresh: onRefresh }: ErrorProps) {
  const uiStore = useUiStore()
  const router = useRouter()

  return (
    <div className="flex h-full w-full flex-col bg-black/10 backdrop-blur-sm">
      <div className="flex flex-1 flex-col items-center justify-center gap-3">
        <AlertCircle className="h-8 w-8 text-red-500" />
        <p className="text-sm font-bold text-gray-700">{uiStore.message}</p>
        <div className="flex gap-2">
          <button
            onClick={() => router.push('/')}
            className="flex items-center gap-2 rounded-md bg-gray-100 px-3 py-1.5 text-xs font-bold text-gray-600 hover:bg-gray-200"
          >
            <Home className="h-3 w-3" />
            홈으로
          </button>
          <button
            onClick={onRefresh ? onRefresh : undefined}
            className="flex items-center gap-2 rounded-md bg-gray-100 px-3 py-1.5 text-xs font-bold text-gray-600 hover:bg-gray-200"
          >
            <RefreshCw className="h-3 w-3" />
            다시 시도
          </button>
        </div>
      </div>
    </div>
  )
}
