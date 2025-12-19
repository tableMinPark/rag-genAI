export const config: {
  mode: 'local' | 'development' | 'production'
  streamUrl: string
  output: 'standalone' | 'export'
  imageUnOptimized: boolean
} = {
  mode:
    (process.env.NEXT_PUBLIC_MODE as 'local' | 'development' | 'production') ||
    'development',
  streamUrl: process.env.RAG_GENAI_NEXTJS_STREAM_URL || '',
  output: (process.env.OUTPUT as 'standalone' | 'export') || 'standalone',
  imageUnOptimized: process.env.IMAGE_UNOPTIMIZED === 'true',
}
