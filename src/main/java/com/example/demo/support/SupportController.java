package com.example.demo.support;

import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** 담당 C: 가상 지원사업 매칭 API의 고정 응답을 제공한다. */
@RestController
@RequestMapping("/api/supports")
public class SupportController {
    /** 입력을 판정하지 않고 화면 개발용 가상 사업 두 건을 반환한다. */
    @PostMapping("/match")
    public List<SupportMatch> match(@RequestBody MatchRequest request) {
        return List.of(
                new SupportMatch("support-example-a", "예시 지원사업 A", "가상청년센터",
                        MatchVerdict.ELIGIBLE, "가능 상태 표시용 고정 예시입니다. 실제 자격 판정이 아닙니다.",
                        LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                        "https://example.com/support-a", List.of("예시 신청서")),
                new SupportMatch("support-example-b", "예시 지원사업 B", "가상지원재단",
                        MatchVerdict.NEED_CHECK, "추가 확인 상태 표시용 고정 예시입니다.",
                        LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31),
                        "https://example.com/support-b", List.of("예시 신청서", "예시 확인서")));
    }
}
