# 팀 공통 API 계약

## 범위와 공통 규칙

네 API 모두 외부 API(AI·OCR·공공데이터) 없이 서버 안의 규칙으로 동작한다.
추출은 정규식 규칙, 점검은 정해진 법령 기준 규칙, 지원사업은 코드에 들어 있는 가상 사업 목록과의 조건 비교다.
PDF·이미지 인식과 실제 지원사업 데이터 연동은 외부 API가 필요해 구현하지 않았다.
본 서비스는 정보 제공용이며 법률 자문이 아닙니다.

계약서에 적혀 있지 않은 값은 `null`로 둔다. 0, false, 빈 문자열 등으로 추정하지 않는다.
목록도 미기재 시 null, 명시적으로 항목이 없는 경우만 []로 둔다.
Boolean 필드는 세 값을 구분한다. 언급이 없으면 null, 명시적으로 부정하면 false, 명시적으로 긍정하면 true다.

- weeklyHolidayPayIncluded: 주휴수당 언급 없음은 null, "주휴수당은 별도로 지급한다"는 false, "시급에 주휴수당을 포함한다"는 true.
- probation: 수습 언급 없음은 null, "수습 적용: 없음"은 false, "수습 3개월"은 true.
- socialInsuranceMentioned: 사회보험 언급 없음은 null, "사회보험에 가입하지 않는다"는 false, "관계 법령에 따라 가입한다"는 true.
날짜는 `yyyy-MM-dd`, 시간은 `HH:mm`, 요일과 enum은 대문자 문자열이다.
ContractInfo JSON의 전체 18개 키를 유지한다. 누락 키로 요청해도 역직렬화 시 null이 된다.

## ContractInfo

| 이름 | 타입 | 의미 | null 규칙 |
| --- | --- | --- | --- |
| employerName | String | 사용자 상호 | 원문 미기재 시 null |
| workplace | String | 근무 장소 | 원문 미기재 시 null |
| jobDescription | String | 업무 내용 | 원문 미기재 시 null |
| hourlyWage | Integer | 시급 (원) | 원문 미기재 시 null |
| weeklyHours | Double | 주 소정근로시간 | 원문 미기재 시 null |
| workDays | List<DayOfWeek> | 근무 요일, MONDAY ~ SUNDAY | 원문 미기재 시 null |
| startTime | LocalTime | 근무 시작, HH:mm | 원문 미기재 시 null |
| endTime | LocalTime | 근무 종료, HH:mm | 원문 미기재 시 null |
| breakMinutes | Integer | 휴게시간 (분) | 원문 미기재 시 null |
| contractStart | LocalDate | 계약 시작, yyyy-MM-dd | 원문 미기재 시 null |
| contractEnd | LocalDate | 계약 종료, yyyy-MM-dd | 원문 미기재 시 null |
| payDay | Integer | 매월 지급일 | 원문 미기재 시 null |
| weeklyHolidayPayIncluded | Boolean | 주휴수당의 시급 포함 여부 | 원문 미기재 시 null |
| probation | Boolean | 수습 여부 | 원문 미기재 시 null |
| probationMonths | Integer | 수습 개월 수 | 원문 미기재 시 null |
| probationWageRate | Integer | 수습 임금 비율 (%) | 원문 미기재 시 null |
| socialInsuranceMentioned | Boolean | 사회보험 언급 여부 | 원문 미기재 시 null |
| clauses | List<String> | 계약서 원문 조항 문장들 | 원문 미기재 시 null |

## 담당 분담

| 담당 | 패키지 (com.example.demo 하위) | 브랜치 | 화면 |
| --- | --- | --- | --- |
| A | contract | feature/contract-upload | upload.html, confirm.html |
| B | check | feature/contract-check | report.html |
| C | support | feature/support-match | support.html |
| D | calendar | feature/calendar | calendar.html, index.html |

**common 패키지와 common.js를 바꿀 때는 팀 전체에 먼저 알린다.**
ContractInfo는 common 패키지에 한 번만 정의한다. contract API도 이 record를 직접 반환한다.
나머지 기능별 응답 record는 각 기능 패키지에 둔다.
이 단계에서는 브랜치 생성·변경을 수행하지 않는다.

## 1. POST /api/contracts/extract

`multipart/form-data`의 `file` 또는 `text` 파라미터를 받는다.
텍스트만 보내면 `application/x-www-form-urlencoded`도 지원한다.
파일이 있으면 파일을, 없으면 text를 사용한다. 파일은 UTF-8 텍스트(.txt 또는 text/*)만 받는다.
둘 다 비어 있으면 400, 텍스트가 아닌 파일은 415를 반환한다.
`ContractTextParser`가 "라벨: 값" 줄(사용자, 근무 장소, 업무, 계약 기간, 근무 요일, 근무 시간)과
문장 속 표현(시급 ○원, 매월 ○일, ○분의 휴게시간, 주휴수당, 수습, 사회보험)을 읽는다.
찾지 못한 값은 null이다. 라벨이 없는 문장 줄은 clauses에 원문 그대로 담는다.
JSON 요청 본문은 사용하지 않는다. 아래 JSON은 폼 파라미터를 설명하는 표현이다.

```json
{
  "text": "개발용 계약서 내용"
}
```
실제 요청 예시:

```sh
curl -X POST -F 'file=@src/main/resources/samples/contract1.txt' http://localhost:8080/api/contracts/extract
curl -X POST --data-urlencode 'text=개발용 계약서 내용' http://localhost:8080/api/contracts/extract
```

응답 `200 application/json` (ContractInfo):

```json
{
  "employerName": "가상별빛상점",
  "workplace": "가상시 상상구 예시로 가상건물",
  "jobDescription": "상품 정리 및 고객 안내",
  "hourlyWage": 15000,
  "weeklyHours": 20.0,
  "workDays": [
    "MONDAY",
    "TUESDAY",
    "WEDNESDAY",
    "THURSDAY",
    "FRIDAY"
  ],
  "startTime": "14:00",
  "endTime": "18:30",
  "breakMinutes": 30,
  "contractStart": "2026-11-01",
  "contractEnd": "2027-02-28",
  "payDay": 10,
  "weeklyHolidayPayIncluded": false,
  "probation": false,
  "probationMonths": null,
  "probationWageRate": null,
  "socialInsuranceMentioned": true,
  "clauses": [
    "시급은 15,000원이며 주휴수당은 시급에 포함하지 않고 별도로 지급한다.",
    "임금은 매월 10일에 전월분을 지급한다.",
    "근무 중 30분의 휴게시간을 자유롭게 이용한다.",
    "사회보험은 관계 법령에 따라 가입한다."
  ]
}
```

## 2. POST /api/checks

요청 `application/json` (ContractInfo):

```json
{
  "employerName": "가상별빛상점",
  "workplace": "가상시 상상구 예시로 가상건물",
  "jobDescription": "상품 정리 및 고객 안내",
  "hourlyWage": 15000,
  "weeklyHours": 20.0,
  "workDays": [
    "MONDAY",
    "TUESDAY",
    "WEDNESDAY",
    "THURSDAY",
    "FRIDAY"
  ],
  "startTime": "14:00",
  "endTime": "18:30",
  "breakMinutes": 30,
  "contractStart": "2026-11-01",
  "contractEnd": "2027-02-28",
  "payDay": 10,
  "weeklyHolidayPayIncluded": false,
  "probation": false,
  "probationMonths": null,
  "probationWageRate": null,
  "socialInsuranceMentioned": true,
  "clauses": [
    "시급은 15,000원이며 주휴수당은 시급에 포함하지 않고 별도로 지급한다.",
    "임금은 매월 10일에 전월분을 지급한다.",
    "근무 중 30분의 휴게시간을 자유롭게 이용한다.",
    "사회보험은 관계 법령에 따라 가입한다."
  ]
}
```

응답 `200 application/json` (CheckReport):

```json
{
  "dangerCount": 0,
  "warningCount": 0,
  "missingCount": 0,
  "estimatedMonthlyPay": 1564286,
  "items": [
    {
      "id": "minimum-wage",
      "title": "최저임금 이상",
      "level": "OK",
      "originalText": "시급은 15,000원이며 주휴수당은 시급에 포함하지 않고 별도로 지급한다.",
      "reason": "시급 15,000원은 2026년 최저임금 10,320원 이상입니다.",
      "legalBasis": "최저임금법 제6조(최저임금의 효력)",
      "questionForEmployer": null
    },
    {
      "id": "weekly-holiday-pay",
      "title": "주휴수당 별도 지급",
      "level": "OK",
      "originalText": "시급은 15,000원이며 주휴수당은 시급에 포함하지 않고 별도로 지급한다.",
      "reason": "주휴수당을 시급과 별도로 지급한다고 적혀 있습니다.",
      "legalBasis": "근로기준법 제55조(휴일), 제18조(단시간근로자의 근로조건)",
      "questionForEmployer": null
    }
  ]
}
```

위 items는 앞의 2건만 보인 것이다. `CheckLevel`: DANGER, WARNING, MISSING, OK. 건수는 해당 수준의 items 개수이고 OK는 집계하지 않는다.
items는 DANGER, WARNING, MISSING, OK 순이다. originalText·questionForEmployer는 없으면 null이다.

`ContractChecker`의 점검 항목(id): 최저임금(minimum-wage), 주휴수당(weekly-holiday-pay), 휴게시간(break-time),
임금 지급일(pay-day), 필수 기재사항 누락(missing-*), 주 40·52시간 초과(weekly-hours-limit),
위약금 조항(penalty-clause-n), 임금 미지급·공제 조항(unpaid-clause-n), 수습 감액(probation), 사회보험(social-insurance).
최저임금은 계약 시작 연도 기준이며 표(2025년 10,030원, 2026년 10,320원)에 없는 연도는 가장 가까운 연도 값으로 비교한다.
estimatedMonthlyPay는 시급 × (주 근로시간 + 주휴시간) × (365 ÷ 7 ÷ 12)의 세전 금액이다.
주휴시간은 주 15시간 이상이고 시급에 포함되지 않은 경우에만 더하며, 시급이나 주 근로시간이 없으면 null이다.

## 3. POST /api/supports/match

요청 `application/json` (MatchRequest):

```json
{
  "profile": {
    "age": 22,
    "sido": "가상시",
    "sigungu": "상상구",
    "status": "대학생",
    "housing": "예시 거주형태",
    "householdIncomeLevel": "미확인"
  },
  "contract": {
    "employerName": "가상별빛상점",
    "workplace": "가상시 상상구 예시로 가상건물",
    "jobDescription": "상품 정리 및 고객 안내",
    "hourlyWage": 15000,
    "weeklyHours": 20.0,
    "workDays": [
      "MONDAY",
      "TUESDAY",
      "WEDNESDAY",
      "THURSDAY",
      "FRIDAY"
    ],
    "startTime": "14:00",
    "endTime": "18:30",
    "breakMinutes": 30,
    "contractStart": "2026-11-01",
    "contractEnd": "2027-02-28",
    "payDay": 10,
    "weeklyHolidayPayIncluded": false,
    "probation": false,
    "probationMonths": null,
    "probationWageRate": null,
    "socialInsuranceMentioned": true,
    "clauses": [
      "시급은 15,000원이며 주휴수당은 시급에 포함하지 않고 별도로 지급한다.",
      "임금은 매월 10일에 전월분을 지급한다.",
      "근무 중 30분의 휴게시간을 자유롭게 이용한다.",
      "사회보험은 관계 법령에 따라 가입한다."
    ]
  }
}
```

응답 `200 application/json` (List<SupportMatch>):

```json
[
  {
    "id": "support-example-a",
    "name": "예시 청년 근로자 교통비 지원",
    "organization": "가상청년센터",
    "verdict": "ELIGIBLE",
    "reason": "[가상 예시] 나이, 근로시간 조건을 충족합니다.",
    "applyStart": "2026-11-01",
    "applyEnd": "2026-11-30",
    "applyUrl": "https://example.com/support-a",
    "requiredDocuments": [
      "예시 신청서",
      "근로계약서 사본"
    ]
  },
  {
    "id": "support-example-b",
    "name": "예시 대학생 근로 장학 지원",
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
    ]
  }
]
```

위 응답은 5건 중 2건만 보인 것이다. `MatchVerdict`: ELIGIBLE, NEED_CHECK, NOT_ELIGIBLE 순으로 정렬한다.
`SupportMatcher`에 가상 사업 5건이 들어 있고 나이, 시·도, 상태, 거주 형태, 가구 소득, 주 근로시간 조건을 비교한다.
조건에 맞지 않는 값이 하나라도 있으면 NOT_ELIGIBLE, 값이 없거나 "미확인"인 조건이 있으면 NEED_CHECK다.
모든 사업·기관·조건·일정·서류는 가상이며 example.com은 예시 링크다. 실제 지원 조건이나 금액을 뜻하지 않는다.

프로필 선택지: status는 대학생·구직자·재직자·기타, housing은 월세·전세·자가·가족과 거주·기숙사·기타,
householdIncomeLevel은 중위소득 50% 이하·100% 이하·150% 이하·150% 초과·미확인이다. profile과 contract는 null일 수 있다.

## 4. POST /api/calendar/events

요청 `application/json` (ContractInfo):

```json
{
  "employerName": "가상별빛상점",
  "workplace": "가상시 상상구 예시로 가상건물",
  "jobDescription": "상품 정리 및 고객 안내",
  "hourlyWage": 15000,
  "weeklyHours": 20.0,
  "workDays": [
    "MONDAY",
    "TUESDAY",
    "WEDNESDAY",
    "THURSDAY",
    "FRIDAY"
  ],
  "startTime": "14:00",
  "endTime": "18:30",
  "breakMinutes": 30,
  "contractStart": "2026-11-01",
  "contractEnd": "2027-02-28",
  "payDay": 10,
  "weeklyHolidayPayIncluded": false,
  "probation": false,
  "probationMonths": null,
  "probationWageRate": null,
  "socialInsuranceMentioned": true,
  "clauses": [
    "시급은 15,000원이며 주휴수당은 시급에 포함하지 않고 별도로 지급한다.",
    "임금은 매월 10일에 전월분을 지급한다.",
    "근무 중 30분의 휴게시간을 자유롭게 이용한다.",
    "사회보험은 관계 법령에 따라 가입한다."
  ]
}
```

응답 `200 application/json` (List<CalendarEvent>):

```json
[
  {
    "id": "work-2026-11-02",
    "title": "가상별빛상점 근무",
    "type": "WORK",
    "date": "2026-11-02",
    "startTime": "14:00",
    "endTime": "18:30",
    "description": "가상시 상상구 예시로 가상건물 · 휴게시간 30분",
    "sourceLink": "/confirm.html"
  },
  {
    "id": "payday-2026-11-10",
    "title": "급여일",
    "type": "PAYDAY",
    "date": "2026-11-10",
    "startTime": null,
    "endTime": null,
    "description": "매월 10일 지급",
    "sourceLink": "/confirm.html"
  },
  {
    "id": "payday-2027-03-10",
    "title": "급여일",
    "type": "PAYDAY",
    "date": "2027-03-10",
    "startTime": null,
    "endTime": null,
    "description": "계약 종료 후 정산분 지급 여부를 확인하세요.",
    "sourceLink": "/confirm.html"
  }
]
```

`EventType`: PAYDAY, WORK, CONTRACT_END, PROBATION_END, SUPPORT_DEADLINE, TODO, CUSTOM.
위 응답은 일부만 보인 것이며 날짜순으로 정렬된다. 종일 일정의 startTime과 endTime은 null이다. 외부 캘린더 연동이나 저장은 하지 않는다.
`CalendarEventGenerator`는 계약 시작일이 없으면 []를 반환한다. 근무일(work-날짜)은 시작일부터 종료일까지의 근무 요일이며,
종료일이 없으면 3개월, 길어도 12개월까지만 만든다. 급여일(payday-날짜)은 시작일 다음 날부터 종료 한 달 뒤까지의 매월 지급일이다.
수습 종료(probation-end)는 수습 개월 수가 있을 때, 계약 종료(contract-end)는 종료일이 있을 때 만든다.
SUPPORT_DEADLINE, TODO, CUSTOM은 서버가 만들지 않고 화면에서 추가해 localStorage에 함께 저장한다.
JSON API는 유효한 JSON 본문이 필요하며 형식이 깨지거나 본문이 없으면 기본 Spring MVC 오류 응답을 사용한다.

## 프런트 공통 도구

모든 화면에서 `/js/common.js`, `/js/ui.js`, 화면별 `/js/<화면>.js`를 순서대로 불러온다. 외부 라이브러리는 없다.
`ui.js`의 `window.EasytonUI`는 표시용 도구(h, won, dateLabel, errorText, notify)와
점검·일정 생성을 함께 실행해 저장하는 analyze(contract), 사용자 추가 일정을 보존하는 saveGeneratedEvents(events)를 제공한다.
`window.Easyton`의 함수는 다음과 같다.

| 함수 | 동작 |
| --- | --- |
| saveContractInfo(value) / getContractInfo() | 계약 저장 / 조회, 기본 null |
| saveCheckReport(value) / getCheckReport() | 리포트 저장 / 조회, 기본 null |
| saveCalendarEvents(value) / getCalendarEvents() | 일정 배열 저장 / 조회, 기본 [] |
| saveUserProfile(value) / getUserProfile() | 프로필 저장 / 조회, 기본 null |
| saveSupportMatches(value) / getSupportMatches() | 추천 결과 배열 저장 / 조회, 기본 [] |
| apiFetch(url, options) | JSON 응답 반환, HTTP 오류 시 Error, 204는 null |
| postJson(url, body) | JSON POST |
| extractContract({file, text}) | multipart POST |

save 함수에 null을 넣으면 해당 키를 삭제한다. localStorage 키는 각각
`easyton.contractInfo`, `easyton.checkReport`, `easyton.calendarEvents`,
`easyton.userProfile`, `easyton.supportMatches`이다.
손상된 JSON은 조회 기본값을 반환하고, 저장소 접근·용량 오류는 호출자에게 전달한다.
localStorage는 브라우저에 남으므로 개발에는 제공된 가상 샘플을 사용한다.
fetch 함수는 자동 저장하지 않으며 각 화면 담당자가 저장 함수를 호출한다.
날짜·시간은 Date 객체로 변환하지 않고 API 문자열을 그대로 저장한다.

```js
const contract = await Easyton.extractContract({ text: "개발용 계약서" });
Easyton.saveContractInfo(contract);
const report = await Easyton.postJson("/api/checks", contract);
Easyton.saveCheckReport(report);
const events = await Easyton.postJson("/api/calendar/events", contract);
Easyton.saveCalendarEvents(events);
```

## 샘플과 JSON 설정

`src/main/resources/samples/contract1~3.txt`와 같은 번호의 JSON이 한 쌍이다.
1번은 정상 표시용 예시, 2번은 매우 낮은 시급과 주휴수당 미기재,
3번은 위약금 조항·급여 미지급 조항과 지급일·휴게시간 미기재 예시다. 법률 적합성 검증을 수행한 자료는 아니다.
연락처·주민등록번호는 수록하지 않으며 모든 인물·상호·장소는 가상이다.
샘플은 클래스패스 리소스이며 정적 다운로드 경로에는 노출하지 않는다.

Spring Boot 4의 기본 Jackson 3 `tools.jackson.databind.json.JsonMapper`를 사용한다.
날짜·시간은 record 필드의 `com.fasterxml.jackson.annotation.JsonFormat`으로 고정한다.
별도 ObjectMapper나 Jackson 2 의존성, 전역 날짜 설정은 추가하지 않는다.
공식 참고: [Spring Boot JSON](https://docs.spring.io/spring-boot/reference/features/json.html),
[Boot 4 마이그레이션 가이드](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide).
