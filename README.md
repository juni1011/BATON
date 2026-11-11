# Idiots

## SSAP Idiots

청년 지원사업 도우미: 내 정보에 맞는 지원사업을 찾고 신청 마감일과 내 일정을 캘린더로 관리합니다.

## 실행

Java 21이 필요합니다. 프로젝트 루트에서 실행합니다.

```sh
./gradlew bootRun
```

접속 주소: http://localhost:8080/ (index.html).
화면: /support.html(지원사업 추천), /calendar.html(캘린더).
외부 API(공공데이터 등)는 연동하지 않았습니다. 지원사업은 코드에 들어 있는 가상 목록입니다.
서버 종료는 Ctrl+C를 사용합니다.

## 테스트

```sh
./gradlew test
```

컨텍스트 로딩과 지원사업 매칭 판정, JSON 날짜 형식을 확인합니다.
결과 보고서는 `build/reports/tests/test/index.html`에서 확인합니다.

## 팀 공통 규약

[docs/API.md](docs/API.md)에 요청·응답 예시와 프런트 헬퍼가 있습니다.
common.js를 바꿀 때는 팀 전체에 먼저 알립니다.
지원사업은 모두 가상이며 실제 제도·기관·신청 링크가 아닙니다.
