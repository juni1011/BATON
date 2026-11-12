package com.example.demo.support;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

/** 화면 시연용 가상 지원사업. 모든 사업·기관·조건·일정은 실제 제도가 아니다. */
@Component
public class SampleSupportProgramSource implements SupportProgramSource {
    private static final List<SupportProgram> PROGRAMS = List.of(
            new SupportProgram("support-example-a", "예시 청년 교통비 지원", "가상청년센터", 19, 34, null,
                    null, null, null,
                    LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), "https://example.com/support-a",
                    List.of("예시 신청서")),
            new SupportProgram("support-example-b", "예시 대학생 장학 지원", "가상지원재단", null, null, null,
                    List.of("대학생"), null, "중위소득 100% 이하",
                    LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31), "https://example.com/support-b",
                    List.of("예시 신청서", "재학증명서", "소득 확인서")),
            new SupportProgram("support-example-c", "예시 청년 월세 지원", "가상주거지원센터", 19, 34, null,
                    null, List.of("월세"), "중위소득 150% 이하",
                    LocalDate.of(2026, 11, 15), LocalDate.of(2026, 12, 15), "https://example.com/support-c",
                    List.of("예시 신청서", "임대차계약서 사본", "소득 확인서")),
            new SupportProgram("support-example-d", "예시 가상시 청년 상담 지원", "가상시청", 15, 39, "가상시",
                    null, null, null,
                    LocalDate.of(2026, 11, 1), LocalDate.of(2027, 1, 31), "https://example.com/support-d",
                    List.of("예시 신청서")),
            new SupportProgram("support-example-e", "예시 청년 생활안정 지원", "가상생활지원단", null, null, null,
                    List.of("대학생", "구직자", "재직자"), null, "중위소득 100% 이하",
                    LocalDate.of(2026, 12, 1), LocalDate.of(2027, 1, 15), "https://example.com/support-e",
                    List.of("예시 신청서", "소득 확인서")));

    @Override
    public Catalog catalog() {
        return new Catalog(PROGRAMS, true);
    }
}
