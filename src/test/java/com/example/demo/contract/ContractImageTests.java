package com.example.demo.contract;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 가짜 codex 스크립트로 사진 분석 흐름(동의, 형식 검사, 실행 인자, 실패 처리, 값 범위 검사)을 검증한다. */
@SpringBootTest
@AutoConfigureMockMvc
class ContractImageTests {
    private static final String RESULT = "{\"employerName\":\"가상상점\",\"workplace\":null,\"jobDescription\":null,"
            + "\"hourlyWage\":HOURLY,\"weeklyHours\":20,\"workDays\":[\"MONDAY\",\"FRIDAY\"],\"startTime\":\"14:00\","
            + "\"endTime\":\"18:30\",\"breakMinutes\":30,\"contractStart\":\"2026-11-01\",\"contractEnd\":null,"
            + "\"payDay\":10,\"weeklyHolidayPayIncluded\":false,\"probation\":null,\"probationMonths\":null,"
            + "\"probationWageRate\":null,\"socialInsuranceMentioned\":true,\"clauses\":[\"임금은 매월 10일\"]}";

    private static final Path SCRIPT = createFakeCodex();

    private static Path createFakeCodex() {
        try {
            Path script = Files.createTempFile("fake-codex", ".sh");
            // 사진 파일 내용의 표식에 따라 성공, 실패(exit 1), 깨진 JSON, 범위 밖 시급을 흉내 낸다.
            Files.writeString(script, """
                    #!/bin/sh
                    [ "$1" = "exec" ] || exit 2
                    out=""; img=""
                    while [ $# -gt 0 ]; do
                      case "$1" in -o) out="$2"; shift;; -i) img="$2"; shift;; esac
                      shift
                    done
                    [ -f "$img" ] || exit 3
                    grep -q "" /dev/stdin || exit 4
                    if grep -aq FAILCASE "$img"; then exit 1; fi
                    if grep -aq BADJSON "$img"; then echo '{oops' > "$out"; exit 0; fi
                    wage=15000
                    if grep -aq HUGEWAGE "$img"; then wage=99999999; fi
                    cat > "$out" <<EOF
                    %s
                    EOF
                    """.formatted(RESULT.replace("HOURLY", "$wage")), StandardCharsets.UTF_8);
            Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rwxr-xr-x"));
            return script;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void fakeCodex(DynamicPropertyRegistry registry) {
        registry.add("contract.image.codex-command", SCRIPT::toString);
    }

    @Autowired MockMvc mvc;

    private static MockMultipartFile png(String marker) {
        byte[] magic = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        byte[] tail = marker.getBytes(StandardCharsets.UTF_8);
        byte[] data = new byte[magic.length + tail.length];
        System.arraycopy(magic, 0, data, 0, magic.length);
        System.arraycopy(tail, 0, data, magic.length, tail.length);
        return new MockMultipartFile("file", "contract.png", "image/png", data);
    }

    @Test
    void imageIsAnalyzedWhenUserConsents() throws Exception {
        mvc.perform(multipart("/api/contracts/extract").file(png("OK")).param("consent", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employerName").value("가상상점"))
                .andExpect(jsonPath("$.hourlyWage").value(15000))
                .andExpect(jsonPath("$.workDays[0]").value("MONDAY"))
                .andExpect(jsonPath("$.startTime").value("14:00"))
                .andExpect(jsonPath("$.contractStart").value("2026-11-01"))
                .andExpect(jsonPath("$.contractEnd").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void imageWithoutConsentIsRejectedBeforeAnyAnalysis() throws Exception {
        mvc.perform(multipart("/api/contracts/extract").file(png("OK"))).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/contracts/extract").file(png("OK")).param("consent", "false"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fileThatIsNotReallyAnImageIsNotSentToCodex() throws Exception {
        var fake = new MockMultipartFile("file", "contract.png", "image/png", "not an image".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/api/contracts/extract").file(fake).param("consent", "true"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void codexFailureAndBrokenResultMapToGatewayErrors() throws Exception {
        mvc.perform(multipart("/api/contracts/extract").file(png("FAILCASE")).param("consent", "true"))
                .andExpect(status().isBadGateway());
        mvc.perform(multipart("/api/contracts/extract").file(png("BADJSON")).param("consent", "true"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void implausibleValuesAreDroppedInsteadOfGuessed() throws Exception {
        mvc.perform(multipart("/api/contracts/extract").file(png("HUGEWAGE")).param("consent", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hourlyWage").doesNotExist())
                .andExpect(jsonPath("$.employerName").value("가상상점"));
    }

    @Test
    void sanitizeLimitsRangesAndText() {
        var info = new com.example.demo.common.ContractInfo(" ", "x".repeat(500), null, -5, 200.0, null, null, null,
                5000, null, null, 40, null, null, 99, 150, null, java.util.List.of("a", " ", "b"));
        var clean = ContractImageAnalyzer.sanitize(info);
        org.junit.jupiter.api.Assertions.assertNull(clean.employerName());
        org.junit.jupiter.api.Assertions.assertEquals(200, clean.workplace().length());
        org.junit.jupiter.api.Assertions.assertNull(clean.hourlyWage());
        org.junit.jupiter.api.Assertions.assertNull(clean.weeklyHours());
        org.junit.jupiter.api.Assertions.assertNull(clean.breakMinutes());
        org.junit.jupiter.api.Assertions.assertNull(clean.payDay());
        org.junit.jupiter.api.Assertions.assertNull(clean.probationMonths());
        org.junit.jupiter.api.Assertions.assertNull(clean.probationWageRate());
        org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of("a", "b"), clean.clauses());
    }
}
