import { Project, PromptParameter } from '@/types/domain'
import { client } from './client'
import { ApiResponse, PageResponse } from '@/types/api'
import { FetchEventSource, streamApi } from './stream'
import { StreamEvent } from '@/types/streamEvent'

export interface GetPromptRoleResponseDto extends PromptParameter {}

export interface GetPromptToneResponseDto extends PromptParameter {}

export interface GetPromptStyleResponseDto extends PromptParameter {}

export interface GetProjectResponseDto extends Project {}

export interface GetProjectsResponseDto extends PageResponse {
  content: Project[]
}

export interface GetProjectSources {
  fileDetailId: number
  fileOriginName: string
  ext: string
  fileSize: number
}

/**
 * 프롬프트 역할 목록 조회
 */
export const getPromptRolesApi = async (): Promise<
  ApiResponse<GetPromptRoleResponseDto[]>
> => {
  const response = await client.get(`/myai/role`)
  return response.data
}

/**
 * 프롬프트 톤 목록 조회
 */
export const getPromptTonesApi = async (): Promise<
  ApiResponse<GetPromptToneResponseDto[]>
> => {
  const response = await client.get(`/myai/tone`)
  return response.data
}
/**
 * 프롬프트 스타일 목록 조회
 */
export const getPromptStylesApi = async (): Promise<
  ApiResponse<GetPromptStyleResponseDto[]>
> => {
  const response = await client.get(`/myai/style`)
  return response.data
}

/**
 * 프로젝트 조회 API
 *
 * @param projectId 프로젝트 ID
 */
export const getProjectApi = async (
  projectId: number,
): Promise<ApiResponse<GetProjectResponseDto>> => {
  const response = await client.get(`/myai/${projectId}`)
  return response.data
}

/**
 * 프로젝트 목록 조회 API
 *
 * @param page 페이지
 * @param size 사이즈
 * @param keyword 키워드
 */
export const getProjectsApi = async (
  page: number,
  size: number,
  keyword?: string,
): Promise<ApiResponse<GetProjectsResponseDto>> => {
  let params = `page=${page}&size=${size}`

  if (keyword && keyword.trim() !== '') {
    params += `&keyword=${keyword}`
  }

  const response = await client.get(`/myai?${params}`)
  return response.data
}

/**
 * 프로젝트 생성 API
 *
 * @param projectName 프로젝트명
 * @param projectDesc 프로젝트 설명
 * @param roleCode 역할 코드
 * @param toneCode 톤 코드
 * @param styleCode 스타일 코드
 * @param uploadFiles 임베딩 문서 목록
 */
export const createProjectApi = async (
  projectName: string,
  projectDesc: string,
  roleCode: string,
  toneCode: string,
  styleCode: string,
  uploadFiles: File[],
  streamEvent: StreamEvent,
): Promise<FetchEventSource> => {
  const formData = new FormData()
  uploadFiles.forEach((uploadFile) =>
    formData.append('uploadFiles', uploadFile),
  )
  formData.append(
    'requestDto',
    new Blob(
      [
        JSON.stringify({
          projectName,
          roleCode,
          toneCode,
          styleCode,
          projectDesc,
        }),
      ],
      {
        type: 'application/json',
      },
    ),
  )

  return new Promise((resolve) => {
    const stream = streamApi(
      client.defaults.baseURL + `/myai`,
      formData,
      streamEvent,
    )
    resolve(stream)
  })
}

/**
 * 프로젝트 삭제 API
 *
 * @param projectId 프로젝트 ID
 */
export const deleteProjectApi = async (
  projectId: number,
): Promise<ApiResponse<void>> => {
  const response = await client.delete<ApiResponse<void>>(`/myai/${projectId}`)

  return response.data
}

/**
 * 프로젝트 임베딩 문서 목록 조회 API
 *
 * @param projectId 프로젝트 ID
 */
export const getProjectSourcesApi = async (
  projectId: number,
): Promise<ApiResponse<GetProjectSources[]>> => {
  const response = await client.get<ApiResponse<GetProjectSources[]>>(
    `/myai/${projectId}/source`,
  )

  return response.data
}

/**
 * 프로젝트 임베딩 문서 목록 수정 API
 *
 * @param projectId 프로젝트 ID
 * @param deleteFileDetailIds 삭제 대상 파일 상세 ID 목록
 * @param uploadFiles 임베딩 문서 목록
 */
export const updateProjectSourcesApi = async (
  projectId: number,
  deleteFileDetailIds: number[],
  uploadFiles: File[],
  streamEvent: StreamEvent,
): Promise<FetchEventSource> => {
  const formData = new FormData()
  uploadFiles.forEach((uploadFile) => {
    formData.append('uploadFiles', uploadFile)
  })
  formData.append(
    'requestDto',
    new Blob([JSON.stringify({ deleteFileDetailIds })], {
      type: 'application/json',
    }),
  )

  return new Promise((resolve) => {
    const stream = streamApi(
      client.defaults.baseURL + `/myai/${projectId}/source`,
      formData,
      streamEvent,
    )
    resolve(stream)
  })
}
