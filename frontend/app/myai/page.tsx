'use client'

import React, { useState, useEffect, useRef } from 'react'
import { useRouter } from 'next/navigation' // 라우터 추가
import {
  Plus,
  MoreVertical,
  Trash2,
  Upload,
  FileText,
  Database,
  Bot,
  Search,
  X,
  Loader2,
  Layers,
  CheckCircle2,
  Save,
} from 'lucide-react'
import { FileDetail, Project } from '@/types/domain'
import { createProjectApi, getProjectsApi } from '@/api/myai'
import { formatDateToYYYYMMDD } from '@/public/ts/commonUtil'

export default function MyAiPage() {
  const router = useRouter()

  // ###################################################
  // 상태 관리 (State)
  // ###################################################
  // 프로세스 상태
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // 프로젝트 리스트 상태
  const [projects, setProjects] = useState<Project[]>([])
  const [searchQuery, setSearchQuery] = useState('')
  const [page, setPage] = useState(1)
  const [size, setSize] = useState(10)
  const [activeMenuId, setActiveMenuId] = useState<number | null>(null)

  // 모달 제어 상태
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false)
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false)

  // [생성 모달] 상태
  const [newProjectName, setNewProjectName] = useState('')
  const [newProjectDesc, setNewProjectDesc] = useState('')
  const [newProjectFiles, setNewProjectFiles] = useState<File[]>([])
  const [isCreating, setIsCreating] = useState(false)

  // [재업로드/관리 모달] 상태
  const [existingFiles, setExistingFiles] = useState<FileDetail[]>([])
  const [additionalFiles, setAdditionalFiles] = useState<File[]>([])
  const [isSaving, setIsSaving] = useState(false)

  const menuRef = useRef<HTMLDivElement>(null)

  // ###################################################
  // 이펙트 (Effects)
  // ###################################################

  const loadData = async () => {
    setIsLoading(true)
    setError(null)

    await getProjectsApi(page, size, searchQuery)
      .then((response) => {
        console.log(`📡 ${response.message}`)
        setProjects((prev) => {
          const prevProjectIds = prev.map((project) => project.projectId)
          const projects = response.result.content.filter(
            (project) => !prevProjectIds.includes(project.projectId),
          )

          return [...projects, ...prev]
        })
      })
      .catch((error) => {
        console.error(error)
        setError('프로젝트 목록을 조회할 수 없습니다.')
      })
      .finally(() => {
        setIsLoading(false)
      })
  }

  useEffect(() => {
    loadData()
    function handleClickOutside(event: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setActiveMenuId(null)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  // ###################################################
  // 핸들러 (Handlers)
  // ###################################################
  // --- 리스트 관련 핸들러 ---
  const handleMenuToggle = (e: React.MouseEvent, projectId: number) => {
    e.stopPropagation()
    setActiveMenuId(activeMenuId === projectId ? null : projectId)
  }

  const handleDeleteProject = (e: React.MouseEvent, projectId: number) => {
    e.stopPropagation()
    if (confirm('정말로 이 프로젝트를 삭제하시겠습니까?')) {
      setProjects((prev) => prev.filter((p) => p.projectId !== projectId))
      setActiveMenuId(null)
    }
  }

  const handleOpenUploadModal = (e: React.MouseEvent) => {
    e.stopPropagation()
    // [Mock] 서버 데이터 로드
    setExistingFiles([
      // { id: 'f1', name: '기존_학습문서_v1.pdf', size: 1024 * 500 },
      // { id: 'f2', name: '업무_가이드라인.docx', size: 1024 * 1200 },
    ])
    setAdditionalFiles([])
    setIsUploadModalOpen(true)
    setActiveMenuId(null)
  }

  // --- 파일 관리 핸들러 ---
  const handleRemoveExistingFile = (fileDetailId: number) => {
    if (confirm('이 문서를 학습 데이터에서 제외하시겠습니까?')) {
      setExistingFiles((prev) =>
        prev.filter((f) => f.fileDetailId !== fileDetailId),
      )
    }
  }

  const handleSelectAdditionalFiles = (
    e: React.ChangeEvent<HTMLInputElement>,
  ) => {
    if (e.target.files) {
      const filesArray = Array.from(e.target.files)
      setAdditionalFiles((prev) => [...prev, ...filesArray])
    }
  }

  const handleRemoveAdditionalFile = (index: number) => {
    setAdditionalFiles((prev) => prev.filter((_, i) => i !== index))
  }

  const handleSaveDocuments = () => {
    setIsSaving(true)
    setTimeout(() => {
      alert(
        '문서 목록이 업데이트되었습니다.\n변경된 내용으로 재학습을 시작합니다.',
      )
      setIsSaving(false)
      setIsUploadModalOpen(false)
    }, 1500)
  }

  // --- 새 프로젝트 생성 핸들러 ---
  const handleCreateFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      const filesArray = Array.from(e.target.files)
      setNewProjectFiles((prev) => [...prev, ...filesArray])
    }
  }

  const handleCreateRemoveFile = (index: number) => {
    setNewProjectFiles((prev) => prev.filter((_, i) => i !== index))
  }

  const handleCreateProject = async () => {
    if (!newProjectName.trim()) return alert('프로젝트 이름을 입력해주세요.')
    if (!newProjectDesc.trim()) return alert('프로젝트 설명을 입력해주세요.')
    if (newProjectFiles.length === 0)
      return alert('최소 1개 이상의 학습 문서를 업로드해주세요.')

    setIsCreating(true)

    await createProjectApi(newProjectName, newProjectDesc, newProjectFiles)
      .then((response) => {
        loadData()
        setNewProjectName('')
        setNewProjectDesc('')
        setNewProjectFiles([])
        setIsCreateModalOpen(false)
        alert('프로젝트가 생성되었습니다.')
      })
      .catch((error) => {
        console.error(error)
        setError('프로젝트 생성에 실패했습니다.')
      })
      .finally(() => {
        setIsCreating(false)
      })
  }

  // ###################################################
  // 렌더링 (Render)
  // ###################################################
  return (
    <div className="flex h-screen w-full flex-col bg-gray-50/50">
      {/* -----------------------------------------------------------------
          PROJECT LIST VIEW
      ------------------------------------------------------------------ */}
      <div className="flex h-full flex-col overflow-y-auto p-6">
        <div className="mb-8 flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
          <div>
            <h1 className="flex items-center gap-2 text-2xl font-bold text-gray-900">
              <Bot className="text-primary h-7 w-7" />
              나만의 AI 프로젝트
            </h1>
            <p className="mt-1 text-sm text-gray-500">
              학습된 문서를 기반으로 대화하는 봇을 관리합니다.
            </p>
          </div>

          <div className="flex items-center gap-3">
            <div className="relative hidden md:block">
              <Search className="absolute top-1/2 left-3 h-4 w-4 -translate-y-1/2 text-gray-400" />
              <input
                type="text"
                placeholder="프로젝트 검색..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="focus:border-primary focus:ring-primary w-64 rounded-lg border border-gray-200 bg-white py-2 pr-4 pl-9 text-sm outline-none focus:ring-1"
              />
            </div>
            <button
              onClick={() => setIsCreateModalOpen(true)}
              className="bg-primary hover:bg-primary-hover flex items-center gap-2 rounded-lg px-4 py-2 text-sm font-bold text-white shadow-md transition-all active:scale-95"
            >
              <Plus className="h-4 w-4" />새 프로젝트
            </button>
          </div>
        </div>

        {projects.length > 0 ? (
          <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {projects.map((project) => (
              <div
                key={project.projectId}
                onClick={() => {
                  router.push(`/myai/chat?projectId=${project.projectId}`)
                }}
                className={`group 'opacity-70' relative flex cursor-pointer flex-col justify-between rounded-2xl border border-gray-200 bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md`}
              >
                <div className="mb-4 flex items-start justify-between">
                  <div
                    className={`text-blue-600'} flex h-12 w-12 items-center justify-center rounded-xl bg-blue-50`}
                  >
                    <Bot className="h-6 w-6" />
                  </div>

                  <div className="relative">
                    <button
                      onClick={(e) => handleMenuToggle(e, project.projectId)}
                      className="flex h-8 w-8 items-center justify-center rounded-full text-gray-400 hover:bg-gray-100 hover:text-gray-600"
                    >
                      <MoreVertical className="h-4 w-4" />
                    </button>

                    {activeMenuId === project.projectId && (
                      <div
                        ref={menuRef}
                        className="animate-in fade-in zoom-in-95 absolute top-9 right-0 z-10 w-48 rounded-lg border border-gray-100 bg-white shadow-xl"
                        onClick={(e) => e.stopPropagation()}
                      >
                        <div className="p-1">
                          <button
                            onClick={(e) => handleOpenUploadModal(e)}
                            className="flex w-full items-center gap-2 rounded-md px-3 py-2 text-sm text-gray-700 hover:bg-gray-50"
                          >
                            <Upload className="h-4 w-4" />
                            학습 문서 관리
                          </button>
                          <button
                            onClick={(e) =>
                              handleDeleteProject(e, project.projectId)
                            }
                            className="flex w-full items-center gap-2 rounded-md px-3 py-2 text-sm text-red-600 hover:bg-red-50"
                          >
                            <Trash2 className="h-4 w-4" />
                            프로젝트 삭제
                          </button>
                        </div>
                      </div>
                    )}
                  </div>
                </div>

                <div className="mb-6">
                  <h3 className="mb-1 line-clamp-1 text-lg font-bold text-gray-900">
                    {project.projectName}
                  </h3>
                  <p className="line-clamp-2 min-h-[40px] text-sm text-gray-500">
                    {project.projectDesc}
                  </p>
                </div>

                <div className="mt-auto flex items-center justify-between border-t border-gray-50 pt-4">
                  <div className="flex items-center gap-4">
                    <div className="flex items-center gap-1.5">
                      <Database className="h-3.5 w-3.5 text-gray-400" />
                      <span className="text-xs font-bold text-gray-600">
                        {project.chunkCount.toLocaleString()}
                      </span>
                    </div>
                    <div className="flex items-center gap-1.5">
                      <FileText className="h-3.5 w-3.5 text-gray-400" />
                      <span className="text-xs font-bold text-gray-600">
                        {project.sourceCount}
                      </span>
                    </div>
                  </div>
                  <span className="text-[10px] text-gray-400">
                    {formatDateToYYYYMMDD(project.sysCreateDt)}
                  </span>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="flex h-64 w-full flex-col items-center justify-center rounded-2xl border-2 border-dashed border-gray-200 bg-gray-50/50">
            <Layers className="mb-3 h-10 w-10 text-gray-300" />
            <p className="font-medium text-gray-500">
              검색된 프로젝트가 없습니다.
            </p>
          </div>
        )}
      </div>

      {/* -----------------------------------------------------------------
          [MODAL 1] 새 프로젝트 생성
      ------------------------------------------------------------------ */}
      {isCreateModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm">
          <div className="animate-in fade-in zoom-in-95 w-full max-w-lg rounded-2xl bg-white p-6 shadow-2xl duration-300">
            <div className="mb-6 flex items-center justify-between border-b border-gray-100 pb-4">
              <h3 className="text-lg font-bold text-gray-900">
                새 프로젝트 생성
              </h3>
              <button
                onClick={() => setIsCreateModalOpen(false)}
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
                  value={newProjectName}
                  onChange={(e) => setNewProjectName(e.target.value)}
                  placeholder="예: 신입사원 온보딩 봇"
                  className="focus:border-primary focus:ring-primary w-full rounded-lg border border-gray-300 px-3 py-2.5 text-sm outline-none focus:ring-1"
                />
              </div>

              <div>
                <label className="mb-1.5 block text-xs font-bold text-gray-600">
                  프로젝트 설명 <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={newProjectDesc}
                  onChange={(e) => setNewProjectDesc(e.target.value)}
                  placeholder="예: 신입사원 온보딩을 위한 커스텀 봇입니다."
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
                    <p className="text-[10px] text-gray-400">PDF, WORD, HWP</p>
                  </div>
                  <input
                    type="file"
                    className="hidden"
                    multiple
                    onChange={handleCreateFileSelect}
                  />
                </label>
                {newProjectFiles.length > 0 && (
                  <div className="mt-3 flex max-h-32 flex-col gap-2 overflow-y-auto pr-1">
                    {newProjectFiles.map((file, index) => (
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
                          onClick={() => handleCreateRemoveFile(index)}
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
                  onClick={() => setIsCreateModalOpen(false)}
                  className="flex-1 rounded-lg border border-gray-300 bg-white py-2.5 text-sm font-bold text-gray-600 hover:bg-gray-50"
                >
                  취소
                </button>
                <button
                  onClick={handleCreateProject}
                  disabled={isCreating}
                  className="bg-primary hover:bg-primary-hover flex flex-1 items-center justify-center gap-2 rounded-lg py-2.5 text-sm font-bold text-white shadow-md transition-all active:scale-95 disabled:bg-gray-300"
                >
                  {isCreating ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    '프로젝트 생성'
                  )}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* -----------------------------------------------------------------
          [MODAL 2] 문서 재업로드/관리
      ------------------------------------------------------------------ */}
      {isUploadModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm">
          <div className="animate-in fade-in zoom-in-95 w-full max-w-lg rounded-2xl bg-white p-6 shadow-2xl duration-300">
            {/* 헤더 */}
            <div className="mb-6 flex items-center justify-between border-b border-gray-100 pb-4">
              <div>
                <h3 className="text-lg font-bold text-gray-900">
                  학습 문서 관리
                </h3>
                <p className="text-xs text-gray-500">
                  문서를 추가하거나 제외할 수 있습니다.
                </p>
              </div>
              <button
                onClick={() => setIsUploadModalOpen(false)}
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
                    {existingFiles.length}개 파일
                  </span>
                </div>

                <div className="flex max-h-40 flex-col gap-2 overflow-y-auto rounded-xl border border-gray-200 bg-gray-50 p-2">
                  {existingFiles.length > 0 ? (
                    existingFiles.map((file) => (
                      <div
                        key={file.fileDetailId}
                        className="flex items-center justify-between rounded-lg bg-white px-3 py-2 text-sm shadow-sm"
                      >
                        <div className="flex items-center gap-2 overflow-hidden">
                          <CheckCircle2 className="h-4 w-4 shrink-0 text-green-500" />
                          <span className="truncate text-gray-700">
                            {file.fileOriginName}
                          </span>
                          <span className="shrink-0 text-[10px] text-gray-400">
                            ({(file.fileSize / 1024).toFixed(1)} KB)
                          </span>
                        </div>
                        <button
                          onClick={() =>
                            handleRemoveExistingFile(file.fileDetailId)
                          }
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
                <label className="group hover:border-primary flex h-24 w-full cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed border-gray-300 bg-white transition-colors hover:bg-blue-50">
                  <div className="group-hover:text-primary flex items-center justify-center gap-2 text-gray-400">
                    <Upload className="h-5 w-5" />
                    <span className="text-sm font-bold">파일 추가하기</span>
                  </div>
                  <input
                    type="file"
                    className="hidden"
                    multiple
                    onChange={handleSelectAdditionalFiles}
                  />
                </label>

                {/* 추가된 파일 목록 */}
                {additionalFiles.length > 0 && (
                  <div className="mt-3 flex max-h-32 flex-col gap-2 overflow-y-auto pr-1">
                    {additionalFiles.map((file, index) => (
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
                          onClick={() => handleRemoveAdditionalFile(index)}
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
                  onClick={() => setIsUploadModalOpen(false)}
                  className="flex-1 rounded-lg border border-gray-300 bg-white py-2.5 text-sm font-bold text-gray-600 hover:bg-gray-50"
                >
                  취소
                </button>
                <button
                  onClick={handleSaveDocuments}
                  disabled={isSaving}
                  className="bg-primary hover:bg-primary-hover flex flex-1 items-center justify-center gap-2 rounded-lg py-2.5 text-sm font-bold text-white shadow-md transition-all active:scale-95 disabled:bg-gray-300"
                >
                  {isSaving ? (
                    <>
                      <Loader2 className="h-4 w-4 animate-spin" />
                      저장 중...
                    </>
                  ) : (
                    <>
                      <Save className="h-4 w-4" />
                      변경사항 저장
                    </>
                  )}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
