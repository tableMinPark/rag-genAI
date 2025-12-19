import type { NextConfig } from 'next'
import { config } from './public/ts/config'

const nextConfig: NextConfig = {
  output: config.output,
  images: {
    unoptimized: config.imageUnOptimized,
  },
  compress: false,
}

export default nextConfig
