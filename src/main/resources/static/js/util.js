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
 * SSE 이벤트 문자열 치환
 */
export const replaceEventDataToText = (eventData) => {
    return eventData
        .replaceAll("&nbsp", " ")
        .replaceAll("\\n", "\n\n");
}

/**
 * Mermaid 포함 Markdown 렌더링 유틸
 * @param {string} markdownText - 마크다운 원본 문자열
 * @param {HTMLElement} targetEl - 렌더링 결과를 표시할 DOM 요소
 */
export function renderMarkdownWithMermaid(markdownText, targetEl) {
    const md = window.markdownit({
        highlight: (str, lang) => {
            if (lang === "mermaid") {
                return `<div class="mermaid">${str}</div>`;
            }
            return `<pre><code>${md.utils.escapeHtml(str)}</code></pre>`;
        },
    });

    targetEl.innerHTML = md.render(markdownText);

    if (/```mermaid([\s\S]*?)```/.test(markdownText)) {
        window.mermaid.run({
            querySelector: ".mermaid",
            suppressErrors: true,
        });
    }
}