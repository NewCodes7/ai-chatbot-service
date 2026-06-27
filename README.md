# ai-chatbot-service

## 빠른 시작 (Quick Start)

사전 임베딩된 증권 도메인 문서가 포함된 PostgreSQL 컨테이너와 앱을 한 번에 실행합니다.

**사전 요구사항:** Docker, Docker Compose, Gemini API Key

```bash
# 1. 저장소 클론
git clone <repo-url>
cd ai-chatbot-service

# 2. Gemini API Key 설정
echo "GEMINI_API_KEY=your-api-key" > .env

# 3. 전체 실행 (DB + 앱 빌드 및 시작)
docker compose up --build
```

앱이 시작되면 `http://localhost:8080`으로 API를 사용할 수 있습니다.

**PostgreSQL만 실행하고 앱은 로컬에서 개발하는 경우:**
```bash
# DB만 실행 (초기 데이터 자동 로드)
docker compose up postgres

# 앱 로컬 실행
GEMINI_API_KEY=your-api-key ./gradlew bootRun
```

**초기 계정:**
| 이메일 | 비밀번호 | 역할 |
|---|---|---|
| member@test.com | member1234 | MEMBER |
| admin@test.com | admin1234 | ADMIN |

> `docker/initdb/01_dump.sql`에 증권 도메인 문서 청크 6개(3072차원 임베딩 포함)가 저장되어 있어,
> 별도 임베딩 작업 없이 바로 RAG 기반 대화를 테스트할 수 있습니다.

## 결과물

**질문:** KOSPI란 무엇이지?

**응답 (RAG 기반, `POST /chats`):**

> **KOSPI(Korea Composite Stock Price Index)** 는 한국거래소(KRX)에 상장된 모든 보통주의 시가총액을 기준으로 산출하는 **한국의 대표 주가지수**입니다.
>
> - **기준시점**: 1980년 1월 4일, 기준지수 **100**으로 설정
> - **산출 방식**: 시가총액 가중방식 — 삼성전자, SK하이닉스, LG에너지솔루션 등 대형주가 지수 전체에 큰 영향
> - **시장 특성**: 외국인 투자자 비중이 높아 **글로벌 위험 선호도에 민감하게 반응**

---

## 문제 상황 파악 

1. 잠재 고객사에게 긴급 시연이 목표인 상황 - 시연의 목표는 api를 통해 ai를 활용할 수 있음, 향후 자사의 대외비 문서 학습 고려 
2. 이를 사용하는 영업 직원은 api spec에 대한 깊은 이해는 없음 
3. 우선순위를 잘 고려할 것 + 확장성도 고려  
4. 3시간만에 완성해야 하는 상황 
5. 크게 4가지 기능이 필요함 - 회원, 대화, 피드백, 분석 
    1. 가장 중요한 기능은 대화 > 피드백, 분석 > 회원 
6. 하나의 대화에는 서로 다른 사용자가 생성한 n개 피드백? -> 관리자 또한 사용자 

## 설계 

1. 문서를 친절하게 이해하기 쉽게 작성하기
2. 회원 기능은 마지막에 ai 통해 빠르게 구현하기
3. 어떤 결과물이 나와야 할까? 
    - '시연'을 해야 하기에 라이브로 동작하는 모습을 보여줘야 함
    - 서버를 띄워둬야 함 (영업 직원의 컴퓨터에서 쉽게 요청 가능하게) 지금은 리포지토리만 제출하지만 실제 상황이라면 서버 띄우는 것까지 시간 고려해야 함 
    - 시연은 시각적인 게 중요하기에 간단한 프론트도 만들면 좋을 듯 
    - 기업의 문서를 임의로 넣어두고, 사용자가 넣을만한 프롬프트 예시 넣어서 테스트해서 스크린샷 남겨두기 (ai를 활용할 수 있다는 걸 보여주는 게 중요하기에 실제 대화하는 모습을 보여줘야 함) 
    - 테스트 코드 통과 기록을 통해 검증된 api임을 어필하기 
4. 검증은 컴파일, 테스트코드 통과, 보안 점검, E2E 테스트 
5. 짧은 시간 내에 내가 생각하지 못한 설계 포인트가 있을 수 있기에 grill-me 활용
6. 확장성을 고려해야 함. 프로젝트 코드 간결하면서도 최소한으로 문제 해결하기
7. 기업 문서를 바탕으로 답변하려면 무슨 기술을 쓰는 게 좋을까? 
    - RAG를 바탕으로 하기 
    - query rewriting과 같은 최적화/개선 기술은 일단 접어두기
    - 최소한으로 기본만 잘 구현하기 (chunking, embedding, BM25, rerank, 답변생성)
    - BM25는 일단 제외하고 vector를 통한 semantic search만 우선 구현 
8. 클로드 코드 대화 기록 남기기 
9. 잠재 고객사가 증권 회사라 가정하고 증권과 관련된 문서들 가져오기 
10. 스레드 30분 로직 설계 - 해당 user에 대한 가장 최신 대화 시각 로컬에 캐시해두기 
11. 스트리밍은 커넥션을 오래 잡아먹음 -> webflux는 지금 당장 하기에는 러닝커브 높고 디버깅 어려울 수 있음. 그런데 지금 당장은 성능은 중요한 게 아니기에 mvc로 일단 가되, 향후 virtual thread 고려할 것. 
12. 스레드 단위 그룹화 형식: { threadId, question, answer, ... }[]
13. 요청 시점으로부터 하루 동안의 기록 - 지난 24시간으로 해석 
14. ai api는 gemini api 쓰기. gemini 3.5 flash. 임베딩은 gemini embedding 2.

## 계획 (우선순위 고려)

- [x] 내 현재 설계 문서에서 놓친 부분 있는지 AI 통해서 확인
- [x] AI에게 현 문제 상황 인식시키기
- [x] 대화 기능 구현 -> 검증 
    - [x] AI API KEY 넣기
    - [x] CLAUDE.md 업데이트 
- [x] postgresql과 spring 띄울 수 있는 docker compose 만들기 하나의 서버에서 2개 실행되게 (시연용이니)
    - [x] pgvector도 지원 
    - [x] 예시 문서(/docs) 넣기, 예시 문서 chunking 및 embedding해서 저장 
    - [x] 문서를 바탕으로 실제로 답변하는지 확인 

- [x] 사용자 피드백 관리 기능 구현 -> 검증 (userId는 임시로 처리해두기)
- [x] 분석 및 보고 기능 구현 -> 검증 
- [x] 사용자 관리 및 인증 기능 구현 -> 검증 

- [ ] 프론트 구현 (대화 -> 피드백 -> 분석 -> 회원)
- [ ] 프론트 통한 E2E 테스트 
- [ ] ai 통한 전체 프로젝트 검증 


## 기술 스택

- postgres 15.8 + pgvector / kotlin 1.9.25 / spring boot 3.3.6
- Gemini API: `gemini-3.5-flash` (채팅), `gemini-embedding-2` (임베딩, 3072차원)
- Java 21 (gradle.properties에 JAVA_HOME 설정됨)

## 과제 구현 후기

- 과제 분석 방법
    - 요구사항 꼼꼼히 읽어보며 목표를 달성하기 위한 수단들 생각
    - 실제 시연에서 해당 서비스가 유용하다는 걸 어떻게 하면 입증할 수 있을까를 생각
    - 스스로 한 설계에서 놓친 부분은 없는지 확인하기 위해 ai를 통해 점검 및 개선
- AI 활용 방법
    - claude code cli를 주로 사용
    - /grill-me skill을 통해 가장 핵심 기능인 대화 관리 기능을 제한된 시간 내에 의도한 대로 구현함
    - git worktree를 사용하여 각종 기능 구현 병렬 처리 
    - 

## 빌드 & 실행

### Docker (시연용 — 권장)

```bash
# 1. API 키 설정
cp .env.example .env
# .env 파일에 GEMINI_API_KEY 입력

# 2. 실행 (PostgreSQL + pgvector + 앱 한 번에)
docker compose up --build

# 종료
docker compose down
```

### 로컬 개발

```bash
# 빌드
./gradlew build

# 실행 (PostgreSQL은 별도로 필요)
GEMINI_API_KEY=your-key ./gradlew bootRun

# 테스트
./gradlew test
```

### 환경변수

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `GEMINI_API_KEY` | (필수) | Gemini API 키 |
| `DB_HOST` | localhost | PostgreSQL 호스트 |
| `DB_PORT` | 5432 | PostgreSQL 포트 |
| `DB_NAME` | chatbot | DB 이름 |
| `DB_USERNAME` | postgres | DB 사용자 |
| `DB_PASSWORD` | postgres | DB 비밀번호 |

### 테스트 계정 (DataSeeder 자동 생성)

| 이메일 | 비밀번호 | 역할 |
|--------|----------|------|
| member@test.com | member1234 | MEMBER |
| admin@test.com | admin1234 | ADMIN |