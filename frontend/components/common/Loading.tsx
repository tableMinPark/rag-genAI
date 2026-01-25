import { useUiStore } from '@/stores/uiStore'
import { Loader2 } from 'lucide-react'

/**
 * 로딩 컴포넌트
 * @returns 로딩 화면
 */
export default function Loading() {
  const uiStore = useUiStore()

  return (
    <div className="flex h-full w-full flex-col bg-black/10 backdrop-blur-sm">
      <div className="flex flex-1 flex-col items-center justify-center gap-3">
        <Loader2 className="text-primary h-8 w-8 animate-spin" />
        <p className="text-sm font-bold text-gray-700">{uiStore.message}</p>
      </div>
    </div>
  )
}
