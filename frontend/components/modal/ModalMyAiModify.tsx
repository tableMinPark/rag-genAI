import { getProjectSourcesApi, updateProjectSourcesApi } from '@/api/myai'
import { FetchEventSource } from '@/api/stream'
import { useModalStore } from '@/stores/modalStore'
import { useUiStore } from '@/stores/uiStore'
import { FileDetail, Project } from '@/types/domain'
import { StreamEvent } from '@/types/streamEvent'
import {
  CheckCircle2,
  FileSearch,
  FileText,
  Loader2,
  Save,
  Upload,
  X,
} from 'lucide-react'
import { useEffect, useRef, useState } from 'react'

const ALLOW_EXT = ['pdf', 'hwp', 'hwpx']

interface ModalMyAiModifyProps {
  onModify: () => void
  onClose: () => void
  project: Project
}

export default function ModalMyAiModify({
  onModify,
  onClose,
  project,
}: ModalMyAiModifyProps) {
  const modalStore = useModalStore()
  const uiStore = useUiStore()

  // ###################################################
  // 상태 관리
  // ###################################################
  const [projectFileDetails, setProjectFileDetails] = useState<FileDetail[]>([])
  const [deleteProjectFileDetailIds, setDeleteProjectFileDetailIds] = useState<
    number[]
  >([])
  const [projectFiles, setProjectFiles] = useState<File[]>([])
  const [isLoading, setIsLoading] = useState(false)
  // 스트리밍 여부 상태
  const [isStreaming, setIsStreaming] = useState(false)
  const streamRef = useRef<FetchEventSource | null>(null)

  // ###################################################
  // 랜더링 이펙트
  // ###################################################
  useEffect(() => {
    handleGetProjectFiles()
    setProjectFiles([])
  }, [])

  // ###################################################
  // 핸들러
  // ###################################################
  /**
   * 프로젝트 파일 목록 조회 핸들러
   */
  const handleGetProjectFiles = async () => {
    uiStore.setLoading('프로젝트 파일 목록을 불러오는중 입니다.')
    await getProjectSourcesApi(project.projectId)
      .then((response) => {
        console.log(`📡 ${response.message}`)
        setProjectFileDetails(response.result)
        uiStore.reset()
      })
      .catch((reason) => {
        console.error(reason)
        uiStore.setError('프로젝트를 삭제할 수 없습니다.', () =>
          handleGetProjectFiles(),
        )
      })
  }

  /**
   * 기존 파일 삭제 핸들러
   * @param fileDetail 파일 상세
   */
  const handleRemoveProjectFileDetail = (fileDetail: FileDetail) => {
    modalStore.setConfirm(
      '프로젝트 문서 제외',
      '이 문서를 학습 데이터에서 제외하시겠습니까?',
      '제외한 문서는 복구할 수 없습니다.',
      () => {
        setDeleteProjectFileDetailIds((prev) => [
          ...prev,
          fileDetail.fileDetailId,
        ])
        setProjectFileDetails((prev) =>
          prev.filter((f) => f.fileDetailId !== fileDetail.fileDetailId),
        )
      },
    )
  }

  /**
   * 새로운 파일 추가 핸들러
   * @param e 변경 이벤트
   */
  const handleSelectProjectFile = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      const files = Array.from(e.target.files)
      setProjectFiles((prev) => [...prev, ...files])
    }
  }

  /**
   * 새로운 파일 삭제 핸들러
   * @param index 파일 인덱스
   */
  const handleRemoveProjectFile = (index: number) => {
    setProjectFiles((prev) => prev.filter((_, i) => i !== index))
  }

  /**
   * 프로젝트 수정 핸들러
   */
  const handleModifyProject = async () => {
    // 스트림 상태 체크
    if (isStreaming) return
    // 스트림 시작 상태 변경
    setIsStreaming(true)
    // 스트림 옵션 설정
    const streamEvent = new StreamEvent({
      onConnect: async (_) => {
        console.log(`📡 프로젝트 생성 요청`)
        setIsLoading(true)
      },
      onDisconnect: (_) => {
        setIsStreaming(false)
        streamRef.current = null
        setIsLoading(false)
      },
      onException: (event) => {
        modalStore.setError(
          '에러 발생',
          '문서 목록 수정 실패',
          event.data ||
            '문서 목록 수정 중 에러가 발생했습니다. 문서를 재등록해주세요.',
        )
        setIsStreaming(false)
        streamRef.current = null
        setIsLoading(false)
      },
      onPrepareDone: () => {
        onModify()
      },
    })
    // 세션 기반 SSE 연결
    await updateProjectSourcesApi(
      project.projectId,
      deleteProjectFileDetailIds,
      projectFiles,
      streamEvent,
    )
      .then((stream) => {
        console.log(`📡 프로젝트 수정 요청 성공`)
        streamRef.current = stream
      })
      .catch((reason) => {
        console.error(reason)
        uiStore.setError('프로젝트 문서를 수정할 수 없습니다.', () =>
          handleModifyProject(),
        )
        setIsStreaming(false)
        streamRef.current = null
      })
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
              프로젝트 수정
            </h3>
            <p className="mt-1 text-xs text-gray-500">
              문서를 추가하거나 제외할 수 있습니다.
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
          {/* 1. 기존 학습 문서 목록 */}
          <div>
            <div className="mb-2 flex items-center justify-between">
              <label className="text-xs font-bold text-gray-600">
                현재 학습된 문서
              </label>
              <span className="text-[10px] text-gray-400">
                {projectFileDetails.length}개 파일
              </span>
            </div>
            <div className="flex max-h-40 flex-col gap-2 overflow-y-auto rounded-xl border border-gray-200 bg-gray-50 p-2">
              {projectFileDetails.length > 0 ? (
                projectFileDetails.map((fileDetail) => (
                  <div
                    key={fileDetail.fileDetailId}
                    className="flex items-center justify-between rounded-lg bg-white px-3 py-2 text-sm shadow-sm"
                  >
                    <div className="flex items-center gap-2 overflow-hidden">
                      <CheckCircle2 className="h-4 w-4 shrink-0 text-green-500" />
                      <span className="truncate text-gray-700">
                        {fileDetail.fileOriginName}
                      </span>
                      <span className="shrink-0 text-[10px] text-gray-400">
                        ({(fileDetail.fileSize / 1024).toFixed(1)} KB)
                      </span>
                    </div>
                    <button
                      onClick={() => handleRemoveProjectFileDetail(fileDetail)}
                      title="학습에서 제외(삭제)"
                      className="rounded p-1 text-gray-400 hover:bg-red-50 hover:text-red-500"
                    >
                      <X className="h-4 w-4" />
                    </button>
                  </div>
                ))
              ) : (
                <div className="py-4 text-center text-xs text-gray-400">
                  학습된 문서가 없습니다.
                </div>
              )}
            </div>
          </div>
          {/* 2. 추가 업로드 영역 */}
          <div>
            <label className="mb-2 block text-xs font-bold text-gray-600">
              추가 업로드
            </label>
            <label className="group hover:border-primary flex h-32 w-full cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed border-gray-300 bg-gray-50 transition-colors hover:bg-blue-50">
              <div className="flex flex-col items-center justify-center py-4 text-center">
                <Upload className="group-hover:text-primary mb-2 h-8 w-8 text-gray-400 transition-colors" />
                <p className="group-hover:text-primary text-sm font-bold text-gray-600">
                  클릭하여 파일 선택
                </p>
                <p className="text-[10px] text-gray-400">
                  {ALLOW_EXT.map((ext) => ext.toUpperCase()).join(', ')}
                </p>
              </div>
              <input
                type="file"
                className="hidden"
                accept={ALLOW_EXT.map((ext) => `.${ext}`).join(', ')}
                multiple
                onChange={handleSelectProjectFile}
              />
            </label>
            {/* 추가된 파일 목록 */}
            {projectFiles.length > 0 && (
              <div className="mt-3 flex max-h-32 flex-col gap-2 overflow-y-auto pr-1">
                {projectFiles.map((file, index) => (
                  <div
                    key={index}
                    className="flex items-center justify-between rounded-lg border border-blue-100 bg-blue-50 px-3 py-2 text-sm"
                  >
                    <div className="flex items-center gap-2 overflow-hidden">
                      <FileText className="h-4 w-4 shrink-0 text-blue-500" />
                      <span className="truncate text-gray-700">
                        {file.name}
                      </span>
                      <span className="rounded bg-blue-100 px-1.5 py-0.5 text-[10px] font-bold text-blue-600">
                        NEW
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
          {/* 하단 버튼 */}
          <div className="mt-2 flex gap-3">
            <button
              onClick={onClose}
              className="flex-1 rounded-lg border border-gray-300 bg-white py-2.5 text-sm font-bold text-gray-600 hover:bg-gray-50"
            >
              취소
            </button>
            <button
              onClick={handleModifyProject}
              disabled={isLoading}
              className="bg-primary hover:bg-primary-hover flex flex-1 items-center justify-center gap-2 rounded-lg py-2.5 text-sm font-bold text-white shadow-md transition-all active:scale-95 disabled:bg-gray-300"
            >
              {isLoading ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  수정중
                </>
              ) : (
                <>
                  <Save className="h-4 w-4" />
                  프로젝트 수정
                </>
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
