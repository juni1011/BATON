# 대한민국 행정경계 데이터 출처

## 포함한 데이터

- 기준 시점: **2026년 4월 1일**.
- 범위: 17개 시·도, 255개 시·군·구. 일반구가 있는 도시는 구 단위로 포함합니다.
- 지도용 데이터: `korea-regions.js`.
- 경계의 원자료: 통계청 통계지리정보서비스(SGIS).
- 행정구역 변경 반영·가공: [vuski/admdongkor](https://github.com/vuski/admdongkor).
- 고정 원본 커밋: `d5d5a821c8de6cc337d47eeca604d7af76271c9b`.

원본 파일:

- [시군구 light parquet](https://raw.githubusercontent.com/vuski/admdongkor/d5d5a821c8de6cc337d47eeca604d7af76271c9b/parquet/simplified/sgg_20260401_light.parquet)
- [시도 light parquet](https://raw.githubusercontent.com/vuski/admdongkor/d5d5a821c8de6cc337d47eeca604d7af76271c9b/parquet/simplified/sido_20260401_light.parquet)

## 라이선스와 출처 표시

가공된 경계 데이터에는 **Creative Commons Attribution 4.0 International (CC BY 4.0)**이 적용됩니다. 원자료의 **공공누리 제1유형(출처표시)** 조건도 유지됩니다.

- [원 데이터의 LICENSE-DATA](https://github.com/vuski/admdongkor/blob/d5d5a821c8de6cc337d47eeca604d7af76271c9b/LICENSE-DATA)
- [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/)
- [공공누리 제1유형](https://www.kogl.or.kr/info/licenseType1.do)

원 데이터가 요구하는 출처 표시:

> 본 데이터는 통계청 통계지리정보서비스(SGIS, https://sgis.kostat.go.kr)에서 공공누리 제1유형으로 개방한 행정동 경계를 가공한 것이며(가공: vuski/admdongkor, https://github.com/vuski/admdongkor), CC BY 4.0으로 배포됩니다.

지원사업 추천 페이지 하단의 서비스 제작자 문구 바로 위에 출처·기준 시점·라이선스 링크와 가공 여부를 표시하고, 배포 소스에는 이 문서를 함께 보관합니다. 소스 코드의 라이선스와 지도 데이터의 라이선스는 구분됩니다.

## 이 프로젝트에서 한 가공

1. `admdongkor`의 시군구 light 경계를 빌드 단계에서 받아 로컬 파일로 저장했습니다.
2. mapshaper로 경계 정합성을 정리하고, weighted 방식과 100m 간격으로 단순화했습니다. 모든 255개 시군구 도형을 유지했습니다.
3. 같은 시군구 경계를 시도 코드별로 합쳐 시도 외곽선을 만들었습니다. 시도와 시군구가 같은 공유 경계를 사용합니다.
4. 60,000단계 양자화 TopoJSON의 공유 아크를 signed-varint/base64로 손실 없이 압축했습니다. 런타임은 로컬 데이터만 읽으며 외부 요청은 하지 않습니다.
5. Mercator 좌표를 SVG 뷰박스에 맞추고, 화면 좌표는 소수 둘째 자리로 표시합니다. 원래 위경도 위치를 유지하며 섬을 임의로 이동하지 않았습니다.
6. 위치 핀은 각 실제 도형의 가장 큰 외곽선 내부점을 사용합니다. 모든 17개 시도와 255개 시군구 핀이 최종 도형 내부에 있는지 검증했습니다.

데이터 모듈은 약 186KB입니다. 지도 선택에 사용하는 명칭과 코드는 위 기준 시점의 데이터에 따릅니다.
