### Git 브랜치 및 커밋 규칙

- 브랜치 전략
    - `main`: 최종 제출/배포용
    - `develop`: 기능 통합/테스트용
    - `feature/*`: 기능 개발 브랜치
    - `feature/*` 에서 개발 후 → `develop` 으로 PR → (데모/제출 전) `main`으로 PR
- 브랜치 네이밍
    - 기능 개발: `feature/#이슈번호-기능명`
    - 버그 수정: `fix/#이슈번호-버그명`
    - 긴급 수정: `hotfix/#이슈번호-설명`
    - 리팩토링: `refactor/#이슈번호-리팩토링명`
    - 문서: `docs/#이슈번호-문서명`
    - 설정: `chore/#이슈번호-설정명`
- 커밋 메시지
    - `Feat:` 기능 추가
    - `Fix:` 버그 수정
    - `Refactor:` 리팩토링
    - `Docs:` 문서
    - `Test:` 테스트
    - `Chore:` 기타 설정
    - [타입]#이슈번호 작업 내용

### PR 규칙

- 이슈 단위로 PR 생성
- Conflict 해결 책임자는 PR 작성자
- PR에는 작업 내용, 변경 사항, 테스트 결과, 리뷰 포인트, 관련 이슈를 포함. (PR 템플릿 사용)
- 코드 리뷰
    - coderabbitai 사용, 수정 및 검토 후 merge. (리뷰어는 필요시 지정)

### Issue 규칙

- Issue 제목 및 라벨은 이슈 템플릿에서 정의한 Prefix([Feat], [Bug], [Maintenance] 등)를 기준으로 사용합니다.
    - 이슈 생성 기준: 기능/버그/리팩토링
    - 이슈-브랜치-PR 연결 방식: branch 이름에 `#이슈번호` 포함
    - 이슈 템플릿 사용

### 코드, API응답, 예외처리 컨벤션

- Java 코드 컨벤션 (네이밍 규칙)
    - 클래스: PascalCase, 변수/메서드: camelCase, 상수: UPPER_SNAKE_CASE
    - Entity를 API 응답으로 직접 반환 X
- API 응답 형식
    - 공통 응답 사용
        - isSuccess, code, message, result, errorDetail 형태로 성공/실패 응답을 통일.
- 예외 처리 구조
    - 예외 처리 사용
        - BaseException + ErrorCode + GlobalException + CustomException 형태로 통일.

### API 명세

- Notion API 명세 사용
- Swagger/OpenAPI 사용