# IMS - Inventory Management System

> 제조업 환경의 본사-하청 협업 재고관리 시스템

## Overview

본사(MAIN)와 하청업체(SUB)가 협업하는 제조 환경에서 부품 재고와 완성품 생산을 효율적으로 관리하는 시스템입니다.
엑셀 기반 수작업 관리에서 벗어나 실시간 재고 현황, BOM 기반 생산 계획, 자동 결산을 제공합니다.

## Tech Stack

| 영역 | 기술 |
|------|------|
| Backend | Java 21, Spring Boot 3.x, Spring Security, Spring Data JPA |
| Database | MySQL 8.x |
| Frontend | Next.js 14 (App Router), TypeScript, Tailwind CSS, shadcn/ui |
| Auth | JWT (Access Token + Refresh Token) |
| Infra | Docker, Docker Compose |

## Project Structure

```
IMS/
├── backend/          # Spring Boot
├── frontend/         # Next.js
├── docs/             # 설계 문서
└── docker-compose.yml
```

## Getting Started

### Prerequisites

- Java 21
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

## Features

- **IAM 기반 권한 관리** - MAIN(본사) / SUB(하청) 역할 분리
- **BOM 기반 생산 계획** - 최대 생산 가능 수량 자동 계산
- **생산 기록 & 자정 결산** - 당일 기록 후 자정에 일괄 재고 차감
- **선택적 백업/복구** - 제품별 핫 백업, 7일 보관
- **실시간 재고 현황** - 창고별 재고, 부족 경고
- **감사 로그** - 모든 입출고 이력 추적

## Roles

| 역할 | 설명 |
|------|------|
| MAIN | 본사. 전체 조회, 마스터 데이터 관리, 멤버 관리 |
| SUB_PRODUCTION | 생산 하청. 담당 창고 재고 + 생산 계획/기록 |
| SUB_LOGISTICS | 물류 하청. 담당 창고 완성품 입출고만 |

## License

MIT
# IMS
