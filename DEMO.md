# AI 챗봇 서비스 — 시연 가이드

> 증권사 고객사 대상 API 시연용 문서입니다.
> 모든 예시는 서버가 `http://localhost:8080` 에서 동작한다고 가정합니다.

---

## 목차

1. [서비스 개요](#1-서비스-개요)
2. [시연 환경 준비](#2-시연-환경-준비)
3. [시연 시나리오](#3-시연-시나리오)
   - [3-1. 회원가입 & 로그인](#3-1-회원가입--로그인)
   - [3-2. AI 대화 (일반 응답)](#3-2-ai-대화--일반-응답)
   - [3-3. AI 대화 (스트리밍)](#3-3-ai-대화--스트리밍)
   - [3-4. RAG 기반 문서 질의응답](#3-4-rag-기반-문서-질의응답)
   - [3-5. 대화 목록 조회 (스레드 그룹핑)](#3-5-대화-목록-조회--스레드-그룹핑)
   - [3-6. 스레드 삭제](#3-6-스레드-삭제)
   - [3-7. 피드백 생성 & 관리](#3-7-피드백-생성--관리)
   - [3-8. 관리자: 활동 분석 & CSV 보고서](#3-8-관리자-활동-분석--csv-보고서)
4. [웹 UI 시연](#4-웹-ui-시연)
5. [전체 API 목록](#5-전체-api-목록)
6. [구현 포인트 Q&A](#6-구현-포인트-qa)

---

## 1. 서비스 개요

| 항목 | 내용 |
|------|------|
| 목적 | API를 통해 AI를 활용할 수 있음을 시연 |
| 대상 | 잠재 고객사 (증권사) VIP Onboarding |
| 핵심 기능 | JWT 인증 · AI 대화(스트리밍) · RAG 문서 검색 · 피드백 · 분석 |
| 확장성 | 자사 대외비 문서 학습 및 모델 교체 가능 구조 |
| 기술 스택 | Kotlin 1.9 · Spring Boot 3.3 · PostgreSQL 15 + pgvector · Gemini API |

**시연의 핵심 메시지**

> "별도 AI 인프라 없이 REST API 한 줄로 기업 맞춤형 AI 챗봇을 즉시 운용할 수 있습니다.
> 향후 내부 문서를 학습시켜 더 정확한 답변을 제공하는 RAG 파이프라인도 이미 내장되어 있습니다."

---

## 2. 시연 환경 준비

### Docker Compose로 전체 실행 (권장)

```bash
# 1. 환경 변수 설정
cp .env.example .env
# .env 파일에서 GEMINI_API_KEY 값을 설정

# 2. 전체 스택 기동 (PostgreSQL + pgvector + Spring Boot)
docker compose up --build
```

서버가 정상 기동되면 `DataSeeder`가 자동으로 다음 작업을 수행합니다:

- 테스트 계정 2개 생성 (member / admin)
- 증권 관련 예시 문서 3개를 임베딩하여 pgvector에 저장

### 기본 제공 테스트 계정

| 역할 | 이메일 | 비밀번호 |
|------|--------|----------|
| 일반 회원 | `user@example.com` | `password123` |
| 관리자 | `admin@example.com` | `password123` |

---

## 3. 시연 시나리오

### 3-1. 회원가입 & 로그인

#### 회원가입

```bash
curl -X POST http://localhost:8080/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "demo@example.com",
    "password": "demo1234",
    "name": "시연 계정"
  }'
```

응답: `201 Created`

#### 로그인 → JWT 토큰 발급

```bash
curl -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "demo@example.com",
    "password": "demo1234"
  }'
```

응답 예시:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer"
}
```

> 이후 모든 요청에는 `Authorization: Bearer <accessToken>` 헤더가 필요합니다.

---

### 3-2. AI 대화 — 일반 응답

```bash
TOKEN="위에서 발급받은 JWT"

curl -X POST http://localhost:8080/chats \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "question": "주식 투자 시 분산 투자가 중요한 이유는 무엇인가요?",
    "isStreaming": false
  }'
```

응답: `text/event-stream` (SSE) — 전체 답변을 1회 emit 후 종료

```
data: {"type":"answer","content":"분산 투자는 특정 자산의 가격 하락이..."}

data: [DONE]
```

---

### 3-3. AI 대화 — 스트리밍

```bash
curl -X POST http://localhost:8080/chats \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{
    "question": "ETF란 무엇이고 어떻게 활용하나요?",
    "isStreaming": true
  }'
```

응답: Gemini API의 SSE 스트림을 실시간으로 릴레이

```
data: {"type":"chunk","content":"ETF(상장지수펀드)는"}
data: {"type":"chunk","content":" 주식처럼 거래소에서"}
data: {"type":"chunk","content":" 매매할 수 있는 펀드입니다."}
...
data: [DONE]
```

> 실시간으로 글자가 타이핑되듯 나타나는 효과를 시연할 수 있습니다.

---

### 3-4. RAG 기반 문서 질의응답

서버 기동 시 아래 증권 관련 예시 문서가 자동으로 임베딩됩니다:

- 주식 투자 기초 가이드
- 펀드 & ETF 상품 설명
- 시장 지표(PER, PBR, ROE) 해설

RAG가 활성화된 상태에서 질문하면, 벡터 유사도 검색으로 관련 청크 Top-3를 추출해 Gemini의 `system_instruction`에 주입합니다.

```bash
curl -X POST http://localhost:8080/chats \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "question": "PER이 낮은 주식은 무조건 저평가된 건가요?",
    "isStreaming": false
  }'
```

> 학습된 내부 문서 기반으로 답변하므로, 일반 AI보다 정확하고 맥락에 맞는 응답이 나옵니다.

**향후 확장:** 고객사의 대외비 문서(PDF, Word 등)를 파이프라인에 추가하면 즉시 RAG가 해당 문서를 참조합니다.

---

### 3-5. 대화 목록 조회 — 스레드 그룹핑

```bash
# 기본 조회 (최신순, 1페이지)
curl http://localhost:8080/chats \
  -H "Authorization: Bearer $TOKEN"

# 오름차순, 2페이지 (페이지당 5개)
curl "http://localhost:8080/chats?sort=asc&page=1&size=5" \
  -H "Authorization: Bearer $TOKEN"
```

응답 구조: 스레드 단위로 그룹핑된 대화 목록

```json
{
  "content": [
    {
      "threadId": 1,
      "createdAt": "2026-06-27T10:00:00Z",
      "chats": [
        {
          "id": 1,
          "question": "ETF란 무엇인가요?",
          "answer": "ETF는...",
          "createdAt": "2026-06-27T10:00:00Z"
        },
        {
          "id": 2,
          "question": "ETF 수수료는 어느 정도인가요?",
          "answer": "ETF 수수료는...",
          "createdAt": "2026-06-27T10:05:00Z"
        }
      ]
    }
  ],
  "totalPages": 3,
  "totalElements": 12
}
```

> **스레드 규칙:** 마지막 질문으로부터 30분 이내 재질문 시 같은 스레드에 누적됩니다. 30분 초과 시 새 스레드가 생성됩니다. 이를 통해 대화 컨텍스트가 자연스럽게 관리됩니다.

> **권한:** 일반 회원은 본인 대화만, 관리자는 전체 사용자 대화 조회 가능

---

### 3-6. 스레드 삭제

```bash
curl -X DELETE http://localhost:8080/threads/1 \
  -H "Authorization: Bearer $TOKEN"
```

응답: `204 No Content`

> 소프트 딜리트 방식으로 실제 데이터는 보존되고 `deleted_at`만 기록됩니다.

---

### 3-7. 피드백 생성 & 관리

#### 피드백 생성 (긍정/부정)

```bash
# 긍정 피드백
curl -X POST http://localhost:8080/feedbacks \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "chatId": 1,
    "positive": true
  }'

# 부정 피드백
curl -X POST http://localhost:8080/feedbacks \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "chatId": 2,
    "positive": false
  }'
```

응답 예시:

```json
{
  "id": 1,
  "chatId": 1,
  "positive": true,
  "status": "pending",
  "createdAt": "2026-06-27T10:10:00Z"
}
```

#### 피드백 목록 조회

```bash
# 긍정 피드백만 필터링
curl "http://localhost:8080/feedbacks?positive=true&sort=desc" \
  -H "Authorization: Bearer $TOKEN"
```

#### 피드백 상태 변경 (관리자 전용)

```bash
ADMIN_TOKEN="관리자 JWT"

curl -X PATCH http://localhost:8080/feedbacks/1 \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "resolved"}'
```

---

### 3-8. 관리자: 활동 분석 & CSV 보고서

#### 최근 24시간 활동 통계

```bash
ADMIN_TOKEN="관리자 JWT"

curl http://localhost:8080/analytics/activity \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

응답 예시:

```json
{
  "signupCount": 3,
  "loginCount": 12,
  "chatCount": 47,
  "since": "2026-06-26T10:00:00Z",
  "until": "2026-06-27T10:00:00Z"
}
```

#### 대화 내역 CSV 보고서 다운로드

```bash
curl http://localhost:8080/analytics/report \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -o report.csv
```

다운로드된 `report.csv` 예시:

```csv
userId,userName,threadId,chatId,question,answer,createdAt
1,홍길동,1,1,"ETF란 무엇인가요?","ETF(상장지수펀드)는...","2026-06-27T10:00:00Z"
```

> **권한 보호:** 일반 회원이 해당 엔드포인트 호출 시 `403 Forbidden` 반환

---

## 4. 웹 UI 시연

브라우저에서 `http://localhost:8080` 으로 접속하면 웹 UI를 통해 API를 시각적으로 시연할 수 있습니다.

| 화면 | 설명 |
|------|------|
| 메인 (`/`) | 서비스 소개 및 로그인/회원가입 링크 |
| 로그인 (`/login`) | JWT 발급 및 세션 저장 |
| 회원가입 (`/signup`) | 신규 계정 생성 |
| 채팅 (`/chat`) | 실시간 스트리밍 대화 UI |

---

## 5. 전체 API 목록

| 메서드 | 경로 | 설명 | 인증 | 권한 |
|--------|------|------|------|------|
| `POST` | `/signup` | 회원가입 | 불필요 | - |
| `POST` | `/login` | 로그인 (JWT 발급) | 불필요 | - |
| `POST` | `/chats` | AI 대화 생성 (SSE) | JWT | member |
| `GET` | `/chats` | 대화 목록 조회 | JWT | member / admin |
| `DELETE` | `/threads/{id}` | 스레드 삭제 | JWT | member (본인) / admin |
| `POST` | `/feedbacks` | 피드백 생성 | JWT | member |
| `GET` | `/feedbacks` | 피드백 목록 조회 | JWT | member (본인) / admin |
| `PATCH` | `/feedbacks/{id}` | 피드백 상태 변경 | JWT | admin |
| `DELETE` | `/feedbacks/{id}` | 피드백 삭제 | JWT | member (본인) / admin |
| `GET` | `/analytics/activity` | 24h 활동 통계 | JWT | admin |
| `GET` | `/analytics/report` | 대화 CSV 보고서 | JWT | admin |

---

## 6. 구현 포인트 Q&A

**Q. 자사 문서를 어떻게 학습시킬 수 있나요?**
> 문서를 청크 단위로 분할하여 Gemini 임베딩 API로 벡터화한 뒤 PostgreSQL(pgvector)에 저장합니다. 질문 시 코사인 유사도로 관련 청크를 검색해 AI 답변에 반영합니다. 현재 증권 예시 문서 3개가 내장되어 있으며, 실 데이터 적용도 동일한 파이프라인으로 처리됩니다.

**Q. AI 모델을 교체할 수 있나요?**
> `POST /chats` 요청 시 `model` 파라미터로 모델을 지정할 수 있습니다 (화이트리스트 기반). 현재 Gemini 계열 모델을 지원하며, 클라이언트 레이어만 교체하면 다른 provider로의 전환도 가능합니다.

**Q. 스레드(대화 맥락)는 어떻게 관리되나요?**
> 마지막 질문으로부터 30분 이내 재질문 시 동일 스레드로 유지됩니다. 이 판단은 메모리 캐시(`ConcurrentHashMap`)와 DB를 함께 활용하여 빠르게 처리됩니다.

**Q. 보안은 어떻게 처리되나요?**
> JWT 기반 인증을 사용합니다. 회원가입/로그인 외 모든 API는 토큰 검증이 필수이며, RBAC(Role-Based Access Control)으로 member/admin 권한이 분리됩니다.

**Q. 확장 개발이 가능한 구조인가요?**
> Spring Boot 기반의 도메인 분리 구조(chat / feedback / analytics / user / rag)로 설계되어, 기능 추가 시 다른 도메인에 영향 없이 독립적으로 확장할 수 있습니다.
