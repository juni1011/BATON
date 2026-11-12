package com.example.demo.support;

import java.time.LocalDate;
import java.util.List;

/** 지원사업 한 건의 신청 조건. null인 조건은 제한이 없거나 알 수 없다는 뜻이며 매칭에서 "확인 필요"로 다룬다. */
public record SupportProgram(String id, String name, String organization, Integer minAge, Integer maxAge, String sido,
                             List<String> statuses, List<String> housings, String maxIncomeLevel,
                             LocalDate applyStart, LocalDate applyEnd, String applyUrl, List<String> documents) {}
