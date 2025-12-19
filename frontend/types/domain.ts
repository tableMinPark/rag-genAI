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

// 정규식 패턴 타입
export interface RegexPattern {
  prefix: string
  isTitle: boolean
}

// Repo 리소스 타입
export interface RepoResource {
  originFileName: string
  fileName: string
  ext: string
  path: string
  urn: string
}

// 문서
export interface Source {
  sourceId: number
  version: string
  sourceType: string
  sourceTypeName: string
  categoryCode: string
  categoryName: string
  name: string
  collectionId: string
  selectType: string
  selectTypeName: string
  isAuto: boolean
  isBatch: boolean
  sysCreateDt: string
  sysModifyDt: string
}

// 패시지
export interface Passage {
  passageId: number
  sourceId: number
  version: number
  title: string
  subTitle: string
  thirdTitle: string
  content: string
  subContent: string
  contentTokenSize: number
  subContentTokenSize: number
  sysCreateDt: string
  sysModifyDt: string
  updateState: string
  sortOrder: number
  parentSortOrder: number
}

// 청크
export interface Chunk {
  chunkId: number
  passageId: number
  version: number
  title: string
  subTitle: string
  thirdTitle: string
  content: string
  compactContent: string
  subContent: string
  contentTokenSize: number
  compactContentTokenSize: number
  subContentTokenSize: number
  sysCreateDt: string
  sysModifyDt: string
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
