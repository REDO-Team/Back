# ReDO 모니터링 운영 가이드

ReDO 운영 환경의 애플리케이션·EC2·Redis 메트릭과 컨테이너 로그를 수집하고, Grafana 대시보드와 Discord 알림으로 관찰하기 위한 구성입니다.

이 문서의 서버 실행, SSH 터널과 Prometheus 관리 절차는 Monitoring EC2 접근 권한이 있는 운영 담당자를 대상으로 합니다. 팀원은 별도로 발급받은 Grafana Viewer 계정으로 대시보드를 조회하며, SSH 키나 Grafana Admin 계정은 공유하지 않습니다.

## 1. 구성 개요

```text
Service EC2
├─ Spring Boot Actuator :9101 ─┐
├─ Node Exporter        :9100 ─┼─> Monitoring EC2의 Prometheus
├─ Redis Exporter       :9121 ─┘
└─ Docker Socket Proxy -> Alloy -> TLS + Basic Auth -> Loki Proxy :3100

Monitoring EC2
├─ Prometheus :9090 ─┐
├─ Loki               ├─> Grafana :3000 -> Discord 장애·복구 알림
└─ Loki Proxy :3100 ──┘

k6 실행 환경 -> Prometheus Remote Write :9090/api/v1/write
```

| 구성 요소 | 역할 | 보관 정책 |
| --- | --- | --- |
| Prometheus | 애플리케이션·EC2·Redis·k6 메트릭 저장 | 최대 7일 또는 2GB |
| Loki | Service EC2의 Docker 로그 저장 | 72시간 |
| Grafana | 메트릭·로그 조회, 대시보드, Discord 알림 | Grafana 볼륨에 저장 |
| Alloy | Service EC2 컨테이너 로그 수집 및 Loki 전송 | 로컬 상태만 저장 |
| Docker Socket Proxy | Alloy에 Docker 조회 권한만 제한적으로 제공 | 해당 없음 |

## 2. 디렉터리 구조

```text
monitoring/
├─ compose.yaml
├─ .env.example
├─ prometheus/prometheus.yml
├─ loki/loki.yaml
├─ loki-proxy/nginx.conf
├─ alloy/config.alloy
└─ grafana/
   ├─ dashboards/
   └─ provisioning/
      ├─ alerting/
      ├─ dashboards/
      └─ datasources/
```

- 루트 `compose.yaml`: Service EC2의 애플리케이션, Redis, Exporter, Alloy를 실행합니다.
- `monitoring/compose.yaml`: Monitoring EC2의 Prometheus, Loki, Loki Proxy, Grafana를 실행합니다.
- 인증서, `.htpasswd`, 실제 `.env`는 Git에서 제외됩니다.

## 3. 네트워크 준비

두 EC2가 같은 VPC의 사설 IP로 통신할 수 있어야 합니다.

| 방향 | 포트 | 용도 |
| --- | --- | --- |
| Monitoring EC2 → Service EC2 | TCP 9100 | EC2 메트릭 수집 |
| Monitoring EC2 → Service EC2 | TCP 9101 | Spring Boot 메트릭 수집 |
| Monitoring EC2 → Service EC2 | TCP 9121 | Redis 메트릭 수집 |
| Service EC2 → Monitoring EC2 | TCP 3100 | Alloy 로그 전송 |
| 관리자 → Monitoring EC2 | TCP 22 | SSH 및 Grafana·Prometheus 터널 |

보안 그룹의 소스는 가능하면 상대 EC2의 보안 그룹으로 제한합니다. `3000`, `9090`, `3100`, `9100`, `9101`, `9121`을 인터넷 전체에 공개하지 않습니다.

## 4. Loki 전송 인증정보 준비

Loki Proxy는 TLS와 Basic Auth를 모두 확인합니다. 다음 파일은 저장소에 커밋하지 않습니다.

```text
monitoring/loki-proxy/secrets/server.crt
monitoring/loki-proxy/secrets/server.key
monitoring/loki-proxy/secrets/.htpasswd
monitoring/alloy/certs/loki-ca.crt
```

Monitoring EC2에서 사설 IP가 SAN에 포함된 인증서를 준비합니다. 아래 명령은 자체 서명 인증서를 만드는 예시입니다.

```bash
mkdir -p monitoring/loki-proxy/secrets

openssl req -x509 -nodes -newkey rsa:2048 -days 365 \
  -keyout monitoring/loki-proxy/secrets/server.key \
  -out monitoring/loki-proxy/secrets/server.crt \
  -subj "/CN=<MONITORING_PRIVATE_IP>" \
  -addext "subjectAltName=IP:<MONITORING_PRIVATE_IP>"
```

Basic Auth 파일도 생성합니다. 사용자명과 비밀번호는 이후 Service EC2의 `.env` 값과 같아야 합니다.

```bash
docker run --rm --entrypoint htpasswd httpd:2.4-alpine \
  -Bbn '<LOKI_USERNAME>' '<LOKI_PASSWORD>' \
  > monitoring/loki-proxy/secrets/.htpasswd
```

생성한 `server.crt`를 Service EC2의 다음 경로로 안전하게 복사합니다.

```text
monitoring/alloy/certs/loki-ca.crt
```

인증서를 갱신하면 Monitoring EC2의 `server.crt`, `server.key`와 Service EC2의 `loki-ca.crt`를 함께 교체해야 합니다.

## 5. 환경변수 설정

### Monitoring EC2

```bash
cp monitoring/.env.example monitoring/.env
```

```dotenv
MONITORING_PRIVATE_IP=<모니터링 EC2 사설 IP>
SERVICE_PRIVATE_IP=<서비스 EC2 사설 IP>

GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=<강한 비밀번호>
GRAFANA_ALERT_DISCORD_WEBHOOK_URL=<Discord Webhook URL>
```

### Service EC2

루트 `.env`에 다음 값을 설정합니다.

```dotenv
SERVICE_PRIVATE_IP=<서비스 EC2 사설 IP>
MONITORING_PRIVATE_IP=<모니터링 EC2 사설 IP>

LOKI_TENANT_ID=redo
LOKI_BASIC_AUTH_USERNAME=<htpasswd 생성 시 사용한 사용자명>
LOKI_BASIC_AUTH_PASSWORD=<htpasswd 생성 시 사용한 비밀번호>
```

실제 비밀번호와 Webhook URL은 `.env.example`에 작성하지 않습니다.

## 6. 실행 순서

Monitoring EC2를 먼저 실행한 뒤 Service EC2를 실행합니다.

### 6.1 Monitoring EC2

```bash
docker compose \
  --env-file monitoring/.env \
  -f monitoring/compose.yaml \
  config

docker compose \
  --env-file monitoring/.env \
  -f monitoring/compose.yaml \
  up -d
```

### 6.2 Service EC2

```bash
docker compose config
docker compose up -d
```

환경변수나 설정 파일을 변경한 경우 해당 서비스를 다시 생성합니다.

```bash
docker compose up -d --force-recreate alloy

docker compose \
  --env-file monitoring/.env \
  -f monitoring/compose.yaml \
  up -d --force-recreate prometheus loki loki-proxy grafana
```

볼륨에는 메트릭, 로그, Grafana 설정이 저장되므로 장애 대응 과정에서 `down -v`를 실행하지 않습니다.

## 7. 정상 동작 확인

### 컨테이너 상태

```bash
docker compose ps

docker compose \
  --env-file monitoring/.env \
  -f monitoring/compose.yaml \
  ps
```

### 메트릭 수집 경로

Monitoring EC2에서 Service EC2의 Exporter에 접근되는지 확인합니다.

```bash
curl http://<SERVICE_PRIVATE_IP>:9101/actuator/health
curl http://<SERVICE_PRIVATE_IP>:9100/metrics
curl http://<SERVICE_PRIVATE_IP>:9121/metrics
```

Prometheus의 `Status > Target health` 화면에서 다음 작업이 모두 `UP`인지 확인합니다.

- `prometheus`
- `redo-spring`
- `redo-node`
- `redo-redis`

### 로그 수집 경로

Service EC2와 Monitoring EC2에서 오류 로그를 확인합니다.

```bash
docker compose logs --tail=100 alloy docker-socket-proxy

docker compose \
  --env-file monitoring/.env \
  -f monitoring/compose.yaml \
  logs --tail=100 loki loki-proxy
```

Grafana의 `Explore`에서 Loki를 선택한 뒤 다음 쿼리로 로그 유입을 확인합니다.

```logql
{environment="production"}
```

## 8. Grafana와 Prometheus 접속

### 운영 담당자: Grafana·Prometheus 관리

운영 담당자는 로컬 PC에서 Monitoring EC2로 SSH 터널을 연결합니다.

```bash
ssh -N \
  -L 3000:127.0.0.1:3000 \
  -L 9090:127.0.0.1:9090 \
  ubuntu@<MONITORING_EC2_PUBLIC_IP>
```

- Grafana: `http://localhost:3000`
- Prometheus: `http://localhost:9090`

Grafana에는 다음 항목이 자동 프로비저닝됩니다.

- Prometheus·Loki 데이터 소스
- `ReDO Operations Overview` 운영 대시보드
- `ReDO k6 Load Test` 부하 테스트 대시보드
- 애플리케이션·EC2·Redis 장애 및 자원 알림
- Discord 장애·복구 알림

대시보드나 알림 설정 파일을 변경했는데 반영되지 않으면 Grafana를 재시작합니다.

```bash
docker compose \
  --env-file monitoring/.env \
  -f monitoring/compose.yaml \
  restart grafana
```

### 팀원: Grafana Viewer

팀용 HTTPS 접속 경로가 구성된 환경에서 관리자가 팀원별 Grafana 계정을 생성하고 조직 역할을 `Viewer`로 지정합니다.

- 접속 주소: `https://<GRAFANA_DOMAIN>`
- 계정은 팀원별로 발급하며 공용 계정을 사용하지 않습니다.
- Viewer 계정으로 대시보드와 부하 테스트 결과를 조회합니다.
- Grafana Admin 계정, Monitoring EC2 SSH 키와 Prometheus 접근 권한은 공유하지 않습니다.
- 회원가입은 비활성화하고 계정 생성과 회수는 관리자가 수행합니다.

## 9. k6 부하 테스트 연동

### 운영 담당자: k6 결과 전송

Prometheus는 운영 담당자가 실행한 k6의 Remote Write 결과를 받을 수 있도록 구성되어 있습니다. 담당자는 로컬 PC에서 Monitoring EC2의 `9090` 포트로 SSH 터널을 연결한 뒤 `k6/.env`를 설정합니다.

```dotenv
K6_OUTPUT=prometheus
K6_PROMETHEUS_RW_SERVER_URL=http://host.docker.internal:9090/api/v1/write
```

테스트를 실행하면 자동 생성된 `testid`가 모든 k6 메트릭에 추가됩니다.

```bash
./k6/run.sh scenarios/examples/authenticated-read.js
```

### 팀원: Viewer 계정으로 결과 확인

일반 팀원은 k6의 `local` 출력으로 기본 결과를 확인하고, 담당자가 공유한 `TEST_ID`로 Grafana의 `ReDO k6 Load Test` 대시보드를 조회합니다. Viewer 계정은 Prometheus나 Monitoring EC2 접근 권한을 요구하지 않습니다.

Grafana의 `ReDO k6 Load Test` 대시보드에서 `testid`, `domain`, `scenario`, `endpoint`를 선택해 결과와 애플리케이션·EC2·Redis 자원 사용량을 함께 확인합니다. 자세한 실행 방법과 안전장치는 [k6 부하 테스트 운영 가이드](../k6/README.md)를 참고합니다.

## 10. 자주 발생하는 문제

### Prometheus Target이 DOWN인 경우

- 두 EC2의 사설 IP가 환경변수와 일치하는지 확인합니다.
- Service EC2의 `9100`, `9101`, `9121` 포트가 사설 IP에 바인딩됐는지 확인합니다.
- 보안 그룹이 Monitoring EC2에서 오는 요청을 허용하는지 확인합니다.
- `docker compose ps`와 Exporter 로그를 확인합니다.

### Alloy에서 TLS 검증이 실패하는 경우

- `loki-ca.crt`가 현재 Loki Proxy의 `server.crt`와 대응하는지 확인합니다.
- 인증서 SAN에 Monitoring EC2의 사설 IP가 포함됐는지 확인합니다.
- 인증서 갱신 후 Alloy와 Loki Proxy를 모두 재생성합니다.

### Loki Proxy가 401을 반환하는 경우

- `.htpasswd`의 사용자명·비밀번호와 Service EC2 `.env` 값을 확인합니다.
- 비밀번호 변경 후 `.htpasswd`와 `.env`를 함께 변경하고 두 서비스를 재생성합니다.

### Grafana에 k6 결과가 나타나지 않는 경우

- SSH 터널의 `9090` 연결이 유지되는지 확인합니다.
- `K6_OUTPUT=prometheus`인지 확인합니다.
- k6 출력에 Prometheus Remote Write 오류가 없는지 확인합니다.
- Prometheus 컨테이너에 `--web.enable-remote-write-receiver`가 적용됐는지 확인합니다.

### Discord 알림이 오지 않는 경우

- `GRAFANA_ALERT_DISCORD_WEBHOOK_URL` 값을 확인합니다.
- Grafana의 `Alerting > Contact points`에서 테스트 알림을 보냅니다.
- Grafana 로그에서 provisioning 또는 webhook 오류를 확인합니다.

```bash
docker compose \
  --env-file monitoring/.env \
  -f monitoring/compose.yaml \
  logs --tail=100 grafana
```

## 11. 운영 시 주의사항

- `.env`, 인증서, 개인키, `.htpasswd`, Discord Webhook을 Git에 커밋하지 않습니다.
- 모니터링 포트는 사설망과 SSH 터널을 통해서만 접근합니다.
- Prometheus·Loki·Grafana 볼륨 삭제 전에는 데이터 유실 범위를 반드시 확인합니다.
- 부하 테스트 전 Grafana에서 기존 자원 사용량과 모든 Target의 `UP` 상태를 확인합니다.
- 운영 환경의 `stress` 또는 쓰기 부하 테스트는 팀 합의와 전용 테스트 데이터 준비 후 실행합니다.
