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
  rewrites: async () => {
    return [
      {
        source: `/api/:path*`,
        destination: `http://${config.apiHost}:${config.apiPort}${config.apiBasePath}/:path*`,
      },
    ]
  },
}

export default nextConfig
