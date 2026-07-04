ReDO Spring 백엔드입니다.

- Java 17
- Spring Boot 3.5.x
- 인증 보안: Spring Security + JWT
- Spring Data JPA 사용
- DB
    - MySQL
    - Redis
    - AWS S3

## 프로젝트 구조

```
  ReDO
  ├── build.gradle
  ├── settings.gradle
  ├── gradle/
  │   └── wrapper/
  └── src/
      ├── main/
      │   ├── java/
      │   │   └── com/redo/
      │   │       ├── domain/
      │   │       │   ├── certification/          
      │   │       │   ├── community/
      │   │       │   ├── contribution/
      │   │       │   ├── point/
      │   │       │   ├── recycleGuide/
      │   │       │   ├── reward/
      │   │       │   ├── term/
      │   │       │   └── user/
      │   │       └── global/
      │   │           ├── apiPayload/
      │   │           │   ├── code/
      │   │           │   ├── exception/
      │   │           │   └── handler/
      │   │           ├── config/
      │   │           ├── security/
      │   │           └── util/
      │   └── resources/
      └── test/
          └── java/
              └── com/redo/
```

- 도메인별로 패키지를 나누고 내부에 `controller, service, repository, entity, dto` 작성
- global에는 config, error, response, security, util 같은 공통 코드 작성

## 로컬 실행 준비 & 실행 방법

### Redis 준비 (macOS + Homebrew)

#### Redis 설치

```bash
brew install redis
```

#### ***Redis 실행***

```bash
brew services start redis
```

#### ***Redis 실행 확인***

```bash
redis-cli ping
```

정상적으로 실행 중이면 아래처럼 응답합니다.

`PONG`

#### ***Redis 중지***

```bash
brew services stop redis
```

### Redis 준비 (Windows)

Windows에서는 Redis 공식 지원이 제한적이므로 아래 방법 중 하나를 사용합니다.

#### *Docker 사용*

Docker Desktop이 설치되어 있다면 아래 명령으로 Redis를 실행합니다.

```bash
docker run --name redo-redis -p 6379:6379 -d redis
```

Redis 실행 확인:

```bash
docker exec -it redo-redis redis-cli ping
```

정상 응답:

`PONG`

Redis 중지:

```bash
docker stop redo-redis
```

Redis 다시 시작:

```bash
docker start redo-redis
```

Redis 컨테이너 삭제:

```bash
docker rm redo-redis
```

#### *WSL 사용*

WSL Ubuntu 환경에서 Redis를 설치합니다.

```bash
sudo apt update

sudo apt install redis-server
```

Redis 실행:

```bash
sudo service redis-server start
```

Redis 실행 확인:

```bash
redis-cli ping
```

정상 응답:

`PONG`

Redis 중지:

```bash
sudo service redis-server stop
```

### **MySQL 준비 (macOS + Homebrew)**

#### ***MySQL 설치***

```bash
brew install mysql
```

#### ***MySQL 실행***

```bash
brew services start mysql
```

#### ***MySQL 접속***

```bash
mysql -u root -p
```

비밀번호가 설정되어 있지 않은 경우 아래 명령으로 접속될 수도 있습니다.

```bash
mysql -u root
```

#### **데이터베이스 및 유저 생성**

MySQL에 접속한 뒤 아래 SQL을 실행합니다.

```sql
CREATE DATABASE IF NOT EXISTS redo

DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'redo'@'localhost'
IDENTIFIED BY 'redo2026';

GRANT ALL PRIVILEGES ON redo.* TO 'redo'@'localhost';

FLUSH PRIVILEGES;
```

#### ***접속 테스트***

```bash
mysql -u redo -p redo
```

비밀번호는 위 예시 기준 아래 값입니다.

redo2026

### **MySQL 준비 (Windows)**

Windows에서는 MySQL Installer를 사용하는 방법을 권장합니다.

#### *MySQL 설치*

1. MySQL 공식 다운로드 페이지에 접속합니다.
   - https://dev.mysql.com/downloads/installer/

2. MySQL Installer for Windows를 다운로드합니다.

3. 설치 유형은 보통 아래 중 하나를 선택합니다.
   - Developer Default
   - 또는 Server only

4. 설치 중 root 비밀번호를 설정합니다.

5. MySQL Server가 정상적으로 실행 중인지 확인합니다.

#### *MySQL 접속*

명령 프롬프트 또는 PowerShell에서 아래 명령을 실행합니다.

```bash
mysql -u root -p
```

설치 중 설정한 root 비밀번호를 입력합니다.

만약 mysql 명령어를 찾을 수 없다는 오류가 발생하면 MySQL bin 경로를 환경변수 Path에 추가해야 합니다.

예시 경로:

`C:\Program Files\MySQL\MySQL Server 8.0\bin`

환경변수 설정 후 새 터미널을 열고 다시 실행합니다.

```bash
mysql -u root -p
```

설치 이후 유저 등록과 테이블 생성 방법은 Mac과 동일합니다.

### **환경변수 설정**

이 프로젝트는 src/main/resources/application.yaml에서 아래 환경변수를 사용합니다.

`DB_USERNAME, DB_PASSWORD, JWT_SECRET`

터미널에서 실행할 경우:

```bash
export DB_USERNAME=redo
export DB_PASSWORD=redo2026
export JWT_SECRET=my-secret-key-for-local-development-my-secret-key
```

터미널을 새로 열어도 유지하려면 ~/.zshrc에 추가합니다. (MacOS 기준)

```bash
echo 'export DB_USERNAME=redo' >> ~/.zshrc
echo 'export DB_PASSWORD=redo2026' >> ~/.zshrc
echo 'export JWT_SECRET=my-secret-key-for-local-development-my-secret-key' >> ~/.zshrc
source ~/.zshrc
```

#### **IntelliJ로 실행 시, 환경변수 설정**

IntelliJ에서 실행하는 경우 터미널의 export 값이 자동으로 적용되지 않을 수 있습니다.

아래 위치에서 환경변수를 직접 추가합니다.

Run > Edit Configurations...

> Spring Boot 실행 설정 선택

> Modify options

> Environment variables 추가

환경변수 값:

DB_USERNAME=redo;DB_PASSWORD=redo2026;JWT_SECRET=my-secret-key-for-local-development-my-secret-key

만약 Environment variables 항목이 보이지 않으면 Modify options에서 추가합니다.

### **프로젝트 실행**

터미널로 실행할 경우, 프로젝트 루트에서 실행합니다.

```bash
# mysql, redis 준비
brew services start mysql
brew services start redis

# 터미널에서 실행할때는 환경변수 미리 export 필요
export DB_USERNAME=redo
export DB_PASSWORD=redo2026
export JWT_SECRET=....

# 실행
./gradlew bootRun
```

IntelliJ에서 실행할 경우, RedoApplication을 Spring Boot Configuration으로 실행합니다.

실행 후 Swagger UI는 아래 주소에서 확인할 수 있습니다.

**`http://localhost:8080/swagger-ui.html`**

### **테스트 실행**

```bash
./gradlew test
```

### **빌드**

```bash
./gradlew build
```

빌드 결과물은 아래 경로에 생성됩니다.

`build/libs/`

빌드된 JAR 실행:

`java -jar build/libs/redo-0.0.1-SNAPSHOT.jar`

### **application.yaml 주요 설정**

로컬 실행 시 MySQL은 localhost:3306, Redis는 localhost:6379에서 실행 중이어야 합니다.