/**
 * 랜덤 ID 생성
 * @returns {string}
 */
export const randomUUID = () => {
    function _s4() {
        return ((1 + Math.random()) * 0x10000 | 0).toString(16).substring(1);
    }
    return _s4() + _s4() + '-' + _s4() + '-' + _s4() + '-' + _s4() + '-' + _s4() + _s4() + _s4();
};

/**
 * 문자를 HTML 태그로 변환
 * - 개행 => <br>
 * @param str 문자열
 * @returns {*} 변환 문자열
 */
export const replaceToHtmlTag = (str) => {
    return str.replaceAll("\\n", "<br>");
};