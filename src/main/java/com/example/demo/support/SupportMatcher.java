package com.example.demo.support;

import com.example.demo.common.ContractInfo;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 가상 지원사업 목록과 프로필·계약 정보를 조건별로 비교한다.
 * 모든 사업·기관·조건·일정은 화면 시연용 가상 데이터이며 실제 제도가 아니다.
 */
@Component
public class SupportMatcher {
    /** 소득 구간 선택지. 뒤로 갈수록 소득이 높다. 이 밖의 값(미확인 등)은 확인 필요로 본다. */
    public static final List<String> INCOME_LEVELS =
            List.of("중위소득 50% 이하", "중위소득 100% 이하", "중위소득 150% 이하", "중위소득 150% 초과");

    /** 가상 사업 한 건의 조건. null인 조건은 제한이 없다는 뜻이다. */
    record Program(String id, String name, String organization, Integer minAge, Integer maxAge, String sido,
                   List<String> statuses, List<String> housings, String maxIncomeLevel,
                   Double minWeeklyHours, Double maxWeeklyHours,
                   LocalDate applyStart, LocalDate applyEnd, String applyUrl, List<String> documents) {}

    static final List<Program> PROGRAMS = List.of(
            new Program("support-example-a", "예시 청년 근로자 교통비 지원", "가상청년센터", 19, 34, null,
                    null, null, null, 15.0, null,
                    LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), "https://example.com/support-a",
                    List.of("예시 신청서", "근로계약서 사본")),
            new Program("support-example-b", "예시 대학생 근로 장학 지원", "가상지원재단", null, null, null,
                    List.of("대학생"), null, "중위소득 100% 이하", null, null,
                    LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31), "https://example.com/support-b",
                    List.of("예시 신청서", "재학증명서", "소득 확인서")),
            new Program("support-example-c", "예시 청년 월세 지원", "가상주거지원센터", 19, 34, null,
                    null, List.of("월세"), "중위소득 150% 이하", null, null,
                    LocalDate.of(2026, 11, 15), LocalDate.of(2026, 12, 15), "https://example.com/support-c",
                    List.of("예시 신청서", "임대차계약서 사본", "소득 확인서")),
            new Program("support-example-d", "예시 가상시 청년 알바 권리 상담", "가상시청", 15, 39, "가상시",
                    null, null, null, null, null,
                    LocalDate.of(2026, 11, 1), LocalDate.of(2027, 1, 31), "https://example.com/support-d",
                    List.of("예시 신청서")),
            new Program("support-example-e", "예시 단시간 근로자 생활안정 지원", "가상생활지원단", null, null, null,
                    List.of("대학생", "구직자", "재직자"), null, "중위소득 100% 이하", null, 30.0,
                    LocalDate.of(2026, 12, 1), LocalDate.of(2027, 1, 15), "https://example.com/support-e",
                    List.of("예시 신청서", "근로계약서 사본", "소득 확인서")));

    /** 사업마다 판정하고 가능, 확인 필요, 불가 순으로 정렬한다. */
    public List<SupportMatch> match(MatchRequest request) {
        UserProfile profile = request == null ? null : request.profile();
        ContractInfo contract = request == null ? null : request.contract();
        List<SupportMatch> matches = new ArrayList<>();
        for (Program program : PROGRAMS) matches.add(evaluate(program, profile, contract));
        matches.sort(Comparator.comparing(SupportMatch::verdict));
        return matches;
    }

    private SupportMatch evaluate(Program p, UserProfile profile, ContractInfo contract) {
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

        if (p.minWeeklyHours() != null || p.maxWeeklyHours() != null) {
            Double hours = contract == null ? null : contract.weeklyHours();
            String range = "주 " + (p.minWeeklyHours() != null ? trim(p.minWeeklyHours()) + "시간 이상" : "")
                    + (p.maxWeeklyHours() != null ? trim(p.maxWeeklyHours()) + "시간 미만" : "") + " 근로";
            if (hours == null) unknown.add("근로시간(" + range + ")");
            else if ((p.minWeeklyHours() != null && hours < p.minWeeklyHours())
                    || (p.maxWeeklyHours() != null && hours >= p.maxWeeklyHours())) {
                failed.add("근로시간 조건(" + range + ")에 맞지 않습니다");
            } else passed.add("근로시간");
        }

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
        return new SupportMatch(p.id(), p.name(), p.organization(), verdict, "[가상 예시] " + reason,
                p.applyStart(), p.applyEnd(), p.applyUrl(), p.documents());
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

    private static String trim(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }
}
