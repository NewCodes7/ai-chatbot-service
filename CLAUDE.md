# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

An AI chatbot service API for enterprise demos. The client (assumed securities firm) wants to demonstrate AI-powered chat via REST API, with future RAG expansion over proprietary documents.

**Tech stack:** Kotlin 1.9.25 · Spring Boot 3.3.6 · PostgreSQL 15.8 · pgvector · Gemini API

## Build & Run Commands

```bash
./gradlew build                          # 빌드
./gradlew test                           # 테스트
GEMINI_API_KEY=your-key ./gradlew bootRun  # 로컬 실행 (PostgreSQL 별도 필요)
docker compose up --build                # Docker로 전체 실행 (DB + 앱)
```

> `gradle.properties`에 `org.gradle.java.home=/usr/local/sdkman/candidates/java/21.0.10-ms` 설정됨.
> 시스템 Java가 21이면 이 줄을 제거해도 됨.

## Project Structure

```
src/main/kotlin/com/chatbot/
├── ChatbotApplication.kt
├── config/
│   ├── GeminiProperties.kt     # @ConfigurationProperties(prefix = "gemini")
│   ├── GeminiClient.kt         # RestClient 기반 Gemini REST API 클라이언트
│   └── SecurityConfig.kt       # HTTP Basic 임시 인증 (JWT로 교체 예정)
├── domain/
│   ├── chat/
│   │   ├── controller/ChatController.kt
│   │   ├── dto/                # ChatCreateRequest, ChatResponse, ThreadWithChatsResponse
│   │   ├── entity/             # ChatThread, Chat
│   │   ├── repository/         # ThreadRepository, ChatRepository
│   │   └── service/
│   │       ├── ChatService.kt
│   │       └── ThreadCacheService.kt
│   ├── rag/service/RagService.kt
│   └── user/
│       ├── entity/User.kt      # Role: MEMBER | ADMIN
│       └── repository/UserRepository.kt
└── support/
    ├── DataSeeder.kt           # 앱 시작 시 테스트 유저 + 증권 문서 청크 자동 삽입
    └── exception/GlobalExceptionHandler.kt
```

## Architecture Decisions

### Domain Model

Four core domains in priority order:

1. **Chat** (highest priority) — `ChatThread` (1:N) → `Chat`
2. **Feedback** — one `Feedback` per `(userId, chatId)` pair; unique constraint required
3. **Analytics** — admin-only reporting
4. **User** — `member` or `admin` role

### Thread Logic (most complex feature)

A new thread is created when:
- It is the user's **first** question, OR
- The user's **last question** was more than **30 minutes ago**

Otherwise the existing thread is reused.

**Implementation:** `ThreadCacheService` holds `ConcurrentHashMap<UserId, Instant>` — last chat time per user.
- Cache miss → query `MAX(createdAt)` from DB and populate cache
- Thread soft-delete → `cache.remove(userId)` to prevent stale cache bug

### Auth & Security

- **Current**: HTTP Basic auth (임시). `UserDetailsServiceImpl`이 DB에서 email로 사용자 조회
- **Planned**: JWT 필터로 교체 예정 — `SecurityContext` 추출 코드는 이미 올바르게 작성됨
- Extract `userId` from `SecurityContext` — do not use placeholder userId values
- RBAC: `ROLE_MEMBER` / `ROLE_ADMIN`; admin can access/modify all users' data

### AI Integration (Gemini)

- **Chat model**: `gemini-3.5-flash` (기본값; `model` 파라미터로 교체 가능)
- **Embedding model**: `gemini-embedding-2` (3072차원)
- **Client**: `GeminiClient` — Spring `RestClient`로 Gemini REST API 직접 호출
  - `com.google.ai.client.generativeai` SDK는 Android AAR 전용이라 JVM 불가 → RestClient 사용
- **Allowed models** 화이트리스트: `application.yml`의 `gemini.allowed-models`
- **System instruction**: RAG 검색 결과를 `system_instruction` 필드에 주입

### RAG (Phase 1 — MVP)

Vector-only semantic search via **pgvector**. BM25 and reranking are deferred post-MVP.

Pipeline: `DataSeeder`가 시작 시 문서 청크 임베딩 → pgvector 저장 → 질문 임베딩 후 cosine similarity Top-3 검색 → `system_instruction`에 주입 → 답변 생성.

`document_chunks` 테이블은 JPA 엔티티 없이 `JdbcTemplate`으로만 접근 (Hibernate의 vector 타입 매핑 이슈 방지).

### API Design Decisions

- **Streaming**: `POST /chats` 단일 엔드포인트, 항상 `SseEmitter` 반환.
  `isStreaming=true` → Gemini SSE 스트리밍 파싱 후 청크 emit.
  `isStreaming=false` → 전체 응답 1회 emit 후 `complete()`.
  실패 시 Chat DB 미저장 (`StringBuilder`에 누적 후 완료 시에만 저장).
- **Thread grouping**: `{ threadId, chats: [{ id, question, answer, createdAt }] }[]`
- **Pagination**: Thread 단위. `page`/`size` 파라미터, `createdAt` 기준 asc/desc 정렬.
- **Thread deletion**: Soft delete (`deleted_at` 컬럼). Chat은 삭제하지 않음.
- **Analytics "last 24 hours"**: interpreted as the **past 24 hours** from request time (not calendar day)
- **Feedback n-per-chat**: unique constraint `(user_id, chat_id)` — 한 유저가 한 채팅에 피드백 1개.

### Validation Strategy

1. Compile (type safety via Kotlin)
2. Unit + integration tests (document test results as proof of API correctness)
3. Security review
4. E2E test via simple frontend or curl scripts

## Implementation Status

- [x] 대화(Chat) 관리 기능 — `POST /chats`, `GET /chats`, `DELETE /threads/{id}`
- [ ] 피드백 관리 기능
- [ ] 분석 및 보고 기능
- [ ] 사용자 관리 및 인증 (JWT)
- [ ] Docker Compose — PostgreSQL + pgvector + Spring Boot
