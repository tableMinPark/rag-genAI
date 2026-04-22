'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import Link from 'next/link'
import { registerApi } from '@/api/auth'
import { Sparkles } from 'lucide-react'

export default function RegisterPage() {
  const router = useRouter()
  const [form, setForm] = useState({ userId: '', password: '', name: '', email: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await registerApi(form)
      router.push('/login')
    } catch {
      setError('회원가입에 실패했습니다. 아이디가 중복되었거나 입력값을 확인해 주세요.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-50">
      <div className="w-full max-w-sm rounded-2xl bg-white p-8 shadow-xl">
        <div className="mb-8 flex flex-col items-center gap-2">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-white shadow ring-1 ring-gray-100">
            <Sparkles className="text-primary fill-primary/20 h-6 w-6" />
          </div>
          <h1 className="text-xl font-bold text-gray-900">회원가입</h1>
        </div>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <input name="userId" type="text" placeholder="아이디 (3~50자)" value={form.userId}
            onChange={handleChange} required minLength={3} maxLength={50}
            className="rounded-lg border border-gray-200 px-4 py-2.5 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100" />
          <input name="password" type="password" placeholder="비밀번호 (8자 이상)" value={form.password}
            onChange={handleChange} required minLength={8}
            className="rounded-lg border border-gray-200 px-4 py-2.5 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100" />
          <input name="name" type="text" placeholder="이름" value={form.name}
            onChange={handleChange} required maxLength={100}
            className="rounded-lg border border-gray-200 px-4 py-2.5 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100" />
          <input name="email" type="email" placeholder="이메일" value={form.email}
            onChange={handleChange}
            className="rounded-lg border border-gray-200 px-4 py-2.5 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100" />
          {error && <p className="text-sm text-red-500">{error}</p>}
          <button type="submit" disabled={loading}
            className="bg-primary hover:bg-primary-hover rounded-lg py-2.5 text-sm font-medium text-white transition disabled:opacity-50">
            {loading ? '처리 중...' : '가입하기'}
          </button>
        </form>
        <p className="mt-4 text-center text-sm text-gray-500">
          이미 계정이 있으신가요?{' '}
          <Link href="/login" className="text-primary font-medium hover:underline">로그인</Link>
        </p>
      </div>
    </div>
  )
}
