'use client'

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import {
  FileText,
  Database,
  Layers,
  Bot,
  Sparkles,
  Zap,
  Activity,
  ArrowRight,
  FileSearch,
  Languages,
  FileCode,
  CheckCircle2,
  XCircle,
  Clock,
  RefreshCw,
  FlaskConical,
} from 'lucide-react'

// ###################################################
// 상수 및 타입 정의 (Constants & Types)
// ###################################################

interface DashboardStats {
  totalDocuments: number
  totalPassages: number
  totalChunks: number
  vectorDbStatus: 'ONLINE' | 'OFFLINE'
  llmStatus: 'ONLINE' | 'OFFLINE'
}

interface RecentDocument {
  id: number
  name: string
  type: string
  date: string
  status: 'Complete' | 'Processing' | 'Failed'
}

// [API Mock] 대시보드 데이터 조회
const fetchDashboardData = async (): Promise<{
  stats: DashboardStats
  recentDocs: RecentDocument[]
}> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        stats: {
          totalDocuments: 128,
          totalPassages: 4052,
          totalChunks: 12405,
          vectorDbStatus: 'ONLINE',
          llmStatus: 'ONLINE',
        },
        recentDocs: [
          {
            id: 1,
            name: '2024년_인사규정_개정안.pdf',
            type: 'PDF',
            date: '2024-03-15 14:30',
            status: 'Complete',
          },
          {
            id: 2,
            name: '신규_입사자_가이드.docx',
            type: 'DOCX',
            date: '2024-03-15 11:20',
            status: 'Processing',
          },
          {
            id: 3,
            name: 'AI_서비스_기획서_v2.hwp',
            type: 'HWP',
            date: '2024-03-14 18:00',
            status: 'Complete',
          },
          {
            id: 4,
            name: '3월_마케팅_보고서.txt',
            type: 'TXT',
            date: '2024-03-14 09:15',
            status: 'Failed',
          },
        ],
      })
    }, 800)
  })
}

export default function HomePage() {
  const router = useRouter()

  // ###################################################
  // 상태 정의 (State)
  // ###################################################
  const [stats, setStats] = useState<DashboardStats>({
    totalDocuments: 0,
    totalPassages: 0,
    totalChunks: 0,
    vectorDbStatus: 'OFFLINE',
    llmStatus: 'OFFLINE',
  })
  const [recentDocs, setRecentDocs] = useState<RecentDocument[]>([])
  const [isLoading, setIsLoading] = useState(true)

  // ###################################################
  // 이펙트 및 로직 (Effects)
  // ###################################################
  useEffect(() => {
    const loadData = async () => {
      try {
        const data = await fetchDashboardData()
        setStats(data.stats)
        setRecentDocs(data.recentDocs)
      } catch (error) {
        console.error('Failed to load dashboard data', error)
      } finally {
        setIsLoading(false)
      }
    }
    loadData()
  }, [])

  // ###################################################
  // 렌더링 (Render)
  // ###################################################
  return (
    <div className="flex h-full w-full flex-col overflow-y-auto bg-gray-50/50 p-8">
      {/* 1. 헤더 영역 */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-800">
          안녕하세요, 관리자님 👋
        </h1>
        <p className="mt-2 text-gray-500">
          <span className="text-primary font-bold">RAG System</span>의 현황을
          확인하고 작업을 시작하세요.
        </p>
      </div>

      {/* 2. 통계 카드 영역 (KPIs) - Primary 컬러 테마 적용 */}
      <div className="mb-8 grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-4">
        <StatCard
          title="총 문서 (Documents)"
          value={stats.totalDocuments.toLocaleString()}
          icon={<FileText className="text-primary h-6 w-6" />}
          isLoading={isLoading}
        />
        <StatCard
          title="총 청크 (Chunks)"
          value={stats.totalChunks.toLocaleString()}
          icon={<Layers className="text-primary h-6 w-6" />}
          isLoading={isLoading}
        />
        <StatCard
          title="Vector DB Status"
          value={stats.vectorDbStatus}
          icon={<Database className="text-primary h-6 w-6" />}
          isStatus
          isLoading={isLoading}
        />
        <StatCard
          title="LLM Service Status"
          value={stats.llmStatus}
          icon={<Zap className="text-primary h-6 w-6" />}
          isStatus
          isLoading={isLoading}
        />
      </div>

      <div className="grid flex-1 grid-cols-1 gap-8 lg:grid-cols-3">
        {/* 3. 빠른 실행 (Quick Actions) */}
        <div className="flex flex-col gap-6 lg:col-span-2">
          <h3 className="flex items-center gap-2 text-lg font-bold text-gray-700">
            <Sparkles className="text-primary h-5 w-5" />
            빠른 실행 (Quick Actions)
          </h3>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <QuickActionCard
              title="RAG Chat"
              desc="등록된 문서를 기반으로 AI와 대화합니다."
              icon={<Bot className="text-primary h-8 w-8" />}
              color="bg-white border-2 border-primary/20 hover:border-primary"
              textColor="text-gray-800 group-hover:text-primary"
              subTextColor="text-gray-500"
              onClick={() => router.push('/ai')}
            />
            <QuickActionCard
              title="나만의 AI"
              desc="즉석에서 문서를 올리고 분석합니다."
              icon={<FileSearch className="text-primary h-8 w-8" />}
              color="bg-white border-2 border-primary/20 hover:border-primary"
              textColor="text-gray-800 group-hover:text-primary"
              subTextColor="text-gray-500"
              onClick={() => router.push('/myai')}
            />
            <QuickActionCard
              title="LLM Chat"
              desc="AI와 자유롭게 대화합니다."
              icon={<FileSearch className="text-primary h-8 w-8" />}
              color="bg-white border-2 border-primary/20 hover:border-primary"
              textColor="text-gray-800 group-hover:text-primary"
              subTextColor="text-gray-500"
              onClick={() => router.push('/llm')}
            />
            <QuickActionCard
              title="번역"
              desc="다국어 문서 번역 도구"
              icon={
                <Languages className="group-hover:text-primary h-6 w-6 text-gray-600" />
              }
              color="bg-white border border-gray-200 hover:border-primary/50"
              textColor="text-gray-800"
              onClick={() => router.push('/translate')}
            />
            <QuickActionCard
              title="요약"
              desc="문서 요약 도구"
              icon={
                <FileText className="group-hover:text-primary h-6 w-6 text-gray-600" />
              }
              color="bg-white border border-gray-200 hover:border-primary/50"
              textColor="text-gray-800"
              onClick={() => router.push('/summary')}
            />
            <QuickActionCard
              title="보고서"
              desc="보고서 초안 생성 도구"
              icon={
                <FileText className="group-hover:text-primary h-6 w-6 text-gray-600" />
              }
              color="bg-white border border-gray-200 hover:border-primary/50"
              textColor="text-gray-800"
              onClick={() => router.push('/report')}
            />
            <QuickActionCard
              title="시뮬레이션"
              desc="RAG Chat 시뮬레이션 도구"
              icon={
                <FlaskConical className="group-hover:text-primary h-6 w-6 text-gray-600" />
              }
              color="bg-white border border-gray-200 hover:border-primary/50"
              textColor="text-gray-800"
              onClick={() => router.push('/simulation')}
            />
            <QuickActionCard
              title="문서 추출"
              desc="PDF/Word 텍스트 추출 테스트"
              icon={
                <FileCode className="group-hover:text-primary h-6 w-6 text-gray-600" />
              }
              color="bg-white border border-gray-200 hover:border-primary/50"
              textColor="text-gray-800"
              onClick={() => router.push('/extract')}
            />
          </div>

          {/* 시스템 관리 바로가기 */}
          <div
            onClick={() => router.push('/source')}
            className="group hover:border-primary mt-2 flex cursor-pointer items-center justify-between rounded-xl border border-gray-200 bg-white p-6 shadow-sm transition-all hover:shadow-md"
          >
            <div className="flex items-center gap-4">
              <div className="group-hover:bg-primary/10 flex h-12 w-12 items-center justify-center rounded-full bg-gray-100 transition-colors">
                <Database className="group-hover:text-primary h-6 w-6 text-gray-600 transition-colors" />
              </div>
              <div>
                <h4 className="group-hover:text-primary text-base font-bold text-gray-800 transition-colors">
                  지식 베이스 관리 (Knowledge Base)
                </h4>
                <p className="text-sm text-gray-500">
                  문서 등록, 수정, 청크 관리 및 임베딩 현황을 관리합니다.
                </p>
              </div>
            </div>
            <ArrowRight className="group-hover:text-primary h-5 w-5 text-gray-400 transition-transform group-hover:translate-x-1" />
          </div>
        </div>

        {/* 4. 최근 문서 목록 (Recent Activity) */}
        <div className="flex flex-col gap-4">
          <div className="flex items-center justify-between">
            <h3 className="flex items-center gap-2 text-lg font-bold text-gray-700">
              <Activity className="h-5 w-5 text-gray-500" />
              최근 문서
            </h3>
            <button
              onClick={() => router.push('/source')}
              className="hover:text-primary text-xs font-bold text-gray-400 hover:underline"
            >
              전체보기
            </button>
          </div>

          <div className="flex flex-1 flex-col overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
            <div className="flex-1 overflow-y-auto p-2">
              {isLoading ? (
                <div className="flex h-full items-center justify-center py-10">
                  <div className="border-t-primary h-6 w-6 animate-spin rounded-full border-2 border-gray-200"></div>
                </div>
              ) : (
                <ul className="flex flex-col gap-1">
                  {recentDocs.map((doc) => (
                    <li
                      key={doc.id}
                      onClick={() => router.push(`/source/${doc.id}`)}
                      className="group flex cursor-pointer items-center justify-between rounded-lg p-3 transition-colors hover:bg-gray-50"
                    >
                      <div className="flex items-center gap-3 overflow-hidden">
                        <div className="group-hover:bg-primary/10 group-hover:text-primary flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-gray-100 text-xs font-bold text-gray-500 transition-colors">
                          {doc.type}
                        </div>
                        <div className="flex flex-col overflow-hidden">
                          <span className="group-hover:text-primary truncate text-sm font-bold text-gray-700 transition-colors">
                            {doc.name}
                          </span>
                          <span className="flex items-center gap-1 text-[10px] text-gray-400">
                            <Clock className="h-3 w-3" /> {doc.date}
                          </span>
                        </div>
                      </div>
                      <StatusBadge status={doc.status} />
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

// ###################################################
// [Sub Components]
// ###################################################

const StatCard = ({ title, value, icon, isStatus = false, isLoading }: any) => (
  <div className="hover:border-primary/30 flex flex-col rounded-2xl border border-gray-100 bg-white p-6 shadow-sm transition-all hover:shadow-md">
    <div className="mb-4 flex items-center justify-between">
      <span className="text-sm font-bold text-gray-500">{title}</span>
      {/* 아이콘 배경을 Primary 연한색으로 통일 */}
      <div className="bg-primary/10 flex h-10 w-10 items-center justify-center rounded-xl">
        {icon}
      </div>
    </div>
    {isLoading ? (
      <div className="h-8 w-24 animate-pulse rounded bg-gray-200" />
    ) : (
      <div className="flex items-end gap-2">
        <span
          className={`text-3xl font-extrabold ${
            isStatus
              ? value === 'ONLINE'
                ? 'text-green-500' // 상태값은 의미 전달을 위해 색상 유지
                : 'text-red-500'
              : 'text-gray-800'
          }`}
        >
          {value}
        </span>
        {isStatus && (
          <span className="relative mb-1 flex h-3 w-3">
            <span
              className={`absolute inline-flex h-full w-full animate-ping rounded-full opacity-75 ${
                value === 'ONLINE' ? 'bg-green-400' : 'bg-red-400'
              }`}
            ></span>
            <span
              className={`relative inline-flex h-3 w-3 rounded-full ${
                value === 'ONLINE' ? 'bg-green-500' : 'bg-red-500'
              }`}
            ></span>
          </span>
        )}
      </div>
    )}
  </div>
)

const QuickActionCard = ({
  title,
  desc,
  icon,
  color,
  textColor = 'text-gray-800',
  subTextColor = 'text-gray-500',
  onClick,
}: any) => (
  <div
    onClick={onClick}
    className={`group relative cursor-pointer overflow-hidden rounded-2xl p-6 shadow-sm transition-all hover:-translate-y-1 hover:shadow-lg ${color}`}
  >
    <div className="relative z-10 flex flex-col gap-4">
      {/* 아이콘 배경 처리는 카드 색상에 따라 다르게 보일 수 있으므로 단순화 */}
      <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-black/5 backdrop-blur-sm transition-colors group-hover:bg-black/10">
        {icon}
      </div>
      <div>
        <h4 className={`text-lg font-bold transition-colors ${textColor}`}>
          {title}
        </h4>
        <p className={`text-sm transition-colors ${subTextColor}`}>{desc}</p>
      </div>
    </div>
  </div>
)

const StatusBadge = ({ status }: { status: string }) => {
  // 상태별 색상 (의미 전달을 위해 Primary만 쓰지 않고 유지하되 Processing은 Primary로 변경)
  let badgeStyle = 'bg-gray-100 text-gray-600'
  let icon = null

  switch (status) {
    case 'Complete':
      badgeStyle =
        'bg-green-50 text-green-700 ring-1 ring-inset ring-green-600/20'
      icon = <CheckCircle2 className="h-3 w-3" />
      break
    case 'Processing':
      // 기존 파란색 -> Primary 색상으로 변경
      badgeStyle =
        'bg-primary/10 text-primary ring-1 ring-inset ring-primary/20'
      icon = <RefreshCw className="h-3 w-3 animate-spin" />
      break
    case 'Failed':
      badgeStyle = 'bg-red-50 text-red-700 ring-1 ring-inset ring-red-600/10'
      icon = <XCircle className="h-3 w-3" />
      break
  }

  return (
    <span
      className={`inline-flex shrink-0 items-center gap-1 rounded-full px-2 py-1 text-[10px] font-medium ${badgeStyle}`}
    >
      {icon}
      {status}
    </span>
  )
}
