import { client } from './client'
import { ApiResponse } from '@/types/api'
import { FetchEventSource, streamApi } from './stream'
import { StreamEvent } from '@/types/streamEvent'

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
  streamEvent: StreamEvent,
): Promise<FetchEventSource> => {
  return new Promise((resolve) => {
    const stream = streamApi(
      client.defaults.baseURL + `/summary/text`,
      {
        sessionId,
        lengthRatio,
        context,
      },
      streamEvent,
    )
    resolve(stream)
  })
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
  streamEvent: StreamEvent,
): Promise<FetchEventSource> => {
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

  return new Promise((resolve) => {
    const stream = streamApi(
      client.defaults.baseURL + `/summary/file`,
      formData,
      streamEvent,
    )
    resolve(stream)
  })
}
