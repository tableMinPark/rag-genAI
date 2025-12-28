// 참고 문서
export interface Document {
  id: number
  title: string
  subTitle: string
  thirdTitle: string
  content: string
  subContent: string
  originFileName: string
  url: string
  categoryCode: string
  sourceType: string
  ext: string
}

// 나만의 AI 프로젝트
export interface Project {
  projectId: number
  projectName: string
  projectDesc: string
  sysCreateDt: string
  sysModifyDt: string
  sourceCount: number
  chunkCount: number
}

// 파일 상세
export interface FileDetail {
  fileDetailId: number
  fileOriginName: string
  ext: string
  fileSize: number
}

// 번역 언어
export interface TranslateLanguage {
  code: string
  name: string
}

// 번역 언어
export interface Category {
  code: string
  name: string
}

// 문서 추출 본문
export interface ExtractContent {
  type: string
  content: string
}
