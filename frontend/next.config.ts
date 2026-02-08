import type { NextConfig } from 'next'
import { config } from './public/ts/config'

const nextConfig: NextConfig = {
  basePath: config.basePath,
  assetPrefix: config.assetPrefix,
  output: config.output,
  images: {
    unoptimized: config.imageUnOptimized,
  },
  compress: false,
}

export default nextConfig
