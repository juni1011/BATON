# Idiots

## SSAP Idiots


## 개발용 스텁 실행

Java 21이 필요합니다. 프로젝트 루트에서 실행합니다.

```sh
./gradlew bootRun
```

접속 주소: http://localhost:8080/ (index.html).
화면: /upload.html, /confirm.html, /report.html, /support.html, /calendar.html.
모든 화면은 담당자 안내만 있는 빈 화면이며 실제 기능은 아직 구현하지 않았습니다.
서버 종료는 Ctrl+C를 사용합니다.

## 테스트

```sh
./gradlew test
```

컨텍스트 로딩, MockMvc 스텁 API, 샘플 JSON 3개 역직렬화와 날짜·시간 형식을 확인합니다.
결과 보고서는 `build/reports/tests/test/index.html`에서 확인합니다.

## 팀 공통 규약

[docs/API.md](docs/API.md)에 공통 필드, 요청·응답 예시, 담당 분담, 프런트 헬퍼가 있습니다.
common 패키지와 common.js를 바꿀 때는 팀 전체에 먼저 알립니다.
지원사업·계약서·급여 계산 결과는 모두 개발용 고정 예시입니다.
본 서비스는 정보 제공용이며 법률 자문이 아닙니다.
