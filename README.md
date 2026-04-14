# IMS - Inventory Management System

> 제조업 환경의 본사-하청 협업 재고관리 시스템

## Overview

중소 제조업체에서는 ERP를 도입해도 본사-하청 간 재고 협업은 여전히 엑셀로 관리하는 경우가 많습니다.
IMS는 이 문제에 집중하여, BOM 기반 생산 계획, 실시간 재고 추적, 자정 배치 결산을 제공합니다.

## 프로젝트 포지션

| 구분 | 껄무새 (GGeolmuse) | IMS |
|------|-------------------|-----|
| 초점 | MSA / 인프라 / 클라우드 | 도메인 설계 / TDD / 테스트 전략 |
| DB | PostgreSQL (AWS RDS) | MySQL 8.x + Redis (Cache) |
| 아키텍처 | Spring Cloud MSA, Kafka, K3s | 모놀리식 (Spring Boot) |
| 핵심 어필 | 분산 시스템 운영 능력 | 비즈니스 로직 설계 + 테스트 역량 |

## Tech Stack

| 영역 | 기술 |
|------|------|
| Backend | Java 21, Spring Boot 3.x, Spring Security, Spring Data JPA, Spring Batch |
| Database | MySQL 8.x, Redis (Cache) |
| Frontend | Next.js 14 (App Router), TypeScript, Tailwind CSS, shadcn/ui |
| Auth | JWT (Access Token 1h + Refresh Token 2주) |
| Test | JUnit 5, Mockito, Spring Boot Test (TDD) |
| Infra | Docker, Docker Compose |

## Project Structure

```
IMS/
├── backend/
│   ├── src/
│   │   ├── main/java/com/ims/
│   │   │   ├── user/                # User, SubUser 엔티티 및 관리
│   │   │   ├── warehouse/           # 창고 CRUD, 담당자 배정
│   │   │   ├── item/                # 품목 마스터 (FINISHED / PART / SEMI)
│   │   │   ├── bom/                 # 자재 명세서 (셀프 조인)
│   │   │   ├── inventory/           # 창고별 실시간 재고
│   │   │   ├── production/          # 생산 기록 + 결산
│   │   │   └── global/
│   │   │       ├── common/          # ApiResponse, Role
│   │   │       ├── security/        # JwtAuthFilter, 역할별 접근 제어
│   │   │       ├── scheduler/       # 자정 결산 배치 (00:05)
│   │   │       ├── config/
│   │   │       └── exception/
│   │   └── test/                    # 단위 / 통합 / 슬라이스 테스트
│   └── build.gradle
├── frontend/                        # Next.js (App Router)
└── docs/                            # ERD, API 명세
```

## ERD

![IMS ERD](docs/IMS_ERD.png)

> 상세 엔티티 설명: [Entity Description](docs/IMS_Entity_Description.png)

주요 테이블:

| 테이블 | 설명 |
|--------|------|
| `USER` | 본사 계정. 이메일 로그인, SUB_USER 생성 권한 |
| `SUB_USER` | 하청 유저. role: PRODUCTION \| LOGISTICS |
| `WAREHOUSE` | 창고. USER 소속, SUB_USER 담당자 배정 (nullable) |
| `ITEM` | 품목 통합. type: FINISHED \| PART \| SEMI |
| `BOM` | 자재 명세서. ITEM 셀프 조인으로 다단계 BOM 표현 |
| `INVENTORY` | 창고별 실시간 재고. 복합 UK: warehouse_id + item_id |
| `PRODUCTION_RECORD` | 생산 기록. PENDING → SETTLED \| CANCELLED |
| `SETTLEMENT` | 결산 결과. SUCCESS \| PARTIAL \| FAILED |

## Core Features

### 1. 권한 체계

USER(본사)가 SUB_USER(하청)를 직접 생성하고 관리합니다.
SUB_USER의 role에 따라 접근 가능한 기능이 제한됩니다.

| 역할 | 접근 범위 |
|------|----------|
| USER (본사) | 전체 조회, 마스터 데이터 관리, 하청 계정 관리 |
| SUB_USER PRODUCTION | 담당 창고 재고 조회 + 생산 기록 |
| SUB_USER LOGISTICS | 담당 창고 완성품 입출고만 |

### 2. BOM 기반 생산 계획

`ITEM` 테이블 하나로 완성품(FINISHED), 부품(PART), 반제품(SEMI)을 통합 관리합니다.
`BOM`이 ITEM 간 셀프 조인으로 다단계 구조를 표현합니다.

```
ITEM (FINISHED) ──< BOM >── ITEM (PART / SEMI)
                parent            child
```

반제품(SEMI)은 상위 완성품의 하위 품목이면서 동시에 다른 부품의 상위가 될 수 있어
**다단계 BOM 트리 탐색**이 필요합니다. 탐색 결과와 창고 재고를 비교해 최대 생산 가능 수량을 자동 계산합니다.

### 3. 생산 기록 & 자정 결산

현장에서 생산하면 `PRODUCTION_RECORD`가 `PENDING` 상태로 쌓입니다.
자정 배치(Spring Batch, 00:05)가 전날 PENDING 레코드를 일괄 처리합니다.

```
생산 기록 (PENDING)
    ├─ 결산 전: CANCELLED 처리 가능
    └─ 자정 배치
         ├─ BOM 기반 부품 재고 일괄 차감
         ├─ SETTLEMENT 생성 (SUCCESS / PARTIAL / FAILED)
         └─ PRODUCTION_RECORD.status → SETTLED
```

`SETTLEMENT`를 별도 엔티티로 분리한 이유: **결산 로직을 생산 기록과 독립적으로 테스트하기 위해**.
성공/부분실패/전체실패 케이스를 `PRODUCTION_RECORD` fixture 없이 단위 테스트로 검증할 수 있습니다.

`PARTIAL`은 일부 부품 재고가 부족한 경우로, `failure_reason`에 부족한 부품과 수량을 기록합니다.

### 4. 창고별 재고 관리

`INVENTORY`는 `warehouse_id + item_id` 복합 유니크 키로 창고별 품목 재고를 관리합니다.
`safety_stock` 기준 이하 시 부족 경고를 트리거합니다.

## 테스트 전략

| 레이어 | 테스트 종류 | 검증 대상 |
|--------|-----------|----------|
| Domain | 단위 테스트 | BOM 트리 탐색, 최대 생산량 계산, 재고 차감 로직 |
| Service | 단위 테스트 (Mockito) | 결산 배치 로직 (SUCCESS / PARTIAL / FAILED), 권한 검증 |
| Batch | 통합 테스트 | 자정 결산 전체 플로우 (PENDING → SETTLED) |
| API | 슬라이스 테스트 (@WebMvcTest) | Controller + Security 필터 |
| Repository | 슬라이스 테스트 (@DataJpaTest) | BOM 다단계 조회, 재고 집계 쿼리 |

## 확장 가능성

- **발주 자동화** - 안전 재고 이하 시 본사에 발주 요청 자동 생성
- **대시보드** - 월별 생산 추이, 부품 소모율, 결산 실패율 통계
- **멀티 테넌시** - 여러 본사가 하나의 시스템을 사용하는 SaaS 구조

## Getting Started

### Prerequisites

- Java 17
- Node.js 20+
- Docker & Docker Compose

### Run with Docker Compose

```bash
docker-compose up -d
```

### Backend

```bash
cd backend
./gradlew bootRun
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

## License

MIT
