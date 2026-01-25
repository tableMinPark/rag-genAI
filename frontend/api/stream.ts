import { client } from './client'
import { ApiResponse } from '@/types/api'
import { config } from '@/public/ts/config'
import { StreamEvent } from '@/types/streamEvent'

/**
 * 스트림 요청 API
 *
 * @param sessionId 세션 ID
 */
export const streamApi = async (
  sessionId: string,
  streamEvent: StreamEvent,
): Promise<void> => {
  const eventSource = new EventSource(
    `${config.apiBasePath}/stream/${sessionId}`,
  )
  // SSE 연결 이벤트
  eventSource.addEventListener('connect', (event: MessageEvent) => {
    console.log(`📡 스트림 연결`)
    streamEvent.onConnect(event)
  })
  // SSE 준비 시작 이벤트
  eventSource.addEventListener('prepare-start', (event: MessageEvent) => {
    console.log(`📋 준비 과정 시작`)
    streamEvent.onPrepareStart(event)
  })
  // SSE 준비 이벤트
  eventSource.addEventListener('prepare', (event: MessageEvent) => {
    streamEvent.onPrepare(event)
  })
  // SSE 준비 완료 이벤트
  eventSource.addEventListener('prepare-done', (event: MessageEvent) => {
    streamEvent.onPrepareDone(event)
    console.log(`📋 준비 과정 종료`)
  })
  // SSE 추론 시작 이벤트
  eventSource.addEventListener('inference-start', (event: MessageEvent) => {
    console.log('📋 추론 과정 표출 시작')
    streamEvent.onInferenceStart(event)
  })
  // SSE 추론 이벤트
  eventSource.addEventListener('inference', (event: MessageEvent) => {
    streamEvent.onInference(event)
  })
  // SSE 추론 종료 이벤트
  eventSource.addEventListener('inference-done', (event: MessageEvent) => {
    streamEvent.onInferenceDone(event)
    console.log('📋 추론 과정 표출 종료')
  })
  // SSE 답변 시작 이벤트
  eventSource.addEventListener('answer-start', (event: MessageEvent) => {
    console.log('📋 답변 시작')
    streamEvent.onAnswerStart(event)
  })
  // SSE 답변 이벤트
  eventSource.addEventListener('answer', (event: MessageEvent) => {
    streamEvent.onAnswer(event)
  })
  // SSE 답변 종료 이벤트
  eventSource.addEventListener('answer-done', (event: MessageEvent) => {
    streamEvent.onAnswerDone(event)
    console.log(`📋 답변 종료`)
  })
  // SSE 참고 문서 시작 이벤트
  eventSource.addEventListener('reference-start', (event: MessageEvent) => {
    console.log('📋 참고 문서 시작')
    streamEvent.onReferenceStart(event)
  })
  // SSE 참고 문서 이벤트
  eventSource.addEventListener('reference', (event: MessageEvent) => {
    streamEvent.onReference(event)
  })
  // SSE 참고 문서 종료 이벤트
  eventSource.addEventListener('reference-done', (event: MessageEvent) => {
    streamEvent.onReferenceDone(event)
    console.log(`📋 참고 문서 종료`)
  })
  // SSE 연결 종료 이벤트
  eventSource.addEventListener('disconnect', (event: MessageEvent) => {
    console.log(`❌ 스트림 닫힘`)
    eventSource.close()
    streamEvent.onDisconnect(event)
  })
  // SSE 예외 이벤트
  eventSource.addEventListener('exception', (event: MessageEvent) => {
    console.log(`❌ 예외 발생`)
    eventSource.close()
    streamEvent.onException(event)
  })
  eventSource.addEventListener('error', (event: MessageEvent) => {
    console.log(`❌ 스트림 에러`)
    eventSource.close()
    streamEvent.onError(event)
  })
  return Promise.resolve()
}

/**
 * 스트림 중지 요청 API
 *
 * @param sessionId 세션 ID
 */
export const cancelStreamApi = async (
  sessionId: string,
): Promise<ApiResponse<void>> => {
  const response = await client.delete<ApiResponse<void>>(
    `/stream/${sessionId}`,
  )

  return response.data
}
