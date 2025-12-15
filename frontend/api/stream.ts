import { client } from './client'
import { ApiResponse } from '@/types/api'

/**
 * 스트림 중지 요청 API
 *
 * @param sessionId 세션 ID
 */
export const streamApi = (sessionId: string): EventSource => {
  return new EventSource(`/api/rag-gen/stream/${sessionId}`)
}

/**
 * 스트림 중지 요청 API
 *
 * @param sessionId 세션 ID
 */
export const cancelStreamApi = async (
  sessionId: string,
): Promise<ApiResponse<void>> => {
  const response = await client.delete<ApiResponse<void>>(
    `/api/rag-gen/stream/${sessionId}`,
  )

  return response.data
}
