import { useUiStore } from '@/stores/uiStore'
import { Loader2 } from 'lucide-react'

type LoadingProps = {
  onCancel?: () => void
}

/**
 * 로딩 컴포넌트
 * @returns 로딩 화면
 */
export default function Loading({ onCancel }: LoadingProps) {
  const uiStore = useUiStore()

  return (
    <div className="flex h-full w-full flex-col bg-black/10 backdrop-blur-sm">
      <div className="flex flex-1 flex-col items-center justify-center gap-3">
        <Loader2 className="text-primary h-8 w-8 animate-spin" />
        <p className="text-sm font-bold text-gray-700">{uiStore.message}</p>
        <button
          onClick={onCancel ? onCancel : undefined}
          className="flex items-center gap-2 rounded-md bg-gray-100 px-3 py-1.5 text-xs font-bold text-gray-600 hover:bg-gray-200"
        >
          취소
        </button>
      </div>
    </div>
  )
}
