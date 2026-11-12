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
        String category = request == null ? null : text(request.category());
        String sido = profile == null ? null : text(profile.sido());
        for (SupportProgram program : catalog.programs()) {
            if (!regionMatches(program, sido, catalog.sample())) continue;
            if (category != null && program.categories().stream()
                    .noneMatch(value -> categoryKey(value).equals(categoryKey(category)))) continue;
            matches.add(evaluate(program, profile, catalog.sample()));
        }
        // 가능, 확인 필요, 불가 순으로, 같은 판정 안에서는 마감이 가까운 사업을 먼저(마감일을 모르면 뒤로) 둔다.
        matches.sort(Comparator.<SupportMatch>comparingInt(match -> match.verdict().ordinal())
                .thenComparing(SupportMatch::applyEnd, Comparator.nullsLast(Comparator.naturalOrder())));
        return matches.size() > MAX_RESULTS ? List.copyOf(matches.subList(0, MAX_RESULTS)) : matches;
    }

    private static final java.util.Map<String,String> REGIONS = java.util.Map.ofEntries(
            java.util.Map.entry("서울특별시", "11"),
            java.util.Map.entry("부산광역시", "26"),
            java.util.Map.entry("대구광역시", "27"),
            java.util.Map.entry("인천광역시", "28"),
            java.util.Map.entry("광주광역시", "29"),
            java.util.Map.entry("대전광역시", "30"),
            java.util.Map.entry("울산광역시", "31"),
            java.util.Map.entry("세종특별자치시", "36"),
            java.util.Map.entry("경기도", "41"),
            java.util.Map.entry("강원특별자치도", "51"),
            java.util.Map.entry("충청북도", "43"),
            java.util.Map.entry("충청남도", "44"),
            java.util.Map.entry("전북특별자치도", "52"),
            java.util.Map.entry("전라남도", "46"),
            java.util.Map.entry("경상북도", "47"),
            java.util.Map.entry("경상남도", "48"),
            java.util.Map.entry("제주특별자치도", "50"));
    private static String categoryKey(String value) {
        return switch (value.replaceAll("[·･ㆍ ]", "")) {
            case "금융복지문화" -> "복지문화";
            case "교육직업훈련" -> "교육";
            case "참여기반" -> "참여권리";
            default -> value.strip();
        };
    }

    private static boolean regionMatches(SupportProgram p, String sido, boolean sample) {
        if (sido == null) return true;
        if (sample) return p.sido() == null || p.sido().equals(sido);
        String prefix = REGIONS.get(sido);
        return p.regionCodes().stream().anyMatch(code -> code.equals("00000") || code.equals("00")
                || prefix != null && (code.startsWith(prefix)
                || prefix.equals("51") && code.startsWith("42")
                || prefix.equals("52") && code.startsWith("45")));
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
