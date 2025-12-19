import axios from 'axios'

export const client = axios.create({
  baseURL: '/api/rag-genai',
  // headers: {
  //   'Content-Type': 'application/json',
  // },
})

export const extractorClient = axios.create({
  baseURL: '/api/extractor',
  // headers: {
  //   'Content-Type': 'application/json',
  // },
})

// [요청 인터셉터] 토큰이 있다면 헤더에 자동 추가
// client.interceptors.request.use((config) => {
//   const token = localStorage.getItem('accessToken') // 혹은 쿠키에서 가져옴
//   if (token) {
//     config.headers.Authorization = `Bearer ${token}`
//   }
//   return config
// })

// [응답 인터셉터] 공통 에러 처리 (예: 401 시 로그아웃)
client.interceptors.response.use(
  (response) => response,
  (error) => {
    // if (error.response?.status === 401) {
    //   // 로그아웃 로직 or 토큰 재발급 로직
    //   console.error('Unauthorized!')
    // }
    return Promise.reject(error)
  },
)
