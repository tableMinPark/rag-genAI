import { client } from './client'
import { Source } from '@/types/domain'
import { ExtractApiResponse, PageResponse } from '@/types/api'

export interface GetSourceResponse extends Source {}

export interface GetSourcesResponse extends PageResponse {
  content: Source[]
}

/**
 * 문서 목록 조회 API
 *
 * @param page 페이지
 * @param size 사이즈
 * @param isAuto 자동화 여부
 * @param orderBy 정렬 필드
 * @param order 정렬 방향
 * @param keyword 검색어
 */
export const getSourcesApi = async (
  page: number,
  size: number,
  isAuto?: boolean,
  orderBy?: string,
  order?: 'asc' | 'desc',
  keyword?: string,
): Promise<ExtractApiResponse<GetSourcesResponse>> => {
  let param = `page=${page}&size=${size}`
  param += isAuto ? `&isAuto=${isAuto}}` : ''
  param += orderBy ? `&orderBy=${orderBy}` : ''
  param += order ? `&order=${order}` : ''
  param += keyword ? `&keyword=${keyword}` : ''

  const response = await client.get<ExtractApiResponse<GetSourcesResponse>>(
    `/api/extractor/source?${param}`,
  )

  return response.data
}

/**
 * 문서 조회 API
 *
 * @param sourceId 문서 ID
 */
export const getSourceApi = async (
  sourceId: number,
): Promise<ExtractApiResponse<GetSourceResponse>> => {
  const response = await client.get<ExtractApiResponse<GetSourceResponse>>(
    `/api/extractor/source/${sourceId}`,
  )

  return response.data
}
