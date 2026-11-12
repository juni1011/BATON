# 팀 공통 API 계약

## 범위와 공통 규칙

회원가입 API는 MySQL에 회원을 저장한다. 계약서 처리·점검·일정 생성 및 지원사업 매칭 API도 제공한다.
지원사업 매칭은 외부 API(공공데이터 등) 없이 코드에 들어 있는 가상 사업 목록과 프로필을 비교한다.
캘린더는 서버 API 없이 화면에서 localStorage로만 동작한다.
모든 사업·기관·조건·일정·서류는 가상이며 실제 제도가 아니다.

입력하지 않은 값은 `null`로 둔다. 0, false, 빈 문자열 등으로 추정하지 않는다.
날짜는 `yyyy-MM-dd`, 시간은 `HH:mm`, enum은 대문자 문자열이다.

## POST /api/contracts/extract

`multipart/form-data`. `file`(텍스트 파일 또는 사진)이 있으면 파일을, 없으면 `text`를 읽는다. 응답은 `ContractInfo` JSON이다.
사진(PNG·JPEG·WEBP·GIF, 10MB 이하)은 파일 앞부분의 형식으로 판별하며 서버의 Codex CLI(`codex exec`)가 읽는다.
사진은 외부 AI로 전송되므로 `consent=true`를 함께 보내야 한다.

| 상태 | 의미 |
| --- | --- |
| 400 | 파일과 텍스트가 모두 없음, 또는 사진인데 `consent=true`가 아님 |
| 413 | 사진이 10MB 초과 |
| 415 | 텍스트도 지원하는 사진도 아님(PDF 등) |
| 422 | Codex 결과를 해석할 수 없음 |
| 429 | 동시 사진 분석 2건 초과 |
| 502 | Codex 실행이 실패함 |
| 503 | Codex를 쓸 수 없음(미설치, 비활성화 등) |
| 504 | 180초 안에 분석이 끝나지 않음 |

사진에서 읽은 값은 범위를 검사해 벗어나면 null로 둔다(시급 1~1,000,000, 지급일 1~31 등).

## POST /api/auth/signup

요청 `application/json`:

```json
{
  "name": "홍길동",
  "email": "member@example.com",
  "password": "example-password-123!",
  "passwordConfirm": "example-password-123!"
}
```

이름은 앞뒤 공백을 제거한 뒤 1~40자, 이메일은 앞뒤 공백 제거·소문자 변환 후 올바른 이메일 형식과 최대 254자를 검사한다.
비밀번호는 원문 그대로 8~128자이며 비밀번호 확인과 같아야 한다. 공백만 있는 비밀번호는 허용하지 않는다.
동일 이메일의 동시 가입도 DB UNIQUE 제약으로 차단한다.

성공 `201 application/json`:

```json
{"id": 1, "name": "홍길동", "email": "member@example.com"}
```

비밀번호와 해시는 응답하지 않는다. 회원가입은 로그인 세션을 생성하지 않는다.

| 상태 | 의미 | 응답 |
| --- | --- | --- |
| 400 | 필드 검증 실패 | `{"message":"입력 내용을 확인해 주세요.","errors":{"passwordConfirmed":"비밀번호가 일치하지 않습니다."}}` |
| 400 | JSON 파싱 실패 또는 본문 없음 | `{"message":"올바른 JSON 요청을 보내 주세요.","errors":{}}` |
| 409 | 이메일 중복 | `{"message":"이미 가입된 이메일입니다.","errors":{}}` |
| 503 | 요청 처리 중 DB 오류 | `{"message":"회원가입을 처리할 수 없습니다. 잠시 후 다시 시도해 주세요.","errors":{}}` |

검증 오류의 `errors`에는 필드별 메시지만 포함하며 입력된 비밀번호나 SQL 오류는 노출하지 않는다.
DB 연결 설정과 로컬 실행 방법은 [README.md](../README.md)를 참고한다.

## 로그인 (세션 쿠키)

- `POST /api/auth/signin` `{"email","password"}` → `200 {"id","name","email"}` 및 `JSESSIONID` 쿠키. 이메일 또는 비밀번호가 틀리면 `401 {"message":"이메일 또는 비밀번호가 올바르지 않습니다.","errors":{}}` (어느 쪽이 틀렸는지 알리지 않음). 필드 누락은 400.
- `GET /api/auth/me` → 로그인 상태면 `200 {"id","name","email"}`, 아니면 `401`.
- `POST /api/auth/signout` → `204`, 세션을 무효화한다.

## 계정별 데이터 저장 (로그인 필요)

로그인하면 화면 데이터가 계정에 저장되어 다른 브라우저에서도 이어서 쓸 수 있다. 비로그인 상태는 기존처럼 localStorage에만 저장한다.
키는 `contract`, `report`, `events`, `profile`, `matches`만 허용하며 값은 임의의 JSON(최대 약 1MB)이다.

- `GET /api/me/data` → `200 {"events":[...],"profile":{...}}` (저장된 키만 포함)
- `PUT /api/me/data/{key}` 본문은 JSON 값 → `204`. 잘못된 JSON은 400, 1MB 초과는 413.
- `DELETE /api/me/data/{key}` → `204`
- 로그인하지 않았으면 401, 허용하지 않는 키는 404.

화면(`common.js`)은 페이지를 열 때 서버 데이터로 localStorage를 맞추고, 저장할 때 서버에도 반영한다.
서버가 비어 있는 계정으로 처음 로그인하면 로그인 전에 이 브라우저에 저장한 데이터를 계정으로 옮기고, 로그아웃하면 이 브라우저의 계정 데이터를 지운다.

## POST /api/supports/match

요청 `application/json` (MatchRequest):

```json
{
  "profile": {
    "age": 22,
    "sido": "가상시",
    "sigungu": "상상구",
    "status": "대학생",
    "housing": "월세",
    "householdIncomeLevel": "미확인"
  }
}
```

응답 `200 application/json` (List<SupportMatch>), 5건 중 2건만 보인 예시:

```json
[
  {
    "id": "support-example-a",
    "name": "예시 청년 교통비 지원",
    "organization": "가상청년센터",
    "verdict": "ELIGIBLE",
    "reason": "[가상 예시] 나이 조건을 충족합니다.",
    "applyStart": "2026-11-01",
    "applyEnd": "2026-11-30",
    "applyUrl": "https://example.com/support-a",
    "requiredDocuments": [
      "예시 신청서"
    ],
    "sample": true
  },
  {
    "id": "support-example-b",
    "name": "예시 대학생 장학 지원",
    "organization": "가상지원재단",
    "verdict": "NEED_CHECK",
    "reason": "[가상 예시] 확인이 필요한 조건: 가구 소득(중위소득 100% 이하).",
    "applyStart": "2026-12-01",
    "applyEnd": "2026-12-31",
    "applyUrl": "https://example.com/support-b",
    "requiredDocuments": [
      "예시 신청서",
      "재학증명서",
      "소득 확인서"
    ],
    "sample": true
  }
]
```

응답 항목 `sample`은 가상 예시 사업이면 true, 온통청년 실제 정책이면 false다.

`MatchVerdict`: ELIGIBLE, NEED_CHECK, NOT_ELIGIBLE 순으로 정렬하고, 같은 판정 안에서는 신청 마감이 가까운 사업을 먼저(마감일을 모르면 뒤로) 둔다.
응답은 최대 50건이다.
`SupportProgramSource`가 사업 목록을 공급한다. 환경변수 `YOUTH_API_KEY`가 없으면 `SampleSupportProgramSource`의 가상 사업 5건을,
있으면 `YouthCenterProgramSource`가 온통청년 청년정책 Open API(`/go/ythip/getPlcy`)에서 받은 실제 정책을 쓴다.
실제 정책은 서버 시작 직후와 24시간마다 전체를 받아 메모리에 두며, 갱신에 실패하면 마지막 성공 목록을 유지하고 첫 로딩 전에는 가상 사업을 돌려준다.
신청 마감이 지난 사업은 제외하고, 신청 기간이 "상시" 등으로 비어 있으면 날짜 없이 남긴다.
실제 정책은 나이(0은 제한 없음)와 신청 기간만 판정에 쓰고, 소득·신분·거주지는 코드값이라 해석하지 않고 항상 "확인 필요"로 둔다.
따라서 실제 정책은 ELIGIBLE이 되지 않는다. 비교 조건은 나이, 시·도, 상태, 거주 형태, 가구 소득이다.
조건에 맞지 않는 값이 하나라도 있으면 NOT_ELIGIBLE, 값이 없거나 "미확인"인 조건이 있으면 NEED_CHECK다.
example.com은 예시 링크다. 실제 지원 조건이나 금액을 뜻하지 않는다.

프로필 선택지: status는 대학생·구직자·재직자·기타, housing은 월세·전세·자가·가족과 거주·기숙사·기타,
householdIncomeLevel은 중위소득 50% 이하·100% 이하·150% 이하·150% 초과·미확인이다. profile은 null일 수 있다.
JSON 본문 형식이 깨지거나 본문이 없으면 기본 Spring MVC 오류 응답을 사용한다.

## 캘린더 일정 (프런트 전용)

일정은 `easyton.calendarEvents`에 아래 형태의 배열로 저장한다.

```json
{
  "id": "support-support-example-a",
  "title": "예시 청년 교통비 지원 신청 마감",
  "type": "SUPPORT_DEADLINE",
  "date": "2026-11-30",
  "startTime": null,
  "endTime": null,
  "description": "가상청년센터",
  "sourceLink": "/support.html"
}
```

type은 SUPPORT_DEADLINE(추천 결과에서 추가), TODO, CUSTOM(캘린더에서 직접 추가)이다.
종일 일정의 startTime과 endTime은 null이다. 외부 캘린더 연동은 하지 않는다.

## 프런트 공통 도구

모든 화면에서 `/js/common.js`, `/js/ui.js`, 화면별 `/js/<화면>.js`를 순서대로 불러온다. 지원사업 추천은 `frontend/support/`의 React 소스를 빌드한 `/js/support.js`를 사용하며 API·localStorage 계약은 동일하다. 빌드 명령은 README를 참고한다.
`window.Easyton`의 함수는 다음과 같다.

| 함수 | 동작 |
| --- | --- |
| saveCalendarEvents(value) / getCalendarEvents() | 일정 배열 저장 / 조회, 기본 [] |
| saveUserProfile(value) / getUserProfile() | 프로필 저장 / 조회, 기본 null |
| saveSupportMatches(value) / getSupportMatches() | 추천 결과 배열 저장 / 조회, 기본 [] |
| apiFetch(url, options) | JSON 응답 반환, HTTP 오류 시 Error, 204는 null |
| postJson(url, body) | JSON POST |

save 함수에 null을 넣으면 해당 키를 삭제한다. localStorage 키는 각각
`easyton.calendarEvents`, `easyton.userProfile`, `easyton.supportMatches`이다.
손상된 JSON은 조회 기본값을 반환하고, 저장소 접근·용량 오류는 호출자에게 전달한다.
날짜·시간은 Date 객체로 변환하지 않고 문자열을 그대로 저장한다.
`ui.js`의 `window.EasytonUI`는 표시용 도구(h, dateLabel, todayIso, errorText, notify)와 라벨 표를 제공한다.

## JSON 설정

Spring Boot 4의 기본 Jackson 3 `tools.jackson.databind.json.JsonMapper`를 사용한다.
날짜는 record 필드의 `com.fasterxml.jackson.annotation.JsonFormat`으로 고정한다.
별도 ObjectMapper나 Jackson 2 의존성, 전역 날짜 설정은 추가하지 않는다.
