import type { Metadata } from 'next'
import '@/public/css/globals.css'
import LayoutWrapper from '@/components/LayoutWrapper'
import Providers from './providers'

export const metadata: Metadata = {
  title: 'Rag Gen AI',
  description: 'RAG based Generative AI Service',
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <Providers>
      <html lang="ko">
        <body className="flex">
          <LayoutWrapper>{children}</LayoutWrapper>
        </body>
      </html>
    </Providers>
  )
}
