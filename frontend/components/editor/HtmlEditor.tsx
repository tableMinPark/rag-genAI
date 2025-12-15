'use client'

import React, { useMemo } from 'react'
import dynamic from 'next/dynamic'

// SSR 방지
const JoditEditor = dynamic(() => import('jodit-react'), { ssr: false })

interface HtmlEditorProps {
  value: string
  onChange: (value: string) => void
  placeholder?: string
  height?: number
}

export default function HtmlEditor({
  value,
  onChange,
  placeholder,
  height = 400,
}: HtmlEditorProps) {
  const config = useMemo(
    () => ({
      readonly: false,
      placeholder: placeholder || '내용을 입력하세요...',

      // [높이 고정]
      height: height,
      minHeight: height,
      maxHeight: height,

      // [기능 비활성화] 리사이즈, 상태바, 개행 유도 박스 제거
      allowResizeX: false,
      allowResizeY: false,
      statusbar: false,
      showXPathInStatusbar: false,
      showCharsCounter: false,
      showWordsCounter: false,
      showPoweredBy: false,

      // [핵심] 마우스 오버 시 뜨는 네모 박스(새 줄 추가 버튼) 제거
      addNewLine: false,

      menubar: true,
      toolbar: true,
      // 툴바 구성
      buttons: [
        'source',
        '|',
        'bold',
        'italic',
        'underline',
        'strikethrough',
        '|',
        'ul',
        'ol',
        '|',
        'font',
        'fontsize',
        'brush',
        'paragraph',
        '|',
        'table',
        'link',
        'image',
        '|',
        'align',
        'undo',
        'redo',
        'hr',
      ],
      uploader: {
        insertImageAsBase64URI: true,
      },

      // 스타일 강제 적용
      style: {
        fontFamily: 'Pretendard, sans-serif',
        fontSize: '14px',
        height: '100%',
      },
      className: 'h-full',
    }),
    [placeholder, height],
  )

  return (
    <div className="jodit-container w-full">
      {/* 글로벌 스타일 오버라이드 */}
      <style jsx global>{`
        /* 컨테이너 테두리 및 둥글기 */
        .jodit-container {
          border: 1px solid #e5e7eb !important; /* Tailwind gray-200 */
          border-radius: 0.5rem; /* rounded-lg */
          overflow: hidden;
        }

        /* 툴바 스타일 */
        .jodit-toolbar__box {
          border-bottom: 1px solid #e5e7eb !important;
          background-color: #f9fafb !important; /* bg-gray-50 */
        }

        /* [중요] 편집 영역이 부모 높이를 100% 채우도록 강제 */
        .jodit-wysiwyg {
          padding: 1rem !important;
          height: ${height}px !important; /* 높이값 강제 주입 */
          min-height: 100% !important;
          box-sizing: border-box;
        }

        /* [중요] 개행 유도 UI 완전 숨김 (설정으로 안 잡힐 경우 대비) */
        .jodit-add-new-line {
          display: none !important;
        }

        /* 플레이스홀더 스타일 */
        .jodit-placeholder {
          padding: 1rem !important;
        }

        /* 포커스 시 테두리 색상 (Primary) */
        .jodit-container:focus-within {
          border-color: #c64f4f !important;
          box-shadow: 0 0 0 1px #c64f4f;
        }
      `}</style>

      <JoditEditor
        value={value}
        config={config as any}
        onBlur={(newContent) => onChange(newContent)}
      />
    </div>
  )
}
