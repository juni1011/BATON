package com.example.demo.contract;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.InetSocketAddress;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import com.sun.net.httpserver.HttpServer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
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

/** 가짜 Gemini 서버로 사진 분석 흐름(동의, 형식 검사, 실행 인자, 실패 처리, 값 범위 검사)을 검증한다. */
@SpringBootTest
@AutoConfigureMockMvc
class ContractImageTests {
    private static final String RESULT = "{\"employerName\":\"가상상점\",\"workplace\":null,\"jobDescription\":null,"
            + "\"hourlyWage\":HOURLY,\"weeklyHours\":20,\"workDays\":[\"MONDAY\",\"FRIDAY\"],\"startTime\":\"14:00\","
            + "\"endTime\":\"18:30\",\"breakMinutes\":30,\"contractStart\":\"2026-11-01\",\"contractEnd\":null,"
            + "\"payDay\":10,\"weeklyHolidayPayIncluded\":false,\"probation\":null,\"probationMonths\":null,"
            + "\"probationWageRate\":null,\"socialInsuranceMentioned\":true,\"clauses\":[\"임금은 매월 10일\"]}";

    private static final String API_KEY = "test-key";
    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final HttpServer SERVER = startFakeGemini();

    /** 가짜 Gemini 서버: 입력(사진 바이트 또는 본문)의 표식에 따라 성공, 오류(500), 깨진 JSON, 범위 밖 시급을 흉내 낸다. */
    private static HttpServer startFakeGemini() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                int status = 200;
                String body;
                try {
                    String input = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    JsonNode request = MAPPER.readTree(input);
                    StringBuilder content = new StringBuilder();
                    boolean hasImage = false;
                    for (JsonNode part : request.path("contents").path(0).path("parts")) {
                        if (part.has("text")) content.append(part.path("text").asString());
                        if (part.has("inline_data")) {
                            hasImage = true;
                            content.append(new String(Base64.getDecoder().decode(part.path("inline_data").path("data").asString()),
                                    StandardCharsets.ISO_8859_1));
                        }
                    }
                    String text = content.toString();
                    boolean valid = API_KEY.equals(exchange.getRequestHeaders().getFirst("x-goog-api-key"))
                            && "application/json".equals(request.path("generationConfig").path("responseMimeType").asString())
                            && request.path("generationConfig").has("responseJsonSchema")
                            && (hasImage || text.contains("<contract>"));
                    if (!valid) {
                        status = 400;
                        body = "{}";
                    } else if (text.contains("FAILCASE")) {
                        status = 500;
                        body = "{}";
                    } else {
                        String result = text.contains("BADJSON") ? "{oops"
                                : RESULT.replace("HOURLY", text.contains("HUGEWAGE") ? "99999999" : "15000");
                        if (text.contains("WEEKDAYTIMES")) {
                            result = result.replace("\"startTime\":\"14:00\"", "\"startTime\":null")
                                    .replace("\"endTime\":\"18:30\"", "\"endTime\":null")
                                    .replace("\"clauses\"", "\"workTimes\":{\"MONDAY\":{\"startTime\":\"09:00\",\"endTime\":\"13:00\"},"
                                            + "\"FRIDAY\":{\"startTime\":\"14:00\",\"endTime\":\"18:00\"},"
                                            + "\"SUNDAY\":{\"startTime\":null,\"endTime\":null}},\"clauses\"");
                        }
                        body = MAPPER.writeValueAsString(Map.of("candidates", List.of(Map.of("content",
                                Map.of("parts", List.of(Map.of("text", result)))))));
                    }
                } catch (RuntimeException e) {
                    status = 400;
                    body = "{}";
                }
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void fakeGemini(DynamicPropertyRegistry registry) {
        registry.add("contract.image.gemini-base-url", () -> "http://127.0.0.1:" + SERVER.getAddress().getPort());
        registry.add("contract.image.gemini-api-key", () -> API_KEY);
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
    void weekdaySpecificHoursAreKeptAndEmptyDaysDropped() throws Exception {
        mvc.perform(multipart("/api/contracts/extract").file(png("WEEKDAYTIMES")).param("consent", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workTimes.MONDAY.startTime").value("09:00"))
                .andExpect(jsonPath("$.workTimes.FRIDAY.endTime").value("18:00"))
                .andExpect(jsonPath("$.workTimes.SUNDAY").doesNotExist());
    }

    @Test
    void imageWithoutConsentIsRejectedBeforeAnyAnalysis() throws Exception {
        mvc.perform(multipart("/api/contracts/extract").file(png("OK"))).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/contracts/extract").file(png("OK")).param("consent", "false"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fileThatIsNotReallyAnImageIsNotSentToAi() throws Exception {
        var fake = new MockMultipartFile("file", "contract.png", "image/png", "not an image".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/api/contracts/extract").file(fake).param("consent", "true"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void aiFailureAndBrokenResultMapToGatewayErrors() throws Exception {
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
    void proseTextIsAnalyzedByAiOnlyWithConsent() throws Exception {
        String prose = "근로개시일 2026년 11월 1일부터. 시간급 15,000원을 지급한다. OK";
        mvc.perform(multipart("/api/contracts/extract").param("text", prose).param("consent", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hourlyWage").value(15000))
                .andExpect(jsonPath("$.employerName").value("가상상점"));
        // 동의하지 않으면 AI가 아니라 규칙 파서가 읽는다(가짜 Gemini의 값이 나오지 않는다).
        mvc.perform(multipart("/api/contracts/extract").param("text", prose))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employerName").doesNotExist());
    }

    @Test
    void aiFailureOnTextIsReportedInsteadOfSilentlyFallingBack() throws Exception {
        mvc.perform(multipart("/api/contracts/extract").param("text", "계약서 FAILCASE").param("consent", "true"))
                .andExpect(status().isBadGateway());
        mvc.perform(multipart("/api/contracts/extract").param("text", "x".repeat(ContractImageAnalyzer.MAX_TEXT_CHARS + 1))
                .param("consent", "true")).andExpect(status().isPayloadTooLarge());
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

    @Test
    void sanitizeKeepsNewTermsAndLimitsPayMethod() {
        var info = new com.example.demo.common.ContractInfo(null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, java.time.DayOfWeek.SUNDAY, true,
                "가".repeat(80), false, null);
        var clean = ContractImageAnalyzer.sanitize(info);
        org.junit.jupiter.api.Assertions.assertEquals(java.time.DayOfWeek.SUNDAY, clean.weeklyHolidayDay());
        org.junit.jupiter.api.Assertions.assertEquals(Boolean.TRUE, clean.annualLeaveMentioned());
        org.junit.jupiter.api.Assertions.assertEquals(50, clean.payMethod().length());
        org.junit.jupiter.api.Assertions.assertEquals(Boolean.FALSE, clean.contractDelivered());
    }
}
