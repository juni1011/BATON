package com.example.demo.check;


/** 조항별 점검 결과와 사용자 확인 질문을 전달한다. */
public record CheckItem(String id, String title, CheckLevel level, String originalText, String reason,
                        String legalBasis, String questionForEmployer) {}
