'use client'

import { KeyboardEvent } from 'react'
import React, { useState, useEffect, useRef } from 'react'
import { useRouter } from 'next/navigation'
import {
  Plus,
  MoreVertical,
  Trash2,
  Upload,
  FileText,
  Database,
  Bot,
  Search,
  Layers,
} from 'lucide-react'
import { Project } from '@/types/domain'
import { deleteProjectApi, getProjectApi, getProjectsApi } from '@/api/myai'
import { formatDateToYYYYMMDD } from '@/public/ts/commonUtil'
import { getMenuInfo } from '@/public/const/menu'
import { useUiStore } from '@/stores/uiStore'
import { useModalStore } from '@/stores/modalStore'
import ModalMyAiCreate from '@/components/modal/ModalMyAiCreate'
import ModalMyAiModify from '@/components/modal/ModalMyAiModify'

export default function MyAiPage() {
  const menuInfo = getMenuInfo('myai')
  const router = useRouter()
  const uiStore = useUiStore()
  const modalStore = useModalStore()

  // ###################################################
  // 상태 관리
  // ###################################################
  // 프로젝트 목록
  const [projects, setProjects] = useState<Project[]>([])
  // 프로젝트 검색 키워드
  const [keyword, setKeyword] = useState('')
  // 페이지
  const [page, setPage] = useState(1)
  // 사이즈
  const [size, setSize] = useState(10)
  // 프로젝트 문서 업데이트 모달 상태
  const [modifyModalIsOpen, setModifyModalIsOpen] = useState(false)
  // 프로젝트 생성 모달 상태
  const [createModalIsOpen, setCreateModalIsOpen] = useState(false)
  // 선택한 프로젝트 상태
  const [selectedProject, setSelectedProject] = useState<Project | null>(null)
  const bottomRef = useRef<HTMLDivElement>(null)
  const menuRef = useRef<HTMLDivElement>(null)

  // ###################################################
  // 랜더링 이펙트
  // ###################################################
  /**
   * 스크롤 하단 감지 이펙트
   */
  useEffect(() => {
    if (!bottomRef.current) return
    return observeScrollBottom(
      bottomRef.current,
      () => {
        setPage((prev) => prev + 1)
        handleGetProjects()
      },
      { rootMargin: '100px' },
    )
  }, [])

  // ###################################################
  // 핸들러
  // ###################################################
  /**
   * 프로젝트 목록 조회 핸들러
   */
  const handleGetProjects = async () => {
    uiStore.setLoading('프로젝트 목록을 불러오는 중입니다')
    await getProjectsApi(page, size, keyword)
      .then((response) => {
        console.log(`📡 ${response.message}`)
        setProjects((prev) => {
          const prevProjectIds = prev.map((project) => project.projectId)
          const projects = response.result.content.filter(
            (project) => !prevProjectIds.includes(project.projectId),
          )
          return [...projects, ...prev]
        })
        setPage(response.result.pageNo)
        setSize(response.result.pageSize)
        uiStore.reset()
      })
      .catch((reason) => {
        console.error(reason)
        uiStore.setError('프로젝트를 조회할 수 없습니다.', handleGetProjects)
      })
  }

  /**
   * 프로젝트 단건 조회 핸들러
   */
  const handleGetProject = async (projectId: number) => {
    uiStore.setLoading('프로젝트를 불러오는 중입니다')
    await getProjectApi(projectId)
      .then((response) => {
        console.log(`📡 ${response.message}`)
        setProjects((prev) => {
          const projects = prev.map((project) =>
            project.projectId === response.result.projectId
              ? {
                  projectId: response.result.projectId,
                  projectName: response.result.projectName,
                  projectDesc: response.result.projectDesc,
                  sysCreateDt: response.result.sysCreateDt,
                  sysModifyDt: response.result.sysModifyDt,
                  sourceCount: response.result.sourceCount,
                  chunkCount: response.result.chunkCount,
                }
              : project,
          )
          return projects
        })
        uiStore.reset()
      })
      .catch((reason) => {
        console.error(reason)
        uiStore.setError('프로젝트를 조회할 수 없습니다.', () =>
          handleGetProject(projectId),
        )
      })
  }

  /**
   * 프로젝트 설정 메뉴 토글 핸들러
   * @param e 마우스 이벤트
   * @param project 선택 프로젝트
   */
  const handleToggleProject = (e: React.MouseEvent, project: Project) => {
    e.stopPropagation()
    setSelectedProject(selectedProject === project ? null : project)
  }

  /**
   * 프로젝트 삭제 이벤트 핸들러
   * @param e 마우스 이벤트
   * @param project 프로젝트
   */
  const handleDeleteProjectEvent = (e: React.MouseEvent, project: Project) => {
    e.stopPropagation()
    modalStore.setConfirm(
      '프로젝트 삭제',
      '정말로 이 프로젝트를 삭제하시겠습니까?',
      '삭제한 프로젝트는 복구할 수 없습니다.',
      async () => {
        setSelectedProject(null)
        handleDeleteProject(project)
      },
    )
  }

  /**
   * 프로젝트 삭제 핸들러
   * @param project 프로젝트
   */
  const handleDeleteProject = async (project: Project) => {
    uiStore.setLoading('프로젝트 삭제중 입니다.')
    await deleteProjectApi(project.projectId)
      .then((response) => {
        console.log(`📡 ${response.message}`)
        setProjects((prev) =>
          prev.filter((p) => p.projectId !== project.projectId),
        )
        uiStore.reset()
      })
      .catch((reason) => {
        console.error(reason)
        uiStore.setError('프로젝트를 삭제할 수 없습니다.', () =>
          handleDeleteProject(project),
        )
      })
  }

  /**
   * 프로젝트 수정 모달 오픈 이벤트 핸들러
   * @param e 마우스 이벤트
   */
  const handleOpenModifyModal = (e: React.MouseEvent) => {
    e.stopPropagation()
    setModifyModalIsOpen(true)
  }

  /**
   * 히딘 감지 핸들러
   * @param target 하단 감지 Element
   * @param onIntersect 핸들러
   * @param options 옵션
   */
  const observeScrollBottom = (
    target: Element,
    onIntersect: () => void,
    options?: IntersectionObserverInit,
  ) => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          onIntersect()
        }
      },
      {
        root: null,
        rootMargin: '0px',
        threshold: 0,
        ...options,
      },
    )
    observer.observe(target)
    return () => observer.disconnect()
  }

  /**
   * 키 다운 이벤트 핸들러
   * @param e 키보드 이벤트
   */
  const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && !e.nativeEvent.isComposing) {
      setProjects([])
      setPage(1)
      setSize(10)
      handleGetProjects()
    }
  }

  // ###################################################
  // 렌더링 (Render)
  // ###################################################
  return (
    <div className="flex h-full w-full flex-col p-6">
      {/* 헤더 영역 */}
      <div className="mb-6 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div>
            <h2 className="flex items-center gap-2 text-2xl font-bold text-gray-800">
              <menuInfo.icon className="text-primary h-6 w-6" />
              {menuInfo.name}
            </h2>
            <p className="mt-1 text-xs text-gray-500">{menuInfo.description}</p>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <div className="relative hidden md:block">
            <Search className="absolute top-1/2 left-3 h-4 w-4 -translate-y-1/2 text-gray-400" />
            <input
              type="text"
              placeholder="프로젝트 검색..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              onKeyDown={handleKeyDown}
              className="focus:border-primary focus:ring-primary w-64 rounded-lg border border-gray-200 bg-white py-2 pr-4 pl-9 text-sm outline-none focus:ring-1"
            />
          </div>
          <button
            onClick={() => setCreateModalIsOpen(true)}
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
                    onClick={(e) => handleToggleProject(e, project)}
                    className="flex h-8 w-8 items-center justify-center rounded-full text-gray-400 hover:bg-gray-100 hover:text-gray-600"
                  >
                    <MoreVertical className="h-4 w-4" />
                  </button>

                  {selectedProject === project && (
                    <div
                      ref={menuRef}
                      className="animate-in fade-in zoom-in-95 absolute top-9 right-0 z-10 w-48 rounded-lg border border-gray-100 bg-white shadow-xl"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <div className="p-1">
                        <button
                          onClick={(e) => handleOpenModifyModal(e)}
                          className="flex w-full items-center gap-2 rounded-md px-3 py-2 text-sm text-gray-700 hover:bg-gray-50"
                        >
                          <Upload className="h-4 w-4" />
                          학습 문서 관리
                        </button>
                        <button
                          onClick={(e) => handleDeleteProjectEvent(e, project)}
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
                <p className="line-clamp-2 min-h-10 text-sm text-gray-500">
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
      <div ref={bottomRef} />
      {createModalIsOpen && (
        <ModalMyAiCreate
          onCreate={() => {
            setCreateModalIsOpen(false)
            handleGetProjects()
          }}
          onClose={() => setCreateModalIsOpen(false)}
        />
      )}
      {modifyModalIsOpen && selectedProject && (
        <ModalMyAiModify
          project={selectedProject}
          onModify={() => {
            setModifyModalIsOpen(false)
            handleGetProject(selectedProject.projectId)
            setSelectedProject(null)
          }}
          onClose={() => {
            setModifyModalIsOpen(false)
            setSelectedProject(null)
          }}
        />
      )}
    </div>
  )
}
