/**
 * API 기본 응답
 */
export interface ApiResponse<T = unknown> {
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

/**
 * 페이징 래퍼
 */
export interface PageWrapper<T> extends PageResponse {
  content: T[]
}
