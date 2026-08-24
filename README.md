# IMS — Inventory Management System

[![CI](https://github.com/musqat/IMS/actions/workflows/ci.yml/badge.svg)](https://github.com/musqat/IMS/actions/workflows/ci.yml)

> 제조업 파트너사 협업을 위한 BOM 기반 재고관리 시스템

![IMS Demo](docs/demo.gif)

## Overview

파트너사 간 재고 협업은 ERP 도입 후에도 엑셀로 처리되는 경우가 많습니다. 협력사마다 재고 기준이 달라 생산 계획이 어긋나고, 부품 부족은 결산 후에야 드러납니다.

IMS는 회사 단위 계정과 초대 기반 Partnership으로 협력사를 연결합니다. 창고 공유 권한(VIEW/FULL), BOM 기반 최대 생산량 자동 계산, 자정 배치 결산까지 하나의 시스템에서 처리합니다.


## Live Demo

**https://ims-green-nu.vercel.app**

<details>
<summary><b>데모 계정 5개</b> — 초대 방향과 PENDING 상태까지 확인할 수 있습니다</summary>

<br>

| 계정 | 회사 | 역할 |
|------|------|------|
| `a@ims.dev` | 아이테크조립(주) | **메인 데모 계정** — E가 초대했고, B·C를 초대했다 |
| `b@ims.dev` | 비전전자(주) | A가 초대 (전자부품) |
| `c@ims.dev` | 씨메카닉스(주) | A가 초대 (기계부품) |
| `d@ims.dev` | 디로지스(주) | 초대 대기(PENDING) 상태 확인용 |
| `e@ims.dev` | 이스마트코리아(주) | A를 초대 |

비밀번호는 공통 `Test1234!` 입니다. 최근 90일치 데모 데이터(생산 75건, 결산 70건)가 적재되어 있어 분석·결산 화면을 바로 확인할 수 있습니다.

</details>

<details>
<summary><b>첫 접속이 느리거나 화면이 설명과 다를 수 있습니다</b></summary>

<br>

**콜드 스타트**

Render 무료 인스턴스는 15분간 요청이 없으면 슬립됩니다. 깨어나 JVM이 기동하기까지 약 160초가 걸립니다.

외부 스케줄러가 10분마다 헬스 체크를 보내 낮 시간대(KST 08:00~19:59)와 자정 결산 배치 구간을 깨워둡니다. 무료 인스턴스의 월 750시간 한도 때문에 종일 유지하지는 않습니다. 그 밖의 시간대는 첫 요청에서 기동을 기다립니다.

**공개 데모 데이터**

방문자 누구나 재고를 수정하거나 생산 기록을 취소할 수 있습니다. 화면이 설명과 다르다면 앞선 방문자의 조작입니다.

둘 다 로컬 실행에는 없습니다. `docker compose up -d` 한 번으로 같은 데모 데이터가 적재됩니다.

</details>

## 배포 구성

```
브라우저 ──▶ Vercel        정적 페이지 (전 페이지 클라이언트 렌더링)
브라우저 ──▶ Render        Spring Boot API (Singapore)
                ├──▶ Supabase   PostgreSQL 17
                └──▶ Upstash    Redis
```

## Tech Stack

| 영역 | 기술 |
|------|------|
| Backend | Java 21, Spring Boot 3.x, Spring Security, Spring Data JPA, Spring Batch |
| Database | PostgreSQL 16 (주 저장소), Redis (Refresh Token 저장소) |
| Frontend | Next.js 14 (App Router), TypeScript, Tailwind CSS, shadcn/ui |
| Auth | JWT — Access Token (1h) + Refresh Token (2주, Redis 저장) |
| Test | JUnit 5, Mockito, @WebMvcTest, @DataJpaTest |
| Infra | Docker, Docker Compose |

## 시스템 구조

```
┌──────────────────────────────────────────────────────┐
│  Next.js 14 (App Router)                             │
│  대시보드 · 재고 · 생산 · 파트너 · 분석              │
└─────────────────────┬────────────────────────────────┘
                      │ REST API (JWT Bearer)
┌─────────────────────▼────────────────────────────────┐
│  Spring Boot 3 — 기능별 패키지                        │
│  user · partnership · warehouse · item               │
│  inventory · production · global                     │
│                  ┌───────────────┐                   │
│  Spring Batch ──▶│ 자정 결산 배치 │ (cron 00:05)      │
│                  └───────────────┘                   │
└──────┬──────────────────────────────┬────────────────┘
       │ JPA                          │ RedisTemplate
┌──────▼──────┐               ┌───────▼───────┐
│ PostgreSQL16│               │     Redis     │
│  (주 DB)    │               │ (Refresh 토큰) │
└─────────────┘               └───────────────┘
```

## 핵심 도메인

<details>
<summary><b>회사 간 협력 구조 (Partnership)</b> — 초대로 관계를 맺고, 창고를 VIEW/FULL로 공유한다</summary>

<br>

각 회사가 독립 계정으로 가입 후, 초대로 파트너 관계를 맺습니다.

```
이스마트코리아(E) ──[ACCEPTED]──▶ 아이테크조립(A) ──[ACCEPTED]──▶ 비전전자(B)
                                        │
                                        ├──[ACCEPTED]──▶ 씨메카닉스(C)
                                        │
                                        └──[PENDING] ──▶ 디로지스(D)  ← 초대 대기 중
```

- 한 회사가 여러 파트너를 동시에 가질 수 있습니다 (다중 Partnership)
- 창고 공유(WarehouseShare)는 ACCEPTED 관계인 파트너에게만 부여 가능
- 공유 권한: `VIEW` (조회만) / `FULL` (입출고 포함)
- 화살표는 초대 방향입니다. 관계가 맺어진 뒤로는 두 회사가 대등합니다 —
  창고 공유 판정이 양방향이고 권한은 소유자·FULL·VIEW로만 갈립니다

</details>

<details>
<summary><b>BOM 기반 생산 계획</b> — ITEM 셀프 조인 다단계 트리에서 최대 생산 가능 수량을 산출한다</summary>

<br>

`ITEM` 테이블 하나로 완성품 · 반제품 · 부품을 통합 관리합니다.  
BOM은 ITEM 간 셀프 조인으로 다단계 구조를 구성합니다.

```
스마트스피커 (PRODUCT)
├── 메인보드 (SEMI) × 1
│    ├── PCB기판 (PART) × 1
│    ├── WiFi/BT모듈 (PART) × 1
│    └── 전원IC (PART) × 4
├── 스피커유닛 (PART) × 1
├── 마이크모듈 (PART) × 2
├── 케이스(소) (PART) × 1
└── DC어댑터 (PART) × 1
```

- BOM 트리 전체 탐색 → 부품별 필요 수량 계산 → 창고 재고와 비교 → **최대 생산 가능 수량 자동 산출**
- 순환 참조 방지 (DFS 검사)
- 인접 리스트를 한 번에 로드한 뒤 인메모리 DFS로 탐색 (탐색 중 추가 쿼리 없음)
- 서브트리 탐색 결과를 재활용하여 중복 순회 방지

</details>

<details>
<summary><b>생산 기록 &amp; 자정 결산 배치</b> — Spring Batch가 전날 PENDING을 일괄 결산한다</summary>

<br>

```
생산 기록 등록 (PENDING)
    │
    ├─ 결산 전 → CANCELLED 처리 가능
    ├─ 강제 결산 → 즉시 Settlement 생성
    │
    └─ 자정 배치 (Spring Batch, 00:05)
         │  전날 PENDING 레코드 일괄 처리
         ├─ BOM 기반 부품 재고 차감
         ├─ InventoryHistory (PRODUCTION_DEDUCTION) 기록
         ├─ Settlement 생성 — SUCCESS / ANOMALY
         └─ ProductionRecord.status → SETTLED
```

레코드별 독립 트랜잭션 (`REQUIRES_NEW`). 한 건 실패가 다른 결산을 롤백하지 않음.  
BOM 부품 재고는 단일 IN 쿼리로 일괄 조회하여 N+1 방지.

`Settlement`를 별도 엔티티로 분리한 이유: **결산 로직을 생산 기록 없이 독립 단위 테스트로 검증**하기 위해.  
부품 재고 부족 시 `anomalyDetail`에 품목별 필요량 · 현재 재고를 JSON으로 기록합니다.

</details>

## 테스트 전략

TDD로 개발. 총 **396개 테스트** — 백엔드 327 · 프론트 단위 51 · E2E 18.
백엔드는 JaCoCo 기준 **라인 92.1% / 브랜치 84.3%**

<details>
<summary><b>레이어별 테스트 구성과 커버리지 기준</b></summary>

<br>

| 레이어 | 도구 | 검증 대상 |
|--------|------|----------|
| Service 단위 | Mockito | 결산 SUCCESS/ANOMALY, 권한 검증, BOM 탐색, 재고 차감 |
| Controller 슬라이스 | @WebMvcTest | 엔드포인트 응답 코드, 인증 필터, JSON 직렬화 |
| Repository 슬라이스 | @DataJpaTest | BOM 다단계 조회, 재고 집계 쿼리 |
| 배치 통합 | @SpringBootTest | PENDING → SETTLED 전체 플로우, ANOMALY 처리 |
| Security | Spring Security Test | JWT 필터 분기, 미인증 401, 권한 없음 403 |
| 동시성 | 실제 PostgreSQL | 비관적 락, 동시 출고 시 lost update |
| 프론트 단위 | Vitest | 날짜 변환, 조회 실패 판정, 캐시 키, 비밀번호 정책 |
| 화면 흐름 | Playwright | 로그인·권한·공유 창고·초대 수락을 브라우저로 |

```bash
./gradlew test jacocoTestReport   # 백엔드 + 커버리지 리포트
npm test                          # 프론트 단위
npm run e2e                       # 화면 흐름 (백엔드 실행 필요)
```

프론트 테스트는 화면 개수가 아니라 **실제로 틀렸던 곳**을 기준으로 골랐습니다.
`toISOString()`이 UTC라 날짜가 밀리던 것, 조회 실패가 "데이터 없음"으로 보이던 것,
캐시 키에 `size`가 빠져 목록이 서로 덮어쓰던 것 — 전부 [문제 해결](docs/problem-solving.md)에
기록된 결함입니다.

커버리지 수치는 DTO · 엔티티 · 설정 클래스를 제외한 값입니다. 롬복이 생성하는 게터·빌더가
포함되면 수치는 올라가지만 실제 로직의 검증 정도를 나타내지 못하기 때문입니다.
`jacocoTestCoverageVerification`에 라인 90% / 브랜치 80% 하한선을 두어 회귀를 막습니다.

세 가지 모두 CI에서 돌아갑니다. 커버리지 게이트와 린트도 같이 걸려 있어,
수치가 내려가거나 경고가 하나라도 생기면 PR이 막힙니다.

</details>

## 만들며 겪은 것

이 프로젝트에서 가장 값이 컸던 부분

- [시행착오](docs/trial-and-error.md) — 될 거라 보고 정한 것들을 확인해보니 반대인 게 여럿이었다. 무엇을 될 거라 봤고 무엇으로 갈렸는지
- [문제 해결](docs/problem-solving.md) — 겪은 장애와 버그를 문제 → 원인 → 해결

## Getting Started

<details>
<summary><b>설치와 실행 방법</b></summary>

<br>

### Prerequisites

- Docker & Docker Compose
- Java 21 (로컬 실행 시)
- Node.js 20+ (로컬 실행 시)

### Docker Compose로 전체 실행 (권장)

```bash
# 최초 실행 (이미지 빌드 + DB/Redis 포함)
docker-compose up --build -d

# 재시작 (코드 변경 없을 때)
docker-compose up -d
```

앱 기동 시 `DataInitializer`가 자동으로 데모 데이터를 삽입합니다.

| 서비스 | 주소 |
|--------|------|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080/api/v1 |
| PostgreSQL | localhost:5432 |
| Redis | localhost:6379 |

### 로컬 개발 실행

```bash
# PostgreSQL + Redis만 Docker로 실행
docker-compose up -d postgres redis

# 백엔드
cd backend
./gradlew bootRun

# 프론트엔드
cd frontend
npm install
npm run dev
```

</details>

## 프로젝트 구조

<details>
<summary><b>디렉터리 트리</b></summary>

<br>

```
IMS/
├── backend/
│   ├── src/main/java/com/ims/
│   │   ├── user/            # 회사 계정 관리, JWT 인증
│   │   ├── partnership/     # 파트너 초대 · 수락 · 관리
│   │   ├── warehouse/       # 창고 CRUD, 공유 권한(WarehouseShare)
│   │   ├── item/            # 품목 마스터 (PRODUCT · PART · SEMI)
│   │   ├── inventory/       # 창고별 실시간 재고, 입출고 이력
│   │   ├── production/      # 생산 기록, Settlement, ProductionOrder
│   │   └── global/
│   │       ├── security/    # JwtProvider, JwtAuthFilter, SecurityConfig
│   │       ├── scheduler/   # SettlementBatchConfig, SettlementJobScheduler
│   │       ├── config/      # RedisConfig, BatchConfig, DataInitializer
│   │       └── exception/   # ImsException, ErrorCode, GlobalExceptionHandler
│   └── src/test/            # 단위 · 통합 · 슬라이스 테스트 (240개 이상)
│
├── frontend/
│   └── app/
│       ├── (auth)/login/    # 로그인 페이지
│       └── (main)/
│           ├── dashboard/   # KPI, 생산 추이
│           ├── inventory/   # 재고 목록, 입출고
│           ├── production/  # 생산 기록, 결산
│           ├── partners/    # Partnership 관리
│           └── analytics/   # 분석, 안전재고 추천
│
├── docs/
│   └── API.md               # REST API 전체 명세
└── docker-compose.yml
```

</details>

## License

MIT
