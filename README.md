# Idiots

## SSAP Idiots


## 실행

Java 21이 필요합니다. 프로젝트 루트에서 실행합니다.

```sh
./gradlew bootRun
```

접속 주소: http://localhost:8080/ (index.html).
화면: /upload.html, /confirm.html, /report.html, /support.html, /calendar.html.
업로드 → 추출 값 확인 → 점검 리포트 → 지원사업 추천·캘린더 순서로 이어지며, 업로드 화면의 "가상 예시 넣기"로 바로 시연할 수 있습니다.
외부 API(AI·OCR·공공데이터)는 연동하지 않았습니다. 그래서 .txt 파일과 붙여넣은 텍스트만 읽고, 지원사업은 가상 목록입니다.
서버 종료는 Ctrl+C를 사용합니다.

## 테스트

```sh
./gradlew test
```

컨텍스트 로딩, 샘플 3개의 텍스트 추출·점검·일정·매칭 결과, JSON 날짜·시간 형식을 확인합니다.
결과 보고서는 `build/reports/tests/test/index.html`에서 확인합니다.

## 팀 공통 규약

[docs/API.md](docs/API.md)에 공통 필드, 요청·응답 예시, 담당 분담, 프런트 헬퍼가 있습니다.
common 패키지와 common.js를 바꿀 때는 팀 전체에 먼저 알립니다.
지원사업과 샘플 계약서는 모두 가상이며, 점검 결과와 예상 월급은 규칙으로 계산한 참고 정보입니다.
본 서비스는 정보 제공용이며 법률 자문이 아닙니다.
