# AIPO_BE

## 1. 프로젝트 소개
AIPO 백엔드 서버입니다.
Spring Boot 기반으로 사용자 인증, 공모주, 관심종목, 캘린더 API를 개발합니다.

## 2. 개발 환경
- Java 21
- Spring Boot 4.0.5
- PostgreSQL 18

## 3. 프로젝트 실행 전 준비
- JDK 21 설치
- PostgreSQL 18 설치
- Git 설치
- aipo 데이터베이스 생성


(0405)
현재 인증 기능은 회원가입, 로그인, JWT access token 발급, refresh token 저장/재발급, 로그아웃까지의 1차 구현을 완료한 상태이다.
초기 개발 단계에서는 기능 동작 검증을 우선하여 최소 구조로 구현하였으며,
추후 프로젝트 확장에 따라 다음 항목을 순차적으로 보완할 예정이다.

- 사용자 상태(탈퇴/비활성/정지) 검증 강화
- USER / ADMIN 권한 분리
- refresh token rotation 및 다중 기기 관리
- secret 및 환경설정 외부화
- 공통 예외 처리 및 응답 형식 통일
- 테스트 코드 및 CI 정상화
- CORS 및 운영 보안 정책 반영

## 4. Swagger 운영 배포 가이드 (백엔드 직접 제공)

### 4-1. 배포본 실행 상태 확인 (EC2/ECS)
- 애플리케이션이 정상 기동되어 `/api/health` 응답이 200인지 확인
- 배포 직후 `https://{api-domain}/swagger-ui/index.html` 접속 확인

### 4-2. 인프라 라우팅/접근 허용
- 보안그룹/ALB/Nginx에서 아래 경로를 외부에서 접근 가능하게 설정
  - `/swagger-ui/**`
  - `/v3/api-docs/**`

### 4-3. 프론트 공유용 URL 규칙
- Swagger UI 기본 URL: `{APP_SWAGGER_PUBLIC_BASE_URL}/swagger-ui/index.html`
- API Docs URL: `{APP_SWAGGER_PUBLIC_BASE_URL}/v3/api-docs`
- 환경변수 `APP_SWAGGER_PUBLIC_BASE_URL` 예시: `https://api.example.com`

### 4-4. 운영 보안 정책 (권장 적용: Swagger Basic Auth)
- 운영에서는 Swagger Basic Auth 사용 권장
- 아래 환경변수 설정 시 Swagger 경로에 Basic Auth가 적용됨
  - `APP_SWAGGER_BASIC_AUTH_ENABLED=true`
  - `APP_SWAGGER_BASIC_AUTH_USERNAME={username}`
  - `APP_SWAGGER_BASIC_AUTH_PASSWORD={strong_password}`

### 4-5. 문서 갱신 운영 규칙
- 백엔드 배포 완료 후 Swagger UI 접속 및 주요 엔드포인트 스키마 확인
- 프론트에 공유한 Swagger URL이 동일하게 동작하는지 확인
- 배포 체크리스트에 Swagger 확인 항목을 포함하여 릴리즈마다 검증
