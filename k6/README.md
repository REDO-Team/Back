# ReDO k6 부하 테스트 운영 가이드

팀원이 담당 도메인의 시나리오를 추가하고 동일한 실행 환경과 성능 기준으로 API를 검증하기 위한 공통 k6 구성입니다.

## 1. 구성 개요

```text
k6/run.sh
  └─ Docker Compose
      └─ k6
          ├─ 시나리오 실행
          ├─ 로그인 또는 Access Token 인증
          ├─ API 응답·성능 기준 검증
          └─ 실행 결과 출력
              ├─ local      : 터미널 요약
              └─ prometheus : Prometheus Remote Write -> Grafana
```

테스트는 Docker 컨테이너에서 실행되므로 팀원의 로컬 k6 설치 여부와 관계없이 동일한 버전을 사용합니다. 공통 인증, HTTP 요청, 실행 프로필, Threshold와 안전장치는 재사용하고 도메인별 요청과 검증 조건만 각 시나리오에 작성합니다.

## 2. 디렉터리 구조

```text
k6/
├─ compose.yaml
├─ .env.example
├─ run.sh
├─ config/
│  ├─ profiles.js
│  └─ thresholds.js
├─ lib/
│  ├─ auth.js
│  ├─ client.js
│  └─ guards.js
└─ scenarios/
   ├─ template.js
   ├─ examples/
   └─ <domain>/
```

| 경로 | 역할 |
| --- | --- |
| `run.sh` | 시나리오 경로, Test ID와 출력 방식을 적용해 k6 실행 |
| `config/profiles.js` | `smoke`, `load`, `stress` 실행 프로필 정의 |
| `config/thresholds.js` | 공통 오류율과 응답 시간 기준 정의 |
| `lib/auth.js` | Access Token 사용 또는 로그인 처리 |
| `lib/client.js` | 공통 HTTP 요청, 태그와 응답 검증 제공 |
| `lib/guards.js` | 원격·쓰기·stress 테스트 실행 차단 |
| `scenarios/template.js` | 팀원이 새 시나리오를 만들 때 사용하는 원본 |
| `scenarios/examples/` | 공통 구성 사용 방법을 보여주는 실행 예제 |
| `scenarios/<domain>/` | 도메인별 실제 부하 테스트 시나리오 |

## 3. 실행 준비

Docker Desktop을 실행하고 프로젝트 루트에서 예시 환경 파일을 복사합니다.

```bash
cp k6/.env.example k6/.env
```

기본 테스트 대상은 로컬 `8080` 포트입니다.

```dotenv
BASE_URL=http://host.docker.internal:8080
PROFILE=smoke
K6_OUTPUT=local
```

인증 API는 테스트 계정을 입력합니다.

```dotenv
TEST_LOGIN_ID=<TEST_LOGIN_ID>
TEST_PASSWORD=<TEST_PASSWORD>
ACCESS_TOKEN=
AUTH_REQUIRED=true
```

`ACCESS_TOKEN`을 입력하면 로그인 계정보다 우선 사용합니다. 인증이 필요 없는 공개 API는 인증값을 비우고 `AUTH_REQUIRED=false`로 설정합니다.

실제 계정, 비밀번호와 토큰이 포함된 `k6/.env`는 Git에서 제외되며 커밋하지 않습니다.

## 4. 시나리오 실행

실행 명령은 다음 형식을 사용합니다.

```bash
./k6/run.sh scenarios/<domain>/<scenario>.js
```

처음에는 범용 인증 조회 예제로 실행 환경과 API 연결을 확인할 수 있습니다.

```dotenv
ENDPOINT_PATH=/api/<domain>/<resource>
ENDPOINT_NAME=<endpoint-name>
DOMAIN=<domain-name>
```

```bash
./k6/run.sh scenarios/examples/authenticated-read.js
```

실행 시 `scenario-name-실행시각` 형식의 `TEST_ID`가 자동 생성됩니다. 결과를 특정 이름으로 구분하려면 `.env` 또는 명령 실행 환경에 직접 지정할 수 있습니다.

```dotenv
TEST_ID=community-list-before-index
```

시나리오 경로 뒤의 인수는 `k6 run`에 전달됩니다.

```bash
./k6/run.sh scenarios/examples/authenticated-read.js \
  --summary-trend-stats "avg,p(95),p(99),max"
```

## 5. 실행 프로필

| 프로필 | 용도 | 기본 동작 |
| --- | --- | --- |
| `smoke` | 스크립트와 API의 정상 동작 확인 | 1 VU, 1회 |
| `load` | 예상되는 일반 부하에서 성능 측정 | 최대 20 VU까지 점진 증가 |
| `stress` | 시스템 한계와 병목 확인 | 최대 50 VU까지 점진 증가 |

실행 전 `.env`의 프로필을 변경합니다.

```dotenv
PROFILE=load
LOAD_MAX_VUS=20
```

`stress`는 팀 합의 후 다음 값을 함께 설정해야 실행됩니다.

```dotenv
PROFILE=stress
ALLOW_STRESS=true
STRESS_MAX_VUS=50
```

각 프로필의 VU 수는 `SMOKE_VUS`, `LOAD_MAX_VUS`, `STRESS_MAX_VUS`로 조정합니다.

## 6. 공통 성능 기준

공통 설정을 사용하는 모든 시나리오에는 기본적으로 다음 Threshold가 적용됩니다.

| 항목 | 기본 기준 |
| --- | --- |
| HTTP 오류율 | 1% 미만 |
| check 성공률 | 99% 초과 |
| 응답 시간 p95 | 500ms 미만 |
| 응답 시간 p99 | 1,000ms 미만 |

```dotenv
MAX_ERROR_RATE=0.01
MAX_P95_MS=500
MAX_P99_MS=1000
```

Threshold를 초과하면 k6가 0이 아닌 종료 코드를 반환합니다. API 특성상 별도 기준이 필요하면 시나리오에서 공통 Threshold를 확장합니다.

## 7. 실행 안전장치

- `localhost`, `127.0.0.1`, `host.docker.internal`, `app` 외 대상은 `ALLOW_REMOTE=true`가 필요합니다.
- POST·PUT·PATCH·DELETE 시나리오는 `assertSafeExecution({ write: true })`를 사용해야 합니다.
- 데이터가 변경되는 시나리오는 `ALLOW_WRITE=true`가 필요합니다.
- `stress` 프로필은 `ALLOW_STRESS=true`가 필요합니다.
- 운영 서버 테스트는 팀 합의 후 전용 계정과 테스트 데이터를 사용합니다.
- 쓰기 테스트는 반복 실행 전 데이터 초기화 또는 재사용 가능 여부를 확인합니다.
- 동적 ID가 포함된 URL은 `endpoint` 태그를 정규화해 Prometheus 시계열 증가를 방지합니다.

원격 환경에서 smoke 테스트를 실행할 경우에도 대상을 명시적으로 허용해야 합니다.

```dotenv
BASE_URL=https://<TEST_SERVER_DOMAIN>
PROFILE=smoke
ALLOW_REMOTE=true
```

## 8. 팀원 시나리오 추가

`k6/scenarios/template.js`를 담당 도메인 폴더로 복사합니다.

```bash
SCENARIO_DOMAIN=your-domain
SCENARIO_NAME=your-scenario

mkdir -p "k6/scenarios/${SCENARIO_DOMAIN}"
cp k6/scenarios/template.js \
  "k6/scenarios/${SCENARIO_DOMAIN}/${SCENARIO_NAME}.js"
```

새 시나리오에는 다음 내용을 작성합니다.

1. API 경로와 HTTP 메서드
2. 요청 본문과 추가 헤더
3. `domain`, `endpoint` 태그
4. HTTP 상태와 응답 본문 검증
5. 도메인 결과 또는 데이터 정합성 검증
6. 쓰기 요청일 경우 `assertSafeExecution({ write: true })`
7. 시나리오 특성에 필요한 추가 Threshold

공통 인증과 요청 처리는 `k6/lib`, 실행 프로필과 성능 기준은 `k6/config` 모듈을 재사용합니다. 시나리오 전용 환경변수가 필요하면 이름이 충돌하지 않도록 도메인 접두사를 사용해 `k6/.env.example`에 빈 예시값을 추가합니다.

## 9. Prometheus·Grafana 연동

일반 팀원은 기본값인 `K6_OUTPUT=local`로 실행하고 터미널에서 Threshold와 응답 시간을 확인합니다. 팀용 HTTPS 접속 경로와 Grafana Viewer 계정을 발급받은 경우에는 `ReDO k6 Load Test` 대시보드에서 통합 결과도 조회할 수 있습니다.

Viewer는 Grafana 대시보드 조회에만 사용하며 Monitoring EC2의 SSH 키, Grafana Admin 계정과 Prometheus 접근 권한은 공유하지 않습니다. k6 결과를 Prometheus로 전송하는 테스트는 인프라 담당자가 실행합니다.

### 인프라 담당자: k6 결과 전송

인프라 담당자는 외부에 공개되지 않은 Prometheus에 SSH 터널로 접근합니다.

```bash
ssh -N \
  -L 9090:127.0.0.1:9090 \
  -L 3000:127.0.0.1:3000 \
  ubuntu@<MONITORING_EC2_PUBLIC_IP>
```

`k6/.env`에서 출력 방식을 변경합니다.

```dotenv
K6_OUTPUT=prometheus
K6_PROMETHEUS_RW_SERVER_URL=http://host.docker.internal:9090/api/v1/write
```

이후 동일한 실행 명령을 사용합니다.

```bash
./k6/run.sh scenarios/<domain>/<scenario>.js
```

### 팀원: Viewer 계정으로 결과 확인

인프라 담당자가 공유한 `TEST_ID`를 기준으로 Grafana의 `ReDO k6 Load Test` 대시보드에서 `testid`, `domain`, `scenario`, `endpoint`를 선택해 다음 항목을 확인합니다.

- 가상 사용자 수와 초당 요청 수
- HTTP 오류율
- 응답 시간 p95·p99
- 애플리케이션과 EC2 CPU
- JVM Heap과 Redis 메모리
- Spring Boot 요청 처리량

모니터링 스택의 실행과 점검 방법은 [모니터링 운영 가이드](../monitoring/README.md)를 참고합니다.

## 10. 자주 발생하는 문제

### `Scenario not found`가 출력되는 경우

프로젝트 루트에서 `k6/scenarios` 아래의 상대 경로를 전달했는지 확인합니다.

```bash
./k6/run.sh scenarios/examples/authenticated-read.js
```

### 로그인에 실패하는 경우

- 테스트 계정의 아이디와 비밀번호를 확인합니다.
- 직접 입력한 `ACCESS_TOKEN`이 만료되지 않았는지 확인합니다.
- 공개 API라면 `AUTH_REQUIRED=false`인지 확인합니다.

### 로컬 API에 연결되지 않는 경우

- Docker Desktop과 백엔드 서버가 실행 중인지 확인합니다.
- 로컬 백엔드가 `8080` 포트로 공개됐는지 확인합니다.
- Docker 컨테이너에서는 `localhost` 대신 `host.docker.internal`을 사용합니다.

### 원격·쓰기·stress 테스트가 차단되는 경우

오류 메시지와 테스트 대상을 다시 확인한 뒤 필요한 안전 플래그만 명시적으로 활성화합니다. 안전 플래그를 기본값 `true`로 저장하지 않습니다.

### Grafana에 결과가 나타나지 않는 경우

- 팀원은 인프라 담당자가 공유한 `TEST_ID`와 대시보드 필터가 일치하는지 확인합니다.
- Viewer 계정으로 로그인했는지 확인합니다.
- 인프라 담당자는 SSH 터널의 `9090` 연결이 유지되는지 확인합니다.
- `K6_OUTPUT=prometheus`인지 확인합니다.
- `K6_PROMETHEUS_RW_SERVER_URL`이 `/api/v1/write`를 가리키는지 확인합니다.
- k6 실행 로그에 Prometheus Remote Write 오류가 없는지 확인합니다.

## 11. 실행 전 확인사항

- 테스트 대상과 실행 프로필이 의도한 환경인지 확인합니다.
- 인증 계정이 전용 테스트 계정인지 확인합니다.
- 쓰기 시나리오는 변경되는 데이터와 복구 방법을 확인합니다.
- 부하 테스트 전 Prometheus Target과 Grafana 대시보드가 정상인지 확인합니다.
- 첫 실행은 `smoke`로 검증한 뒤 `load`, `stress` 순서로 진행합니다.
- 테스트가 끝나면 실행 결과와 `TEST_ID`, 프로필, VU 수, 대상 환경을 함께 기록합니다.
