package com.example.demo.contract;

import com.example.demo.common.ContractInfo;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 계약서 추출 API의 요청·응답 형식을 검증한다. */
@SpringBootTest
@AutoConfigureMockMvc
class ContractControllerTests {
    @Autowired MockMvc mvc;
    @Autowired JsonMapper mapper;

    /** 지정 번호 샘플의 txt 또는 json 리소스를 UTF-8로 읽는다. */
    private String sample(int number, String extension) throws IOException {
        try (var stream = new ClassPathResource("samples/contract" + number + "." + extension).getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** 샘플 원문 텍스트를 보내면 같은 번호의 JSON과 동일한 추출 결과가 나오는지 확인한다. */
    @ParameterizedTest(name = "샘플 {0} 텍스트 추출")
    @ValueSource(ints = {1, 2, 3})
    void extractTextMatchesSampleJson(int number) throws Exception {
        var response = mvc.perform(post("/api/contracts/extract")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED).param("text", sample(number, "txt")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertEquals(mapper.readValue(sample(number, "json"), ContractInfo.class),
                mapper.readValue(response, ContractInfo.class));
    }

    /** multipart 텍스트 파일도 같은 방식으로 추출하는지 확인한다. */
    @Test
    void extractMultipartFile() throws Exception {
        var file = new MockMultipartFile("file", "contract1.txt", "text/plain",
                sample(1, "txt").getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/api/contracts/extract").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employerName").value("가상별빛상점"))
                .andExpect(jsonPath("$.hourlyWage").value(15000))
                .andExpect(jsonPath("$.startTime").value("14:00"))
                .andExpect(jsonPath("$.clauses.length()").value(6))
                .andExpect(jsonPath("$.weeklyHolidayDay").value("SUNDAY"))
                .andExpect(jsonPath("$.annualLeaveMentioned").value(true))
                .andExpect(jsonPath("$.payMethod").value("계좌이체"))
                .andExpect(jsonPath("$.contractDelivered").value(true));
    }

    /** 읽을 수 있는 항목이 없는 텍스트는 추정 없이 모든 값을 null로 돌려주는지 확인한다. */
    @Test
    void unknownTextStaysNull() throws Exception {
        mvc.perform(post("/api/contracts/extract")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED).param("text", "안녕하세요"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hourlyWage").value(nullValue()))
                .andExpect(jsonPath("$.probation").value(nullValue()))
                .andExpect(jsonPath("$.clauses").value(nullValue()));
    }

    /** 입력이 없으면 400, 텍스트가 아닌 파일은 415인지 확인한다. */
    @Test
    void rejectsEmptyAndNonTextInput() throws Exception {
        mvc.perform(post("/api/contracts/extract").contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isBadRequest());
        var pdf = new MockMultipartFile("file", "contract.pdf", "application/pdf", new byte[] {1, 2, 3});
        mvc.perform(multipart("/api/contracts/extract").file(pdf))
                .andExpect(status().isUnsupportedMediaType());
    }

    /** 새 항목은 문구가 분명할 때만 읽고, 부정 표현은 false, 언급이 없으면 null로 두는지 확인한다. */
    @Test
    void extractNewTermsFromKeywords() throws Exception {
        var text = "근무 장소: 가상시 상상구\n주휴일: 매주 토요일\n임금은 현금 지급한다.\n"
                + "연차유급휴가는 부여하지 않는다.\n계약서는 근로자에게 교부하지 않는다.";
        mvc.perform(post("/api/contracts/extract")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED).param("text", text))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeklyHolidayDay").value("SATURDAY"))
                .andExpect(jsonPath("$.payMethod").value("현금"))
                .andExpect(jsonPath("$.annualLeaveMentioned").value(false))
                .andExpect(jsonPath("$.contractDelivered").value(false));
        mvc.perform(post("/api/contracts/extract")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("text", "근무 장소: 가상시\n주휴일: 월요일 또는 화요일\n임금은 매월 10일에 지급한다."))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeklyHolidayDay").value(nullValue()))
                .andExpect(jsonPath("$.annualLeaveMentioned").value(nullValue()))
                .andExpect(jsonPath("$.payMethod").value(nullValue()))
                .andExpect(jsonPath("$.contractDelivered").value(nullValue()));
    }
}
