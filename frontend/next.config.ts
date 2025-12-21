import type { NextConfig } from 'next'
import { config } from './public/ts/config'

const nextConfig: NextConfig = {
  output: config.output,
  images: {
    unoptimized: config.imageUnOptimized,
  },
  compress: false,
  rewrites: async () => {
    if (config.mode === 'production') {
      return []
    } else {
      return [
        {
          source: '/api/rag-genai/:path*',
          destination: 'http://localhost:8080/api/:path*',
        },
      ]
    }
  },
}

export default nextConfig
