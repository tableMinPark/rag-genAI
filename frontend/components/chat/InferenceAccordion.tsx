'use client'

import { useState } from 'react'
import { ChevronDown, ChevronRight, BrainCircuit } from 'lucide-react'

interface InferenceAccordionProps {
  content: string
}

export default function InferenceAccordion({
  content,
}: InferenceAccordionProps) {
  const [isOpen, setIsOpen] = useState(false)

  return (
    <div className="flex w-full flex-col items-start">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex w-fit items-center gap-1.5 rounded-lg bg-gray-100 px-3 py-1.5 text-xs font-bold text-gray-500 transition-colors hover:bg-gray-200"
      >
        <BrainCircuit className="h-3.5 w-3.5" />
        <span>생각하는 과정</span>
        <span className="ml-1 opacity-50">
          {isOpen ? (
            <ChevronDown className="h-3 w-3" />
          ) : (
            <ChevronRight className="h-3 w-3" />
          )}
        </span>
      </button>

      {isOpen && (
        <div className="animate-in fade-in slide-in-from-top-1 mt-2 w-full duration-200">
          <div className="rounded-xl border border-gray-100 bg-gray-50/50 p-4 text-xs leading-relaxed text-gray-600">
            <div className="font-mono whitespace-pre-wrap">{content}</div>
          </div>
        </div>
      )}
    </div>
  )
}
