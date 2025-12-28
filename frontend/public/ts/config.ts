export const config: {
  mode: 'local' | 'development' | 'production'
  apiHost: string
  apiPort: string
  output: 'standalone' | 'export'
  imageUnOptimized: boolean
  apiBasePath: string
  basePath: string
  assetPrefix: string
} = {
  mode:
    (process.env.NEXT_PUBLIC_MODE as 'local' | 'development' | 'production') ||
    'development',
  apiHost: process.env.NEXT_PUBLIC_API_HOST || '',
  apiPort: process.env.NEXT_PUBLIC_API_PORT || '',
  output:
    (process.env.NEXT_PUBLIC_OUTPUT as 'standalone' | 'export') || 'standalone',
  imageUnOptimized: process.env.NEXT_PUBLIC_IMAGE_UNOPTIMIZED === 'true',
  apiBasePath: process.env.NEXT_PUBLIC_API_BASE_PATH || '',
  basePath: process.env.NEXT_PUBLIC_BASE_PATH || '',
  assetPrefix: process.env.NEXT_PUBLIC_ASSET_PREFIX || '',
}
