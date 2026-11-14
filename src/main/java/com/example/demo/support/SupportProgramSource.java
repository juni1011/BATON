package com.example.demo.support;

import java.util.List;

/** 매칭에 쓸 지원사업 목록을 공급한다. 외부 API 연동은 이 인터페이스의 구현체로 추가한다. */
public interface SupportProgramSource {
    /** 같은 시점의 사업 목록. 실패 시에도 예외 대신 마지막으로 성공한 목록(없으면 빈 목록)을 돌려준다. */
    Catalog catalog();

    record Catalog(List<SupportProgram> programs) {}
}
