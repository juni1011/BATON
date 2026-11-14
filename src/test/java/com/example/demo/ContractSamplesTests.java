package com.example.demo;

import com.example.demo.common.ContractInfo;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

/** 세 샘플의 데이터 형식, 원문 미기재 값, JSON 날짜·시간 계약을 검증한다. */
@SpringBootTest
class ContractSamplesTests {
    @Autowired JsonMapper mapper;

    /** 지정 번호의 JSON 리소스를 읽고 ContractInfo로 역직렬화한다. */
    private ContractInfo readSample(int number) throws IOException {
        try (var stream = new ClassPathResource("samples/contract" + number + ".json").getInputStream()) {
            return mapper.readValue(stream, ContractInfo.class);
        }
    }

    /** 각 샘플의 전체 키와 값이 손실 없이 공통 record로 왕복 변환되는지 확인한다. */
    @ParameterizedTest(name = "샘플 {0} 역직렬화")
    @ValueSource(ints = {1, 2, 3})
    void samplesDeserialize(int number) throws IOException {
        var contract = readSample(number);
        assertNotNull(contract.employerName());
        assertFalse(contract.clauses().isEmpty());
        try (var stream = new ClassPathResource("samples/contract" + number + ".json").getInputStream()) {
            var original = mapper.readTree(stream);
            assertEquals(22, original.size());
            assertEquals(original, mapper.readTree(mapper.writeValueAsString(contract)));
        }
    }

    /** 원문에서 빠진 값이 0이나 false로 대체되지 않고 null인지 확인한다. */
    @Test
    void missingValuesStayNull() throws IOException {
        assertNull(readSample(2).weeklyHolidayPayIncluded());
        assertNull(readSample(3).payDay());
        assertNull(readSample(3).breakMinutes());
        assertNull(readSample(1).probationMonths());
        assertEquals(Boolean.FALSE, readSample(1).weeklyHolidayPayIncluded());
    }

    /** 날짜와 시각이 배열이나 초 포함 문자열이 아닌 지정 형식으로 직렬화되는지 확인한다. */
    @Test
    void datesAndTimesUseSpecifiedStrings() throws IOException {
        var json = mapper.readTree(mapper.writeValueAsString(readSample(1)));
        assertEquals("2026-11-01", json.get("contractStart").asString());
        assertEquals("2027-02-28", json.get("contractEnd").asString());
        assertEquals("14:00", json.get("startTime").asString());
        assertEquals("18:30", json.get("endTime").asString());
    }
}
