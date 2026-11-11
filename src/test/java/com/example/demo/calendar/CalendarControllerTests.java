package com.example.demo.calendar;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 캘린더 일정 API의 요청·응답 형식을 검증한다. */
@SpringBootTest
@AutoConfigureMockMvc
class CalendarControllerTests {
    @Autowired MockMvc mvc;

    /** 지정 번호 샘플 JSON을 UTF-8로 읽으며 스트림을 닫는다. */
    private String sample(int number) throws IOException {
        try (var stream = new ClassPathResource("samples/contract" + number + ".json").getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** 샘플 1번으로 근무일, 급여일, 계약 종료일이 날짜순으로 만들어지는지 확인한다. */
    @Test
    void generatesEventsFromContract() throws Exception {
        mvc.perform(post("/api/calendar/events").contentType(MediaType.APPLICATION_JSON).content(sample(1)))
                .andExpect(status().isOk())
                // 2026-11-01(일)에 시작하므로 첫 근무일은 월요일인 11-02
                .andExpect(jsonPath("$[0].id").value("work-2026-11-02"))
                .andExpect(jsonPath("$[0].type").value("WORK"))
                .andExpect(jsonPath("$[0].startTime").value("14:00"))
                .andExpect(jsonPath("$[0].endTime").value("18:30"))
                // 2026-11-02 ~ 2027-02-26 평일 85일
                .andExpect(jsonPath("$[?(@.type == 'WORK')]").value(hasSize(85)))
                .andExpect(jsonPath("$[?(@.type == 'PAYDAY')].date").value(contains(
                        "2026-11-10", "2026-12-10", "2027-01-10", "2027-02-10", "2027-03-10")))
                .andExpect(jsonPath("$[?(@.type == 'PAYDAY')].startTime").value(everyItem(nullValue())))
                .andExpect(jsonPath("$[?(@.type == 'CONTRACT_END')].date").value("2027-02-28"))
                .andExpect(jsonPath("$[?(@.type == 'PROBATION_END')]").isEmpty());
    }

    /** 수습 개월 수가 있으면 수습 종료일을, 시작일이 없으면 빈 목록을 돌려주는지 확인한다. */
    @Test
    void handlesProbationAndMissingStart() throws Exception {
        mvc.perform(post("/api/calendar/events").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contractStart\":\"2026-11-01\",\"probation\":true,\"probationMonths\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("PROBATION_END"))
                .andExpect(jsonPath("$[0].date").value("2027-01-31"));
        mvc.perform(post("/api/calendar/events").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
