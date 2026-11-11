package com.example.demo.contract;

import com.example.demo.common.ContractInfo;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 계약서 추출 스텁 API의 요청·응답 형식을 검증한다. */
@SpringBootTest
@AutoConfigureMockMvc
class ContractControllerTests {
    @Autowired MockMvc mvc;
    @Autowired JsonMapper mapper;

    /** 샘플 1번 요청 본문을 UTF-8로 읽으며 스트림을 닫는다. */
    private String sample() throws IOException {
        try (var stream = new ClassPathResource("samples/contract1.json").getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** 텍스트 입력을 무시하고 샘플 1번 전체와 동일한 추출 정보를 반환하는지 확인한다. */
    @Test
    void extractTextReturnsSampleOne() throws Exception {
        var response = mvc.perform(post("/api/contracts/extract")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED).param("text", "무시할 입력"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employerName").value("가상별빛상점"))
                .andExpect(jsonPath("$.startTime").value("14:00"))
                .andExpect(jsonPath("$.contractStart").value("2026-11-01"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertEquals(mapper.readValue(sample(), ContractInfo.class), mapper.readValue(response, ContractInfo.class));
    }

    /** multipart 파일 입력도 허용하고 동일한 고정 값을 반환하는지 확인한다. */
    @Test
    void extractMultipartReturnsSample() throws Exception {
        var file = new MockMultipartFile("file", "example.txt", "text/plain", "가상 계약서".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/api/contracts/extract").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hourlyWage").value(15000))
                .andExpect(jsonPath("$.clauses").isArray());
    }
}
