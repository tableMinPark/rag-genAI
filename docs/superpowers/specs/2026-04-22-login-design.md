# 로그인 기능 설계 문서

**작성일:** 2026-04-22  
**작성자:** tableminpark

---

## 개요

현재 모든 Controller에서 `String userId = "USER"`로 하드코딩된 사용자 식별자를 실제 인증 기반으로 대체한다. Spring Security 필터 단에서 JWT 인증을 처리하고, `@AuthenticationPrincipal`로 Controller에서 인증 객체를 직접 수령한다.

---

## 기술 스택

- **백엔드:** Spring Security, JWT (jjwt), BCrypt
- **프론트엔드:** Next.js App Router, Zustand, axios interceptor, Next.js Middleware

---

## 백엔드 설계

### 1. Member 엔티티

`UserDetails`를 구현하는 `MemberEntity`를 생성한다.

```java
@Entity
@Table(name = "member")
public class MemberEntity implements UserDetails {
    @Id
    private String userId;       // 로그인 아이디 (PK)
    private String password;     // BCrypt 암호화
    private String name;
    private String email;
    private String role;         // ROLE_USER / ROLE_ADMIN
    private LocalDateTime createdAt;

    // UserDetails 구현 메서드
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { ... }
    @Override
    public String getUsername() { return userId; }
    // isAccountNonExpired, isAccountNonLocked, isCredentialsNonExpired, isEnabled → true 반환
}
```

### 2. 패키지 구조 (신규)

```
com.genai.
├── app.auth/
│   ├── controller/AuthController.java        # POST /api/auth/register
│   └── service/AuthService.java              # 회원가입 (BCrypt 암호화 저장)
├── global.security/
│   ├── config/SecurityConfig.java            # Security 설정, 필터 체인 등록
│   ├── filter/JwtAuthenticationFilter.java   # POST /api/auth/login 처리
│   ├── filter/JwtVerificationFilter.java     # 모든 요청 Access Token 검증
│   ├── filter/TokenReissueFilter.java        # POST /api/auth/reissue 처리
│   ├── handler/AuthSuccessHandler.java       # 로그인 성공 시 토큰 발급
│   ├── handler/AuthFailureHandler.java       # 로그인 실패 시 401 응답
│   └── util/JwtUtil.java                     # 토큰 생성/검증 유틸
└── core.repository/
    ├── entity/MemberEntity.java
    └── MemberRepository.java
```

### 3. 필터 흐름

#### 로그인 (POST /api/auth/login)
1. `JwtAuthenticationFilter`가 요청 가로채기
2. `userId` / `password` 추출 → `UsernamePasswordAuthenticationToken` 생성
3. `AuthenticationManager.authenticate()` 호출
4. 성공 → `AuthSuccessHandler`에서 토큰 발급
   - Access Token: 응답 body (`{ "accessToken": "..." }`)
   - Refresh Token: httpOnly Cookie (`refreshToken`, Secure, SameSite=Strict)
5. 실패 → `AuthFailureHandler`에서 401 응답

#### 토큰 검증 (모든 요청)
1. `JwtVerificationFilter`가 `Authorization: Bearer {accessToken}` 헤더 추출
2. Access Token 유효성 검증
3. 성공 → `SecurityContextHolder`에 `Authentication` 등록
4. Controller에서 `@AuthenticationPrincipal MemberEntity member`로 수령

#### 토큰 재발행 (POST /api/auth/reissue)
1. `TokenReissueFilter`가 httpOnly Cookie에서 Refresh Token 추출
2. Refresh Token 유효성 검증
3. 성공 → 새 Access Token 응답 body로 발급
4. 실패 → 401 응답

#### 회원가입 (POST /api/auth/register)
- `AuthController` → `AuthService`
- BCrypt로 password 암호화 후 `MemberRepository.save()`

### 4. 토큰 설정

| 토큰 | 만료 시간 | 저장 위치 |
|------|----------|----------|
| Access Token | 1시간 | 응답 body → 프론트 메모리(Zustand) |
| Refresh Token | 14일 | httpOnly Cookie |

### 5. Controller 변경 패턴

```java
// 기존
String userId = "USER";

// 변경 후
@AuthenticationPrincipal MemberEntity member
String userId = member.getUserId();
```

변경 대상 Controller:
- `ChatController` (AI, LLM, MyAI, Simulation, 채팅이력 조회)
- 이후 추가되는 모든 Controller

### 6. SecurityConfig 공개 경로

- `POST /api/auth/login` — 공개
- `POST /api/auth/register` — 공개
- `POST /api/auth/reissue` — 공개
- 그 외 모든 `/api/**` — 인증 필요

---

## 프론트엔드 설계

### 1. 신규 파일 구조

```
frontend/
├── app/
│   ├── login/page.tsx           # 로그인 페이지
│   ├── register/page.tsx        # 회원가입 페이지
│   └── middleware.ts             # Next.js Middleware (라우트 보호)
├── api/
│   └── auth.ts                  # 로그인/회원가입/재발행 API 함수
├── stores/
│   └── authStore.ts             # Zustand: accessToken + userInfo 상태
└── lib/
    └── axiosInstance.ts         # axios 인스턴스 + interceptor
```

### 2. 토큰 흐름

```
로그인 성공
  → Access Token: Zustand(메모리) 저장
  → Refresh Token: 서버가 httpOnly Cookie로 자동 세팅

API 요청
  → axiosInstance request interceptor
    → Authorization: Bearer {accessToken} 헤더 자동 첨부

401 응답 수신
  → response interceptor 감지
  → POST /api/auth/reissue 호출 (Cookie 자동 전송, withCredentials: true)
  → 새 Access Token 수신 → Zustand 업데이트
  → 실패한 원래 요청 자동 재시도
  → 재발행도 실패 시 → Zustand 초기화 + /login 리다이렉트
```

### 3. Next.js Middleware 라우트 보호

- `/login`, `/register` — 공개 (Refresh Token 쿠키 있으면 `/`로 리다이렉트)
- 그 외 모든 경로 — Refresh Token 쿠키 없으면 `/login`으로 리다이렉트
- 실제 토큰 유효성 검증은 API 요청 시 서버에서 수행 (Middleware는 쿠키 유무만 체크)

### 4. Zustand authStore 구조

```typescript
interface AuthState {
  accessToken: string | null
  userId: string | null
  name: string | null
  setAuth: (accessToken: string, userId: string, name: string) => void
  clearAuth: () => void
}
```

### 5. 헤더 UI 변경

- 우측에 로그인한 사용자 이름 표시
- 로그아웃 버튼 → `POST /api/auth/logout` (서버 쿠키 삭제) + Zustand 초기화 + `/login` 리다이렉트

### 6. 로그인/회원가입 페이지

- `/login`: userId + password 입력, 로그인 버튼, 회원가입 링크
- `/register`: userId + password + name + email 입력, 가입 버튼, 로그인 링크
- 성공 시 `/`로 리다이렉트

---

## 미결 사항

- Refresh Token 만료 시 재로그인 UX (토스트 메시지 등)
- 로그아웃 API 백엔드 구현 여부 (쿠키 삭제만으로 충분한지)
- role 기반 페이지 접근 제어 (현재 스코프 외)
