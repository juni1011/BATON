package com.example.demo.check;

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

/** 계약서 점검 스텁 API의 요청·응답 형식을 검증한다. */
@SpringBootTest
@AutoConfigureMockMvc
class CheckControllerTests {
    @Autowired MockMvc mvc;

    /** 샘플 1번 요청 본문을 UTF-8로 읽으며 스트림을 닫는다. */
    private String sample() throws IOException {
        try (var stream = new ClassPathResource("samples/contract1.json").getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** 점검 API가 건수와 점검 항목을 포함한 고정 리포트를 반환하는지 확인한다. */
    @Test
    void checksReturnReport() throws Exception {
        mvc.perform(post("/api/checks").contentType(MediaType.APPLICATION_JSON).content(sample()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dangerCount").value(0))
                .andExpect(jsonPath("$.warningCount").value(0))
                .andExpect(jsonPath("$.missingCount").value(0))
                .andExpect(jsonPath("$.estimatedMonthlyPay").value(1560000))
                .andExpect(jsonPath("$.items[0].level").value("OK"))
                .andExpect(jsonPath("$.items[0].questionForEmployer").isString());
    }
}
