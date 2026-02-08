import { ApiResponse } from '@/types/api'
import { client } from './client'
import { PromptParameter } from '@/types/domain'

export interface GetPromptRoleResponseDto extends PromptParameter {}

export interface GetPromptToneResponseDto extends PromptParameter {}

export interface GetPromptStyleResponseDto extends PromptParameter {}

/**
 * 프롬프트 역할 목록 조회
 */
export const getPromptRolesApi = async (): Promise<
  ApiResponse<GetPromptRoleResponseDto[]>
> => {
  const response = await client.get(`/prompt/role`)
  return response.data
}

/**
 * 프롬프트 톤 목록 조회
 */
export const getPromptTonesApi = async (): Promise<
  ApiResponse<GetPromptToneResponseDto[]>
> => {
  const response = await client.get(`/prompt/tone`)
  return response.data
}
/**
 * 프롬프트 스타일 목록 조회
 */
export const getPromptStylesApi = async (): Promise<
  ApiResponse<GetPromptStyleResponseDto[]>
> => {
  const response = await client.get(`/prompt/style`)
  return response.data
}
