package com.example.demo.support;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 지원사업 추천 API의 요청·응답 형식을 검증한다. */
@SpringBootTest
@AutoConfigureMockMvc
class SupportControllerTests {
    @Autowired MockMvc mvc;

    @Test
    void filtersMappedPoliciesByRegionAndCategory() throws Exception {
        try (var input = getClass().getResourceAsStream("/youth/getPlcy-sample.json")) {
            var policies = tools.jackson.databind.json.JsonMapper.builder().build().readTree(input)
                    .path("result").path("youthPolicyList").valueStream()
                    .map(YouthPolicyMapper::map).flatMap(java.util.Optional::stream).toList();
            var matcher = new SupportMatcher(() -> new SupportProgramSource.Catalog(policies, false));
            var profile = new UserProfile(25, "대구광역시", null, null, null, null);
            org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of("20261110005400214811"),
                    matcher.match(new MatchRequest(profile, "일자리")).stream().map(SupportMatch::id).toList());
        }
    }

    private String request(String profile) {
        return "{\"profile\":" + profile + "}";
    }

    /** 모르는 값이 있는 조건은 확인 필요로, 충족한 사업은 가능으로 먼저 정렬되는지 확인한다. */
    @Test
    void unknownProfileValuesNeedCheck() throws Exception {
        String profile = "{\"age\":22,\"sido\":\"가상시\",\"sigungu\":\"상상구\","
                + "\"status\":\"대학생\",\"housing\":null,\"householdIncomeLevel\":null}";
        mvc.perform(post("/api/supports/match").contentType(MediaType.APPLICATION_JSON).content(request(profile)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].id").value("support-example-a"))
                .andExpect(jsonPath("$[0].verdict").value("ELIGIBLE"))
                .andExpect(jsonPath("$[0].applyStart").value("2026-11-01"))
                .andExpect(jsonPath("$[0].requiredDocuments").isArray())
                .andExpect(jsonPath("$[?(@.id == 'support-example-d')].verdict").value("ELIGIBLE"))
                .andExpect(jsonPath("$[?(@.id == 'support-example-b')].verdict").value("NEED_CHECK"))
                .andExpect(jsonPath("$[?(@.id == 'support-example-c')].verdict").value("NEED_CHECK"))
                .andExpect(jsonPath("$[?(@.id == 'support-example-e')].verdict").value("NEED_CHECK"));
    }

    /** 조건에 맞지 않는 값이 있으면 불가로 판정하는지 확인한다. */
    @Test
    void mismatchedProfileIsNotEligible() throws Exception {
        String profile = "{\"age\":40,\"sido\":\"서울특별시\",\"sigungu\":null,"
                + "\"status\":\"재직자\",\"housing\":\"월세\",\"householdIncomeLevel\":\"중위소득 150% 초과\"}";
        mvc.perform(post("/api/supports/match").contentType(MediaType.APPLICATION_JSON).content(request(profile)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.verdict == 'NOT_ELIGIBLE')]").value(hasSize(4)));
    }
}
