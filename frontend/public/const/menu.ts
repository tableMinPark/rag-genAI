import {
  Bot,
  Brain,
  Languages,
  FileText,
  FlaskConical,
  FileSearch,
} from 'lucide-react'

export const menuInfos = {
  ai: {
    name: 'RAG Chat',
    description: '검색 기반 질문 & 답변',
    path: '/ai',
    icon: Bot,
  },
  llm: {
    name: 'LLM Chat',
    description: '일반 질문 & 답변',
    path: '/llm',
    icon: Brain,
  },
  simulation: {
    name: '시뮬레이션',
    description: 'RAG 기반 질문 & 답변 시뮬레이션',
    path: '/simulation',
    icon: FlaskConical,
  },
  myai: {
    name: '나만의 AI',
    description: '프로젝트 학습 문서 기반 질문 & 답변',
    path: '/myai',
    icon: FileSearch,
  },
  report: {
    name: '보고서',
    description: '텍스트 및 파일 기반 보고서 초안 생성',
    path: '/report',
    icon: FileText,
  },
  summary: {
    name: '요약',
    description: '텍스트 및 파일 요약',
    path: '/summary',
    icon: FileText,
  },
  translate: {
    name: '번역',
    description: '텍스트 및 파일 번역',
    path: '/translate',
    icon: Languages,
  },
}
