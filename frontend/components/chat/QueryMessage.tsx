import MarkdownIt from 'markdown-it'

const md = new MarkdownIt()

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

interface QueryMessageProps {
  content: string
  timestamp?: string
}

export default function QueryMessage({ content, timestamp }: QueryMessageProps) {
  return (
    <div className="my-2 flex w-full flex-col items-end gap-1">
      <div className="bg-primary wrap-break-words max-w-[70%] rounded-2xl rounded-br-none px-5 py-3 text-sm leading-relaxed text-white shadow-sm">
        <div
          className="prose prose-invert prose-sm max-w-none"
          dangerouslySetInnerHTML={{ __html: md.render(content) }}
        />
      </div>
      {timestamp && (
        <span className="pr-1 text-[10px] text-gray-400">{formatTimestamp(timestamp)}</span>
      )}
    </div>
  )
}
