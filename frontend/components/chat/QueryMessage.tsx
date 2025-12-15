import MarkdownIt from 'markdown-it'

const md = new MarkdownIt()

interface QueryMessageProps {
  content: string
}

export default function QueryMessage({ content }: QueryMessageProps) {
  return (
    <div className="my-2 flex w-full justify-end">
      <div className="bg-primary wrap-break-words max-w-[70%] rounded-2xl rounded-br-none px-5 py-3 text-sm leading-relaxed text-white shadow-sm">
        {/* 사용자 메시지는 보통 단순 텍스트지만, 필요시 마크다운 렌더링 */}
        <div
          className="prose prose-invert prose-sm max-w-none"
          dangerouslySetInnerHTML={{ __html: md.render(content) }}
        />
      </div>
    </div>
  )
}
