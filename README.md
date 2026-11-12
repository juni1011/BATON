# Idiots

## SSAP Idiots

알바 첫걸음은 근로계약서를 점검하고 근무 일정, 급여일, 지원사업 신청 마감일을 관리하는 서비스입니다.

## 실행

Java 21과 MySQL 8.4가 필요합니다. 프로젝트 루트에서 실행합니다.

### Docker로 MySQL 준비

Docker Desktop을 실행한 뒤 환경변수 파일을 만듭니다.

```sh
cp .env.example .env
```

`.env`의 `DB_PASSWORD`를 개발용 DB 비밀번호로 변경한 뒤, **같은 터미널에서** 실행합니다.
`.env`는 Git에서 제외됩니다. Docker Compose는 이 파일을 자동으로 읽지만 Spring Boot는 읽지 않으므로 아래의 `source` 단계가 필요합니다.

```sh
set -a
source .env
set +a
docker compose up -d --wait
./gradlew bootRun
```

접속 주소: http://localhost:8080/ (index.html).
화면: /signup.html(회원가입), /upload.html(계약서 입력), /confirm.html(추출값 확인), /report.html(점검), /support.html(지원사업 추천), /calendar.html(캘린더).
현재 구현은 텍스트 파일, 붙여넣기, 가상 샘플 3종을 규칙으로 처리합니다. PDF·사진의 AI 추출과 실제 지원사업 데이터는 제출 목표에 포함됩니다.
서버 종료는 Ctrl+C를 사용합니다.

MySQL의 데이터베이스·사용자 이름은 `easyton`, 포트는 `3306`입니다. `app_users` 테이블은 서버 시작 시 `schema.sql`로 생성하며, 기존 테이블과 회원 데이터는 유지합니다. 스키마가 변경되면 별도 마이그레이션이 필요합니다.
`docker compose down`은 DB 컨테이너를 종료하지만 볼륨의 회원 데이터는 보존합니다. DB 비밀번호는 볼륨 최초 생성 시 적용되므로, 이미 생성한 DB의 비밀번호를 변경하려면 MySQL에서도 변경해야 합니다.

### 이미 설치된 MySQL 사용

기존 MySQL을 사용한다면 Docker 실행을 생략하고, 관리자 계정으로 데이터베이스와 앱 계정을 준비합니다. 비밀번호 예시는 실제 값으로 바꿉니다.

```sql
CREATE DATABASE IF NOT EXISTS easyton CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_cs;
CREATE USER 'easyton'@'localhost' IDENTIFIED BY 'your-db-password';
GRANT SELECT, INSERT, CREATE ON easyton.* TO 'easyton'@'localhost';
```

앱 계정이 이미 있다면 `CREATE USER`를 생략합니다. 실행할 터미널 또는 IDE 실행 설정에 아래 환경변수를 설정합니다.

```sh
export DB_URL='jdbc:mysql://localhost:3306/easyton?connectionTimeZone=UTC&characterEncoding=UTF-8'
export DB_USERNAME='easyton'
export DB_PASSWORD='your-db-password'
./gradlew bootRun
```

### 회원가입 동작

`/signup.html`에서 이름, 이메일, 비밀번호, 비밀번호 확인을 입력하면 `POST /api/auth/signup`으로 저장합니다.
이메일 앞뒤 공백과 대소문자를 정규화하고, 같은 이메일은 DB의 UNIQUE 제약으로 중복 가입을 막습니다.
서버에서도 이름(1~40자), 이메일(최대 254자), 비밀번호(8~128자), 비밀번호 일치를 검사합니다.
비밀번호는 Spring Security의 PBKDF2 해시와 무작위 salt로 저장하며 API 응답에 원문이나 해시를 포함하지 않습니다.
가입 성공 시 로그인 화면으로 이동하고 로그인 버튼 아래에 환영 메시지가 표시됩니다.
로그인은 `POST /api/auth/signin`, 세션 확인은 `GET /api/auth/me`, 로그아웃은 `POST /api/auth/signout`을 사용합니다. 홈의 추천 받기·전체 보기 링크는 로그인 상태를 확인하며, 로그인 후 원래 선택한 화면으로 이동합니다. 이메일 인증은 아직 구현하지 않았습니다.

## 지원사업 추천 화면 (React)

`/support.html`의 입력·지도·추천 결과는 React로 구성합니다. 기존 Spring Boot 서버에서 그대로 제공하며, 실행에 외부 CDN은 필요하지 않습니다.
입력 화면은 나이·거주 지역·현재 상태·거주 형태·가구 소득의 5개 항목입니다. 거주 지역은 시·도와 시·군·구 선택창을 같은 카드에 두며, 시·도를 고르면 같은 지도가 확대되어 시·군·구를 선택할 수 있습니다. API에 전달하는 프로필의 6개 필드는 유지합니다.
수정할 소스는 `frontend/support/`, 화면 스타일은 `src/main/resources/static/css/support.css`와 `support-map.css`입니다.

```sh
npm ci
npm run build
./gradlew processResources
```

`npm run dev`는 React 소스 변경을 감지해 기존 정적 번들을 다시 만듭니다. Spring Boot 서버와 함께 사용하며 별도 프론트 서버를 만들지 않습니다. 변경 후 `./gradlew processResources`와 브라우저 새로고침으로 확인합니다.
빌드한 `src/main/resources/static/js/support.js`와 라이선스 파일은 저장소에 포함하므로 Java 서버만 실행해도 화면을 볼 수 있습니다. 생성 파일 대신 `frontend/support/`의 JSX를 수정해 주세요.
연령 그림은 화면 표시를 위해 0~18세·19~34세·35~64세·65세 이상으로 구분하며 추천 판정은 기존 서버 규칙을 사용합니다.
지도 데이터 출처와 라이선스는 `frontend/support/MAP_DATA_LICENSE.md`를 참고합니다.
지도와 선택창은 양방향으로 연동합니다. 항목 선택을 마치면 다음 빈 항목으로 이동하며, 나이·직접 입력은 입력을 멈춘 뒤 1.1초 또는 Enter로 확정합니다. 직접 스크롤하거나 다른 입력에 포커스를 옮기면 대기 중인 자동 이동은 취소됩니다. 전체 입력을 마쳐도 추천 요청은 사용자가 버튼으로 실행합니다.

## 테스트

```sh
./gradlew test
```

테스트는 기본적으로 H2 메모리 DB를 사용하므로 MySQL이나 `DB_PASSWORD` 없이 실행할 수 있습니다.
컨텍스트 로딩, 기존 API, 회원가입 저장·해시 검증·이메일 중복·잘못된 입력·비밀번호 길이 경계를 확인합니다.
결과 보고서는 `build/reports/tests/test/index.html`에서 확인합니다.

별도 MySQL에서 같은 테스트를 실행하려면 `TEST_DB_URL`, `TEST_DB_USERNAME`, `TEST_DB_PASSWORD`를 설정하고 `./gradlew test --rerun-tasks`를 실행합니다.
**회원가입 테스트는 매 테스트 전에 `app_users`를 비우므로 반드시 테스트 전용 DB를 사용합니다.**

## 팀 공통 규약

[docs/API.md](docs/API.md)에 요청·응답 예시와 프런트 헬퍼가 있습니다.
common.js를 바꿀 때는 팀 전체에 먼저 알립니다.
지원사업은 모두 가상이며 실제 제도·기관·신청 링크가 아닙니다.

## 제출 버전 요구사항

2026년 11월 14일 제출 목표는 청년 알바생이 서명 전에 근로계약서를 점검하고, 자신의 조건에 맞는 지원사업과 신청 마감일을 확인할 수 있게 하는 것입니다.

### 사용자 흐름

1. **계약서 입력** — 사진·PDF 업로드, 텍스트 붙여넣기, 가상 샘플 3종 중 하나를 선택할 수 있습니다. AI는 시급, 근로시간, 계약 기간, 지급일과 관련 조항을 추출합니다.
2. **추출값 확인** — 읽은 값과 관련 원문을 보여주고 사용자가 수정할 수 있게 합니다. 확인된 값으로 점검과 계약 일정 생성을 실행합니다. 원문에서 확인할 수 없는 값은 `null`로 전달합니다.
3. **점검 리포트** — 필수 기재사항, 최저임금, 주휴수당, 휴게시간, 임금 지급일과 위약금 조항을 살피고, 계산에 필요한 정보가 있으면 예상 세전 월급을 보여줍니다. 조항의 쉬운 설명과 사업주에게 물어볼 문장, 공식 상담 창구를 제공합니다.
4. **지원사업 추천** — 나이, 거주 지역, 현재 상태, 거주 형태, 가구 소득을 바탕으로 신청 가능·확인 필요·신청 불가를 판정합니다. 공식 자료로 확인한 사업 15건 안팎을 목표로 하며, 사업별 판정 이유, 지원 내용, 신청 기간, 필요 서류, 공식 신청 링크를 표시합니다.
5. **캘린더** — 확인된 계약 정보로 근무일·급여일·계약 종료일을 생성합니다. 사용자는 추천 사업의 마감일과 개인 일정을 직접 추가할 수 있습니다. 월간 달력과 다가오는 일정에 표시하고, 계약 일정을 다시 생성해도 이미 추가한 개인 일정과 지원사업 마감일은 유지합니다.

### 구현 기준

- 기존 Spring Boot 프로젝트에서 HTML·CSS·JavaScript 화면과 API를 함께 제공합니다. 모바일에서도 로그인 없이 이용할 수 있고, 진행 상태를 계약서 입력·확인 → 점검 → 추천 → 일정의 4단계로 표시합니다.
- 임금·근로시간 계산과 지원 자격 판정은 서버 규칙으로 처리합니다. AI는 문서 추출과 쉬운 설명에 사용하고, 리포트에 관련 원문과 `규칙 판정` 또는 `AI 해석`을 표시합니다. 근거가 부족하면 `확인 필요`로 안내합니다.
- 법정 기준과 지원사업 조건에는 공식 출처와 확인일을 기록합니다. 업로드 원본은 요청 처리 후 폐기하고, 주민등록번호와 전화번호를 가린 입력만 AI에 전달합니다. AI 키는 서버 환경 변수로 관리합니다.
- 샘플 모드는 API 키 없이 동작하며 화면에 샘플 결과임을 표시합니다. 일정은 브라우저의 `localStorage`에 저장합니다.

### 제출 전 확인

- 샘플 3종 모두 계약서 입력부터 캘린더까지 이어집니다. 정상 사례, 낮은 시급·주휴수당 미기재 사례, 위약금 조항·항목 누락 사례가 서로 다른 결과를 보여줍니다.
- 추출된 시급을 수정하면 리포트에 반영되고, 추천 결과에서 추가한 신청 마감일은 새로고침 후에도 표시됩니다.
- 개인정보를 제거한 가상 계약서로 실제 AI 추출을 한 번 이상 검증합니다.
