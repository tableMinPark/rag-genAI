'use client'

import { useState, useEffect, useRef } from 'react'
import { useRouter } from 'next/navigation'
import {
  FolderOpen,
  Loader2,
  AlertCircle,
  RefreshCw,
  Upload,
  X,
  FileText,
  CheckCircle2,
  Plus,
  Trash2,
  Settings,
  Database,
  Server,
} from 'lucide-react'
import { RegexPattern, RepoResource, Source } from '@/types/domain'
import { getSourcesApi } from '@/api/source'

// ###################################################
// 로컬 타입 정의
// ###################################################
// 등록 폼 데이터 타입
interface RegisterFormData {
  collectionId: string
  categoryCode: string
  isAuto: boolean
  maxToken: number
  overlapSize: number
  chunkingType: 'regex' | 'none' | 'token'
  // 3 depth 패턴
  regexPatterns: {
    depth1: RegexPattern[]
    depth2: RegexPattern[]
    depth3: RegexPattern[]
  }
  sourceType: 'file' | 'repo'
  // Repo 정보
  repoHost: string
  repoPort: string
  repoResources: RepoResource[]
}

// ###################################################
// [API Mock]
// ###################################################
// [API Mock] 문서 등록 API (Form Data 전체 전송)
const registerDocument = async (
  formData: RegisterFormData,
  file: File | null,
): Promise<void> => {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      console.log('[API Request] Register Document:', { formData, file })
      // File 타입인데 파일이 없으면 에러
      if (formData.sourceType === 'file' && !file) {
        reject(new Error('파일을 업로드해주세요.'))
        return
      }
      resolve()
    }, 2000)
  })
}
// ###################################################
// [Sub Component] 문서 등록 모달 (스타일 개선)
// ###################################################
const DocumentRegisterModal = ({
  isOpen,
  onClose,
  onSuccess,
}: {
  isOpen: boolean
  onClose: () => void
  onSuccess: () => void
}) => {
  // --- 상태 관리 (이전과 동일) ---
  const [formData, setFormData] = useState<RegisterFormData>({
    collectionId: '',
    categoryCode: 'LAW',
    isAuto: true,
    maxToken: 1000,
    overlapSize: 100,
    chunkingType: 'token',
    regexPatterns: {
      depth1: [{ prefix: '', isTitle: false }],
      depth2: [],
      depth3: [],
    },
    sourceType: 'file',
    repoHost: '',
    repoPort: '',
    repoResources: [],
  })

  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [isUploading, setIsUploading] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (!isOpen) {
      setSelectedFile(null)
      setIsUploading(false)
    }
  }, [isOpen])

  // --- 핸들러 (이전과 동일) ---
  const handleInputChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>,
  ) => {
    const { name, value, type } = e.target
    setFormData((prev) => ({
      ...prev,
      [name]:
        type === 'checkbox' ? (e.target as HTMLInputElement).checked : value,
    }))
  }

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) setSelectedFile(file)
  }

  const handlePatternChange = (
    depth: 'depth1' | 'depth2' | 'depth3',
    index: number,
    field: keyof RegexPattern,
    value: any,
  ) => {
    setFormData((prev) => {
      const newPatterns = [...prev.regexPatterns[depth]]
      newPatterns[index] = { ...newPatterns[index], [field]: value }
      return {
        ...prev,
        regexPatterns: { ...prev.regexPatterns, [depth]: newPatterns },
      }
    })
  }

  const addPattern = (depth: 'depth1' | 'depth2' | 'depth3') => {
    setFormData((prev) => ({
      ...prev,
      regexPatterns: {
        ...prev.regexPatterns,
        [depth]: [...prev.regexPatterns[depth], { prefix: '', isTitle: false }],
      },
    }))
  }

  const removePattern = (
    depth: 'depth1' | 'depth2' | 'depth3',
    index: number,
  ) => {
    setFormData((prev) => {
      const newPatterns = prev.regexPatterns[depth].filter(
        (_, i) => i !== index,
      )
      return {
        ...prev,
        regexPatterns: { ...prev.regexPatterns, [depth]: newPatterns },
      }
    })
  }

  const addRepoResource = () => {
    setFormData((prev) => ({
      ...prev,
      repoResources: [
        ...prev.repoResources,
        { originFileName: '', fileName: '', ext: 'json', path: '', urn: '' },
      ],
    }))
  }

  const updateRepoResource = (
    index: number,
    field: keyof RepoResource,
    value: string,
  ) => {
    setFormData((prev) => {
      const newResources = [...prev.repoResources]
      newResources[index] = { ...newResources[index], [field]: value }
      return { ...prev, repoResources: newResources }
    })
  }

  const removeRepoResource = (index: number) => {
    setFormData((prev) => ({
      ...prev,
      repoResources: prev.repoResources.filter((_, i) => i !== index),
    }))
  }

  const handleSubmit = async () => {
    if (!formData.collectionId) return alert('색인 테이블명을 입력해주세요.')
    if (formData.sourceType === 'file' && !selectedFile)
      return alert('파일을 업로드해주세요.')

    setIsUploading(true)
    try {
      await registerDocument(formData, selectedFile)
      alert('성공적으로 등록되었습니다.')
      onSuccess()
      onClose()
    } catch (error) {
      console.error(error)
      alert('등록 중 오류가 발생했습니다.')
    } finally {
      setIsUploading(false)
    }
  }

  if (!isOpen) return null

  // --- 공통 스타일 클래스 ---
  const inputClass =
    'w-full rounded-lg border border-gray-200 px-3 py-2 text-sm text-gray-800 outline-none transition-all placeholder:text-gray-400 focus:border-primary focus:ring-1 focus:ring-primary disabled:bg-gray-50'
  const labelClass = 'mb-1.5 block text-xs font-bold text-gray-500'
  const sectionHeaderClass =
    'flex items-center gap-2 border-b border-gray-100 pb-3 text-sm font-bold text-gray-800'

  return (
    <div className="animate-in fade-in fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm duration-200">
      <div className="flex h-[90vh] w-full max-w-4xl flex-col overflow-hidden rounded-2xl bg-white shadow-2xl ring-1 ring-gray-200">
        {/* 헤더 */}
        <div className="flex shrink-0 items-center justify-between border-b border-gray-100 bg-white px-8 py-5">
          <div className="flex items-center gap-3">
            <div className="bg-primary/10 text-primary flex h-10 w-10 items-center justify-center rounded-full">
              <Upload className="h-5 w-5" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-gray-900">문서 등록</h3>
              <p className="text-xs text-gray-500">
                새로운 지식 베이스 문서를 등록하고 설정합니다.
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            disabled={isUploading}
            className="rounded-full p-2 text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-600 disabled:opacity-50"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* 본문 (스크롤) */}
        <div className="scrollbar-thin scrollbar-thumb-gray-200 scrollbar-track-transparent flex-1 overflow-y-auto bg-white p-8">
          <div className="flex flex-col gap-10">
            {/* 1. 기본 정보 섹션 */}
            <section className="flex flex-col gap-5">
              <h4 className={sectionHeaderClass}>
                <Database className="text-primary h-4 w-4" /> 기본 설정 (Basic
                Info)
              </h4>
              <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
                <div>
                  <label className={labelClass}>
                    색인 테이블명 (Collection ID)
                  </label>
                  <input
                    type="text"
                    name="collectionId"
                    value={formData.collectionId}
                    onChange={handleInputChange}
                    placeholder="ex: rag_collection_2024"
                    className={inputClass}
                  />
                </div>
                <div>
                  <label className={labelClass}>카테고리 분류</label>
                  <select
                    name="categoryCode"
                    value={formData.categoryCode}
                    onChange={handleInputChange}
                    className={`${inputClass} cursor-pointer bg-white`}
                  >
                    <option value="LAW">법률 (LAW)</option>
                    <option value="GUIDE">지침 (GUIDE)</option>
                    <option value="MANUAL">매뉴얼 (MANUAL)</option>
                    <option value="EDU">교육자료 (EDU)</option>
                  </select>
                </div>
              </div>
              <div className="flex items-center gap-3 rounded-lg border border-gray-100 bg-gray-50 p-3">
                <input
                  type="checkbox"
                  id="isAuto"
                  name="isAuto"
                  checked={formData.isAuto}
                  onChange={handleInputChange}
                  className="text-primary focus:ring-primary h-4 w-4 cursor-pointer rounded border-gray-300"
                />
                <label
                  htmlFor="isAuto"
                  className="cursor-pointer text-sm font-medium text-gray-700 select-none"
                >
                  자동화 처리 활성화 (Auto Processing Pipeline)
                </label>
              </div>
            </section>

            {/* 2. 청킹 설정 섹션 */}
            <section className="flex flex-col gap-5">
              <h4 className={sectionHeaderClass}>
                <Settings className="text-primary h-4 w-4" /> 청킹 설정
                (Chunking Strategy)
              </h4>
              <div className="grid grid-cols-1 gap-6 md:grid-cols-3">
                <div>
                  <label className={labelClass}>최대 토큰 수</label>
                  <input
                    type="number"
                    name="maxToken"
                    value={formData.maxToken}
                    onChange={handleInputChange}
                    className={inputClass}
                  />
                </div>
                <div>
                  <label className={labelClass}>오버랩 크기</label>
                  <input
                    type="number"
                    name="overlapSize"
                    value={formData.overlapSize}
                    onChange={handleInputChange}
                    className={inputClass}
                  />
                </div>
                <div>
                  <label className={labelClass}>청킹 타입</label>
                  <select
                    name="chunkingType"
                    value={formData.chunkingType}
                    onChange={handleInputChange}
                    className={`${inputClass} cursor-pointer bg-white`}
                  >
                    <option value="token">Token (글자수 기반)</option>
                    <option value="regex">Regex (정규식 기반)</option>
                    <option value="none">None (지정 안함)</option>
                  </select>
                </div>
              </div>

              {/* [Conditional] Regex 패턴 입력 폼 */}
              {formData.chunkingType === 'regex' && (
                <div className="mt-2 flex flex-col gap-5 rounded-xl border border-blue-100 bg-blue-50/50 p-5">
                  <div className="flex items-center gap-2 text-xs font-bold text-blue-700">
                    <Settings className="h-3 w-3" />
                    정규식 계층 구조 설정 (3 Depth)
                  </div>

                  {(['depth1', 'depth2', 'depth3'] as const).map(
                    (depth, idx) => (
                      <div key={depth} className="flex flex-col gap-3">
                        <div className="flex items-center justify-between">
                          <span className="text-[10px] font-bold tracking-wider text-gray-500 uppercase">
                            Level {idx + 1} Patterns
                          </span>
                          <button
                            onClick={() => addPattern(depth)}
                            className="hover:text-primary hover:border-primary/30 flex items-center gap-1 rounded border border-gray-200 bg-white px-2 py-1 text-[10px] font-medium text-gray-600 shadow-sm transition-all hover:bg-gray-50"
                          >
                            <Plus className="h-3 w-3" /> 추가
                          </button>
                        </div>

                        {formData.regexPatterns[depth].length === 0 && (
                          <div className="rounded border border-dashed border-gray-300 bg-white/50 p-2 text-center text-[10px] text-gray-400">
                            등록된 패턴이 없습니다.
                          </div>
                        )}

                        <div className="flex flex-col gap-2">
                          {formData.regexPatterns[depth].map(
                            (pattern, pIdx) => (
                              <div
                                key={pIdx}
                                className="animate-in fade-in slide-in-from-top-1 flex items-center gap-2 duration-200"
                              >
                                <div className="relative flex-1">
                                  <span className="absolute top-1/2 left-3 -translate-y-1/2 text-xs font-bold text-gray-400">
                                    /
                                  </span>
                                  <input
                                    type="text"
                                    placeholder="^제\d+조"
                                    value={pattern.prefix}
                                    onChange={(e) =>
                                      handlePatternChange(
                                        depth,
                                        pIdx,
                                        'prefix',
                                        e.target.value,
                                      )
                                    }
                                    className="focus:border-primary focus:ring-primary w-full rounded-md border border-gray-200 py-1.5 pr-3 pl-6 text-xs outline-none focus:ring-1"
                                  />
                                  <span className="absolute top-1/2 right-3 -translate-y-1/2 text-xs font-bold text-gray-400">
                                    /gm
                                  </span>
                                </div>
                                <label className="flex cursor-pointer items-center gap-1.5 rounded-md border border-gray-200 bg-white px-2 py-1.5 hover:bg-gray-50">
                                  <input
                                    type="checkbox"
                                    checked={pattern.isTitle}
                                    onChange={(e) =>
                                      handlePatternChange(
                                        depth,
                                        pIdx,
                                        'isTitle',
                                        e.target.checked,
                                      )
                                    }
                                    className="text-primary focus:ring-primary h-3 w-3 rounded border-gray-300"
                                  />
                                  <span className="text-[10px] font-medium text-gray-600">
                                    제목
                                  </span>
                                </label>
                                <button
                                  onClick={() => removePattern(depth, pIdx)}
                                  className="flex h-8 w-8 items-center justify-center rounded-md border border-transparent text-gray-400 transition-colors hover:border-red-100 hover:bg-red-50 hover:text-red-500"
                                >
                                  <Trash2 className="h-4 w-4" />
                                </button>
                              </div>
                            ),
                          )}
                        </div>
                      </div>
                    ),
                  )}
                </div>
              )}
            </section>

            {/* 3. 수집(Source) 설정 섹션 */}
            <section className="flex flex-col gap-5">
              <h4 className={sectionHeaderClass}>
                <Server className="text-primary h-4 w-4" /> 데이터 소스 (Source
                Type)
              </h4>

              {/* 소스 타입 선택 (Radio Cards) */}
              <div className="grid grid-cols-2 gap-4">
                <label
                  className={`relative flex cursor-pointer items-center gap-4 rounded-xl border-2 p-4 transition-all ${
                    formData.sourceType === 'file'
                      ? 'border-primary bg-primary/5 ring-primary/20 ring-1'
                      : 'border-gray-100 bg-white hover:border-gray-300 hover:shadow-sm'
                  }`}
                >
                  <input
                    type="radio"
                    name="sourceType"
                    value="file"
                    checked={formData.sourceType === 'file'}
                    onChange={handleInputChange}
                    className="hidden"
                  />
                  <div
                    className={`flex h-10 w-10 items-center justify-center rounded-full ${formData.sourceType === 'file' ? 'bg-primary text-white' : 'bg-gray-100 text-gray-400'}`}
                  >
                    <FileText className="h-5 w-5" />
                  </div>
                  <div>
                    <div className="text-sm font-bold text-gray-900">
                      파일 업로드 (File)
                    </div>
                    <div className="text-xs text-gray-500">
                      로컬 파일을 직접 등록합니다.
                    </div>
                  </div>
                  {formData.sourceType === 'file' && (
                    <CheckCircle2 className="text-primary absolute top-4 right-4 h-5 w-5" />
                  )}
                </label>

                <label
                  className={`relative flex cursor-pointer items-center gap-4 rounded-xl border-2 p-4 transition-all ${
                    formData.sourceType === 'repo'
                      ? 'border-primary bg-primary/5 ring-primary/20 ring-1'
                      : 'border-gray-100 bg-white hover:border-gray-300 hover:shadow-sm'
                  }`}
                >
                  <input
                    type="radio"
                    name="sourceType"
                    value="repo"
                    checked={formData.sourceType === 'repo'}
                    onChange={handleInputChange}
                    className="hidden"
                  />
                  <div
                    className={`flex h-10 w-10 items-center justify-center rounded-full ${formData.sourceType === 'repo' ? 'bg-primary text-white' : 'bg-gray-100 text-gray-400'}`}
                  >
                    <Server className="h-5 w-5" />
                  </div>
                  <div>
                    <div className="text-sm font-bold text-gray-900">
                      저장소 연동 (Repo)
                    </div>
                    <div className="text-xs text-gray-500">
                      원격 저장소의 파일을 가져옵니다.
                    </div>
                  </div>
                  {formData.sourceType === 'repo' && (
                    <CheckCircle2 className="text-primary absolute top-4 right-4 h-5 w-5" />
                  )}
                </label>
              </div>

              {/* [Conditional] FILE Upload */}
              {formData.sourceType === 'file' && (
                <div className="animate-in fade-in slide-in-from-top-2 duration-300">
                  {!selectedFile ? (
                    <label className="group hover:border-primary flex h-40 w-full cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed border-gray-300 bg-gray-50 transition-all hover:bg-blue-50">
                      <div className="flex flex-col items-center justify-center pt-5 pb-6">
                        <Upload className="group-hover:text-primary mb-3 h-8 w-8 text-gray-400 transition-colors" />
                        <p className="group-hover:text-primary text-sm font-bold text-gray-500">
                          클릭하여 파일 업로드
                        </p>
                        <p className="mt-1 text-xs text-gray-400">
                          PDF, DOCX, HWP, TXT (Max 10MB)
                        </p>
                      </div>
                      <input
                        type="file"
                        className="hidden"
                        ref={fileInputRef}
                        onChange={handleFileChange}
                      />
                    </label>
                  ) : (
                    <div className="border-primary/20 flex items-center gap-4 rounded-xl border bg-blue-50 p-4">
                      <div className="text-primary flex h-12 w-12 items-center justify-center rounded-full bg-white shadow-sm">
                        <FileText className="h-6 w-6" />
                      </div>
                      <div className="flex-1 overflow-hidden">
                        <p className="truncate text-sm font-bold text-gray-800">
                          {selectedFile.name}
                        </p>
                        <p className="text-xs text-gray-500">
                          {(selectedFile.size / 1024).toFixed(1)} KB
                        </p>
                      </div>
                      <button
                        onClick={() => setSelectedFile(null)}
                        className="rounded-full p-2 text-gray-400 transition-all hover:bg-white hover:text-red-500 hover:shadow-sm"
                      >
                        <X className="h-5 w-5" />
                      </button>
                    </div>
                  )}
                </div>
              )}

              {/* [Conditional] REPO Settings */}
              {formData.sourceType === 'repo' && (
                <div className="animate-in fade-in slide-in-from-top-2 flex flex-col gap-6 duration-300">
                  <div className="grid grid-cols-2 gap-6">
                    <div>
                      <label className={labelClass}>Host Address</label>
                      <input
                        type="text"
                        name="repoHost"
                        value={formData.repoHost}
                        onChange={handleInputChange}
                        placeholder="192.168.0.1"
                        className={inputClass}
                      />
                    </div>
                    <div>
                      <label className={labelClass}>Port</label>
                      <input
                        type="text"
                        name="repoPort"
                        value={formData.repoPort}
                        onChange={handleInputChange}
                        placeholder="8080"
                        className={inputClass}
                      />
                    </div>
                  </div>

                  <div className="flex flex-col gap-3">
                    <div className="flex items-center justify-between">
                      <label className={labelClass}>Repository Resources</label>
                      <button
                        onClick={addRepoResource}
                        className="border-primary bg-primary hover:bg-primary-hover flex items-center gap-1 rounded border px-2.5 py-1.5 text-xs font-bold text-white shadow-sm transition-all"
                      >
                        <Plus className="h-3 w-3" /> 리소스 추가
                      </button>
                    </div>

                    {formData.repoResources.length === 0 && (
                      <div className="flex h-20 items-center justify-center rounded-lg border border-dashed border-gray-300 bg-gray-50 text-xs text-gray-400">
                        등록된 리소스가 없습니다. 우측 상단 버튼을 눌러
                        추가해주세요.
                      </div>
                    )}

                    <div className="flex flex-col gap-3">
                      {formData.repoResources.map((res, idx) => (
                        <div
                          key={idx}
                          className="relative grid grid-cols-12 items-end gap-2 rounded-xl border border-gray-200 bg-white p-3 shadow-sm"
                        >
                          {/* Row 1 */}
                          <div className="col-span-4 flex flex-col gap-1">
                            <span className="text-[10px] font-bold text-gray-400">
                              Origin Name
                            </span>
                            <input
                              type="text"
                              value={res.originFileName}
                              onChange={(e) =>
                                updateRepoResource(
                                  idx,
                                  'originFileName',
                                  e.target.value,
                                )
                              }
                              className={`${inputClass} px-2 py-1.5 text-xs`}
                              placeholder="원본 파일명"
                            />
                          </div>
                          <div className="col-span-4 flex flex-col gap-1">
                            <span className="text-[10px] font-bold text-gray-400">
                              File Name
                            </span>
                            <input
                              type="text"
                              value={res.fileName}
                              onChange={(e) =>
                                updateRepoResource(
                                  idx,
                                  'fileName',
                                  e.target.value,
                                )
                              }
                              className={`${inputClass} px-2 py-1.5 text-xs`}
                              placeholder="저장 파일명"
                            />
                          </div>
                          <div className="col-span-2 flex flex-col gap-1">
                            <span className="text-[10px] font-bold text-gray-400">
                              Ext
                            </span>
                            <input
                              type="text"
                              value={res.ext}
                              onChange={(e) =>
                                updateRepoResource(idx, 'ext', e.target.value)
                              }
                              className={`${inputClass} px-2 py-1.5 text-xs`}
                              placeholder="json"
                            />
                          </div>
                          <div className="col-span-2 flex justify-end pb-1">
                            <button
                              onClick={() => removeRepoResource(idx)}
                              className="rounded-md bg-red-50 p-1.5 text-red-500 transition-colors hover:bg-red-100"
                            >
                              <Trash2 className="h-4 w-4" />
                            </button>
                          </div>

                          {/* Row 2 */}
                          <div className="col-span-8 flex flex-col gap-1">
                            <span className="text-[10px] font-bold text-gray-400">
                              Path
                            </span>
                            <input
                              type="text"
                              value={res.path}
                              onChange={(e) =>
                                updateRepoResource(idx, 'path', e.target.value)
                              }
                              className={`${inputClass} px-2 py-1.5 text-xs`}
                              placeholder="/data/extract"
                            />
                          </div>
                          <div className="col-span-4 flex flex-col gap-1">
                            <span className="text-[10px] font-bold text-gray-400">
                              URN
                            </span>
                            <input
                              type="text"
                              value={res.urn}
                              onChange={(e) =>
                                updateRepoResource(idx, 'urn', e.target.value)
                              }
                              className={`${inputClass} px-2 py-1.5 text-xs`}
                              placeholder="urn:code"
                            />
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              )}
            </section>
          </div>
        </div>

        {/* 하단 버튼 (고정) */}
        <div className="flex shrink-0 items-center justify-end gap-3 border-t border-gray-100 bg-gray-50 px-8 py-5">
          <button
            onClick={onClose}
            disabled={isUploading}
            className="rounded-lg border border-gray-300 bg-white px-5 py-2.5 text-sm font-bold text-gray-600 shadow-sm transition-colors hover:bg-gray-50 disabled:opacity-50"
          >
            취소
          </button>
          <button
            onClick={handleSubmit}
            disabled={isUploading}
            className="bg-primary hover:bg-primary-hover flex items-center gap-2 rounded-lg px-6 py-2.5 text-sm font-bold text-white shadow-md transition-all hover:shadow-lg active:scale-95 disabled:cursor-not-allowed disabled:bg-gray-300"
          >
            {isUploading ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />
                등록 중...
              </>
            ) : (
              '등록하기'
            )}
          </button>
        </div>
      </div>
    </div>
  )
}

export default function SourceListPage() {
  const router = useRouter()
  const ITEMS_PER_PAGE = 10

  // ###################################################
  // 상태 정의 (State)
  // ###################################################
  const [sourceList, setSourceList] = useState<Source[]>([])
  const [page, setPage] = useState(1)
  const [size, setSize] = useState(ITEMS_PER_PAGE)
  const [totalPages, setTotalPages] = useState(0)
  const [totalCounts, setTotalCounts] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [isModalOpen, setIsModalOpen] = useState(false)

  const loadData = async () => {
    setIsLoading(true)
    setError(null)
    try {
      await getSourcesApi(page, size).then((response) => {
        console.log(`📡 ${response.message}`)
        setPage(response.result.pageNo)
        setSize(response.result.pageSize)
        setTotalPages(response.result.totalPages)
        setTotalCounts(response.result.totalCount)
        setSourceList(response.result.content)
      })
    } catch (err) {
      console.error(err)
      setError('문서 목록을 불러올 수 없습니다.')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [page, size])

  useEffect(() => {
    loadData()
  }, [])

  // ###################################################
  // 핸들러 (Handler)
  // ###################################################
  const startIndex = (page - 1) * ITEMS_PER_PAGE
  const endIndex = startIndex + ITEMS_PER_PAGE

  const handleRowClick = (sourceId: number) => {
    router.push(`/source/${sourceId}`)
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

  const handleRefresh = () => {
    loadData()
  }

  const handleRegisterSuccess = () => {
    setPage((prev) => {
      if (prev == 1) {
        loadData()
      }
      return 1
    })
  }

  // ###################################################
  // 렌더링 (Render)
  // ###################################################
  return (
    <div className="flex w-full flex-col p-6">
      <div className="mb-4 flex items-center justify-between">
        <div className="flex shrink-0 items-center gap-2">
          <FolderOpen className="text-primary h-6 w-6" />
          <h2 className="text-2xl font-bold text-gray-800">RAG 문서 관리</h2>
        </div>
        <button
          onClick={() => setIsModalOpen(true)}
          className="bg-primary hover:bg-primary-hover flex items-center gap-2 rounded-lg px-4 py-2 text-sm font-bold text-white shadow-sm transition-all active:scale-95"
        >
          <span>+ 문서 등록</span>
        </button>
      </div>

      <div className="flex min-h-[400px] flex-col overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
        {isLoading && (
          <div className="flex flex-1 flex-col items-center justify-center gap-3">
            <Loader2 className="text-primary h-8 w-8 animate-spin" />
            <p className="text-sm font-medium text-gray-500">
              목록을 불러오는 중입니다...
            </p>
          </div>
        )}

        {!isLoading && error && (
          <div className="flex flex-1 flex-col items-center justify-center gap-3">
            <AlertCircle className="h-8 w-8 text-red-500" />
            <p className="text-sm font-bold text-gray-700">{error}</p>
            <button
              onClick={handleRefresh}
              className="flex items-center gap-2 rounded-md bg-gray-100 px-3 py-1.5 text-xs font-bold text-gray-600 hover:bg-gray-200"
            >
              <RefreshCw className="h-3 w-3" />
              다시 시도
            </button>
          </div>
        )}

        {!isLoading && !error && (
          <>
            <div className="overflow-auto">
              <table className="w-full min-w-full text-left text-sm text-gray-600">
                <thead className="bg-gray-50 text-xs font-bold text-gray-500 uppercase shadow-sm">
                  <tr>
                    <th className="w-[60px] px-4 py-4 text-center">ID</th>
                    <th className="px-4 py-4">문서명</th>
                    <th className="w-[160px] px-4 py-4 text-center">분류</th>
                    <th className="w-[160px] px-4 py-4 text-center">타입</th>
                    <th className="w-[160px] px-4 py-4 text-center">전처리</th>
                    <th className="w-[60px] px-4 py-4 text-center">버전</th>
                    <th className="w-[80px] px-4 py-4 text-center">자동화</th>
                    <th className="w-[160px] px-4 py-4 text-center">생성일</th>
                    <th className="w-[160px] px-4 py-4 text-center">수정일</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100 bg-white">
                  {sourceList.map((source) => (
                    <tr
                      key={source.sourceId}
                      onClick={() => handleRowClick(source.sourceId)}
                      className="group cursor-pointer transition-colors hover:bg-gray-50"
                    >
                      <td className="px-4 py-4 text-center font-mono text-gray-400 group-hover:text-gray-600">
                        {source.sourceId}
                      </td>
                      <td
                        className="group-hover:text-primary max-w-[200px] truncate px-4 py-4 font-bold text-gray-800 transition-colors"
                        title={source.name}
                      >
                        {source.name}
                      </td>
                      <td className="px-4 py-4 text-center">
                        <span className="inline-flex items-center rounded-md bg-gray-100 px-2 py-1 text-xs font-medium text-gray-600">
                          {source.categoryCode}
                        </span>
                      </td>
                      <td className="px-4 py-4 text-center">
                        <span className="font-mono text-xs font-bold text-gray-500">
                          {source.sourceType}
                        </span>
                      </td>
                      <td className="px-4 py-4 text-center">
                        <span className="text-xs text-gray-600">
                          {source.selectType}
                        </span>
                      </td>
                      <td className="px-4 py-4 text-center text-gray-500">
                        v{source.version}
                      </td>
                      <td className="px-4 py-4 text-center">
                        {source.isAuto ? (
                          <span className="inline-flex items-center rounded-full bg-blue-50 px-2 py-0.5 text-xs font-medium text-blue-700 ring-1 ring-blue-700/10 ring-inset">
                            자동화
                          </span>
                        ) : (
                          <span className="inline-flex items-center rounded-full bg-yellow-50 px-2 py-0.5 text-xs font-medium text-yellow-800 ring-1 ring-yellow-600/20 ring-inset">
                            수동
                          </span>
                        )}
                      </td>
                      <td className="px-4 py-4 text-center text-xs text-gray-500">
                        {source.sysCreateDt}
                      </td>
                      <td className="px-4 py-4 text-center text-xs text-gray-500">
                        {source.sysModifyDt}
                      </td>
                    </tr>
                  ))}
                  {sourceList.length === 0 && (
                    <tr>
                      <td
                        colSpan={9}
                        className="px-6 py-12 text-center text-gray-500"
                      >
                        등록된 문서가 없습니다.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
            <div className="mt-auto flex items-center justify-between border-t border-gray-100 bg-gray-50 px-6 py-3">
              <span className="text-xs text-gray-500">
                문서 목록 <span className="font-bold">{startIndex + 1}</span> ~{' '}
                <span className="font-bold">
                  {Math.min(endIndex, totalCounts)}
                </span>{' '}
                {'(전체 '}
                <span className="font-bold">{totalCounts}</span> {' 개의 문서)'}
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
          </>
        )}
      </div>

      <DocumentRegisterModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSuccess={handleRegisterSuccess}
      />
    </div>
  )
}
