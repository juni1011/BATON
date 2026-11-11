package com.example.demo.check;

import java.util.List;

/** 점검 건수와 예상 월급 및 항목 목록을 전달한다. 월급은 원 단위이다. */
public record CheckReport(Integer dangerCount, Integer warningCount, Integer missingCount,
                          Integer estimatedMonthlyPay, List<CheckItem> items) {}
