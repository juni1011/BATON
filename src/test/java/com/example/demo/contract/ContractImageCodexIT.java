package com.example.demo.contract;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 실제 Codex CLI로 가상 계약서 사진(samples/contract1.txt를 그린 이미지)을 분석한다.
 * 로그인된 codex가 필요하고 시간이 걸리므로 {@code RUN_CODEX_IT=1}일 때만 실행한다.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RUN_CODEX_IT", matches = ".+")
class ContractImageCodexIT {
    @Autowired ContractImageAnalyzer analyzer;

    @Test
    void readsSampleContractPhotoWithRealCodex() throws Exception {
        byte[] image = Files.readAllBytes(Path.of("src/test/resources/contract-sample.png"));
        var info = analyzer.analyze(image, ContractImageAnalyzer.imageExtension(image));
        assertEquals(15000, info.hourlyWage());
        assertEquals(20.0, info.weeklyHours());
        assertEquals(java.util.List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY), info.workDays());
        assertEquals(java.time.LocalTime.of(14, 0), info.startTime());
        assertEquals(java.time.LocalTime.of(18, 30), info.endTime());
        assertEquals(10, info.payDay());
        assertEquals(30, info.breakMinutes());
        assertEquals(java.time.LocalDate.of(2026, 11, 1), info.contractStart());
        assertEquals(java.time.LocalDate.of(2027, 2, 28), info.contractEnd());
        assertEquals(Boolean.FALSE, info.weeklyHolidayPayIncluded());
    }
}
