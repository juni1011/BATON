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
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 계약서 점검 API의 요청·응답 형식을 검증한다. */
@SpringBootTest
@AutoConfigureMockMvc
class CheckControllerTests {
    @Autowired MockMvc mvc;

    /** 지정 번호 샘플 JSON을 UTF-8로 읽으며 스트림을 닫는다. */
    private String sample(int number) throws IOException {
        try (var stream = new ClassPathResource("samples/contract" + number + ".json").getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private ResultActions check(int number) throws Exception {
        return mvc.perform(post("/api/checks").contentType(MediaType.APPLICATION_JSON).content(sample(number)))
                .andExpect(status().isOk());
    }

    /** 정상 예시인 샘플 1번은 문제 항목이 없고 주휴수당을 더한 예상 월급이 나오는지 확인한다. */
    @Test
    void sampleOneHasNoIssues() throws Exception {
        check(1).andExpect(jsonPath("$.dangerCount").value(0))
                .andExpect(jsonPath("$.warningCount").value(0))
                .andExpect(jsonPath("$.missingCount").value(0))
                // 15,000원 × (20시간 + 주휴 4시간) × (365 ÷ 7 ÷ 12)주
                .andExpect(jsonPath("$.estimatedMonthlyPay").value(1564286))
                .andExpect(jsonPath("$.items[?(@.level != 'OK')]").isEmpty())
                .andExpect(jsonPath("$.items[?(@.id == 'minimum-wage')].level").value("OK"));
    }

    /** 샘플 2번은 최저임금 미달과 주휴수당 미기재를 잡는지 확인한다. */
    @Test
    void sampleTwoFlagsLowWageAndMissingHolidayPay() throws Exception {
        check(2).andExpect(jsonPath("$.dangerCount").value(1))
                .andExpect(jsonPath("$.missingCount").value(1))
                .andExpect(jsonPath("$.items[0].id").value("minimum-wage"))
                .andExpect(jsonPath("$.items[0].level").value("DANGER"))
                .andExpect(jsonPath("$.items[0].questionForEmployer").isString())
                .andExpect(jsonPath("$.items[?(@.id == 'weekly-holiday-pay')].level").value("MISSING"));
    }

    /** 샘플 3번은 위약금·급여 미지급 조항과 지급일·휴게시간 미기재를 잡는지 확인한다. */
    @Test
    void sampleThreeFlagsClausesAndMissingTerms() throws Exception {
        check(3).andExpect(jsonPath("$.dangerCount").value(2))
                .andExpect(jsonPath("$.warningCount").value(1))
                .andExpect(jsonPath("$.missingCount").value(3))
                .andExpect(jsonPath("$.items[?(@.id == 'annual-leave')].level").value("MISSING"))
                .andExpect(jsonPath("$.items[?(@.id == 'contract-delivery')].level").value("WARNING"))
                .andExpect(jsonPath("$.items[?(@.id == 'penalty-clause-1')].originalText")
                        .value("계약 기간 중 퇴사할 경우 근로자는 위약금으로 500,000원(가상 금액)을 사용자에게 지급한다."))
                .andExpect(jsonPath("$.items[?(@.id == 'unpaid-clause-1')].level").value("DANGER"))
                .andExpect(jsonPath("$.items[?(@.id == 'pay-day')].level").value("MISSING"))
                .andExpect(jsonPath("$.items[?(@.id == 'break-time')].level").value("MISSING"));
    }

    /** 값이 전혀 없는 계약도 오류 없이 누락 항목으로 보고하고 월급은 null인지 확인한다. */
    @Test
    void emptyContractReportsMissing() throws Exception {
        mvc.perform(post("/api/checks").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimatedMonthlyPay").value(nullValue()))
                .andExpect(jsonPath("$.missingCount").value(11))
                .andExpect(jsonPath("$.items[?(@.id == 'contract-period')].level").value("WARNING"))
                .andExpect(jsonPath("$.items[?(@.id == 'contract-delivery')].level").value("WARNING"));
    }

    private ResultActions checkJson(String json) throws Exception {
        return mvc.perform(post("/api/checks").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk());
    }

    /** 주휴일 요일이 적혀 있으면 OK, 없으면 "휴일 미기재" MISSING이다. */
    @Test
    void weeklyHolidayDayItem() throws Exception {
        checkJson("{\"weeklyHolidayDay\":\"SUNDAY\"}")
                .andExpect(jsonPath("$.items[?(@.id == 'weekly-holiday-day')].level").value("OK"))
                .andExpect(jsonPath("$.items[?(@.id == 'weekly-holiday-day')].reason")
                        .value("매주 일요일이 주휴일로 적혀 있습니다."));
        checkJson("{}")
                .andExpect(jsonPath("$.items[?(@.id == 'weekly-holiday-day')].level").value("MISSING"))
                .andExpect(jsonPath("$.items[?(@.id == 'weekly-holiday-day')].title").value("휴일 미기재"))
                .andExpect(jsonPath("$.items[?(@.id == 'weekly-holiday-day')].questionForEmployer").isNotEmpty());
    }

    /** 연차유급휴가는 true면 OK, null이면 MISSING, 없다고 적혀 있으면 WARNING이다. */
    @Test
    void annualLeaveItem() throws Exception {
        checkJson("{\"annualLeaveMentioned\":true}")
                .andExpect(jsonPath("$.items[?(@.id == 'annual-leave')].level").value("OK"));
        checkJson("{}")
                .andExpect(jsonPath("$.items[?(@.id == 'annual-leave')].level").value("MISSING"))
                .andExpect(jsonPath("$.items[?(@.id == 'annual-leave')].title").value("연차유급휴가 미기재"));
        checkJson("{\"annualLeaveMentioned\":false}")
                .andExpect(jsonPath("$.items[?(@.id == 'annual-leave')].level").value("WARNING"));
    }

    /** 임금 지급방법이 있으면 OK, 없거나 공백이면 MISSING이다. */
    @Test
    void payMethodItem() throws Exception {
        checkJson("{\"payMethod\":\"계좌이체\"}")
                .andExpect(jsonPath("$.items[?(@.id == 'pay-method')].level").value("OK"));
        checkJson("{}")
                .andExpect(jsonPath("$.items[?(@.id == 'pay-method')].level").value("MISSING"))
                .andExpect(jsonPath("$.items[?(@.id == 'pay-method')].title").value("임금 지급방법 미기재"));
        checkJson("{\"payMethod\":\"  \"}")
                .andExpect(jsonPath("$.items[?(@.id == 'pay-method')].level").value("MISSING"));
    }

    /** 계약 종료일이 있으면 OK, 없으면 기간 확인 WARNING이다. */
    @Test
    void contractPeriodItem() throws Exception {
        checkJson("{\"contractEnd\":\"2027-02-28\"}")
                .andExpect(jsonPath("$.items[?(@.id == 'contract-period')].level").value("OK"));
        checkJson("{}")
                .andExpect(jsonPath("$.items[?(@.id == 'contract-period')].level").value("WARNING"))
                .andExpect(jsonPath("$.items[?(@.id == 'contract-period')].title").value("계약 기간 확인 필요"));
    }

    /** 교부 조항이 있으면 OK, 없거나 교부하지 않는다고 적혀 있으면 WARNING이다. */
    @Test
    void contractDeliveryItem() throws Exception {
        checkJson("{\"contractDelivered\":true}")
                .andExpect(jsonPath("$.items[?(@.id == 'contract-delivery')].level").value("OK"));
        checkJson("{}")
                .andExpect(jsonPath("$.items[?(@.id == 'contract-delivery')].level").value("WARNING"))
                .andExpect(jsonPath("$.items[?(@.id == 'contract-delivery')].title").value("계약서 교부 확인"));
        checkJson("{\"contractDelivered\":false}")
                .andExpect(jsonPath("$.items[?(@.id == 'contract-delivery')].level").value("WARNING"));
    }
}
