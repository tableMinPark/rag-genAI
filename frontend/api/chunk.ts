import { client } from './client'
import { Chunk } from '@/types/domain'
import { ExtractApiResponse, PageResponse } from '@/types/api'

export interface GetChunkResponse extends Chunk {}

export interface GetChunksResponse extends PageResponse {
  content: Chunk[]
}

/**
 * 청크 목록 조회 API
 *
 * @param page 페이지
 * @param size 사이즈
 */
export const getChunksApi = async (
  page: number,
  size: number,
): Promise<ExtractApiResponse<GetChunksResponse>> => {
  const param = `page=${page}&size=${size}`
  const response = await client.get<ExtractApiResponse<GetChunksResponse>>(
    `/api/extractor/chunk?${param}`,
  )

  return response.data
}

/**
 * 청크 조회 API
 *
 * @param chunkId 청크 ID
 */
export const getChunkApi = async (
  chunkId: number,
): Promise<ExtractApiResponse<GetChunkResponse>> => {
  const response = await client.get<ExtractApiResponse<GetChunkResponse>>(
    `/api/extractor/chunk/${chunkId}`,
  )

  return response.data
}

/**
 * 청크 등록 API
 *
 * @param passageId 패시지 ID
 * @param title 제목
 * @param subTitle 중제목
 * @param thirdTitle 소제목
 * @param content 본문
 * @param subContent 부가 본문
 */
export const createChunkApi = async (
  passageId: number,
  title: string,
  subTitle: string,
  thirdTitle: string,
  content: string,
  subContent: string,
): Promise<ExtractApiResponse<void>> => {
  const response = await client.post<ExtractApiResponse<void>>(
    `/api/extractor/chunk`,
    {
      passageId,
      title,
      subTitle,
      thirdTitle,
      content,
      subContent,
    },
  )
  return response.data
}

/**
 * 청크 수정 API
 *
 * @param chunkId 청크 ID
 * @param title 제목
 * @param subTitle 중제목
 * @param thirdTitle 소제목
 * @param content 본문
 * @param subContent 부가 본문
 */
export const updateChunkApi = async (
  chunkId: number,
  title: string,
  subTitle: string,
  thirdTitle: string,
  content: string,
  subContent: string,
): Promise<ExtractApiResponse<void>> => {
  const response = await client.put<ExtractApiResponse<void>>(
    `/api/extractor/chunk/${chunkId}`,
    {
      title,
      subTitle,
      thirdTitle,
      content,
      subContent,
    },
  )
  return response.data
}
