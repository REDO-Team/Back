# ♻️ ReDO Backend Repository

> 분리수거는 쉽게, 실천은 보상으로 이어지도록 사용자 경험의 흐름을 안정적으로 뒷받침하는 ReDO Backend입니다.

---

## ✨ 프로젝트 소개

ReDO는 분리수거 정보를 쉽고 빠르게 제공하고,
실천에 대한 보상을 통해 사용자의 행동을 유도하는 서비스입니다.

- **기간** : 2026.06.29 ~ 2026.08.21
- **인원** : Backend 5명
- **목표** : AI 기반 정보 제공과 리워드 시스템을 결합하여 분리수거를 일상의 습관으로 만들어 지속적인 환경 보호 참여를 이끌어내는 것
- **핵심 흐름** : 분리수거 가이드 → 배출 인증 → 리워드 지급 → 기여도 시각화

---

## 🧑🏻‍💻 팀원 및 역할

|            [천진하(jinha1665)](https://github.com/jinha1665)             |              [안동휘(telling7895-eng)](https://github.com/telling7895-eng)               |              [채민수(miinsoo)](https://github.com/miinsoo)               |           [허건우(woo6629058)](https://github.com/woo6629058)            |           [이정우(Lee-Jungwoo)](https://github.com/Lee-Jungwoo)            |
|:---------------------------------------------------------------------:| :----------------------------------------------------------------------------: | :----------------------------------------------------------------------------: |:---------------------------------------------------------------------:|:---------------------------------------------------------------------:|
| ![천진하의 프로필 사진](https://avatars.githubusercontent.com/u/128897741?v=4) | ![안동휘의 프로필 사진](https://avatars.githubusercontent.com/u/262762536?v=4) | ![채민수의 프로필 사진](https://avatars.githubusercontent.com/u/187004014?v=4) | ![허건우의 프로필 사진](https://avatars.githubusercontent.com/u/156512340?v=4) | ![이정우의 프로필 사진](https://avatars.githubusercontent.com/u/66958341?v=4) |
|                               회원가입&로그인                                |                              AI 분리수거 정보/가이드                              |                                  AI 배출 인증/판정                                  |                             사용자 리워드 및 기여도                             |                                 커뮤니티                                  |

---


## 🚀 주요 기능

> 주요 기능은 추후 업데이트 예정입니다.

---

## ⚙️ 기술 스택

### Backend & Framework
<div>
  <img src="https://img.shields.io/badge/Java 17-007396?style=flat-square&logo=java&logoColor=white">
  <img src="https://img.shields.io/badge/Gradle-02303A?style=flat-square&logo=gradle&logoColor=white">
  <img src="https://img.shields.io/badge/Spring Boot-6DB33F?style=flat-square&logo=springboot&logoColor=white">
  <img src="https://img.shields.io/badge/Spring Data JPA-6DB33F?style=flat-square&logo=hibernate&logoColor=white">
  <img src="https://img.shields.io/badge/Spring Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white">
  <img src="https://img.shields.io/badge/OAuth2-000000?style=flat-square&logo=oauth&logoColor=white">
</div>

### Database
<div>
  <img src="https://img.shields.io/badge/MySQL-4479A1?style=flat-square&logo=mysql&logoColor=white">
  <img src="https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=redis&logoColor=white">
  <img src="https://img.shields.io/badge/Amazon RDS-527FFF?style=flat-square&logo=amazonrds&logoColor=white">
</div>

### Cloud & Infrastructure
<div>
  <img src="https://img.shields.io/badge/AWS EC2-FF9900?style=flat-square&logo=amazonec2&logoColor=white">
  <img src="https://img.shields.io/badge/AWS S3-569A31?style=flat-square&logo=amazons3&logoColor=white">
  <img src="https://img.shields.io/badge/AWS Route53-FF9900?style=flat-square&logo=amazonroute53&logoColor=white">
  <img src="https://img.shields.io/badge/Linux-FCC624?style=flat-square&logo=linux&logoColor=black">
</div>

### CI/CD & Deployment
<div>
  <img src="https://img.shields.io/badge/GitHub-181717?style=flat-square&logo=github&logoColor=white">
  <img src="https://img.shields.io/badge/GitHub Actions-2088FF?style=flat-square&logo=githubactions&logoColor=white">
  <img src="https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white">
  <img src="https://img.shields.io/badge/Docker Hub-2496ED?style=flat-square&logo=dockerhub&logoColor=white">
  <img src="https://img.shields.io/badge/Nginx-009639?style=flat-square&logo=nginx&logoColor=white">
</div>

### API Docs
<div>
  <img src="https://img.shields.io/badge/Swagger-85EA2D?style=flat-square&logo=swagger&logoColor=black">
  <img src="https://img.shields.io/badge/Postman-FF6C37?style=flat-square&logo=postman&logoColor=white">
</div>

> 일부 인프라 및 성능 테스트 도구는 배포 단계에서 적용 예정입니다.

---

## 🏗 Architecture

> 아키텍처 다이어그램은 추후 업데이트 예정입니다.

---

## 🗄 ERD

> ERD 이미지는 추후 업데이트 예정입니다.

---

## 📁 프로젝트 구조

```text
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

---

## 📎 협업 문서

- API 명세 : Notion / Swagger 문서 참고
- 컨벤션 문서 : [.github/CONTRIBUTING.md](.github/CONTRIBUTING.md)
- PR Template : `.github/PULL_REQUEST_TEMPLATE.md`
- Issue Template : `.github/ISSUE_TEMPLATE/`

---