'use client'

import Image from 'next/image'
import Link from 'next/link'
import { usePathname } from 'next/navigation'
import {
  Bot,
  Brain,
  Languages,
  FileText,
  FlaskConical,
  FolderOpen,
  ChevronLeft,
  FileSearch,
} from 'lucide-react'

interface SidebarProps {
  isOpen: boolean
  onToggle?: () => void
}

export const MENU_ITEMS = [
  { name: 'RAG', path: '/ai', icon: Bot },
  { name: '나만의AI', path: '/myai', icon: FileSearch },
  { name: 'LLM', path: '/llm', icon: Brain },
  { name: '번역', path: '/translate', icon: Languages },
  { name: '요약', path: '/summary', icon: FileText },
  { name: '보고서', path: '/report', icon: FileText },
  { name: '시뮬레이션', path: '/simulation', icon: FlaskConical },
  { name: '문서 추출', path: '/extract', icon: FileText },
  { name: 'RAG 문서 관리', path: '/source', icon: FolderOpen },
]

export default function Sidebar({ isOpen, onToggle }: SidebarProps) {
  const pathname = usePathname()

  return (
    <aside
      className={`relative h-screen ${
        isOpen ? 'w-64' : 'w-16'
      } bg-sidebar border-r border-gray-200 transition-all duration-300 ease-in-out`}
    >
      {/* Header */}
      <div
        className={`flex h-14 items-center ${
          isOpen ? 'justify-between px-4' : 'justify-center'
        } border-b border-gray-200`}
      >
        {isOpen && (
          <Link
            href="/"
            className="flex items-center gap-2 rounded-md p-1 hover:bg-gray-200"
          >
            <Image
              src="/img/logo.png"
              alt="Rag Gen AI Logo"
              width={20}
              height={20}
              priority
            />
          </Link>
        )}

        <button
          onClick={onToggle}
          className="rounded-md p-1 text-gray-500 hover:bg-gray-200"
        >
          <ChevronLeft
            className={`h-5 w-5 transition-transform ${
              !isOpen && 'rotate-180'
            }`}
          />
        </button>
      </div>

      {/* Menu */}
      <nav className="mt-2 flex flex-col gap-1 px-2">
        {MENU_ITEMS.map((item) => {
          // [수정됨] RAG 문서 관리 하위 경로(/passage, /chunk) 포함 체크
          const isSourceRelated =
            item.path === '/source' &&
            (pathname.startsWith('/source') ||
              pathname.startsWith('/passage') ||
              pathname.startsWith('/chunk'))

          const isActive = pathname.startsWith(item.path) || isSourceRelated
          const Icon = item.icon

          return (
            <Link
              key={item.path}
              href={item.path}
              title={!isOpen ? item.name : undefined}
              className={`group flex items-center gap-3 rounded-xl px-3 py-2 transition-all duration-200 ${
                isActive
                  ? 'bg-primary text-white shadow-sm'
                  : 'hover:bg-sidebar-hover text-gray-700'
              } ${!isOpen && 'justify-center'} `}
            >
              {/* Icon */}
              <span
                className={`flex h-9 w-9 items-center justify-center rounded-lg transition-colors ${
                  isActive
                    ? 'bg-white/20'
                    : 'bg-white text-gray-600 group-hover:bg-gray-200'
                } `}
              >
                <Icon className="h-5 w-5" />
              </span>

              {/* Label */}
              {isOpen && (
                <span className="flex-1 text-sm font-medium whitespace-nowrap">
                  {item.name}
                </span>
              )}

              {/* Active dot */}
              {isActive && isOpen && (
                <span className="h-2 w-2 rounded-full bg-white" />
              )}
            </Link>
          )
        })}
      </nav>
    </aside>
  )
}
