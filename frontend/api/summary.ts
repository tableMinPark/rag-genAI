import { client } from './client'
import { ApiResponse } from '@/types/api'

export interface SummaryResponse {
  sessionId: string
  msgId: string
  content: string
}

/**
 * 요약 API (텍스트 요약)
 *
 * @param sessionId 세션 ID
 * @param lengthRatio 요약 비율 값
 * @param context 참고문서
 */
export const summaryTextApi = async (
  sessionId: string,
  lengthRatio: number,
  context: string,
): Promise<ApiResponse<SummaryResponse>> => {
  const response = await client.post<ApiResponse<SummaryResponse>>(
    `/summary/text`,
    {
      sessionId,
      lengthRatio,
      context,
    },
  )

  return response.data
}

/**
 * 요약 API (파일 요약)
 *
 * @param sessionId 세션 ID
 * @param lengthRatio 요약 비율 값
 * @param uploadFile 참고문서
 */
export const summaryFileApi = async (
  sessionId: string,
  lengthRatio: number,
  uploadFile: File,
): Promise<ApiResponse<SummaryResponse>> => {
  const formData = new FormData()
  formData.append('uploadFile', uploadFile)
  formData.append(
    'requestDto',
    new Blob(
      [
        JSON.stringify({
          sessionId,
          lengthRatio,
        }),
      ],
      {
        type: 'application/json',
      },
    ),
  )

  const response = await client.post<ApiResponse<SummaryResponse>>(
    `/summary/file`,
    formData,
  )

  return response.data
}
