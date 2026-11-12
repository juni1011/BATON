package com.example.demo.support;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

/** 온통청년 getPlcy 실제 응답에서 뽑은 샘플(담당자 이름 제거)로 매핑을 검증한다. */
class YouthPolicyMapperTests {
    static List<SupportProgram> programs;

    @BeforeAll
    static void load() throws Exception {
        try (InputStream in = YouthPolicyMapperTests.class.getResourceAsStream("/youth/getPlcy-sample.json")) {
            JsonNode root = JsonMapper.builder().build().readTree(in);
            programs = root.path("result").path("youthPolicyList").valueStream()
                    .map(YouthPolicyMapper::map).flatMap(java.util.Optional::stream).toList();
        }
    }

    private SupportProgram byId(String id) {
        return programs.stream().filter(p -> p.id().equals(id)).findFirst().orElseThrow();
    }

    @Test
    void mapsEverySamplePolicy() {
        assertEquals(8, programs.size());
        for (SupportProgram p : programs) {
            assertFalse(p.name().isBlank());
            assertTrue(p.unverified().contains(YouthPolicyMapper.DETAIL_CHECK), "세부 자격은 항상 확인 필요");
        }
    }

    @Test
    void zeroAgeMeansNoLimitAndPeriodIsParsed() {
        SupportProgram p = byId("20261112005400214840");
        assertNull(p.minAge());
        assertNull(p.maxAge());
        assertEquals(LocalDate.of(2026, 1, 1), p.applyStart());
        assertEquals(LocalDate.of(2026, 12, 31), p.applyEnd());
        assertEquals(List.of("산후조리원 영수증, 신분증"), p.documents(), "쉼표로 나누지 않는다");
    }

    @Test
    void ageLimitIsKeptAndMissingPeriodStaysNull() {
        SupportProgram p = byId("20261112005400214838");
        assertEquals(18, p.minAge());
        assertEquals(39, p.maxAge());
        assertNull(p.applyStart());
        assertNull(p.applyEnd());
    }

    @Test
    void onlyHttpUrlsAreUsed() {
        // aplyUrlAddr가 "www.eshare.co.kr"처럼 스킴이 없으면 쓰지 않고 참고 URL로 대체하거나 null로 둔다.
        for (SupportProgram p : programs) {
            assertTrue(p.applyUrl() == null || p.applyUrl().startsWith("http"), p.applyUrl());
        }
        assertTrue(byId("20261112005400214836").applyUrl().startsWith("https://"));
    }

    @Test
    void extraQualificationBecomesUnverifiedCondition() {
        SupportProgram p = byId("20261112005400214833");
        assertEquals(2, p.unverified().size());
        assertTrue(p.unverified().get(1).startsWith("추가 자격: 서산시민"));
        assertFalse(p.unverified().get(1).contains("\n"), "줄바꿈은 공백 하나로");
        assertEquals(1, byId("20261112005400214840").unverified().size());
    }

    @Test
    void policyWithoutIdOrNameIsSkipped() throws Exception {
        var mapper = JsonMapper.builder().build();
        assertTrue(YouthPolicyMapper.map(mapper.readTree("{\"plcyNo\":\"1\",\"plcyNm\":\" \"}")).isEmpty());
        assertTrue(YouthPolicyMapper.map(mapper.readTree("{\"plcyNm\":\"이름만\"}")).isEmpty());
    }
}
