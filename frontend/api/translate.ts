import { config } from '@/public/ts/config'
import { client } from './client'
import { ApiResponse } from '@/types/api'
import { TranslateLanguage } from '@/types/domain'

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
  beforeLang: string,
  afterLang: string,
  containDic: boolean,
  context: string,
): Promise<ApiResponse<TranslateResponse>> => {
  const response = await client.post<ApiResponse<TranslateResponse>>(
    `/translate/text`,
    {
      sessionId,
      beforeLang,
      afterLang,
      containDic,
      context,
    },
  )

  return response.data
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
  beforeLang: string,
  afterLang: string,
  containDic: boolean,
  uploadFile: File,
): Promise<ApiResponse<TranslateResponse>> => {
  const formData = new FormData()
  formData.append('uploadFile', uploadFile)
  formData.append(
    'requestDto',
    new Blob(
      [
        JSON.stringify({
          sessionId,
          beforeLang,
          afterLang,
          containDic,
        }),
      ],
      {
        type: 'application/json',
      },
    ),
  )

  const response = await client.post<ApiResponse<TranslateResponse>>(
    `/translate/file`,
    formData,
  )

  return response.data
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
