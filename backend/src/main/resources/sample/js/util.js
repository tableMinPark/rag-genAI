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
        .replaceAll("\\n", "\n");
}

/**
 * Mermaid 포함 Markdown 렌더링 유틸
 * @param {string} markdownText - 마크다운 원본 문자열
 * @param {HTMLElement} targetEl - 렌더링 결과를 표시할 DOM 요소
 */
export const renderMarkdownWithMermaid = (markdownText, targetEl) => {
    const md = window.markdownit({
        highlight: (str, lang) => {
            if (lang === "mermaid") {
                return `<div class="mermaid">${str}</div>`;
            }
            return `<pre><code>${md.utils.escapeHtml(str)}</code></pre>`;
        },
    });

    const convertMarkdownText = markdownText.replace(/```mermaid([\s\S]*?)```/g, (match, code) => {
        const converted = code.replace(/\[([^\[\]]+)\]/g, '["$1"]');
        return `\`\`\`mermaid${converted}\`\`\``;
    });

    targetEl.innerHTML = md.render(convertMarkdownText);

    if (/```mermaid([\s\S]*?)```/.test(convertMarkdownText)) {
        window.mermaid.run({
            querySelector: ".mermaid",
            suppressErrors: true,
        });
    }
}

export const copyText = (text) => {
    // clipboard API 사용
    if (navigator.clipboard !== undefined) {
        navigator.clipboard
            .writeText(text)
            .then(() => {
                console.log(text);
                console.log("클립 보드 저장 완료!");
            });
    } else {
        // execCommand 사용
        const textArea = document.createElement('textarea');
        textArea.value = text;
        document.body.appendChild(textArea);
        textArea.select();
        textArea.setSelectionRange(0, 99999);
        try {
            document.execCommand('copy');
            console.log(text);
            console.log("클립 보드 저장 완료!");
        } catch (err) {
            console.error('복사 실패', err);
        }

        textArea.setSelectionRange(0, 0);
        document.body.removeChild(textArea);
    }
}
