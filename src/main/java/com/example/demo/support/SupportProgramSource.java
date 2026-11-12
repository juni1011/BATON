package com.example.demo.support;

import java.util.List;

/** 매칭에 쓸 지원사업 목록을 공급한다. 외부 API 연동은 이 인터페이스의 구현체로 추가한다. */
public interface SupportProgramSource {
    /** 현재 사용할 사업 목록. 실패 시에도 예외 대신 마지막으로 성공한 목록이나 빈 목록을 돌려준다. */
    List<SupportProgram> programs();

    /** 사업 데이터가 가상 예시인지 여부. 화면과 판정 사유의 안내 문구에 쓴다. */
    boolean sample();
}
