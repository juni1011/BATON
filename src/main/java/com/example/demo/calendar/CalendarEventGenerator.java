package com.example.demo.calendar;

import com.example.demo.common.ContractInfo;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/** 계약 정보에서 근무일, 급여일, 수습 종료일, 계약 종료일 일정을 만든다. */
@Component
public class CalendarEventGenerator {
    /** 계약 종료일이 없을 때 근무·급여 일정을 만들어 두는 기간(개월). */
    static final int OPEN_ENDED_MONTHS = 3;
    /** 일정을 만드는 최대 기간(개월). */
    static final int MAX_MONTHS = 12;
    private static final String SOURCE = "/confirm.html";

    /** 계약 시작일이 없으면 날짜를 정할 수 없으므로 빈 목록을 반환한다. */
    public List<CalendarEvent> generate(ContractInfo c) {
        List<CalendarEvent> events = new ArrayList<>();
        LocalDate start = c.contractStart();
        if (start == null) return events;
        LocalDate limit = start.plusMonths(MAX_MONTHS).minusDays(1);
        LocalDate end = c.contractEnd() != null ? c.contractEnd() : start.plusMonths(OPEN_ENDED_MONTHS).minusDays(1);
        if (end.isAfter(limit)) end = limit;
        String employer = c.employerName() != null ? c.employerName() : "근무";

        if (c.workDays() != null) {
            String detail = c.breakMinutes() != null ? "휴게시간 " + c.breakMinutes() + "분" : "휴게시간 미기재";
            if (c.workplace() != null) detail = c.workplace() + " · " + detail;
            for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
                if (c.workDays().contains(day.getDayOfWeek())) {
                    var time = c.timeOn(day.getDayOfWeek());
                    events.add(new CalendarEvent("work-" + day, employer + " 근무", EventType.WORK, day,
                            time.startTime(), time.endTime(), detail, SOURCE));
                }
            }
        }

        if (c.payDay() != null) {
            // 마지막 근무분이 다음 달에 지급되는 경우를 위해 종료 후 한 달까지 포함한다.
            LocalDate lastPay = end.plusMonths(1);
            for (YearMonth month = YearMonth.from(start); !month.atDay(1).isAfter(lastPay); month = month.plusMonths(1)) {
                LocalDate day = month.atDay(Math.min(c.payDay(), month.lengthOfMonth()));
                if (day.isAfter(start) && !day.isAfter(lastPay)) {
                    events.add(new CalendarEvent("payday-" + day, "급여일", EventType.PAYDAY, day, null, null,
                            day.isAfter(end) ? "계약 종료 후 정산분 지급 여부를 확인하세요."
                                    : "매월 " + c.payDay() + "일 지급", SOURCE));
                }
            }
        }

        if (Boolean.TRUE.equals(c.probation()) && c.probationMonths() != null) {
            LocalDate day = start.plusMonths(c.probationMonths()).minusDays(1);
            events.add(new CalendarEvent("probation-end", "수습 종료 예정", EventType.PROBATION_END, day, null, null,
                    "수습 " + c.probationMonths() + "개월 종료. 다음 급여부터 시급이 바뀌는지 확인하세요.", SOURCE));
        }

        if (c.contractEnd() != null) {
            events.add(new CalendarEvent("contract-end", "계약 종료일", EventType.CONTRACT_END, c.contractEnd(),
                    null, null, "계약 연장 여부와 마지막 급여 정산을 확인하세요.", SOURCE));
        }

        events.sort(Comparator.comparing(CalendarEvent::date).thenComparing(CalendarEvent::type));
        return events;
    }
}
