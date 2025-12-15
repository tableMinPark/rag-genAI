'use client'

import React, { useState, useEffect } from 'react'
import { Bot, FileSearch, FileText, Loader2, Upload } from 'lucide-react'
import ChatArea, { Message } from '@/components/ChatArea'
import { Document } from '@/types/domain'

// ###################################################
// 상수 및 타입 정의 (Constants & Types)
// ###################################################

type Step = 'INIT' | 'EMBEDDING' | 'CHAT'

// [API Mock] 문서 임베딩 API
const uploadAndEmbedDocument = async (
  file: File,
  sessionId: string,
): Promise<void> => {
  return new Promise((resolve) => {
    console.log(
      `[API] Upload & Embedding... (Session: ${sessionId}, File: ${file.name})`,
    )
    setTimeout(() => {
      resolve()
    }, 3000) // 3초 소요 시뮬레이션
  })
}

// [API Mock] RAG 질의 API (답변 + 추론 + 출처 반환)
const askRagQuestion = async (
  query: string,
  sessionId: string,
  fileName: string,
): Promise<{ answer: string; inference: string; documents: Document[] }> => {
  return new Promise((resolve) => {
    console.log(`[API] RAG Question: "${query}" (Session: ${sessionId})`)
    setTimeout(() => {
      // Mock 출처 데이터
      const mockChunks: Document[] = []

      resolve({
        answer: `**[RAG 답변]**\n\n업로드하신 **${fileName}** 내용을 바탕으로 답변드립니다.\n\n사용자님의 질문 "${query}"에 대한 분석 결과, 문서의 여러 부분에서 관련 내용을 찾았습니다. 자세한 근거는 하단의 **참고 청크**를 확인해주세요.`,
        inference: `1. 사용자 질문 분석: "${query}"\n2. 벡터 DB 검색 (Session: ${sessionId})\n3. 상위 3개 청크 추출 및 재정렬\n4. LLM 답변 생성 중...`,
        documents: mockChunks,
      })
    }, 1500)
  })
}

// [API Mock] 임베딩 문서 삭제 API (Cleanup)
const deleteDocument = async (sessionId: string): Promise<void> => {
  console.log(`[API] Delete Document Resource (Session: ${sessionId})`)
}

export default function MyAiPage() {
  // ###################################################
  // 상태 정의 (State)
  // ###################################################
  const [step, setStep] = useState<Step>('INIT')
  const [sessionId, setSessionId] = useState('')
  const [uploadedFile, setUploadedFile] = useState<File | null>(null)

  // 채팅 관련 상태
  const [messages, setMessages] = useState<Message[]>([])
  const [isStreaming, setIsStreaming] = useState(false)

  // ###################################################
  // 이펙트 및 로직 (Effects)
  // ###################################################

  // 1. 초기 세션 ID 생성 및 Cleanup 처리
  useEffect(() => {
    const newSessionId = `session-${Date.now()}`
    setSessionId(newSessionId)

    // [Cleanup] 화면을 떠날 때(Unmount) 임베딩 삭제 호출
    return () => {
      if (newSessionId) {
        deleteDocument(newSessionId)
      }
    }
  }, [])

  // ###################################################
  // 핸들러 (Handler)
  // ###################################################

  /**
   * 파일 업로드 및 임베딩 시작 핸들러
   */
  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return

    setUploadedFile(file)
    setStep('EMBEDDING') // 로딩 화면으로 전환

    try {
      await uploadAndEmbedDocument(file, sessionId)
      setStep('CHAT') // 채팅 화면으로 전환
      setMessages([
        {
          role: 'assistant',
          content: `파일 **'${file.name}'** 학습이 완료되었습니다.\n이 문서에 대해 무엇이든 물어보세요!`,
        },
      ])
    } catch (error) {
      console.error(error)
      alert('문서 학습 중 오류가 발생했습니다.')
      setStep('INIT')
      setUploadedFile(null)
    }
  }

  /**
   * 메시지 전송 핸들러
   */
  const handleSendMessage = async (query: string) => {
    // 1. 사용자 메시지 추가
    const userMessage: Message = { role: 'user', content: query }
    setMessages((prev) => [...prev, userMessage])

    setIsStreaming(true)

    try {
      // 2. 어시스턴트 메시지 Placeholder 추가
      const assistantMessage: Message = {
        role: 'assistant',
        content: '',
        inference: '', // 초기엔 비어있음
      }
      setMessages((prev) => [...prev, assistantMessage])

      // 3. API 호출
      const {
        answer,
        inference,
        documents: documents,
      } = await askRagQuestion(query, sessionId, uploadedFile?.name || '문서')

      // [Inference] 추론 과정 업데이트
      setMessages((prev) => {
        const newMsgs = [...prev]
        newMsgs[newMsgs.length - 1].inference = inference
        return newMsgs
      })

      // [Streaming] 답변 스트리밍 시뮬레이션
      const chars = answer.split('')
      for (let i = 0; i < chars.length; i++) {
        await new Promise((resolve) => setTimeout(resolve, 20))
        setMessages((prev) => {
          const newMsgs = [...prev]
          const lastMsg = newMsgs[newMsgs.length - 1]
          lastMsg.content = answer.substring(0, i + 1)
          return newMsgs
        })
      }

      // [Chunks] 완료 후 출처 데이터 주입 (버튼 표시됨)
      setMessages((prev) => {
        const newMsgs = [...prev]
        newMsgs[newMsgs.length - 1].documents = documents
        return newMsgs
      })
    } catch (error) {
      console.error(error)
      setMessages((prev) => [
        ...prev,
        { role: 'assistant', content: '답변 생성 중 오류가 발생했습니다.' },
      ])
    } finally {
      setIsStreaming(false)
    }
  }

  /**
   * 답변 중단 핸들러
   */
  const handleStop = () => {
    setIsStreaming(false)
  }

  // ###################################################
  // 렌더링 (Render)
  // ###################################################
  return (
    <div className="relative flex h-full w-full flex-col bg-white">
      {/* ----------------------------------------------------------
          [배경] 채팅 화면
      ----------------------------------------------------------- */}
      <div
        className={`flex h-full flex-col transition-all duration-300 ${
          step !== 'CHAT' ? 'pointer-events-none blur-sm filter' : ''
        }`}
      >
        {/* 헤더 */}
        <div className="flex h-16 shrink-0 items-center justify-between border-b border-gray-100 px-6">
          <div className="flex items-center gap-2">
            <FileSearch className="text-primary h-6 w-6" />
            <h2 className="text-xl font-bold text-gray-800">나만의 AI</h2>
            {uploadedFile && (
              <span className="flex items-center gap-1 rounded-full bg-gray-100 px-2.5 py-0.5 text-xs font-bold text-gray-500">
                <FileText className="h-3 w-3" />
                {uploadedFile.name}
              </span>
            )}
          </div>
        </div>

        {/* 채팅 영역 (ChatArea 내부에 모달 포함됨) */}
        <div className="min-h-0 flex-1">
          <ChatArea
            messages={messages}
            onSendMessage={handleSendMessage}
            onStop={handleStop}
            isStreaming={isStreaming}
            placeholder={
              step === 'CHAT'
                ? '문서 내용에 대해 질문하세요...'
                : '문서 등록이 필요합니다.'
            }
          />
        </div>
      </div>

      {/* ----------------------------------------------------------
          [모달 1] 초기 파일 등록 모달 (INIT)
      ----------------------------------------------------------- */}
      {step === 'INIT' && (
        <div className="absolute inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm">
          <div className="animate-in fade-in zoom-in w-[400px] rounded-2xl bg-white p-8 shadow-2xl duration-300">
            <div className="mb-6 flex flex-col items-center gap-3 text-center">
              <div className="text-primary flex h-16 w-16 items-center justify-center rounded-full bg-blue-50">
                <FileText className="h-8 w-8" />
              </div>
              <h3 className="text-2xl font-bold text-gray-800">
                문서 등록이 필요합니다
              </h3>
              <p className="text-sm text-gray-500">
                나만의 AI와 대화하려면
                <br />
                분석할 문서를 먼저 업로드해주세요.
              </p>
            </div>

            <label className="group hover:border-primary flex h-32 w-full cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed border-gray-300 bg-gray-50 transition-colors hover:bg-blue-50">
              <div className="flex flex-col items-center justify-center pt-5 pb-6">
                <Upload className="group-hover:text-primary mb-2 h-8 w-8 text-gray-400" />
                <p className="group-hover:text-primary text-xs font-bold text-gray-500">
                  클릭하여 파일 업로드
                </p>
                <p className="mt-1 text-[10px] text-gray-400">
                  PDF, DOCX, HWP (Max 10MB)
                </p>
              </div>
              <input
                type="file"
                className="hidden"
                onChange={handleFileUpload}
                accept=".pdf,.docx,.hwp,.txt"
              />
            </label>
          </div>
        </div>
      )}

      {/* ----------------------------------------------------------
          [모달 2] 임베딩 진행 중 모달 (EMBEDDING)
      ----------------------------------------------------------- */}
      {step === 'EMBEDDING' && (
        <div className="absolute inset-0 z-50 flex cursor-wait items-center justify-center bg-black/50 backdrop-blur-sm">
          <div className="animate-in fade-in zoom-in flex flex-col items-center rounded-2xl bg-white p-10 shadow-2xl duration-300">
            <div className="relative mb-6">
              <Loader2 className="text-primary h-16 w-16 animate-spin" />
              <div className="absolute inset-0 flex items-center justify-center">
                <Bot className="text-primary h-6 w-6" />
              </div>
            </div>
            <h3 className="text-2xl font-bold text-gray-800">
              문서를 학습하고 있습니다
            </h3>
            <p className="mt-2 text-base text-gray-500">
              잠시만 기다려주세요...
            </p>
            {uploadedFile && (
              <p className="mt-4 rounded-full bg-gray-100 px-3 py-1 font-mono text-xs text-gray-600">
                Reading: {uploadedFile.name}
              </p>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
