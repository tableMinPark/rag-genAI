'use client'

import { KeyboardEvent, useEffect, useRef } from 'react'

interface InputBoxProps {
  value: string
  onChange: (value: string) => void
  onSend: () => void
  onStop: () => void
  isStreaming: boolean
  disabled?: boolean
}

const MAX_ROWS = 4

export default function InputBox({
  value,
  onChange,
  onSend,
  onStop,
  isStreaming,
  disabled = false,
}: InputBoxProps) {
  const textareaRef = useRef<HTMLTextAreaElement>(null)

  useEffect(() => {
    const el = textareaRef.current
    if (!el) return

    const style = getComputedStyle(el)
    const lineHeight = parseFloat(style.lineHeight)
    const paddingTop = parseFloat(style.paddingTop)
    const paddingBottom = parseFloat(style.paddingBottom)
    const maxHeight = lineHeight * MAX_ROWS + paddingTop + paddingBottom

    // height를 unset → 브라우저가 rows=1 기준 최소 높이로 리셋 후 scrollHeight 측정
    el.style.height = 'unset'
    void el.offsetHeight // reflow 강제
    const scrollH = el.scrollHeight

    if (scrollH >= maxHeight) {
      el.style.height = maxHeight + 'px'
      el.style.overflowY = 'auto'
    } else {
      el.style.height = scrollH + 'px'
      el.style.overflowY = 'hidden'
    }
  }, [value])

  const handleKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) {
      e.preventDefault()
      onSend()
    }
  }

  return (
    <div className="flex shrink-0 items-end gap-2 border-t border-gray-200 bg-white p-4">
      <textarea
        ref={textareaRef}
        rows={1}
        className="focus:border-primary focus:ring-primary scrollbar-hide flex-1 resize-none overflow-hidden rounded-2xl border border-gray-300 px-5 py-3 text-sm leading-6 transition-all focus:ring-1 focus:outline-none disabled:bg-gray-100 disabled:text-gray-400"
        placeholder={
          disabled ? '대화가 불가능한 상태입니다.' : '메시지를 입력하세요'
        }
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onKeyDown={handleKeyDown}
        disabled={disabled || isStreaming}
      />

      {isStreaming ? (
        <button
          onClick={onStop}
          className="self-end rounded-full bg-gray-500 px-6 py-3 text-sm font-bold whitespace-nowrap text-white shadow-sm transition-colors hover:bg-gray-600"
        >
          중지 ■
        </button>
      ) : (
        <button
          onClick={onSend}
          disabled={disabled || !value.trim()}
          className="bg-primary hover:bg-primary-hover self-end rounded-full px-6 py-3 text-sm font-bold whitespace-nowrap text-white shadow-sm transition-colors disabled:bg-gray-300"
        >
          전송 ➤
        </button>
      )}
    </div>
  )
}
