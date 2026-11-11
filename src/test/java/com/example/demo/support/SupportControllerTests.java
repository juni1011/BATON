package com.example.demo.support;

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

/** 지원사업 추천 스텁 API의 요청·응답 형식을 검증한다. */
@SpringBootTest
@AutoConfigureMockMvc
class SupportControllerTests {
    @Autowired MockMvc mvc;

    /** 샘플 1번 요청 본문을 UTF-8로 읽으며 스트림을 닫는다. */
    private String sample() throws IOException {
        try (var stream = new ClassPathResource("samples/contract1.json").getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** 매칭 API가 가상 지원사업 두 건과 날짜 형식을 반환하는지 확인한다. */
    @Test
    void supportsReturnFictionalMatches() throws Exception {
        String request = "{\"profile\":{\"age\":22,\"sido\":\"가상시\",\"sigungu\":\"상상구\","
                + "\"status\":\"대학생\",\"housing\":null,\"householdIncomeLevel\":null},\"contract\":" + sample() + "}";
        mvc.perform(post("/api/supports/match").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("예시 지원사업 A"))
                .andExpect(jsonPath("$[0].verdict").value("ELIGIBLE"))
                .andExpect(jsonPath("$[0].applyStart").value("2026-11-01"))
                .andExpect(jsonPath("$[1].requiredDocuments").isArray());
    }
}
