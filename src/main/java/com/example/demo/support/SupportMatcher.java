package com.example.demo.support;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 지원사업 목록({@link SupportProgramSource})과 프로필을 조건별로 비교한다.
 */
@Component
public class SupportMatcher {
    private final SupportProgramSource source;

    public SupportMatcher(SupportProgramSource source) {
        this.source = source;
    }

    /** 소득 구간 선택지. 뒤로 갈수록 소득이 높다. 이 밖의 값(미확인 등)은 확인 필요로 본다. */
    public static final List<String> INCOME_LEVELS =
            List.of("중위소득 50% 이하", "중위소득 100% 이하", "중위소득 150% 이하", "중위소득 150% 초과");

    /** 응답에 담는 최대 건수. 실제 사업은 수천 건이라 판정 순서대로 상위만 돌려준다. */
    public static final int MAX_RESULTS = 50;

    /** 사업마다 판정하고 상위 {@link #MAX_RESULTS}건을 돌려준다. */
    public List<SupportMatch> match(MatchRequest request) {
        UserProfile profile = request == null ? null : request.profile();
        SupportProgramSource.Catalog catalog = source.catalog();
        List<SupportMatch> matches = new ArrayList<>();
        for (SupportProgram program : catalog.programs()) matches.add(evaluate(program, profile, catalog.sample()));
        // 가능, 확인 필요, 불가 순으로, 같은 판정 안에서는 마감이 가까운 사업을 먼저(마감일을 모르면 뒤로) 둔다.
        matches.sort(Comparator.<SupportMatch>comparingInt(match -> match.verdict().ordinal())
                .thenComparing(SupportMatch::applyEnd, Comparator.nullsLast(Comparator.naturalOrder())));
        return matches.size() > MAX_RESULTS ? List.copyOf(matches.subList(0, MAX_RESULTS)) : matches;
    }

    private SupportMatch evaluate(SupportProgram p, UserProfile profile, boolean sample) {
        List<String> failed = new ArrayList<>();
        List<String> unknown = new ArrayList<>();
        List<String> passed = new ArrayList<>();

        Integer age = profile == null ? null : profile.age();
        if (p.minAge() != null || p.maxAge() != null) {
            String range = "만 " + (p.minAge() != null ? p.minAge() + "세 이상 " : "")
                    + (p.maxAge() != null ? p.maxAge() + "세 이하" : "");
            if (age == null) unknown.add("나이(" + range.strip() + ")");
            else if ((p.minAge() != null && age < p.minAge()) || (p.maxAge() != null && age > p.maxAge())) {
                failed.add("나이 조건(" + range.strip() + ")에 맞지 않습니다");
            } else passed.add("나이");
        }

        if (p.sido() != null) {
            String sido = text(profile == null ? null : profile.sido());
            if (sido == null) unknown.add("거주 지역(" + p.sido() + ")");
            else if (!sido.equals(p.sido())) failed.add(p.sido() + " 거주자 대상입니다");
            else passed.add("거주 지역");
        }

        check(p.statuses(), text(profile == null ? null : profile.status()), "현재 상태", passed, unknown, failed);
        check(p.housings(), text(profile == null ? null : profile.housing()), "거주 형태", passed, unknown, failed);

        if (p.maxIncomeLevel() != null) {
            String income = text(profile == null ? null : profile.householdIncomeLevel());
            int level = income == null ? -1 : INCOME_LEVELS.indexOf(income);
            if (level < 0) unknown.add("가구 소득(" + p.maxIncomeLevel() + ")");
            else if (level > INCOME_LEVELS.indexOf(p.maxIncomeLevel())) {
                failed.add("가구 소득 조건(" + p.maxIncomeLevel() + ")에 맞지 않습니다");
            } else passed.add("가구 소득");
        }

        unknown.addAll(p.unverified());

        MatchVerdict verdict;
        String reason;
        if (!failed.isEmpty()) {
            verdict = MatchVerdict.NOT_ELIGIBLE;
            reason = String.join(", ", failed) + ".";
        } else if (!unknown.isEmpty()) {
            verdict = MatchVerdict.NEED_CHECK;
            reason = "확인이 필요한 조건: " + String.join(", ", unknown) + ".";
        } else {
            verdict = MatchVerdict.ELIGIBLE;
            reason = passed.isEmpty() ? "별도 조건이 없습니다."
                    : String.join(", ", passed) + " 조건을 충족합니다.";
        }
        return new SupportMatch(p.id(), p.name(), p.organization(), verdict, (sample ? "[가상 예시] " : "") + reason,
                p.applyStart(), p.applyEnd(), p.applyUrl(), p.documents(), sample);
    }

    private static void check(List<String> allowed, String value, String name,
                              List<String> passed, List<String> unknown, List<String> failed) {
        if (allowed == null) return;
        String list = String.join("·", allowed);
        if (value == null || value.equals("미확인")) unknown.add(name + "(" + list + ")");
        else if (!allowed.contains(value)) failed.add(name + " 조건(" + list + ")에 맞지 않습니다");
        else passed.add(name);
    }

    private static String text(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
