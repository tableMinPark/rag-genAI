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

/**
 * 날짜 변환 함수
 */
export const formatDateToYYYYMMDD = (isoString: string): string => {
  const date = new Date(isoString)

  // 유효하지 않은 날짜 처리
  if (isNaN(date.getTime())) {
    return ''
  }

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0') // 월은 0부터 시작하므로 +1
  const day = String(date.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
}
