import React, { useEffect, useMemo, useRef, useState } from 'react'
import MarkdownIt from 'markdown-it'
import mermaid from 'mermaid'
import { Bot, BookOpen } from 'lucide-react'
import InferenceAccordion from './InferenceAccordion'
import styles from '@/public/css/markdown.module.css'
import { Document } from '@/types/domain'

/**
 * Mermaid 초기화
 */
if (typeof window !== 'undefined') {
  mermaid.initialize({
    startOnLoad: false,
    theme: 'default',
    securityLevel: 'loose',
    suppressErrorRendering: true,
  })
}

/**
 * Markdown-It 인스턴스 생성 및 Mermaid 블록 처리 규칙 추가
 */
const md = new MarkdownIt({
  html: true,
  breaks: true,
  linkify: true,
})

/**
 * 기본 펜스 렌더러 저장
 */
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
  documents?: Document[]
  onSelectDocument?: (documents: Document[]) => void
}

export default React.memo(function AnswerMessage({
  content,
  inference,
  documents,
  onSelectDocument: onSelectDocument,
}: AnswerMessageProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const [uid] = useState(`mermaid-${Math.random().toString(36).substr(2, 9)}`)

  const convertContent = useMemo(() => {
    if (!content) return ''

    return content.replace(/```mermaid([\s\S]*?)```/g, (match, code) => {
      const converted = code.replace(/\[([^\[\]]+)\]/g, '["$1"]')
      return `\`\`\`mermaid${converted}\`\`\``
    })
  }, [content])

  /**
   * Mermaid 렌더링 이펙트
   */
  useEffect(() => {
    if (!containerRef.current) return

    const renderMermaid = async () => {
      const blocks = containerRef.current!.querySelectorAll('.mermaid-block')

      for (let i = 0; i < blocks.length; i++) {
        const block = blocks[i] as HTMLElement
        if (block.getAttribute('data-processed') === 'true') continue
        let code = block.textContent || ''
        code = code.trim()
        const id = `${uid}-${i}`

        try {
          if (await mermaid.parse(code)) {
            const { svg } = await mermaid.render(id, code)
            block.innerHTML = svg
            block.setAttribute('data-processed', 'true')

            block.style.display = 'flex'
            block.style.justifyContent = 'center'
            block.style.padding = '20px'
            block.style.backgroundColor = '#f9fafb'
            block.style.borderRadius = '8px'
            block.style.overflowX = 'auto'
          }
        } catch (error) {
          block.innerHTML = `<pre class="text-xs text-red-500 p-2 bg-red-50 rounded">${code}</pre>`
          block.setAttribute('data-processed', 'true')
        }
      }
    }

    requestAnimationFrame(() => renderMermaid())
  })

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
        {/* 4. 출처 확인 버튼 */}
        {documents && documents.length > 0 && (
          <div className="flex">
            <button
              onClick={() => onSelectDocument?.(documents)}
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
