import React, { useEffect, useMemo, useRef, useState } from 'react'
import MarkdownIt from 'markdown-it'
import mermaid from 'mermaid'
import { Bot, BookOpen } from 'lucide-react'
import InferenceAccordion from './InferenceAccordion' // 추론 아코디언 컴포넌트 (가정)
import styles from '@/public/css/markdown.module.css'
import { Document } from '@/types/domain'

// 1. Mermaid 초기화
if (typeof window !== 'undefined') {
  mermaid.initialize({
    startOnLoad: false,
    theme: 'default',
    securityLevel: 'loose',
    suppressErrorRendering: true,
  })
}

// 2. MarkdownIt 설정
const md = new MarkdownIt({
  html: true,
  breaks: true,
  linkify: true,
})

// Fence 규칙 재정의 (Mermaid 블록 감지)
const defaultFence =
  md.renderer.rules.fence ||
  function (tokens, idx, options, env, self) {
    return self.renderToken(tokens, idx, options)
  }

md.renderer.rules.fence = function (tokens, idx, options, env, self) {
  const token = tokens[idx]
  const info = token.info ? token.info.trim() : ''

  if (info === 'mermaid') {
    return `<div class="mermaid-block" data-processed="false">${token.content}</div>`
  }

  return defaultFence(tokens, idx, options, env, self)
}

interface AnswerMessageProps {
  content: string
  inference?: string
  documents?: Document[] // [추가] 출처 데이터
  onDocumentClick?: (documents: Document[]) => void // [추가] 출처 클릭 핸들러
}

// [핵심] React.memo로 감싸서 불필요한 리렌더링 방지
const AnswerMessage = React.memo(function AnswerMessage({
  content,
  inference,
  documents,
  onDocumentClick,
}: AnswerMessageProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const [uid] = useState(`mermaid-${Math.random().toString(36).substr(2, 9)}`)

  // [수정됨] 1. Mermaid 문법 오류 방지 전처리 로직 적용
  // content가 바뀔 때마다 실행되며, 결과를 processedContent에 저장
  const convertContent = useMemo(() => {
    if (!content) return ''

    return content.replace(/```mermaid([\s\S]*?)```/g, (match, code) => {
      // 대괄호 [] 내부 텍스트를 쌍따옴표 ["..."] 로 감싸서 특수문자 오류 방지
      const converted = code.replace(/\[([^\[\]]+)\]/g, '["$1"]')
      return `\`\`\`mermaid${converted}\`\`\``
    })
  }, [content])

  // Mermaid 렌더링 로직
  useEffect(() => {
    if (!containerRef.current) return

    const renderMermaid = async () => {
      const blocks = containerRef.current!.querySelectorAll('.mermaid-block')

      for (let i = 0; i < blocks.length; i++) {
        const block = blocks[i] as HTMLElement
        if (block.getAttribute('data-processed') === 'true') continue

        // [수정] 텍스트 가져올 때 공백 정리 (trim) 및 HTML 엔티티 디코딩
        let code = block.textContent || ''

        // 1. 앞뒤 공백 제거
        code = code.trim()

        // 2. (선택사항) HTML 엔티티(&gt; 등)가 넘어올 경우를 대비해 변환이 필요할 수 있음
        // 보통 block.textContent는 디코딩된 텍스트를 가져오므로 대부분 괜찮습니다.

        const id = `${uid}-${i}`

        try {
          // Mermaid 파싱 시도
          if (await mermaid.parse(code)) {
            const { svg } = await mermaid.render(id, code)
            block.innerHTML = svg
            block.setAttribute('data-processed', 'true')

            // 스타일 재적용
            block.style.display = 'flex'
            block.style.justifyContent = 'center'
            block.style.padding = '20px' // 여백 조금 더 줌
            block.style.backgroundColor = '#f9fafb'
            block.style.borderRadius = '8px'
            block.style.overflowX = 'auto' // 가로 스크롤 허용
          }
        } catch (error) {
          // console.warn('Mermaid render error:', error)
          block.innerHTML = `<pre class="text-xs text-red-500 p-2 bg-red-50 rounded">${code}</pre>`
          block.setAttribute('data-processed', 'true')
        }
      }
    }

    requestAnimationFrame(() => renderMermaid())
  }) // 의존성 배열 없이 매 렌더링마다 DOM 체크 (memo 덕분에 content 변경 시에만 실행됨)

  return (
    <div className="mb-6 flex items-start gap-3">
      {/* 1. 봇 아이콘 */}
      <div className="text-primary flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-blue-100">
        <Bot className="h-5 w-5" />
      </div>

      <div className="flex w-full max-w-[90%] flex-col gap-2">
        {/* 2. 추론 과정 (Optional) */}
        {inference && <InferenceAccordion content={inference} />}

        {/* 3. 답변 내용 (Markdown + Mermaid) */}
        {convertContent && (
          <div className="rounded-2xl rounded-tl-none border border-gray-200 bg-white px-5 py-3 text-sm leading-relaxed text-gray-800 shadow-sm">
            <div
              ref={containerRef}
              className={`${styles.markdown} warp-break-words`}
              dangerouslySetInnerHTML={{ __html: md.render(convertContent) }}
            />
          </div>
        )}

        {/* 4. [추가됨] 출처 확인 버튼 */}
        {documents && documents.length > 0 && (
          <div className="flex">
            <button
              onClick={() => onDocumentClick?.(documents)}
              className="group hover:border-primary hover:text-primary flex items-center gap-1.5 rounded-lg border border-gray-200 bg-white px-3 py-1.5 text-xs font-bold text-gray-500 transition-all hover:shadow-sm active:scale-95"
            >
              <BookOpen className="group-hover:text-primary h-3.5 w-3.5" />
              <span>참고 청크 ({documents.length})</span>
            </button>
          </div>
        )}
      </div>
    </div>
  )
})

export default AnswerMessage
