import { client } from './client'
import { ApiResponse } from '@/types/api'
import { Category, Document } from '@/types/domain'
import { FetchEventSource, streamApi } from './stream'
import { StreamEvent } from '@/types/streamEvent'

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
  streamEvent: StreamEvent,
): Promise<FetchEventSource> => {
  return new Promise((resolve) => {
    const stream = streamApi(
      client.defaults.baseURL + `/chat/ai`,
      {
        query,
        sessionId,
        categoryCodes,
      },
      streamEvent,
    )
    resolve(stream)
  })
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
  streamEvent: StreamEvent,
): Promise<FetchEventSource> => {
  return new Promise((resolve) => {
    const stream = streamApi(
      client.defaults.baseURL + `/chat/llm`,
      {
        query,
        sessionId,
      },
      streamEvent,
    )
    resolve(stream)
  })
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
  promptContent: string,
  maxTokens: number,
  temperature: number,
  topP: number,
  streamEvent: StreamEvent,
): Promise<FetchEventSource> => {
  return new Promise((resolve) => {
    const stream = streamApi(
      client.defaults.baseURL + `/chat/simulation`,
      {
        query,
        sessionId,
        context,
        promptContent,
        maxTokens,
        temperature,
        topP,
      },
      streamEvent,
    )
    resolve(stream)
  })
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
  streamEvent: StreamEvent,
): Promise<FetchEventSource> => {
  return new Promise((resolve) => {
    const stream = streamApi(
      client.defaults.baseURL + `/chat/myai`,
      {
        query,
        sessionId,
        projectId,
      },
      streamEvent,
    )
    resolve(stream)
  })
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
