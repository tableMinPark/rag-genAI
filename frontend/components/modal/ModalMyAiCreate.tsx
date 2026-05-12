import {
  createProjectApi,
  getPromptRolesApi,
  getPromptStylesApi,
  getPromptTonesApi,
} from '@/api/myai'
import { FetchEventSource } from '@/api/stream'
import { useModalStore } from '@/stores/modalStore'
import { useUiStore } from '@/stores/uiStore'
import { PromptParameter } from '@/types/domain'
import { StreamEvent } from '@/types/streamEvent'
import { FileSearch, FileText, Loader2, Save, Upload, X } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'

const ALLOW_EXT = ['pdf', 'hwp', 'hwpx']

interface ModalMyAiCreateProps {
  onCreate: () => void
  onClose: () => void
}

export default function ModalMyAiCreate({
  onCreate,
  onClose,
}: ModalMyAiCreateProps) {
  const uiStore = useUiStore()
  const modalStore = useModalStore()

  // ###################################################
  // 상태 관리
  // ###################################################
  // 프로젝트명
  const [projectName, setProjectName] = useState('')
  // 프로젝트 설명
  const [projectDescription, setProjectDescription] = useState('')
  // 프로젝트 역할 목록
  const [promptRoles, setPromptRoles] = useState<PromptParameter[]>([])
  // 프로젝트 역할 목록
  const [answerTones, setAnswerTones] = useState<PromptParameter[]>([])
  // 프로젝트 역할 목록
  const [promptStyles, setPromptStyles] = useState<PromptParameter[]>([])
  // 프로젝트 역할
  const [promptRole, setPromptRole] = useState('')
  // 답변 톤
  const [answerTone, setAnswerTone] = useState('')
  // 답변 스타일
  const [answerStyle, setAnswerStyle] = useState('')
  // 프로젝트 파일 목록
  const [projectFiles, setProjectFiles] = useState<File[]>([])
  // 프로젝트 생성 상태
  const [isLoading, setIsLoading] = useState(false)
  // 스트리밍 여부 상태
  const [isStreaming, setIsStreaming] = useState(false)
  const streamRef = useRef<FetchEventSource | null>(null)

  // ###################################################
  // 랜더링 이펙트
  // ###################################################
  useEffect(() => {
    setProjectName('')
    setProjectDescription('')
    setProjectFiles([])
    handleGetPromptParams()
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
      modalStore.setError(
        '필수 입력값 누락',
        '프로젝트 이름 필수',
        '프로젝트 이름을 입력해주세요.',
      )
      return
    }
    if (!projectDescription.trim()) {
      modalStore.setError(
        '필수 입력값 누락',
        '프로젝트 설명 필수',
        '프로젝트 설명을 입력해주세요.',
      )
      return
    }
    if (!promptRole.trim()) {
      modalStore.setError(
        '필수 입력값 누락',
        '프로젝트 역할 필수',
        '프로젝트 역할을 선택해주세요.',
      )
      return
    }
    if (!answerTone.trim()) {
      modalStore.setError(
        '필수 입력값 누락',
        '답변 톤 필수',
        '답변 톤을 선택해주세요.',
      )
      return
    }
    if (!answerStyle.trim()) {
      modalStore.setError(
        '필수 입력값 누락',
        '답변 스타일 필수',
        '답변 스타일을 선택해주세요.',
      )
      return
    }
    if (projectFiles.length === 0) {
      modalStore.setError(
        '필수 입력값 누락',
        '프로젝트 문서 등록 필수',
        '최소 1개 이상의 학습 문서를 업로드해주세요.',
      )
      return
    }
    projectFiles.map((projectFile) => {
      const ext = projectFile.name.split('.').pop()?.toLowerCase()
      if (!ALLOW_EXT.includes(ext || '')) {
        modalStore.setError(
          '지원하지 않는 파일 형식',
          '파일 형식 오류',
          `${projectFile.name} 파일은 지원되지 않는 형식입니다.`,
        )
        return
      }
    })
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
          '프로젝트 등록 실패',
          event.data ||
            '프로젝트 등록 중 에러가 발생했습니다. 문서를 재등록해주세요.',
        )
        setIsStreaming(false)
        streamRef.current = null
        setIsLoading(false)
      },
      onPrepareDone: () => {
        onCreate()
      },
    })
    // 세션 기반 SSE 연결
    await createProjectApi(
      projectName,
      projectDescription,
      promptRole,
      answerTone,
      answerStyle,
      projectFiles,
      streamEvent,
    )
      .then((stream) => {
        console.log(`📡 프로젝트 생성 요청 성공`)
        streamRef.current = stream
      })
      .catch((reason) => {
        console.error(reason)
        modalStore.setError(
          '서버 통신 에러',
          '프로젝트 생성 실패',
          '프로젝트 생성에 실패했습니다.',
        )
        setIsStreaming(false)
        streamRef.current = null
      })
  }

  /**
   * 프롬프트 생성 파라미터 목록 조회 핸들러
   */
  const handleGetPromptParams = async () => {
    uiStore.setLoading('프롬프트 속성 목록을 불러오는 중입니다')
    await getPromptRolesApi()
      .then((response) => {
        console.log(`📡 ${response.message}`)
        setPromptRoles(response.result)
      })
      .catch((reason) => {
        console.error(reason)
        uiStore.setError('프롬프트 속성이 없습니다.', handleGetPromptParams)
      })
    await getPromptTonesApi()
      .then((response) => {
        console.log(`📡 ${response.message}`)
        setAnswerTones(response.result)
      })
      .catch((reason) => {
        console.error(reason)
        uiStore.setError('프롬프트 속성이 없습니다.', handleGetPromptParams)
      })
    await getPromptStylesApi()
      .then((response) => {
        console.log(`📡 ${response.message}`)
        setPromptStyles(response.result)
        uiStore.reset()
      })
      .catch((reason) => {
        console.error(reason)
        uiStore.setError('프롬프트 속성이 없습니다.', handleGetPromptParams)
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
        <div className="flex flex-col gap-4">
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
              역할 <span className="text-red-500">*</span>
            </label>
            <select
              value={promptRole}
              onChange={(e) => setPromptRole(e.target.value)}
              className="focus:border-primary focus:ring-primary w-full rounded-lg border border-gray-300 px-3 py-2.5 text-sm outline-none focus:ring-1"
            >
              <option value="" disabled>
                역할을 선택해주세요.
              </option>
              {promptRoles.map((role) => (
                <option key={role.code} value={role.code}>
                  {role.name}
                </option>
              ))}
            </select>
          </div>
          <div className="flex w-full flex-row gap-4">
            <div className="w-full">
              <label className="mb-1.5 block text-xs font-bold text-gray-600">
                답변 톤 <span className="text-red-500">*</span>
              </label>
              <select
                value={answerTone}
                onChange={(e) => setAnswerTone(e.target.value)}
                className="focus:border-primary focus:ring-primary w-full rounded-lg border border-gray-300 px-3 py-2.5 text-sm outline-none focus:ring-1"
              >
                <option value="" disabled>
                  답변 톤을 선택해주세요.
                </option>
                {answerTones.map((tone) => (
                  <option key={tone.code} value={tone.code}>
                    {tone.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="w-full">
              <label className="mb-1.5 block text-xs font-bold text-gray-600">
                답변 스타일 <span className="text-red-500">*</span>
              </label>
              <select
                value={answerStyle}
                onChange={(e) => setAnswerStyle(e.target.value)}
                className="focus:border-primary focus:ring-primary w-full rounded-lg border border-gray-300 px-3 py-2.5 text-sm outline-none focus:ring-1"
              >
                <option value="" disabled>
                  답변 스타일을 선택해주세요.
                </option>
                {promptStyles.map((style) => (
                  <option key={style.code} value={style.code}>
                    {style.name}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <div>
            <label className="mb-1.5 block text-xs font-bold text-gray-600">
              학습 문서 업로드 <span className="text-red-500">*</span>
            </label>
            <label className="group hover:border-primary flex h-28 w-full cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed border-gray-300 bg-gray-50 transition-colors hover:bg-blue-50">
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
