package com.example.demo.support;

import java.util.List;

/** 매칭에 쓸 지원사업 목록을 공급한다. 외부 API 연동은 이 인터페이스의 구현체로 추가한다. */
public interface SupportProgramSource {
    /**
     * 같은 시점의 사업 목록과 가상 예시 여부. 실패 시에도 예외 대신 마지막으로 성공한 목록이나 예시 목록을 돌려준다.
     * 목록과 예시 여부가 어긋나지 않도록 한 번에 돌려준다.
     */
    Catalog catalog();

    /** @param sample 목록이 가상 예시이면 true. 화면과 판정 사유의 안내 문구에 쓴다. */
    record Catalog(List<SupportProgram> programs, boolean sample) {}
}
