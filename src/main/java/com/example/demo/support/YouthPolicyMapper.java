package com.example.demo.support;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import tools.jackson.databind.JsonNode;

/**
 * 온통청년 청년정책 API({@code getPlcy})의 정책 한 건을 {@link SupportProgram}으로 바꾼다.
 * 신분·소득·지역은 코드값으로 오고 코드표를 해석하지 않으므로 판단하지 않고 "확인 필요"로 남긴다.
 */
final class YouthPolicyMapper {
    static final String DETAIL_CHECK = "소득·신분·거주지 등 세부 자격(상세 공고 확인)";
    private static final Pattern PERIOD = Pattern.compile("^(\\d{8})\\s*~\\s*(\\d{8})$");
    private static final int MAX_EXTRA_LENGTH = 80;
    private static final int MAX_DOCUMENT_LENGTH = 120;

    private YouthPolicyMapper() {
    }

    static Optional<SupportProgram> map(JsonNode policy) {
        String id = text(policy, "plcyNo");
        String name = text(policy, "plcyNm");
        if (id == null || name == null) return Optional.empty();
        String organization = text(policy, "sprvsnInstCdNm");
        if (organization == null) organization = text(policy, "operInstCdNm");

        LocalDate start = null;
        LocalDate end = null;
        String period = text(policy, "aplyYmd");
        Matcher matcher = period == null ? null : PERIOD.matcher(period);
        if (matcher != null && matcher.matches()) {
            try {
                start = LocalDate.parse(matcher.group(1), DateTimeFormatter.BASIC_ISO_DATE);
                end = LocalDate.parse(matcher.group(2), DateTimeFormatter.BASIC_ISO_DATE);
            } catch (DateTimeParseException e) {
                start = null;
                end = null;
            }
        }

        List<String> unverified = new ArrayList<>();
        unverified.add(DETAIL_CHECK);
        String extra = text(policy, "addAplyQlfcCndCn");
        if (extra != null) {
            extra = extra.replaceAll("\\s+", " ");
            unverified.add("추가 자격: " + (extra.length() > MAX_EXTRA_LENGTH
                    ? extra.substring(0, MAX_EXTRA_LENGTH) + "…" : extra));
        }

        return Optional.of(new SupportProgram(id, name, organization,
                positive(policy, "sprtTrgtMinAge"), positive(policy, "sprtTrgtMaxAge"), null,
                null, null, null, start, end,
                firstUrl(policy, "aplyUrlAddr", "refUrlAddr1", "refUrlAddr2"),
                documents(text(policy, "sbmsnDcmntCn")), unverified,
                categories(text(policy, "lclsfNm"), text(policy, "mclsfNm")),
                codes(text(policy, "zipCd"))));
    }

    /** 비어 있거나 "해당없음"이면 null. 앞뒤 공백은 제거한다. */
    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) return null;
        String text = value.asString("").strip();
        return text.isEmpty() || text.equals("해당없음") ? null : text;
    }

    /** 0이나 숫자가 아닌 값은 "제한 없음"으로 본다(데이터에서 0은 미입력을 뜻한다). */
    private static Integer positive(JsonNode node, String field) {
        String text = text(node, field);
        if (text == null) return null;
        try {
            int value = Integer.parseInt(text);
            return value > 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String firstUrl(JsonNode node, String... fields) {
        for (String field : fields) {
            String url = text(node, field);
            if (url != null && (url.startsWith("https://") || url.startsWith("http://"))) return url;
        }
        return null;
    }

    /** 대분류와 중분류를 모두 분야로 쓴다(예: "일자리"와 "창업"). 여러 분류는 쉼표로 이어져 오고 중복도 있다. */
    private static List<String> categories(String large, String medium) {
        return Stream.concat(codes(large).stream(), codes(medium).stream()).distinct().toList();
    }

    /** 쉼표로 이어진 값을 나눈다. 법정시군구코드(zipCd)와 분류명이 이 형식이다. */
    private static List<String> codes(String text) {
        if (text == null) return List.of();
        return Arrays.stream(text.split(",")).map(String::strip).filter(s -> !s.isEmpty()).distinct().toList();
    }

    private static List<String> documents(String text) {
        if (text == null) return List.of();
        // 자유 서술이라 쉼표로 나누면 문장이 조각나므로 줄 단위로만 나누고 너무 긴 줄은 줄인다.
        return Arrays.stream(text.split("\\R")).map(String::strip).filter(s -> !s.isEmpty())
                .map(s -> s.length() > MAX_DOCUMENT_LENGTH ? s.substring(0, MAX_DOCUMENT_LENGTH) + "…" : s).toList();
    }
}
