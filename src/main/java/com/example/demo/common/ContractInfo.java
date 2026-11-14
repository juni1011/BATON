package com.example.demo.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/** 계약서에서 추출한 공통 정보. 원문에 없는 값은 추정하거나 기본값을 넣지 않고 null로 둔다.
 * 목록도 미기재 시 null이며, 명시적으로 항목이 없는 경우에만 빈 목록을 사용한다.
 * Boolean 필드는 언급이 없으면 null, 명시적으로 부정하면 false, 명시적으로 긍정하면 true로 둔다.
 */
public record ContractInfo(
        String employerName,
        String workplace,
        String jobDescription,
        Integer hourlyWage,
        Double weeklyHours,
        List<DayOfWeek> workDays,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm") LocalTime endTime,
        Integer breakMinutes,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate contractStart,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate contractEnd,
        Integer payDay,
        Boolean weeklyHolidayPayIncluded,
        Boolean probation,
        Integer probationMonths,
        Integer probationWageRate,
        Boolean socialInsuranceMentioned,
        List<String> clauses,
        DayOfWeek weeklyHolidayDay,
        Boolean annualLeaveMentioned,
        String payMethod,
        Boolean contractDelivered,
        @JsonInclude(JsonInclude.Include.NON_NULL) Map<DayOfWeek, WorkTime> workTimes
) {
    public record WorkTime(
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm") LocalTime startTime,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm") LocalTime endTime
    ) {}

    public ContractInfo(String employerName, String workplace, String jobDescription, Integer hourlyWage,
                        Double weeklyHours, List<DayOfWeek> workDays, LocalTime startTime, LocalTime endTime,
                        Integer breakMinutes, LocalDate contractStart, LocalDate contractEnd, Integer payDay,
                        Boolean weeklyHolidayPayIncluded, Boolean probation, Integer probationMonths,
                        Integer probationWageRate, Boolean socialInsuranceMentioned, List<String> clauses) {
        this(employerName, workplace, jobDescription, hourlyWage, weeklyHours, workDays, startTime, endTime,
                breakMinutes, contractStart, contractEnd, payDay, weeklyHolidayPayIncluded, probation,
                probationMonths, probationWageRate, socialInsuranceMentioned, clauses,
                null, null, null, null, null);
    }

    public WorkTime timeOn(DayOfWeek day) {
        if (workTimes == null) return new WorkTime(startTime, endTime);
        WorkTime time = workTimes.get(day);
        return time == null ? new WorkTime(null, null) : time;
    }
}
