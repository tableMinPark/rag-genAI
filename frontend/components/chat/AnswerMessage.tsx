import React, { useEffect, useMemo, useRef, useState } from 'react'
import MarkdownIt from 'markdown-it'
import mermaid from 'mermaid'
import { Bot, BookOpen, Copy, Check } from 'lucide-react'
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

function formatTimestamp(ts?: string) {
  if (!ts) return ''
  const d = new Date(ts)
  return d.toLocaleString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  })
}

interface AnswerMessageProps {
  isStreaming: boolean
  content: string
  inference?: string
  documents?: Document[]
  timestamp?: string
  onSelectDocument?: (documents: Document[]) => void
}

export default React.memo(function AnswerMessage({
  isStreaming,
  content,
  inference,
  documents,
  timestamp,
  onSelectDocument: onSelectDocument,
}: AnswerMessageProps) {
  // ###################################################
  // 상태 관리
  // ###################################################
  const containerRef = useRef<HTMLDivElement>(null)
  const [uid] = useState(`mermaid-${Math.random().toString(36).substr(2, 9)}`)
  const [copied, setCopied] = useState(false)

  const handleCopy = () => {
    navigator.clipboard.writeText(content).then(() => {
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    })
  }
  const convertContent = useMemo(() => {
    if (!content) return ''
    return content.replace(/```mermaid([\s\S]*?)```/g, (match, code) => {
      const converted = code.replace(/\[([^\[\]]+)\]/g, '["$1"]')
      return `\`\`\`mermaid${converted}\`\`\``
    })
  }, [content])

  // ###################################################
  // 랜더링 이펙트
  // ###################################################
  useEffect(() => {
    const container = containerRef.current
    if (!container) return

    let cancelled = false
    const rafId = requestAnimationFrame(async () => {
      if (cancelled || !containerRef.current) return

      const blocks = container.querySelectorAll('.mermaid-block')

      for (let i = 0; i < blocks.length; i++) {
        const block = blocks[i] as HTMLElement
        if (block.dataset.processed === 'true') continue

        const code = (block.textContent || '').trim()
        const id = `${uid}-${i}-${content.length}`

        try {
          const { svg } = await mermaid.render(id, code)
          block.innerHTML = svg
          block.dataset.processed = 'true'
        } catch {
          block.innerHTML = `<pre>${code}</pre>`
          block.dataset.processed = 'true'
        }
      }
    })

    return () => {
      cancelled = true
      cancelAnimationFrame(rafId)
    }
  }, [convertContent])

  // ###################################################
  // 렌더링 (Render)
  // ###################################################
  return (
    <div className="mb-6 flex items-start gap-3">
      <div className="text-primary flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-blue-100">
        <Bot className="h-5 w-5" />
      </div>
      <div className="flex w-full max-w-[90%] flex-col gap-2">
        {(isStreaming || inference) && (
          <InferenceAccordion content={inference || 'thinking...'} />
        )}
        {convertContent && (
          <div className="group relative rounded-2xl rounded-tl-none border border-gray-200 bg-white px-5 py-3 text-sm leading-relaxed text-gray-800 shadow-sm">
            {!isStreaming && (
              <button
                onClick={handleCopy}
                className="absolute right-3 top-3 opacity-0 group-hover:opacity-100 transition-opacity duration-150 rounded-md p-1 text-gray-400 hover:text-gray-600 hover:bg-gray-100"
                title="복사"
              >
                {copied ? (
                  <Check className="h-3.5 w-3.5 text-green-500" />
                ) : (
                  <Copy className="h-3.5 w-3.5" />
                )}
              </button>
            )}
            <div
              ref={containerRef}
              className={`${styles.markdown} warp-break-words`}
              dangerouslySetInnerHTML={{ __html: md.render(convertContent) }}
            />
          </div>
        )}
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
        {timestamp && !isStreaming && (
          <span className="text-[10px] text-gray-400">{formatTimestamp(timestamp)}</span>
        )}
      </div>
    </div>
  )
})
