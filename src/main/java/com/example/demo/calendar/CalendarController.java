package com.example.demo.calendar;

import com.example.demo.common.ContractInfo;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** 담당 D: 캘린더 일정 API의 고정 응답을 제공한다. */
@RestController
@RequestMapping("/api/calendar")
public class CalendarController {
    /** 계약 정보를 분석하지 않고 개발용 급여일, 근무일, 계약 종료일을 반환한다. */
    @PostMapping("/events")
    public List<CalendarEvent> events(@RequestBody ContractInfo contract) {
        return List.of(
                new CalendarEvent("event-payday", "급여일 (예시)", EventType.PAYDAY,
                        LocalDate.of(2026, 12, 10), null, null, "고정 샘플 급여일", "/confirm.html"),
                new CalendarEvent("event-work", "근무 (예시)", EventType.WORK,
                        LocalDate.of(2026, 11, 2), LocalTime.of(14, 0), LocalTime.of(18, 30),
                        "휴게시간 30분을 포함한 고정 샘플", "/confirm.html"),
                new CalendarEvent("event-end", "계약 종료 (예시)", EventType.CONTRACT_END,
                        LocalDate.of(2027, 2, 28), null, null, "고정 샘플 계약 종료일", "/confirm.html"));
    }
}
