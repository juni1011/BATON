package com.example.demo.support;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 외부 사업 목록을 쓸 때의 판정·정렬·건수 제한을 검증한다. */
class SupportMatcherTests {
    private static SupportProgram program(String id, Integer minAge, Integer maxAge, LocalDate end, List<String> unverified) {
        return new SupportProgram(id, "사업 " + id, "기관", minAge, maxAge, null, null, null, null,
                null, end, null, List.of(), unverified);
    }

    private static SupportMatcher matcher(boolean sample, List<SupportProgram> programs) {
        return new SupportMatcher(() -> new SupportProgramSource.Catalog(programs, sample));
    }

    private static MatchRequest request(Integer age) {
        return new MatchRequest(new UserProfile(age, null, null, null, null, null), null);
    }

    @Test
    void unverifiedConditionsAreNeverEligible() {
        var matches = matcher(false, List.of(program("a", null, null, null, List.of("세부 자격")))).match(request(22));
        assertEquals(MatchVerdict.NEED_CHECK, matches.get(0).verdict());
        assertTrue(matches.get(0).reason().contains("세부 자격"));
        assertFalse(matches.get(0).reason().startsWith("[가상 예시]"));
        assertFalse(matches.get(0).sample());
    }

    @Test
    void ageMismatchIsNotEligibleAndSortedLast() {
        var matches = matcher(false, List.of(
                program("old", 19, 34, null, List.of("세부 자격")),
                program("ok", 19, 39, null, List.of("세부 자격")))).match(request(36));
        assertEquals("ok", matches.get(0).id());
        assertEquals(MatchVerdict.NOT_ELIGIBLE, matches.get(1).verdict());
    }

    @Test
    void sameVerdictIsSortedByNearestDeadlineWithUnknownLast() {
        var matches = matcher(false, List.of(
                program("none", null, null, null, List.of("x")),
                program("late", null, null, LocalDate.of(2027, 1, 1), List.of("x")),
                program("soon", null, null, LocalDate.of(2026, 12, 1), List.of("x")))).match(request(22));
        assertEquals(List.of("soon", "late", "none"), matches.stream().map(SupportMatch::id).toList());
    }

    @Test
    void resultIsLimitedToTopFifty() {
        List<SupportProgram> many = new ArrayList<>();
        for (int i = 0; i < 120; i++) many.add(program("p" + i, i < 100 ? 60 : null, null, null, List.of("x")));
        var matches = matcher(false, many).match(request(22));
        assertEquals(SupportMatcher.MAX_RESULTS, matches.size());
        // 나이 조건이 맞지 않는 사업(100건)보다 가능성이 있는 사업(20건)이 먼저 담긴다.
        assertEquals(20, matches.stream().filter(m -> m.verdict() == MatchVerdict.NEED_CHECK).count());
    }

    @Test
    void sampleCatalogKeepsExampleLabel() {
        var matches = matcher(true, List.of(program("a", null, null, null, List.of()))).match(request(22));
        assertTrue(matches.get(0).reason().startsWith("[가상 예시]"));
        assertTrue(matches.get(0).sample());
    }
}
