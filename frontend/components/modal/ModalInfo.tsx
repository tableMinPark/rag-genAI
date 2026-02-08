import { useModalStore } from '@/stores/modalStore'
import { AlertCircleIcon, Loader2, X } from 'lucide-react'

export default function ModalInfo() {
  const modalStore = useModalStore()
  const isLoading = useModalStore((m) => m.isLoading)

  // ###################################################
  // 렌더링 (Render)
  // ###################################################
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm">
      <div className="animate-in fade-in zoom-in-95 w-full max-w-lg rounded-2xl bg-white p-6 shadow-2xl duration-300">
        <div className="mb-6 flex items-center justify-between border-b border-gray-100 pb-4">
          <div className="flex items-center gap-2">
            <AlertCircleIcon className="text-primary h-5 w-5" />
            <h3 className="text-lg font-bold text-gray-800">
              {modalStore.title}
            </h3>
          </div>
          <button
            onClick={modalStore.closeModal}
            className="rounded-full p-1 hover:bg-gray-100"
          >
            <X className="h-5 w-5 text-gray-500" />
          </button>
        </div>
        <div className="mb-8 flex flex-col gap-2">
          <div className="text-lg font-medium text-gray-800">
            {modalStore.message}
          </div>
          <div className="text-sm text-gray-400">{modalStore.description}</div>
        </div>
        <div className="mt-2 flex gap-3">
          <button
            onClick={modalStore.handleConfirm}
            disabled={isLoading}
            className="bg-primary hover:bg-primary-hover flex flex-1 items-center justify-center gap-2 rounded-lg py-2.5 text-sm font-bold text-white shadow-md transition-all active:scale-95 disabled:bg-gray-300"
          >
            {isLoading ? <Loader2 className="h-4 w-4 animate-spin" /> : '확인'}
          </button>
        </div>
      </div>
    </div>
  )
}
