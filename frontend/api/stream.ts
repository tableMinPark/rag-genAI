import { client } from './client'
import { ApiResponse } from '@/types/api'
import { StreamEvent } from '@/types/streamEvent'
import { BASE_URL, fetchWithAuth } from '@/lib/authCore'

export class FetchEventSource {
  private listeners: Record<string, Function[]> = {}
  private controller = new AbortController()

  constructor(
    private url: string,
    private options: RequestInit,
  ) {}

  addEventListener(event: string, callback: (e: MessageEvent) => void) {
    if (!this.listeners[event]) {
      this.listeners[event] = []
    }
    this.listeners[event].push(callback)
  }

  private emit(event: string, data: string) {
    const e = { data } as MessageEvent
    ;(this.listeners[event] || []).forEach((cb) => cb(e))
  }
  async connect() {
    try {
      const res = await fetchWithAuth(this.url, {
        ...this.options,
        signal: this.controller.signal,
      })

      const reader = res.body!.getReader()
      const decoder = new TextDecoder()

      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })

        const chunks = buffer.split('\n\n')
        buffer = chunks.pop() || ''

        for (const chunk of chunks) {
          let event = 'message'
          let data = ''

          chunk.split('\n').forEach((line) => {
            if (line.startsWith('event:')) {
              event = line.replace('event:', '').trim()
            }
            if (line.startsWith('data:')) {
              data += line.replace('data:', '').trim()
            }
          })

          this.emit(event, data)
        }
      }
    } catch (e: any) {
      if (e.name === 'AbortError') {
        console.log('🔌 스트림 정상 종료')
      } else {
        console.error('❌ 스트림 에러', e)
        this.emit('error', e)
      }
    }
  }

  close() {
    this.controller.abort()
  }
}

/**
 * 스트림 요청 API
 *
 * @param sessionId 세션 ID
 */
export const streamApi = (
  url: string,
  body: object,
  streamEvent: StreamEvent,
): FetchEventSource => {
  const eventSource = new FetchEventSource(url, {
    method: 'POST',
    headers:
      body instanceof FormData ? {} : { 'Content-Type': 'application/json' },
    body: body instanceof FormData ? body : JSON.stringify(body),
  })
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

  eventSource.connect()

  return eventSource
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
