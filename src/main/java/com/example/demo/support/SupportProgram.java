package com.example.demo.support;

import java.time.LocalDate;
import java.util.List;

/**
 * 지원사업 한 건의 신청 조건. 조건이 null이면 제한이 없다는 뜻이다.
 * 데이터에서 판단할 수 없는 조건은 null로 두지 않고 {@code unverified}에 적어 "확인 필요"로 다룬다.
 */
public record SupportProgram(String id, String name, String organization, Integer minAge, Integer maxAge, String sido,
                             List<String> statuses, List<String> housings, String maxIncomeLevel,
                             LocalDate applyStart, LocalDate applyEnd, String applyUrl, List<String> documents,
                             List<String> unverified) {
    public SupportProgram {
        unverified = unverified == null ? List.of() : unverified;
    }

    /** 확인되지 않은 조건이 없는 사업. */
    public SupportProgram(String id, String name, String organization, Integer minAge, Integer maxAge, String sido,
                          List<String> statuses, List<String> housings, String maxIncomeLevel,
                          LocalDate applyStart, LocalDate applyEnd, String applyUrl, List<String> documents) {
        this(id, name, organization, minAge, maxAge, sido, statuses, housings, maxIncomeLevel,
                applyStart, applyEnd, applyUrl, documents, List.of());
    }
}
