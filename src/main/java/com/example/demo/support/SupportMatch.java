package com.example.demo.support;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.util.List;

/** 가상 지원사업의 고정 매칭 결과를 전달한다. 실제 자격 판정이 아니다. */
public record SupportMatch(String id, String name, String organization, MatchVerdict verdict, String reason,
                           @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate applyStart,
                           @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate applyEnd,
                           String applyUrl, List<String> requiredDocuments) {}
