'use client'

import { KeyboardEvent } from 'react'

interface InputBoxProps {
  value: string
  onChange: (value: string) => void
  onSend: () => void
  onStop: () => void
  isStreaming: boolean
  disabled?: boolean
}

export default function InputBox({
  value,
  onChange,
  onSend,
  onStop,
  isStreaming,
  disabled = false,
}: InputBoxProps) {
  const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && !e.nativeEvent.isComposing) {
      onSend()
    }
  }

  return (
    <div className="flex shrink-0 items-center gap-2 border-t border-gray-200 bg-white p-4">
      <input
        type="text"
        className="focus:border-primary focus:ring-primary flex-1 rounded-full border border-gray-300 px-5 py-3 text-sm transition-all focus:ring-1 focus:outline-none disabled:bg-gray-100 disabled:text-gray-400"
        placeholder={
          disabled ? '대화가 불가능한 상태입니다.' : '메시지를 입력하세요...'
        }
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onKeyDown={handleKeyDown}
        disabled={disabled || isStreaming}
      />

      {isStreaming ? (
        <button
          onClick={onStop}
          className="rounded-full bg-gray-500 px-6 py-3 text-sm font-bold whitespace-nowrap text-white shadow-sm transition-colors hover:bg-gray-600"
        >
          중지 ■
        </button>
      ) : (
        <button
          onClick={onSend}
          disabled={disabled || !value.trim()}
          className="bg-primary hover:bg-primary-hover rounded-full px-6 py-3 text-sm font-bold whitespace-nowrap text-white shadow-sm transition-colors disabled:bg-gray-300"
        >
          전송 ➤
        </button>
      )}
    </div>
  )
}
