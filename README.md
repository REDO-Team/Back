# ♻️ ReDO Backend Repository

> 분리배출은 쉽게, 실천은 보상으로 이어지도록 사용자 경험의 흐름을 안정적으로 뒷받침하는 ReDO Backend입니다.

---

## ✨ 프로젝트 소개

ReDO는 분리배출 정보를 쉽고 빠르게 제공하고,
실천에 대한 보상을 통해 사용자의 행동을 유도하는 서비스입니다.

- **기간** : 2026.06.29 ~ 2026.08.21
- **인원** : Backend 5명
- **목표** : AI 기반 정보 제공과 리워드 시스템을 결합하여 분리배출을 일상의 습관으로 만들어 지속적인 환경 보호 참여를 이끌어내는 것
- **핵심 흐름** : AI 분리배출 정보 → AI 분리배출 인증 → 포인트 적립 → 리워드 구매 → 기여도 시각화

---

## 🧑🏻‍💻 팀원 및 역할

| [천진하](https://github.com/jinha1665) | [안동휘](https://github.com/telling7895-eng) | [채민수](https://github.com/miinsoo) | [허건우](https://github.com/woo6629058) | [이정우](https://github.com/Lee-Jungwoo) |
|:---:|:---:|:---:|:---:|:---:|
| ![천진하 프로필](https://avatars.githubusercontent.com/u/128897741?v=4) | ![안동휘 프로필](https://avatars.githubusercontent.com/u/262762536?v=4) | ![채민수 프로필](https://avatars.githubusercontent.com/u/187004014?v=4) | ![허건우 프로필](https://avatars.githubusercontent.com/u/156512340?v=4) | ![이정우 프로필](https://avatars.githubusercontent.com/u/66958341?v=4) |
| 회원가입·로그인·프로필 | AI 분리배출 정보 | AI 분리배출 인증 | 인프라·포인트·리워드·기여도 | 커뮤니티 |

---

## ✨ 주요 기능

### 회원가입·로그인·프로필

- 아이디 중복 확인과 이메일 인증을 통한 일반 회원가입
- 서비스 약관 조회 및 필수 약관 동의 처리
- Google, Kakao, Naver 소셜 회원가입 및 로그인
- JWT 기반 Access Token 발급과 Refresh Token을 이용한 토큰 재발급
- HttpOnly Cookie와 Redis를 활용한 Refresh Token 관리 및 로그아웃
- Redis 원자 연산으로 로그인 실패 횟수를 관리하여 반복적인 로그인 시도 제한
- 사용자 프로필 생성 및 조회
- 닉네임, 프로필 이미지, 캐릭터 수정
- 회원 탈퇴 사유 조회 및 회원 탈퇴 처리

### AI 분리배출 정보

- 품목명을 이용한 분리배출 정보 조회
- 촬영하거나 업로드한 이미지를 AI가 분석하여 품목 식별
- 사용자가 입력한 문제 상황을 분석하여 적합한 분리배출 정보 제공
- Spring AI와 Gemini를 활용한 이미지·텍스트 기반 정보 탐색
- JSON Schema 기반의 구조화된 AI 응답 검증
- AI 분석 결과를 서비스에 등록된 품목 및 분리배출 정보와 연결
- 품목별 배출 방법과 단계별 주의사항 제공
- 자주 확인하는 분리배출 정보 즐겨찾기
- 탐색 결과에서 해당 품목의 배출 인증 기능으로 연결

### AI 분리배출 인증

- 당일 인증 성공 횟수와 남은 인증 가능 횟수 조회
- 일반 인증과 분리배출 정보 탐색 후 인증 지원
- 실시간으로 촬영한 배출 완료 이미지를 Gemini VLM으로 판정
- 인증 성공 여부와 인증 품목, 판정 결과 및 인증 일시 제공
- 인증 방식에 따른 포인트 자동 적립
  - 일반 인증: 50P
  - 정보 탐색 후 인증: 100P
- 일일 최대 3회 및 동일 품목 중복 인증 제한
- 인증 성공 후 5분간 신규 인증 제한
- AI 판정 실패 사유와 재촬영 방법 제공
- 실패한 인증 이미지의 재촬영 및 AI 재검수
- 인증 전용 비동기 실행기와 타임아웃을 적용하여 장시간 AI 요청 제어
- 인증 상태 변경, AI 판정 저장, 포인트 적립 및 기여도 이벤트를 하나의 트랜잭션으로 처리

### 포인트·리워드

- 사용자의 보유 포인트와 이번 달 적립 포인트 조회
- 포인트 적립 및 사용 거래 내역 조회
- 제휴 브랜드 배송 상품과 쿠폰·기프티콘 상품 제공
- 상품 유형별 목록, 상세 정보 및 사용자별 일일 추천 상품 조회
- 주소기반산업지원서비스 API를 이용한 배송지 주소 검색
- 배송지 생성, 조회, 수정 및 삭제
- 포인트를 사용한 리워드 구매
- 리워드 교환 내역과 배송·쿠폰 처리 상태 조회
- `Idempotency-Key`와 DB 유니크 제약을 활용한 동일 구매 요청의 중복 처리 방지
- 상품 단위 Redisson 분산 락으로 동일 상품의 동시 구매 요청 제어
- 사용자와 상품 데이터에 DB 비관적 락을 적용하여 포인트와 재고 정합성 보장

### 기여도

- 사용자의 누적 분리배출 인증 횟수 조회
- 인증 횟수에 따른 8단계 환경 기여 마일스톤 제공
- 현재 달성한 결과물과 다음 목표까지 남은 인증 횟수 표시
- 전체 서비스 참여자 수 조회
- 다른 사용자의 최근 분리배출 인증 활동을 피드 형태로 제공
- 커서 기반 페이지네이션을 활용한 기여도 피드 이어보기
- Cache-Aside 방식으로 기여도 이벤트와 참여자 수를 Redis에 캐싱
- 데이터 특성에 따라 서로 다른 TTL을 적용하여 캐시 최신성과 조회 성능 관리
- Redis 장애 시 데이터베이스 조회로 전환하여 기여도 기능 유지

### 커뮤니티

- 정보 공유, 환경 실천, 리워드 후기 카테고리별 게시글 조회
- 게시글 목록 및 상세 정보 조회
- 제목, 내용 및 여러 장의 이미지를 포함한 게시글 등록
- 내가 작성한 게시글 수정 및 삭제
- 게시글별 댓글 조회, 등록, 수정 및 삭제
- 게시글 좋아요 및 좋아요 취소
- 내가 작성한 게시글과 댓글 모아보기
- 게시글 최신순 페이지네이션과 댓글 커서 페이지네이션
- 게시글별 댓글 수와 대표 이미지를 일괄 조회하여 N+1 문제 방지
- 커뮤니티 이미지를 AWS S3에 저장하고 다중 업로드 실패 시 보상 삭제 처리

---

## 🧩 핵심 기술 및 설계

### AI 요청 격리와 응답 신뢰성 확보 (AI)

- **문제**
  - Gemini API는 네트워크와 모델 상태에 따라 응답이 지연되어 애플리케이션 요청 처리 자원을 장시간 점유할 수 있었습니다.
  - AI가 서비스에 존재하지 않는 품목을 생성하거나 약속된 JSON 형식과 다른 응답을 반환할 가능성이 있었습니다.
- **해결**
  - AI 호출 전용 Executor와 배출 인증 전용 Thread Pool을 분리하여 일반 API 요청과 AI 판정 작업의 실행 자원을 격리했습니다.
  - 호출 제한 시간과 인증 전체 제한 시간을 별도로 설정하고, 시간 초과와 Provider 오류를 도메인 예외로 변환했습니다.
  - 서비스 DB에 등록된 품목 목록을 프롬프트에 제공하여 해당 목록에서만 결과를 선택하도록 제한했습니다.
  - 적합한 품목이 없으면 `NOT_FOUND`를 반환하도록 하고, JSON Schema를 적용하여 응답 형식을 검증했습니다.
- **효과**
  - 외부 AI 서비스의 응답 지연이 애플리케이션 전체로 확산되는 범위를 줄였습니다.
  - AI 할루시네이션과 비정형 응답을 제어하여 서비스 데이터와 일치하는 결과만 제공할 수 있게 되었습니다.

### 리워드 구매 동시성 제어와 멱등성 확보 (동시성)

- **문제**
  - 동일 상품에 구매 요청이 동시에 들어오면 상품 재고가 음수가 되거나 포인트가 중복 차감될 수 있었습니다.
  - 네트워크 지연이나 사용자 재요청으로 동일한 구매 요청이 여러 번 처리될 가능성이 있었습니다.
- **해결**
  - 상품 ID 단위의 Redisson 분산 락을 적용하여 동일 상품에 대한 구매 요청을 순차적으로 처리했습니다.
  - 트랜잭션 내부에서 사용자와 상품에 DB 비관적 락을 적용하고 포인트, 상품 상태 및 재고를 다시 검증했습니다.
  - 요청별 `Idempotency-Key`를 받고 사용자 ID와 멱등성 키 조합에 DB 유니크 제약을 적용했습니다.
- **효과**
  - 다중 인스턴스 환경에서도 상품 재고와 사용자 포인트의 정합성을 유지했습니다.
  - 동일 요청의 중복 처리와 서로 다른 상품의 동시 구매로 발생할 수 있는 포인트 중복 사용을 방지했습니다.

### Cache-Aside 기반 기여도 피드 캐싱과 Redis 장애 대응 (캐싱)

- **문제**
  - 기여도 피드 조회마다 이벤트와 사용자 프로필 및 전체 참여자 수를 반복 조회하여 DB 부하가 증가할 수 있었습니다.
  - Redis 장애가 기여도 기능 전체의 장애로 이어지지 않도록 별도의 실패 경로가 필요했습니다.
- **해결**
  - Cache-Aside 방식으로 기여도 이벤트를 캐싱하고, 캐시에 없는 이벤트만 DB에서 일괄 조회한 뒤 다시 저장했습니다.
  - 기여도 이벤트에는 30분, 변경 가능성이 높은 전체 참여자 수에는 1분의 TTL을 적용했습니다.
  - 피드에 필요한 사용자 프로필을 사용자 ID 목록으로 일괄 조회했습니다.
  - Redis 조회 또는 저장에 실패하면 DB에서 데이터를 조회하도록 Fail-Open 구조를 적용했습니다.
- **효과**
  - 기여도 데이터와 사용자 프로필의 반복 조회를 줄여 DB 접근량을 감소시켰습니다.
  - Redis 장애가 발생하더라도 DB 조회를 통해 기여도 기능을 계속 제공할 수 있게 되었습니다.

### 커뮤니티 목록 조회 N+1 문제 해결 (쿼리 최적화)

- **문제**
  - 게시글별 댓글 수와 대표 이미지를 조회하기 위해 게시글마다 추가 쿼리가 실행되었습니다.
  - 게시글이 N개일 때 `1 + 2N`개의 쿼리가 발생하여 10개 게시글 조회 시 총 21개의 쿼리가 필요했습니다.
- **해결**
  - 먼저 페이지에 표시할 게시글 목록을 확정한 뒤 해당 게시글의 댓글 수와 이미지를 각각 일괄 조회했습니다.
  - 댓글 수는 `GROUP BY community_id` 집계 결과를 Projection으로 조회하여 게시글별 Map으로 구성했습니다.
  - 이미지는 `displayOrder` 순으로 조회하고 게시글별 가장 앞선 이미지를 대표 이미지로 선택했습니다.
- **효과**
  - 페이지 크기와 관계없이 커뮤니티 목록 조회 쿼리를 3개로 고정했습니다.
  - 게시글 수가 증가하더라도 추가 쿼리가 비례하여 증가하지 않도록 개선했습니다.

### S3와 DB 사이의 데이터 정합성 확보 (데이터 정합성)

- **문제**
  - S3와 DB는 하나의 트랜잭션으로 묶을 수 없어 다중 이미지 업로드의 일부만 성공하면 고아 객체가 남을 수 있었습니다.
  - 프로필 이미지를 변경할 때 기존 이미지를 먼저 삭제하면 이후 DB 저장 실패 시 사용자 이미지가 유실될 수 있었습니다.
- **해결**
  - 업로드에 성공한 S3 객체 키를 추적하고, 이후 처리에서 오류가 발생하면 먼저 업로드된 객체를 삭제하는 보상 로직을 적용했습니다.
  - 프로필 이미지는 `신규 이미지 업로드 → DB 반영 및 URL 생성 → 기존 이미지 삭제` 순서로 처리했습니다.
  - DB 반영에 실패하면 새로 업로드한 이미지를 정리하고, 보상 처리 실패 원인을 로그로 남기도록 구성했습니다.
- **효과**
  - 다중 이미지 업로드 과정에서 발생할 수 있는 고아 객체를 줄였습니다.
  - 프로필 이미지 변경 실패 시 기존 이미지를 유지하여 사용자 데이터 유실 가능성을 낮췄습니다.

### 통합 모니터링 구축과 로그 수집 보안 강화 (운영 인프라)

- **문제**
  - 서버에 직접 접속하기 전까지 애플리케이션 장애와 EC2·Redis 자원 부족을 빠르게 파악하기 어려웠습니다.
  - 컨테이너 로그 수집을 위한 Docker Socket 권한 노출과 EC2 간 로그 전송 구간의 보안을 고려해야 했습니다.
  - 일시적인 지표 누락으로 동일한 Grafana 장애 알림이 반복되는 문제도 있었습니다.
- **해결**
  - Prometheus로 Spring Boot 애플리케이션, EC2 및 Redis 메트릭을 통합 수집했습니다.
  - Grafana Alloy, Loki, Grafana를 이용해 컨테이너 로그 수집과 운영 대시보드를 구축했습니다.
  - Docker Socket Proxy를 적용하여 Alloy에 필요한 조회 권한만 제한적으로 제공했습니다.
  - Service EC2와 Monitoring EC2 사이의 로그 전송에 TLS와 Basic Auth를 적용했습니다.
  - Grafana의 No Data 처리, 평가 대기시간 및 알림 정책을 조정하고 장애·복구 알림을 Discord로 전송했습니다.
- **효과**
  - 애플리케이션 로그와 인프라 메트릭을 하나의 대시보드에서 확인할 수 있게 되었습니다.
  - Docker Socket과 로그 전송 경로의 보안을 강화하고 장애 알림의 정확도와 신뢰도를 높였습니다.

---

## ⚙️ 기술 스택

### Backend

<div>
  <img src="https://img.shields.io/badge/Java 17-007396?style=flat-square&logo=openjdk&logoColor=white">
  <img src="https://img.shields.io/badge/Spring Boot 3.5.16-6DB33F?style=flat-square&logo=springboot&logoColor=white">
  <img src="https://img.shields.io/badge/Spring Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white">
  <img src="https://img.shields.io/badge/Spring Data JPA-59666C?style=flat-square&logo=hibernate&logoColor=white">
  <img src="https://img.shields.io/badge/Spring AI-6DB33F?style=flat-square&logo=spring&logoColor=white">
  <img src="https://img.shields.io/badge/Gradle-02303A?style=flat-square&logo=gradle&logoColor=white">
</div>

### Database & Cache

<div>
  <img src="https://img.shields.io/badge/MySQL-4479A1?style=flat-square&logo=mysql&logoColor=white">
  <img src="https://img.shields.io/badge/Amazon RDS-527FFF?style=flat-square&logo=amazonrds&logoColor=white">
  <img src="https://img.shields.io/badge/Redis-FF4438?style=flat-square&logo=redis&logoColor=white">
  <img src="https://img.shields.io/badge/Redisson-B82025?style=flat-square&logo=redis&logoColor=white">
</div>

### AI & External API

<div>
  <img src="https://img.shields.io/badge/Google Gemini-8E75B2?style=flat-square&logo=googlegemini&logoColor=white">
  <img src="https://img.shields.io/badge/Google OAuth-4285F4?style=flat-square&logo=google&logoColor=white">
  <img src="https://img.shields.io/badge/Kakao OAuth-FFCD00?style=flat-square&logo=kakao&logoColor=black">
  <img src="https://img.shields.io/badge/Naver OAuth-03C75A?style=flat-square&logo=naver&logoColor=white">
  <img src="https://img.shields.io/badge/주소기반산업지원서비스 API-0054A6?style=flat-square">
</div>

### Infrastructure & CI/CD

<div>
  <img src="https://img.shields.io/badge/AWS EC2-FF9900?style=flat-square&logo=amazonec2&logoColor=white">
  <img src="https://img.shields.io/badge/AWS S3-569A31?style=flat-square&logo=amazons3&logoColor=white">
  <img src="https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white">
  <img src="https://img.shields.io/badge/Docker Compose-2496ED?style=flat-square&logo=docker&logoColor=white">
  <img src="https://img.shields.io/badge/GitHub Actions-2088FF?style=flat-square&logo=githubactions&logoColor=white">
  <img src="https://img.shields.io/badge/GitHub Container Registry-181717?style=flat-square&logo=github&logoColor=white">
</div>

### Monitoring & Test

<div>
  <img src="https://img.shields.io/badge/Prometheus-E6522C?style=flat-square&logo=prometheus&logoColor=white">
  <img src="https://img.shields.io/badge/Grafana-F46800?style=flat-square&logo=grafana&logoColor=white">
  <img src="https://img.shields.io/badge/Loki-F46800?style=flat-square&logo=grafana&logoColor=white">
  <img src="https://img.shields.io/badge/Grafana Alloy-F46800?style=flat-square&logo=grafana&logoColor=white">
  <img src="https://img.shields.io/badge/k6-7D64FF?style=flat-square&logo=k6&logoColor=white">
  <img src="https://img.shields.io/badge/JUnit 5-25A162?style=flat-square&logo=junit5&logoColor=white">
  <img src="https://img.shields.io/badge/Testcontainers-2496ED?style=flat-square&logo=docker&logoColor=white">
</div>

### API Documentation

<div>
  <img src="https://img.shields.io/badge/Swagger-85EA2D?style=flat-square&logo=swagger&logoColor=black">
  <img src="https://img.shields.io/badge/OpenAPI-6BA539?style=flat-square&logo=openapiinitiative&logoColor=white">
</div>

---

## 🏗 시스템 아키텍처

<div align="center">
  <img
    width="100%"
    alt="ReDO 인프라 아키텍처"
    src="https://github.com/user-attachments/assets/74a3a09d-2abc-435e-a18e-336c1588650d?"
  />
</div>

---

## 🗄 ERD

<div align="center">
  <img
    width="100%"
    alt="ReDO ERD"
    src="https://github.com/user-attachments/assets/696c428c-96ab-4f11-b658-674b21e5ce95"
  />
</div>

---

## 🔌 API 구성

| 도메인 | Base Path | 주요 기능 |
| --- | --- | --- |
| 인증 | `/api/auth` | 회원가입, 로그인, 소셜 로그인, 토큰 재발급, 로그아웃 |
| 사용자 | `/api/users` | 프로필 관리, 회원 탈퇴 |
| 약관 | `/api/terms` | 서비스 약관 조회 |
| 분리배출 정보 | `/api/guides` | 가이드 조회, AI 검색, 즐겨찾기 |
| 분리배출 인증 | `/api/certification` | 인증 정책 조회, 신규 인증, 재촬영 |
| 포인트 | `/api/rewards/points` | 보유 포인트 및 거래 내역 조회 |
| 리워드 상품 | `/api/rewards/products` | 상품 목록, 상세, 미리보기 |
| 리워드 교환 | `/api/rewards/redemptions` | 상품 교환 및 교환 내역 조회 |
| 배송지 | `/api/shipping-addresses` | 주소 검색 및 배송지 관리 |
| 기여도 | `/api/contributions` | 나의 기여도, 전체 기여도 피드 |
| 커뮤니티 | `/api/community` | 게시글, 댓글, 좋아요 |

전체 API 명세와 요청·응답 예시는 Swagger에서 확인할 수 있습니다.

- **Swagger UI**: [https://redo-backend.site/swagger-ui/index.html#/](https://redo-backend.site/swagger-ui/index.html#/)
- **로컬 Swagger UI**: `http://localhost:8080/swagger-ui/index.html`

---

## 📁 프로젝트 구조

```text
Back/
├── .github/
│   ├── ISSUE_TEMPLATE/
│   ├── workflows/
│   │   └── ci-cd.yml
│   ├── CONTRIBUTING.md
│   └── PULL_REQUEST_TEMPLATE.md
├── gradle/
├── k6/
│   ├── config/                 # 실행 프로필 및 성능 기준
│   ├── lib/                    # 인증, HTTP 요청, 안전장치
│   └── scenarios/              # 도메인별 부하 테스트
├── monitoring/
│   ├── alloy/                  # 컨테이너 로그 수집
│   ├── grafana/                # 대시보드 및 알림 프로비저닝
│   ├── loki/                   # 로그 저장소
│   ├── loki-proxy/             # Loki TLS·인증 프록시
│   └── prometheus/             # 메트릭 수집 설정
├── src/
│   ├── main/
│   │   ├── java/com/redo/
│   │   │   ├── domain/
│   │   │   │   ├── certification/
│   │   │   │   ├── community/
│   │   │   │   ├── contribution/
│   │   │   │   ├── point/
│   │   │   │   ├── recycleGuide/
│   │   │   │   ├── reward/
│   │   │   │   ├── term/
│   │   │   │   └── user/
│   │   │   └── global/
│   │   │       ├── ai/
│   │   │       ├── apiPayload/
│   │   │       ├── config/
│   │   │       ├── redis/
│   │   │       ├── s3/
│   │   │       ├── security/
│   │   │       └── util/
│   │   └── resources/
│   │       ├── application.yaml
│   │       └── application-certification.yaml
│   └── test/
├── .env.example
├── build.gradle
├── compose.yaml
├── Dockerfile
├── gradlew
└── settings.gradle
```

---

## 🚀 로컬 실행 방법

### 요구사항

- Java 17
- MySQL
- Redis
- AWS S3 Bucket
- Gmail SMTP 계정 및 App Password
- Gemini API Key (AI 기능 사용 시)
- 도로명주소 API Key (배송지 주소 검색 기능 사용 시)

AI 기능을 사용하지 않는 경우 `GEMINI_ENABLED=false`로 실행할 수 있습니다. 이 경우 이미지·텍스트 기반 분리배출 정보 탐색과 AI 배출 인증 기능은 사용할 수 없습니다.

### 1. 저장소 복제

```bash
git clone https://github.com/REDO-Team/Back.git
cd Back
```

### 2. 환경변수 설정

```bash
cp .env.example .env
```

주요 환경변수는 다음과 같습니다.

| 환경변수 | 설명 |
| --- | --- |
| `DB_URL` | MySQL JDBC URL |
| `DB_USERNAME` | MySQL 사용자명 |
| `DB_PASSWORD` | MySQL 비밀번호 |
| `JWT_SECRET` | JWT 서명 Secret |
| `MAIL_USERNAME` | 이메일 인증 발송 계정 |
| `MAIL_PASSWORD` | Gmail App Password |
| `S3_BUCKET` | 이미지 저장 S3 Bucket |
| `AWS_REGION` | AWS Region |
| `JUSO_API_KEY` | 도로명주소 API 승인 키 |
| `APP_BASE_URL` | CORS와 Swagger에 사용할 서비스 URL |
| `GEMINI_ENABLED` | Gemini 기능 활성화 여부 |
| `GEMINI_API_KEY` | Gemini API Key |
| `GEMINI_MODEL` | 사용할 Gemini 모델 |
| `GEMINI_TIMEOUT` | 단일 Gemini 호출 제한 시간 |

AWS 인증 정보는 로컬 AWS Profile 또는 EC2 IAM Role 등 AWS SDK 기본 인증 체인을 통해 제공합니다.

### 3. 환경변수 적용 및 실행

```bash
set -a
source .env
set +a

./gradlew bootRun
```

애플리케이션은 기본적으로 다음 포트를 사용합니다.

| 포트 | 용도 |
| --- | --- |
| `8080` | Backend API |
| `9101` | Actuator Health 및 Prometheus Metrics |
| `6379` | Redis |

---

## 🧪 테스트

일반 테스트는 외부 Gemini API를 호출하지 않습니다.

```bash
./gradlew test
```

실제 Gemini API 연동 테스트는 별도 Gradle Task로 분리되어 있습니다.

```bash
./gradlew geminiIntegrationTest
```

Gemini 통합 테스트를 실행하려면 유효한 API Key와 관련 환경변수가 필요합니다.

### 부하 테스트

k6 테스트는 Docker 기반으로 실행하여 팀원이 동일한 버전과 성능 기준을 사용할 수 있도록 구성했습니다.

```bash
cp k6/.env.example k6/.env
./k6/run.sh scenarios/examples/authenticated-read.js
```

제공하는 실행 프로필은 다음과 같습니다.

| 프로필 | 용도 |
| --- | --- |
| `smoke` | API와 시나리오 정상 동작 확인 |
| `load` | 예상되는 일반 부하에서 성능 측정 |
| `stress` | 시스템 한계와 병목 확인 |

공통 성능 기준은 HTTP 오류율 1% 미만, p95 500ms 미만, p99 1,000ms 미만입니다.

자세한 구성과 운영 방법은 [k6 부하 테스트 운영 가이드](./k6/README.md)를 참고합니다.

---

## 🔄 CI/CD

`develop` 브랜치에 코드가 반영되면 GitHub Actions를 통해 자동 배포가 진행됩니다.

1. GitHub Actions가 Docker 이미지를 빌드
2. 이미지를 GitHub Container Registry에 Push
3. Self-hosted Runner가 배포 서버의 소스를 갱신
4. Docker Compose가 새 이미지를 Pull
5. 애플리케이션과 Redis 등 운영 컨테이너 재기동
6. Swagger UI 응답을 통한 배포 상태 확인
7. 일정 기간이 지난 미사용 이미지 정리

---

## 📊 모니터링

Spring Boot Actuator와 Micrometer를 통해 애플리케이션 메트릭을 수집합니다.

- Prometheus: 애플리케이션, EC2, Redis 메트릭 수집
- Grafana: 운영 현황 및 부하 테스트 대시보드
- Loki: 애플리케이션과 컨테이너 로그 저장
- Grafana Alloy: Docker 컨테이너 로그 수집 및 전송
- Node Exporter: EC2 CPU, Memory, Disk 메트릭
- Redis Exporter: Redis 상태와 메모리 메트릭
- Discord: 장애 발생 및 복구 알림

운영 대시보드는 다음 항목을 제공합니다.

- 애플리케이션 요청 처리량과 응답 시간
- HTTP 오류율
- JVM Heap과 Thread 상태
- EC2 CPU, Memory, Disk 사용량
- Redis 메모리와 연결 상태
- k6 테스트별 p95·p99 응답 시간
- 애플리케이션 및 인프라 장애 알림

자세한 구성과 운영 방법은 [모니터링 운영 가이드](./monitoring/README.md)를 참고합니다.

---

## 🤝 협업 규칙

- 기본 통합 브랜치: `develop`
- 이슈 단위 브랜치 및 Pull Request 생성
- CodeRabbit을 활용한 코드 리뷰
- 공통 API 응답과 예외 처리 규칙 적용

자세한 협업 규칙은 [.github/CONTRIBUTING.md](./.github/CONTRIBUTING.md)를 참고합니다.

---

<div align="center">

**올바른 분리배출이 일상의 습관이 되도록, ReDO ♻️**

</div>
