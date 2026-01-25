import { client } from './client'
import { ApiResponse } from '@/types/api'
import { Category, Document } from '@/types/domain'

export interface ChatAiResponse {
  query: string
  sessionId: string
}

export interface ChatLlmResponse {
  query: string
  sessionId: string
}

export interface ChatMyAiResponse {
  query: string
  sessionId: string
  documents?: Document[]
}

export interface ChatSimulationResponse {
  query: string
  sessionId: string
}

export interface GetCategoriesResponse extends Category {}

/**
 * AI 답변 요청 API
 *
 * @param query 질의문
 * @param sessionId 세션 ID
 * @param categoryCodes 카테고리 코드 목록
 */
export const chatAiApi = async (
  query: string,
  sessionId: string,
  categoryCodes: string[],
): Promise<ApiResponse<ChatAiResponse>> => {
  const response = await client.post<ApiResponse<ChatAiResponse>>(`/chat/ai`, {
    query,
    sessionId,
    categoryCodes,
  })

  return response.data
}

/**
 * LLM 답변 요청 API
 *
 * @param query 질의문
 * @param sessionId 세션 ID
 */
export const chatLlmApi = async (
  query: string,
  sessionId: string,
): Promise<ApiResponse<ChatLlmResponse>> => {
  const response = await client.post<ApiResponse<ChatLlmResponse>>(
    `/chat/llm`,
    {
      query,
      sessionId,
    },
  )

  return response.data
}

/**
 * 시뮬레이션 답변 요청 API
 *
 * @param query 질의문
 * @param sessionId 세션 ID
 * @param context 컨텍스트
 * @param prompt 시스템 프롬프트
 * @param maxTokens 제약 토큰 수
 * @param temperature 창의성
 * @param topP 일관성
 */
export const chatSimulateionApi = async (
  query: string,
  sessionId: string,
  context: string,
  prompt: string,
  maxTokens: number,
  temperature: number,
  topP: number,
): Promise<ApiResponse<ChatSimulationResponse>> => {
  const response = await client.post<ApiResponse<ChatSimulationResponse>>(
    `/chat/simulation`,
    {
      query,
      sessionId,
      context,
      prompt,
      maxTokens,
      temperature,
      topP,
    },
  )

  return response.data
}

/**
 * 나만의 AI 답변 요청 API
 *
 * @param query 질의문
 * @param sessionId 세션 ID
 * @param projectId 프로젝트 ID
 */
export const chatMyAiApi = async (
  query: string,
  sessionId: string,
  projectId: number,
): Promise<ApiResponse<ChatMyAiResponse>> => {
  const response = await client.post<ApiResponse<ChatMyAiResponse>>(
    `/chat/myai`,
    {
      query,
      sessionId,
      projectId,
    },
  )

  return response.data
}

/**
 * 카테고리 목록 조회 API
 */
export const getCategoriesApi = async (): Promise<
  ApiResponse<GetCategoriesResponse[]>
> => {
  const response =
    await client.get<ApiResponse<GetCategoriesResponse[]>>(`/chat/category`)

  return response.data
}
