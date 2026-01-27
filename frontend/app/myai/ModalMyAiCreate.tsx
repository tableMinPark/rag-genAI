import { createProjectApi } from '@/api/myai'
import { useModalStore } from '@/stores/modalStore'
import { FileSearch, FileText, Loader2, Save, Upload, X } from 'lucide-react'
import { useEffect, useState } from 'react'

interface ModalMyAiCreateProps {
  onCreate: () => void
  onClose: () => void
}

export default function ModalMyAiCreate({
  onCreate,
  onClose,
}: ModalMyAiCreateProps) {
  const modalStore = useModalStore()

  // ###################################################
  // 상태 관리
  // ###################################################
  // 프로젝트명
  const [projectName, setProjectName] = useState('')
  // 프로젝트 설명
  const [projectDescription, setProjectDescription] = useState('')
  // 프로젝트 파일 목록
  const [projectFiles, setProjectFiles] = useState<File[]>([])
  // 프로젝트 생성 상태
  const [isLoading, setIsLoading] = useState(false)

  // ###################################################
  // 랜더링 이펙트
  // ###################################################
  useEffect(() => {
    setProjectName('')
    setProjectDescription('')
    setProjectFiles([])
  }, [])

  // ###################################################
  // 핸들러
  // ###################################################
  /**
   * 파일 목록 변경 이벤트 핸들러
   * @param e 변경 이벤트
   */
  const handleSelectProjectFile = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      const filesArray = Array.from(e.target.files)
      setProjectFiles((prev) => [...prev, ...filesArray])
    }
  }

  /**
   * 파일 목록 삭제 이벤트 핸들러
   * @param index 파일 목록 인덱스
   */
  const handleRemoveProjectFile = (index: number) => {
    setProjectFiles((prev) => prev.filter((_, i) => i !== index))
  }

  /**
   * 프로젝트 생성 핸들러
   */
  const handleCreateProject = async () => {
    if (!projectName.trim()) {
      modalStore.setError('프로젝트 이름 필수', '프로젝트 이름을 입력해주세요.')
      return
    }
    if (!projectDescription.trim()) {
      modalStore.setError('프로젝트 설명 필수', '프로젝트 설명을 입력해주세요.')
      return
    }
    if (projectFiles.length === 0) {
      modalStore.setError(
        '프로젝트 문서 등록 필수',
        '최소 1개 이상의 학습 문서를 업로드해주세요.',
      )
      return
    }
    setIsLoading(true)
    await createProjectApi(projectName, projectDescription, projectFiles)
      .then((response) => {
        console.log(`📡 ${response.message}`)
        onCreate()
      })
      .catch((reason) => {
        console.error(reason)
        modalStore.setError(
          '프로젝트 생성 실패',
          '프로젝트 생성에 실패했습니다.',
        )
      })
      .finally(() => setIsLoading(false))
  }

  // ###################################################
  // 렌더링 (Render)
  // ###################################################
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm">
      <div className="animate-in fade-in zoom-in-95 w-full max-w-lg rounded-2xl bg-white p-6 shadow-2xl duration-300">
        {/* 헤더 */}
        <div className="mb-6 flex items-center justify-between border-b border-gray-100 pb-2">
          <div>
            <h3 className="flex items-center gap-2 text-lg font-bold text-gray-800">
              <FileSearch className="text-primary h-6 w-6" />
              프로젝트 생성
            </h3>
            <p className="mt-1 text-xs text-gray-500">
              새로운 프로젝트를 생성합니다.
            </p>
          </div>
          <button
            onClick={onClose}
            className="rounded-full p-1 hover:bg-gray-100"
          >
            <X className="h-5 w-5 text-gray-500" />
          </button>
        </div>
        <div className="flex flex-col gap-6">
          <div>
            <label className="mb-1.5 block text-xs font-bold text-gray-600">
              프로젝트명 <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              value={projectName}
              onChange={(e) => setProjectName(e.target.value)}
              placeholder="예: 메뉴얼 문서 기반 답변 봇"
              className="focus:border-primary focus:ring-primary w-full rounded-lg border border-gray-300 px-3 py-2.5 text-sm outline-none focus:ring-1"
            />
          </div>
          <div>
            <label className="mb-1.5 block text-xs font-bold text-gray-600">
              프로젝트 설명 <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              value={projectDescription}
              onChange={(e) => setProjectDescription(e.target.value)}
              placeholder="예: 메뉴얼 문서를 기반으로 답변을 하는 봇입니다."
              className="focus:border-primary focus:ring-primary w-full rounded-lg border border-gray-300 px-3 py-2.5 text-sm outline-none focus:ring-1"
            />
          </div>
          <div>
            <label className="mb-1.5 block text-xs font-bold text-gray-600">
              학습 문서 업로드 <span className="text-red-500">*</span>
            </label>
            <label className="group hover:border-primary flex h-32 w-full cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed border-gray-300 bg-gray-50 transition-colors hover:bg-blue-50">
              <div className="flex flex-col items-center justify-center py-4 text-center">
                <Upload className="group-hover:text-primary mb-2 h-8 w-8 text-gray-400 transition-colors" />
                <p className="group-hover:text-primary text-sm font-bold text-gray-600">
                  클릭하여 파일 선택
                </p>
                <p className="text-[10px] text-gray-400">PDF, HWP</p>
              </div>
              <input
                type="file"
                className="hidden"
                multiple
                onChange={handleSelectProjectFile}
              />
            </label>
            {projectFiles.length > 0 && (
              <div className="mt-3 flex max-h-32 flex-col gap-2 overflow-y-auto pr-1">
                {projectFiles.map((file, index) => (
                  <div
                    key={index}
                    className="flex items-center justify-between rounded-lg border border-gray-200 bg-gray-50 px-3 py-2 text-sm"
                  >
                    <div className="flex items-center gap-2 overflow-hidden">
                      <FileText className="h-4 w-4 shrink-0 text-blue-500" />
                      <span className="truncate text-gray-700">
                        {file.name}
                      </span>
                    </div>
                    <button
                      onClick={() => handleRemoveProjectFile(index)}
                      className="text-gray-400 hover:text-red-500"
                    >
                      <X className="h-4 w-4" />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
          <div className="mt-2 flex gap-3">
            <button
              onClick={onClose}
              className="flex-1 rounded-lg border border-gray-300 bg-white py-2.5 text-sm font-bold text-gray-600 hover:bg-gray-50"
            >
              취소
            </button>
            <button
              onClick={handleCreateProject}
              disabled={isLoading}
              className="bg-primary hover:bg-primary-hover flex flex-1 items-center justify-center gap-2 rounded-lg py-2.5 text-sm font-bold text-white shadow-md transition-all active:scale-95 disabled:bg-gray-300"
            >
              {isLoading ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  생성중
                </>
              ) : (
                <>
                  <Save className="h-4 w-4" />
                  프로젝트 생성
                </>
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
