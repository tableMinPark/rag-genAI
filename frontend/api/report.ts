import { client } from './client'
import { ApiResponse } from '@/types/api'
import { FetchEventSource, streamApi } from './stream'
import { StreamEvent } from '@/types/streamEvent'

export interface GenerateReportResponse {
  sessionId: string
  msgId: string
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
  requestContent: string,
  title: string,
  context: string,
  streamEvent: StreamEvent,
): Promise<FetchEventSource> => {
  return new Promise((resolve) => {
    const stream = streamApi(
      client.defaults.baseURL + `/report/text`,
      {
        sessionId,
        requestContent,
        title,
        context,
      },
      streamEvent,
    )
    resolve(stream)
  })
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
  requestContent: string,
  title: string,
  uploadFile: File[],
  streamEvent: StreamEvent,
): Promise<FetchEventSource> => {
  const formData = new FormData()
  uploadFile.forEach((file) => formData.append('uploadFile', file))
  formData.append(
    'requestDto',
    new Blob([JSON.stringify({ sessionId, requestContent, title })], {
      type: 'application/json',
    }),
  )

  return new Promise((resolve) => {
    const stream = streamApi(
      client.defaults.baseURL + `/report/file`,
      formData,
      streamEvent,
    )
    resolve(stream)
  })
}
