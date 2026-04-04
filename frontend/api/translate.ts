import { client } from './client'
import { ApiResponse } from '@/types/api'
import { TranslateLanguage } from '@/types/domain'
import { FetchEventSource, streamApi } from './stream'
import { StreamEvent } from '@/types/streamEvent'

export interface GetTranslateLanguageResponse extends TranslateLanguage {}

export interface TranslateResponse {
  sessionId: string
  msgId: string
  content: string
}

/**
 * 번역 API (텍스트 번역)
 *
 * @param sessionId 세션 ID
 * @param beforeLang 원본 언어
 * @param afterLang 번역 언어
 * @param containDic 사전 적용 여부
 * @param context 참고문서
 */
export const translateTextApi = async (
  sessionId: string,
  afterLang: string,
  containDic: boolean,
  context: string,
  streamEvent: StreamEvent,
): Promise<FetchEventSource> => {
  return new Promise((resolve) => {
    const stream = streamApi(
      client.defaults.baseURL + `/translate/text`,
      {
        sessionId,
        afterLang,
        containDic,
        context,
      },
      streamEvent,
    )
    resolve(stream)
  })
}

/**
 * 번역 API (문서 번역)
 *
 * @param sessionId 세션 ID
 * @param beforeLang 원본 언어
 * @param afterLang 번역 언어
 * @param containDic 사전 적용 여부
 * @param uploadFile 참고문서
 */
export const translateFileApi = async (
  sessionId: string,
  afterLang: string,
  containDic: boolean,
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
          afterLang,
          containDic,
        }),
      ],
      {
        type: 'application/json',
      },
    ),
  )

  return new Promise((resolve) => {
    const stream = streamApi(
      client.defaults.baseURL + `/translate/file`,
      formData,
      streamEvent,
    )
    resolve(stream)
  })
}

/**
 * 번역 언어 목록 조회 API
 */
export const getTranslateLanguagesApi = async (): Promise<
  ApiResponse<GetTranslateLanguageResponse[]>
> => {
  const response =
    await client.get<ApiResponse<GetTranslateLanguageResponse[]>>(
      `/translate/language`,
    )

  return response.data
}
