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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 캘린더 일정 스텁 API의 요청·응답 형식을 검증한다. */
@SpringBootTest
@AutoConfigureMockMvc
class CalendarControllerTests {
    @Autowired MockMvc mvc;

    /** 샘플 1번 요청 본문을 UTF-8로 읽으며 스트림을 닫는다. */
    private String sample() throws IOException {
        try (var stream = new ClassPathResource("samples/contract1.json").getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** 일정 API가 종일 일정과 시각이 있는 근무 일정을 반환하는지 확인한다. */
    @Test
    void calendarReturnsFixedEvents() throws Exception {
        mvc.perform(post("/api/calendar/events").contentType(MediaType.APPLICATION_JSON).content(sample()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].type").value("PAYDAY"))
                .andExpect(jsonPath("$[0].date").value("2026-12-10"))
                .andExpect(jsonPath("$[1].startTime").value("14:00"))
                .andExpect(jsonPath("$[1].endTime").value("18:30"))
                .andExpect(jsonPath("$[2].sourceLink").isString());
    }
}
