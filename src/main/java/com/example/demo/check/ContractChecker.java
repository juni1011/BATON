package com.example.demo.check;

import com.example.demo.common.ContractInfo;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** ContractInfo를 규칙으로 점검한다. 정보 제공용이며 법률 자문이 아니다. */
@Component
public class ContractChecker {
    /** 연도별 시간급 최저임금(원). 표에 없는 연도는 가장 가까운 연도의 값으로 비교한다. */
    static final Map<Integer, Integer> MINIMUM_WAGE = Map.of(2025, 10030, 2026, 10320, 2027, 10700);
    private static final int FIRST_YEAR = 2025;
    private static final int LAST_YEAR = 2027;
    /** 1개월 평균 주 수 (365일 ÷ 7일 ÷ 12개월). */
    private static final double WEEKS_PER_MONTH = 365.0 / 7 / 12;

    private static final Pattern PENALTY = Pattern.compile("위약금|위약\\s*벌|벌금|손해\\s*배상|변상");
    private static final Pattern UNPAID = Pattern.compile(
            "(급여|임금|월급|시급|수당).{0,15}(미지급|지급하지\\s*(않|아니))|(급여|임금|월급).{0,10}(공제|차감|삭감)");

    private static final String LAW_17 = "근로기준법 제17조(근로조건의 명시)";

    public CheckReport check(ContractInfo c) {
        List<CheckItem> items = new ArrayList<>();
        int year = c.contractStart() != null ? c.contractStart().getYear() : LocalDate.now().getYear();
        int tableYear = Math.max(FIRST_YEAR, Math.min(LAST_YEAR, year));
        int minimum = MINIMUM_WAGE.get(tableYear);
        String minimumLabel = tableYear + "년 최저임금 " + won(minimum)
                + (year != tableYear ? "(" + year + "년 금액은 반영되어 있지 않아 이 값으로 비교)" : "");

        checkWage(c, items, minimum, minimumLabel);
        checkWeeklyHoliday(c, items, minimum, minimumLabel);
        checkBreak(c, items);
        checkPayDay(c, items);
        checkRequiredTerms(c, items);
        checkWeeklyHours(c, items);
        checkClauses(c, items);
        checkProbation(c, items, minimum, minimumLabel);
        checkInsurance(c, items);

        items.sort(Comparator.comparing(CheckItem::level));
        return new CheckReport(count(items, CheckLevel.DANGER), count(items, CheckLevel.WARNING),
                count(items, CheckLevel.MISSING), estimateMonthlyPay(c), List.copyOf(items));
    }

    /** 세전 예상 월급. 주 15시간 이상이고 시급에 포함되지 않았다면 주휴수당을 더한다. 계산할 수 없으면 null. */
    static Integer estimateMonthlyPay(ContractInfo c) {
        if (c.hourlyWage() == null || c.weeklyHours() == null) return null;
        double paidHours = c.weeklyHours();
        if (c.weeklyHours() >= 15 && !Boolean.TRUE.equals(c.weeklyHolidayPayIncluded())) {
            paidHours += holidayHours(c.weeklyHours());
        }
        return (int) Math.round(c.hourlyWage() * paidHours * WEEKS_PER_MONTH);
    }

    /** 주휴수당으로 인정되는 주당 시간: 주 40시간 기준 8시간에 비례. */
    private static double holidayHours(double weeklyHours) {
        return Math.min(weeklyHours, 40) / 40 * 8;
    }

    private void checkWage(ContractInfo c, List<CheckItem> items, int minimum, String minimumLabel) {
        String clause = clause(c, "시급");
        if (c.hourlyWage() == null) {
            items.add(new CheckItem("minimum-wage", "시급 미기재", CheckLevel.MISSING, clause,
                    "임금의 구성항목·계산방법·지급방법은 서면에 적어야 하는데 시급을 찾지 못했습니다.",
                    LAW_17, "시급이 얼마인지 계약서에 적어 주실 수 있나요?"));
        } else if (c.hourlyWage() < minimum) {
            items.add(new CheckItem("minimum-wage", "최저임금 미달", CheckLevel.DANGER, clause,
                    "시급 " + won(c.hourlyWage()) + "은 " + minimumLabel + "보다 낮습니다. "
                            + "최저임금보다 낮게 정한 임금 부분은 무효이고 최저임금액을 지급해야 합니다.",
                    "최저임금법 제6조(최저임금의 효력)",
                    "시급이 최저임금보다 낮은데, 최저임금 이상으로 수정해 주실 수 있나요?"));
        } else {
            items.add(new CheckItem("minimum-wage", "최저임금 이상", CheckLevel.OK, clause,
                    "시급 " + won(c.hourlyWage()) + "은 " + minimumLabel + " 이상입니다.",
                    "최저임금법 제6조(최저임금의 효력)", null));
        }
    }

    private void checkWeeklyHoliday(ContractInfo c, List<CheckItem> items, int minimum, String minimumLabel) {
        if (c.weeklyHours() == null) return;
        String clause = clause(c, "주휴");
        String law = "근로기준법 제55조(휴일), 제18조(단시간근로자의 근로조건)";
        if (c.weeklyHours() < 15) {
            items.add(new CheckItem("weekly-holiday-pay", "주휴수당 적용 제외 가능", CheckLevel.OK, clause,
                    "4주 평균 주 15시간 미만으로 일하면 유급 주휴일 규정이 적용되지 않습니다. "
                            + "실제 근무가 주 15시간 이상이 되면 주휴수당이 발생할 수 있습니다.", law, null));
        } else if (c.weeklyHolidayPayIncluded() == null) {
            items.add(new CheckItem("weekly-holiday-pay", "주휴수당 미기재", CheckLevel.MISSING, clause,
                    "주 15시간 이상 일하고 정해진 근무일을 모두 출근하면 주휴수당을 받을 수 있는데, "
                            + "계약서에 주휴수당 내용이 없습니다.", law,
                    "주휴수당은 시급과 별도로 지급되나요? 계약서에 적어 주실 수 있나요?"));
        } else if (c.weeklyHolidayPayIncluded()) {
            Double base = c.hourlyWage() == null ? null
                    : c.hourlyWage() * c.weeklyHours() / (c.weeklyHours() + holidayHours(c.weeklyHours()));
            if (base != null && base < minimum) {
                items.add(new CheckItem("weekly-holiday-pay", "주휴수당 포함 시 최저임금 미달 우려",
                        CheckLevel.DANGER, clause,
                        "시급에 주휴수당이 포함되어 있다면 주휴수당을 뺀 기본 시급은 약 "
                                + won((int) Math.floor(base)) + "으로 " + minimumLabel + "보다 낮습니다.",
                        "최저임금법 제6조(최저임금의 효력), 근로기준법 제55조(휴일)",
                        "주휴수당을 뺀 기본 시급이 얼마인지 알려 주실 수 있나요?"));
            } else {
                items.add(new CheckItem("weekly-holiday-pay", "주휴수당이 시급에 포함", CheckLevel.WARNING, clause,
                        "주휴수당이 시급에 포함된 계약입니다. 기본 시급과 주휴수당이 각각 얼마인지 "
                                + "구분되어 있는지 확인하세요.", law,
                        "시급 중 기본 시급과 주휴수당이 각각 얼마인가요?"));
            }
        } else {
            items.add(new CheckItem("weekly-holiday-pay", "주휴수당 별도 지급", CheckLevel.OK, clause,
                    "주휴수당을 시급과 별도로 지급한다고 적혀 있습니다.", law, null));
        }
    }

    private void checkBreak(ContractInfo c, List<CheckItem> items) {
        if (c.startTime() == null || c.endTime() == null) return;
        long span = Duration.between(c.startTime(), c.endTime()).toMinutes();
        if (span <= 0) span += 24 * 60;
        String clause = clause(c, "휴게");
        String law = "근로기준법 제54조(휴게)";
        if (c.breakMinutes() == null) {
            if (span >= 240) {
                items.add(new CheckItem("break-time", "휴게시간 미기재", CheckLevel.MISSING, clause,
                        "근무시간이 4시간 이상이면 30분 이상(8시간 이상이면 1시간 이상)의 휴게시간을 "
                                + "근무 도중에 받아야 하는데, 계약서에 휴게시간이 없습니다.", law,
                        "휴게시간은 언제, 몇 분인가요? 계약서에 적어 주실 수 있나요?"));
            }
            return;
        }
        long work = span - c.breakMinutes();
        int required = work >= 480 ? 60 : work >= 240 ? 30 : 0;
        if (c.breakMinutes() < required) {
            items.add(new CheckItem("break-time", "휴게시간 부족", CheckLevel.DANGER, clause,
                    "휴게 " + c.breakMinutes() + "분을 빼면 하루 " + hours(work) + " 근무인데, 이 경우 "
                            + required + "분 이상의 휴게시간이 필요합니다.", law,
                    "휴게시간을 " + required + "분 이상으로 조정해 주실 수 있나요?"));
        } else {
            items.add(new CheckItem("break-time", "휴게시간 기재", CheckLevel.OK, clause,
                    "하루 " + hours(work) + " 근무에 휴게 " + c.breakMinutes() + "분이 적혀 있습니다.", law, null));
        }
    }

    private void checkPayDay(ContractInfo c, List<CheckItem> items) {
        String law = "근로기준법 제43조(임금 지급), 제17조(근로조건의 명시)";
        if (c.payDay() == null) {
            items.add(new CheckItem("pay-day", "임금 지급일 미기재", CheckLevel.MISSING, null,
                    "임금은 매월 1회 이상 일정한 날짜를 정해 지급해야 하는데, 지급일을 찾지 못했습니다.", law,
                    "급여는 매월 며칠에 지급되나요? 계약서에 적어 주실 수 있나요?"));
        } else {
            items.add(new CheckItem("pay-day", "임금 지급일 기재", CheckLevel.OK, clause(c, "매월"),
                    "매월 " + c.payDay() + "일 지급으로 적혀 있습니다.", law, null));
        }
    }

    private void checkRequiredTerms(ContractInfo c, List<CheckItem> items) {
        missing(items, c.workplace() == null, "workplace", "근무 장소", "근무 장소가 어디인지");
        missing(items, c.jobDescription() == null, "job-description", "업무 내용", "어떤 업무를 하는지");
        missing(items, c.weeklyHours() == null, "weekly-hours", "소정근로시간", "주 몇 시간 근무하는지");
        missing(items, c.workDays() == null, "work-days", "근무 요일", "무슨 요일에 근무하는지");
        missing(items, c.startTime() == null || c.endTime() == null, "work-time", "근무 시간",
                "근무 시작·종료 시각이 언제인지");
        missing(items, c.contractStart() == null, "contract-start", "계약 시작일", "근무 시작일이 언제인지");
    }

    private void missing(List<CheckItem> items, boolean absent, String id, String name, String question) {
        if (!absent) return;
        items.add(new CheckItem("missing-" + id, name + " 미기재", CheckLevel.MISSING, null,
                name + "을(를) 계약서에서 찾지 못했습니다. 근로조건은 서면으로 명시해 교부받아야 합니다.",
                LAW_17, question + " 계약서에 적어 주실 수 있나요?"));
    }

    private void checkWeeklyHours(ContractInfo c, List<CheckItem> items) {
        if (c.weeklyHours() == null || c.weeklyHours() <= 40) return;
        boolean over = c.weeklyHours() > 52;
        items.add(new CheckItem("weekly-hours-limit", over ? "주 52시간 초과" : "주 40시간 초과",
                over ? CheckLevel.DANGER : CheckLevel.WARNING, null,
                "주 " + c.weeklyHours() + "시간으로 적혀 있습니다. 법정 근로시간은 주 40시간이며, "
                        + "합의한 연장근로도 주 12시간까지만 가능하고 가산수당 대상이 될 수 있습니다.",
                "근로기준법 제50조(근로시간), 제53조(연장 근로의 제한), 제56조(연장·야간 및 휴일 근로)",
                "주 40시간을 넘는 시간은 연장근로수당이 지급되나요?"));
    }

    private void checkClauses(ContractInfo c, List<CheckItem> items) {
        if (c.clauses() == null) return;
        int penalty = 0;
        int unpaid = 0;
        for (String clause : c.clauses()) {
            if (PENALTY.matcher(clause).find()) {
                items.add(new CheckItem("penalty-clause-" + ++penalty, "위약금·손해배상 예정 조항",
                        CheckLevel.DANGER, clause,
                        "근로계약을 지키지 못했을 때의 위약금이나 손해배상액을 미리 정하는 계약은 금지됩니다.",
                        "근로기준법 제20조(위약 예정의 금지)",
                        "위약금(손해배상) 조항은 삭제해 주실 수 있나요?"));
            }
            if (UNPAID.matcher(clause).find()) {
                items.add(new CheckItem("unpaid-clause-" + ++unpaid, "임금 미지급·공제 조항",
                        CheckLevel.DANGER, clause,
                        "이미 일한 만큼의 임금은 전액 지급해야 하며, 퇴사 방식 등을 이유로 "
                                + "지급하지 않거나 임의로 공제할 수 없습니다.",
                        "근로기준법 제43조(임금 지급)",
                        "일한 기간의 급여는 전액 지급되는 것이 맞나요? 이 조항을 삭제해 주실 수 있나요?"));
            }
        }
    }

    private void checkProbation(ContractInfo c, List<CheckItem> items, int minimum, String minimumLabel) {
        if (!Boolean.TRUE.equals(c.probation())) return;
        String clause = clause(c, "수습");
        String law = "최저임금법 제5조(최저임금액)";
        if (c.probationWageRate() == null) {
            items.add(new CheckItem("probation", "수습 기간 임금 미기재", CheckLevel.MISSING, clause,
                    "수습이 적용되지만 수습 기간의 임금 비율을 찾지 못했습니다.", LAW_17,
                    "수습 기간에도 시급이 동일한가요? 다르다면 얼마인가요?"));
            return;
        }
        if (c.probationWageRate() >= 100 || c.hourlyWage() == null) return;
        int reduced = c.hourlyWage() * c.probationWageRate() / 100;
        if (reduced >= minimum) {
            items.add(new CheckItem("probation", "수습 감액 후에도 최저임금 이상", CheckLevel.OK, clause,
                    "수습 기간 시급은 약 " + won(reduced) + "으로 " + minimumLabel + " 이상입니다.", law, null));
            return;
        }
        boolean shortContract = c.contractStart() != null && c.contractEnd() != null
                && c.contractEnd().isBefore(c.contractStart().plusYears(1).minusDays(1));
        boolean longProbation = c.probationMonths() != null && c.probationMonths() > 3;
        String reason = "수습 기간 시급은 약 " + won(reduced) + "으로 " + minimumLabel + "보다 낮습니다. "
                + "최저임금의 10% 감액은 1년 이상 계약에서 수습 시작 후 3개월까지만 가능하고, "
                + "단순노무 업무에는 적용할 수 없습니다.";
        if (shortContract || longProbation || c.probationWageRate() < 90) {
            String why = shortContract ? " 이 계약은 기간이 1년 미만입니다."
                    : longProbation ? " 이 계약은 수습이 3개월을 넘습니다." : " 이 계약은 감액 폭이 10%를 넘습니다.";
            items.add(new CheckItem("probation", "수습 감액으로 최저임금 미달", CheckLevel.DANGER, clause,
                    reason + why, law, "수습 기간에도 최저임금 이상으로 지급해 주실 수 있나요?"));
        } else {
            items.add(new CheckItem("probation", "수습 감액 요건 확인 필요", CheckLevel.WARNING, clause,
                    reason, law, "제 업무가 수습 감액이 가능한 직종인지 확인해 주실 수 있나요?"));
        }
    }

    private void checkInsurance(ContractInfo c, List<CheckItem> items) {
        String clause = clause(c, "보험");
        String law = "고용보험법 제8조(적용 범위), 산업재해보상보험법 제6조(적용 범위)";
        if (c.socialInsuranceMentioned() == null) {
            items.add(new CheckItem("social-insurance", "사회보험 언급 없음", CheckLevel.WARNING, clause,
                    "사회보험 가입에 관한 내용이 없습니다. 산재보험은 근무시간과 관계없이 적용되고, "
                            + "다른 보험은 근무시간·기간에 따라 가입 대상이 됩니다.", law,
                    "4대 보험은 어떻게 가입되나요?"));
        } else if (!c.socialInsuranceMentioned()) {
            items.add(new CheckItem("social-insurance", "사회보험 미가입 조항", CheckLevel.WARNING, clause,
                    "사회보험에 가입하지 않는다고 적혀 있습니다. 가입 요건에 해당하면 당사자 합의로 "
                            + "가입을 제외할 수 없으므로 본인이 가입 대상인지 확인하세요.", law,
                    "제 근무시간이면 4대 보험 가입 대상이 아닌지 확인해 주실 수 있나요?"));
        }
    }

    /** 키워드가 들어간 첫 조항 원문. 없으면 null. */
    private static String clause(ContractInfo c, String keyword) {
        if (c.clauses() == null) return null;
        return c.clauses().stream().filter(text -> text.contains(keyword)).findFirst().orElse(null);
    }

    private static int count(List<CheckItem> items, CheckLevel level) {
        return (int) items.stream().filter(item -> item.level() == level).count();
    }

    private static String won(int amount) {
        return String.format("%,d원", amount);
    }

    private static String hours(long minutes) {
        return minutes % 60 == 0 ? minutes / 60 + "시간" : minutes / 60 + "시간 " + minutes % 60 + "분";
    }
}
