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
            var matcher = new SupportMatcher(() -> new SupportProgramSource.Catalog(policies));
            var profile = new UserProfile(25, "대구광역시", null, null, null, null);
            org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of("20261110005400214811"),
                    matcher.match(new MatchRequest(profile, "일자리")).stream().map(SupportMatch::id).toList());
        }
    }

    private String request(String profile) {
        return "{\"profile\":" + profile + "}";
    }

    /** 온통청년 키가 없으면 가상 사업을 만들어 내지 않고 빈 목록을 돌려준다. */
    @Test
    void returnsNoProgramsWithoutRealData() throws Exception {
        mvc.perform(post("/api/supports/match").contentType(MediaType.APPLICATION_JSON)
                        .content(request("{\"age\":22,\"sido\":null,\"sigungu\":null,\"status\":null,\"housing\":null,\"householdIncomeLevel\":null}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
