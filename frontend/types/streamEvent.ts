export type StreamEventHandler = (event: MessageEvent) => void | Promise<void>

export type StreamEventHandlers = Partial<{
  onConnect: StreamEventHandler
  onPrepareStart: StreamEventHandler
  onPrepare: StreamEventHandler
  onPrepareDone: StreamEventHandler
  onInferenceStart: StreamEventHandler
  onInference: StreamEventHandler
  onInferenceDone: StreamEventHandler
  onAnswerStart: StreamEventHandler
  onAnswer: StreamEventHandler
  onAnswerDone: StreamEventHandler
  onReferenceStart: StreamEventHandler
  onReference: StreamEventHandler
  onReferenceDone: StreamEventHandler
  onDisconnect: StreamEventHandler
  onException: StreamEventHandler
}>

const noop: StreamEventHandler = () => {}

export class StreamEvent {
  onConnect = noop
  onPrepareStart = noop
  onPrepare = noop
  onPrepareDone = noop
  onInferenceStart = noop
  onInference = noop
  onInferenceDone = noop
  onAnswerStart = noop
  onAnswer = noop
  onAnswerDone = noop
  onReferenceStart = noop
  onReference = noop
  onReferenceDone = noop
  onDisconnect = noop
  onException = noop

  constructor(handlers: StreamEventHandlers = {}) {
    Object.assign(this, handlers)
  }
}
