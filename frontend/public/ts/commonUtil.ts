/**
 * 랜덤 ID 생성
 * @returns {string}
 */
export const randomUUID = (): string => {
  function _s4() {
    return (((1 + Math.random()) * 0x10000) | 0).toString(16).substring(1)
  }
  return (
    _s4() +
    _s4() +
    '-' +
    _s4() +
    '-' +
    _s4() +
    '-' +
    _s4() +
    '-' +
    _s4() +
    _s4() +
    _s4()
  )
}

/**
 * SSE 이벤트 문자열 치환
 */
export const replaceEventDataToText = (eventData: string) => {
  return eventData
    .replaceAll('&nbsp', ' ')
    .replaceAll('\\n', '\n')
    .replaceAll(`"**`, `\"**`)
    .replaceAll(`**"`, `**\"`)
}

/**
 * API 요청 시간을 측정하는 제네릭 함수
 */
export const measureRequest = async <T>(
  requestFn: () => Promise<T>,
): Promise<{ response: T; duration: number }> => {
  const start = performance.now()
  const response = await requestFn()
  const end = performance.now()
  return { response, duration: end - start }
}
