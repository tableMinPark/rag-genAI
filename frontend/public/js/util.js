/**
 * 랜덤 ID 생성
 * @returns {string}
 */
export const randomUUID = () => {
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
export const replaceEventDataToText = (eventData) => {
  return eventData
    .replaceAll('&nbsp', ' ')
    .replaceAll('\\n', '\n')
    .replaceAll(`"**`, `\"**`)
    .replaceAll(`**"`, `**\"`)
}
