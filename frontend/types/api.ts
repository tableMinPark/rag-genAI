/**
 * API 기본 응답
 */
export interface ApiResponse<T = unknown> {
  status: string
  message: string
  data: T
}

/**
 * API 기본 응답
 */
export interface ExtractApiResponse<T = unknown> {
  code: number
  status: string
  message: string
  result: T
}

/**
 * 페이징 기본 응답
 */
export interface PageResponse {
  isLast: boolean
  pageNo: number
  pageSize: number
  totalCount: number
  totalPages: number
}
