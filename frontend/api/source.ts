import { extractorClient } from './client'
import { Category, Source } from '@/types/domain'
import { ExtractApiResponse, PageResponse } from '@/types/api'
import { config } from '@/public/ts/config'

export interface GetSourceResponse extends Source {}

export interface GetSourcesResponse extends PageResponse {
  content: Source[]
}

export interface GetCategorySource extends Category {}

/**
 * 문서 목록 조회 API
 *
 * @param page 페이지
 * @param size 사이즈
 * @param keyword 검색어
 * @param categoryCode 카테고리코드
 * @param orderBy 정렬 필드
 * @param order 정렬 방향
 */
export const getSourcesApi = async (
  page: number,
  size: number,
  keyword?: string,
  categoryCode?: string,
  orderBy?: string,
  order?: 'asc' | 'desc',
): Promise<ExtractApiResponse<GetSourcesResponse>> => {
  let param = `page=${page}&size=${size}`
  param += keyword ? `&keyword=${keyword}` : ''
  param += categoryCode ? `&categoryCode=${categoryCode}` : ''
  param += orderBy ? `&orderBy=${orderBy}` : ''
  param += order ? `&order=${order}` : ''

  const response = await extractorClient.get<
    ExtractApiResponse<GetSourcesResponse>
  >(`/source?${param}`)

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
  const response = await extractorClient.get<
    ExtractApiResponse<GetSourceResponse>
  >(`/source/${sourceId}`)

  return response.data
}

/**
 * 문서 카테고리 목록 조회 API
 */
export const getCategoriesSource = async (): Promise<
  ExtractApiResponse<GetCategorySource[]>
> => {
  const response = await extractorClient.get<
    ExtractApiResponse<GetCategorySource[]>
  >(`/source/category`)

  return response.data
}
