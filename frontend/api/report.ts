import { client } from './client'
import { ApiResponse } from '@/types/api'

export interface GenerateReportResponse {
  sessionId: string
  msgId: string
  content: string
}

/**
 * 보고서 생성 API (참조 문서 텍스트)
 *
 * @param sessionId 세션 ID
 * @param prompt 프롬프트
 * @param title 보고서 제목
 * @param context 참고문서
 */
export const generateReportTextApi = async (
  sessionId: string,
  prompt: string,
  title: string,
  context: string,
): Promise<ApiResponse<GenerateReportResponse>> => {
  const response = await client.post<ApiResponse<GenerateReportResponse>>(
    `/report/text`,
    {
      sessionId,
      prompt,
      title,
      context,
    },
  )

  return response.data
}

/**
 * 보고서 생성 API (참조 문서 파일)
 *
 * @param sessionId 세션 ID
 * @param prompt 프롬프트
 * @param title 보고서 제목
 * @param uploadFile 참고문서
 */
export const generateReportFileApi = async (
  sessionId: string,
  prompt: string,
  title: string,
  uploadFile: File,
): Promise<ApiResponse<GenerateReportResponse>> => {
  const formData = new FormData()
  formData.append('uploadFile', uploadFile)
  formData.append(
    'requestDto',
    new Blob([JSON.stringify({ sessionId, prompt, title })], {
      type: 'application/json',
    }),
  )

  const response = await client.post<ApiResponse<GenerateReportResponse>>(
    `/report/file`,
    formData,
  )

  return response.data
}
