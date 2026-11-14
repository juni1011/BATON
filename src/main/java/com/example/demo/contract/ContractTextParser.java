package com.example.demo.contract;

import com.example.demo.common.ContractInfo;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** 계약서 텍스트를 정규식 규칙으로 읽어 ContractInfo를 만든다. 찾지 못한 값은 추정하지 않고 null로 둔다. */
@Component
public class ContractTextParser {
    private static final String DAYS = "월화수목금토일";
    private static final Pattern LABEL = Pattern.compile("^([가-힣A-Za-z0-9 ]{1,12}?)\\s*[:：]\\s*(.+)$");
    private static final Pattern DATE = Pattern.compile("(\\d{4})\\s*[-./년]\\s*(\\d{1,2})\\s*[-./월]\\s*(\\d{1,2})");
    private static final Pattern TIME_RANGE =
            Pattern.compile("(\\d{1,2}):(\\d{2})\\s*[~\\-–]\\s*(\\d{1,2}):(\\d{2})");
    private static final Pattern WAGE = Pattern.compile("시급[^\\d\\n]{0,10}(\\d[\\d,]*)\\s*원");
    private static final Pattern WEEKLY_HOURS =
            Pattern.compile("주\\s*(?:소정\\s*근로\\s*시간)?\\s*[:：은]?\\s*(\\d+(?:\\.\\d+)?)\\s*시간");
    private static final Pattern PAY_DAY = Pattern.compile("매월\\s*(\\d{1,2})\\s*일");
    private static final Pattern BREAK_MIN =
            Pattern.compile("(\\d+)\\s*분[의간]?\\s*휴게|휴게\\s*(?:시간)?\\s*[:：은]?\\s*(\\d+)\\s*분");
    private static final Pattern BREAK_HOUR =
            Pattern.compile("(\\d+)\\s*시간[의]?\\s*휴게|휴게\\s*(?:시간)?\\s*[:：은]?\\s*(\\d+)\\s*시간");
    private static final Pattern DAY_RANGE =
            Pattern.compile("([" + DAYS + "])(?:요일)?\\s*[~\\-–]\\s*([" + DAYS + "])");
    private static final Pattern DAY_SINGLE =
            Pattern.compile("([" + DAYS + "])(?:요일)?(?=\\s*[,·/\\s]|$|및)");
    private static final Pattern INSURANCE =
            Pattern.compile("사회\\s*보험|4대\\s*보험|고용\\s*보험|산재\\s*보험|국민\\s*연금|건강\\s*보험");

    private static final Pattern DAY_NAME = Pattern.compile("([" + DAYS + "])요일");
    private static final Pattern PAY_METHOD = Pattern.compile("계좌\\s*이체|계좌\\s*입금|통장\\s*입금|현금");
    private static final Pattern NEGATED = Pattern.compile(
            "(않|아니|없|제외|미부여|미교부|못)");

    /** 텍스트 전체를 읽어 22개 필드를 채운다. */
    public ContractInfo parse(String text) {
        List<String> lines = text.lines().map(String::strip).filter(line -> !line.isEmpty()).toList();
        Map<String, String> labels = new LinkedHashMap<>();
        List<String> clauses = new ArrayList<>();
        boolean labelSeen = false;
        List<String> unlabeledBeforeLabels = new ArrayList<>();
        for (String line : lines) {
            boolean labeled = false;
            for (String segment : line.split("\\s+/\\s+")) {
                Matcher matcher = LABEL.matcher(segment.strip());
                if (matcher.matches()) {
                    labels.putIfAbsent(matcher.group(1).replace(" ", ""), matcher.group(2).strip());
                    labeled = true;
                }
            }
            if (labeled) {
                labelSeen = true;
            } else if (!line.startsWith("[") && line.length() >= 6) {
                (labelSeen ? clauses : unlabeledBeforeLabels).add(line);
            }
        }
        if (!labelSeen) clauses = unlabeledBeforeLabels;

        LocalTime[] times = times(firstNonNull(label(labels, "근무시간", "근로시간"), text));
        if (times == null) times = times(text);
        List<LocalDate> period = dates(label(labels, "계약기간", "근로계약기간", "근로기간", "근무기간"));

        // "주휴일" 줄은 주휴수당 내용이 아니므로 제외하고 찾는다.
        String holidayLine = lines.stream().filter(line -> line.replace("주휴일", "").contains("주휴"))
                .findFirst().orElse(null);
        Boolean holidayIncluded = null;
        if (holidayLine != null) {
            if (holidayLine.matches(".*(포함하지\\s*않|포함하지\\s*아니|미포함|별도).*")) holidayIncluded = false;
            else if (holidayLine.contains("포함")) holidayIncluded = true;
        }

        String probationLine = lineContaining(lines, "수습");
        Boolean probation = null;
        Integer probationMonths = null;
        Integer probationRate = null;
        if (probationLine != null) {
            if (probationLine.matches(".*(없음|없다|없습니다|적용하지\\s*않|미적용|해당\\s*없).*")) {
                probation = false;
            } else {
                probation = true;
                probationMonths = firstInt(Pattern.compile("(\\d{1,2})\\s*개월"), probationLine);
                probationRate = firstInt(Pattern.compile("(\\d{2,3})\\s*(?:%|퍼센트)"), probationLine);
            }
        }

        Boolean insurance = null;
        for (String line : lines) {
            if (INSURANCE.matcher(line).find()) {
                insurance = !line.matches(".*(가입하지\\s*않|가입하지\\s*아니|미가입|가입\\s*안).*");
                break;
            }
        }

        Integer breakMinutes = firstInt(BREAK_MIN, text);
        if (breakMinutes == null) {
            Integer hours = firstInt(BREAK_HOUR, text);
            if (hours != null) breakMinutes = hours * 60;
        }
        Integer payDay = firstInt(PAY_DAY, text);
        if (payDay != null && (payDay < 1 || payDay > 31)) payDay = null;
        Matcher weekly = WEEKLY_HOURS.matcher(text);

        return new ContractInfo(
                label(labels, "사용자", "사업주", "상호", "사업체명", "사업장명", "회사명"),
                label(labels, "근무장소", "근무지", "근로장소", "취업장소"),
                label(labels, "업무", "업무내용", "담당업무", "업무의내용"),
                firstInt(WAGE, text),
                weekly.find() ? Double.valueOf(weekly.group(1)) : null,
                workDays(label(labels, "근무요일", "근무일", "근로일", "근로요일")),
                times == null ? null : times[0],
                times == null ? null : times[1],
                breakMinutes,
                period.isEmpty() ? null : period.get(0),
                period.size() < 2 ? null : period.get(1),
                payDay,
                holidayIncluded,
                probation,
                probationMonths,
                probationRate,
                insurance,
                clauses.isEmpty() ? null : List.copyOf(clauses),
                weeklyHolidayDay(lines),
                annualLeave(lines),
                payMethod(lines),
                contractDelivered(lines),
                null);
    }

    /** "주휴일" 줄에 요일이 정확히 하나만 적혀 있을 때만 그 요일을 읽는다. */
    private static DayOfWeek weeklyHolidayDay(List<String> lines) {
        for (String line : lines) {
            if (!line.contains("주휴일")) continue;
            TreeSet<DayOfWeek> days = new TreeSet<>();
            Matcher matcher = DAY_NAME.matcher(line);
            while (matcher.find()) days.add(DayOfWeek.of(DAYS.indexOf(matcher.group(1)) + 1));
            if (days.size() == 1) return days.first();
        }
        return null;
    }

    /** "연차" 줄이 있으면 부여한다고 적힌 것은 true, 부여하지 않는다는 표현이 있으면 false. */
    private static Boolean annualLeave(List<String> lines) {
        String line = lineContaining(lines, "연차");
        return line == null ? null : !NEGATED.matcher(line).find();
    }

    /** 임금·급여 줄에 계좌이체·현금 같은 지급방법 표현이 있으면 그 표현을 그대로 읽는다. */
    private static String payMethod(List<String> lines) {
        for (String line : lines) {
            if (!line.matches(".*(임금|급여|월급|보수).*")) continue;
            Matcher matcher = PAY_METHOD.matcher(line);
            if (matcher.find()) return matcher.group().replaceAll("\\s+", "");
        }
        return null;
    }

    /** 계약서를 교부한다는 줄이 있으면 true, 교부하지 않는다는 표현이 있으면 false. */
    private static Boolean contractDelivered(List<String> lines) {
        for (String line : lines) {
            if (line.contains("교부") && line.matches(".*(근로\\s*계약|계약서).*")) {
                return !NEGATED.matcher(line).find();
            }
        }
        return null;
    }

    private static String firstNonNull(String first, String second) {
        return first != null ? first : second;
    }

    /** 공백을 제거한 라벨 이름 후보 중 처음 발견한 값을 돌려준다. */
    private static String label(Map<String, String> labels, String... names) {
        for (String name : names) {
            String value = labels.get(name);
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private static String lineContaining(List<String> lines, String keyword) {
        return lines.stream().filter(line -> line.contains(keyword)).findFirst().orElse(null);
    }

    /** 패턴의 첫 번째로 값이 잡힌 그룹을 정수로 읽는다. 쉼표는 무시한다. */
    private static Integer firstInt(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) return null;
        for (int group = 1; group <= matcher.groupCount(); group++) {
            if (matcher.group(group) != null) {
                try {
                    return Integer.valueOf(matcher.group(group).replace(",", ""));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    private static LocalTime[] times(String text) {
        if (text == null) return null;
        Matcher matcher = TIME_RANGE.matcher(text);
        if (!matcher.find()) return null;
        try {
            return new LocalTime[] {
                    LocalTime.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))),
                    LocalTime.of(Integer.parseInt(matcher.group(3)), Integer.parseInt(matcher.group(4)))};
        } catch (DateTimeException e) {
            return null;
        }
    }

    private static List<LocalDate> dates(String text) {
        List<LocalDate> result = new ArrayList<>();
        if (text == null) return result;
        Matcher matcher = DATE.matcher(text);
        while (matcher.find() && result.size() < 2) {
            try {
                result.add(LocalDate.of(Integer.parseInt(matcher.group(1)),
                        Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3))));
            } catch (DateTimeException e) {
                break;
            }
        }
        return result;
    }

    /** "월요일, 화요일", "월~금", "평일", "주말" 형태를 요일 목록으로 바꾼다. */
    private static List<DayOfWeek> workDays(String value) {
        if (value == null) return null;
        TreeSet<DayOfWeek> days = new TreeSet<>();
        if (value.contains("평일")) {
            for (int day = 1; day <= 5; day++) days.add(DayOfWeek.of(day));
        }
        if (value.contains("주말")) {
            days.add(DayOfWeek.SATURDAY);
            days.add(DayOfWeek.SUNDAY);
        }
        Matcher range = DAY_RANGE.matcher(value);
        while (range.find()) {
            int from = DAYS.indexOf(range.group(1));
            int to = DAYS.indexOf(range.group(2));
            for (int i = from; ; i = (i + 1) % 7) {
                days.add(DayOfWeek.of(i + 1));
                if (i == to) break;
            }
        }
        Matcher single = DAY_SINGLE.matcher(value);
        while (single.find()) days.add(DayOfWeek.of(DAYS.indexOf(single.group(1)) + 1));
        return days.isEmpty() ? null : List.copyOf(days);
    }
}
