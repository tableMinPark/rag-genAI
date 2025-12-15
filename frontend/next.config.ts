import type { NextConfig } from 'next'

const nextConfig: NextConfig = {
  // output: 'export',
  compress: false,
  async rewrites() {
    return [
      {
        source: '/api/extractor/:path*', // 프론트에서 /... 로 호출하면
        destination: 'http://localhost:8000/:path*', // 백엔드로 토스
      },
      {
        source: '/api/rag-gen/:path*', // 프론트에서 /... 로 호출하면
        destination: 'http://localhost:8001/:path*', // 백엔드로 토스
      },
    ]
  },
}

export default nextConfig
