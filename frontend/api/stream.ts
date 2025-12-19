import { client } from './client'
import { ApiResponse } from '@/types/api'
import { config } from '@/public/ts/config'

/**
 * 스트림 요청 API
 *
 * @param sessionId 세션 ID
 */
export const streamApi = (sessionId: string): EventSource => {
  return new EventSource(
    `${config.streamUrl}/api/rag-genai/stream/${sessionId}`,
  )
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
    `/stream/${sessionId}`,
  )

  return response.data
}
