package com.example.demo.calendar;

import com.example.demo.common.ContractInfo;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** 담당 D: 캘린더 일정 API. 계약 정보로 일정을 생성한다. */
@RestController
@RequestMapping("/api/calendar")
public class CalendarController {
    private final CalendarEventGenerator generator;

    public CalendarController(CalendarEventGenerator generator) {
        this.generator = generator;
    }

    /** 근무일, 급여일, 수습 종료일, 계약 종료일을 날짜순으로 반환한다. */
    @PostMapping("/events")
    public List<CalendarEvent> events(@RequestBody ContractInfo contract) {
        return generator.generate(contract);
    }
}
