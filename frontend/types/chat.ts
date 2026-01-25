import { Document } from './domain'

export interface Message {
  role: 'user' | 'assistant'
  content: string
  inference?: string
  documents?: Document[] | undefined
}

/**
 * 모델 답변 메시지 생성
 * @param answer 모델 답변
 * @returns 모델 답변 메시지
 */
export const createAnswerMessage = (
  answer: string,
  inference: string = '',
  documents?: Document[],
): Message => {
  return {
    role: 'assistant',
    content: answer,
    inference: inference,
    documents: documents,
  }
}

/**
 * 사용자 질의 메시지 생성
 * @param query 사용자 질의
 * @returns 사용자 질의 메시지
 */
export const createQueryMessage = (query: string): Message => {
  return {
    role: 'user',
    content: query,
  }
}
