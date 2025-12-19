import type { NextConfig } from 'next'
import { config } from './public/ts/config'

const nextConfig: NextConfig = {
  output: config.output,
  images: {
    unoptimized: config.imageUnOptimized,
  },
  compress: false,
  async rewrites() {
    if (config.mode === 'production') {
      return []
    } else {
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
    }
  },
}

export default nextConfig
