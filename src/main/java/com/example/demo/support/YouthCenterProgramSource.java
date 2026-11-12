package com.example.demo.support;

import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 온통청년 청년정책 Open API에서 사업 목록을 받아 메모리에 보관한다. {@code YOUTH_API_KEY}가 있을 때만 사용한다.
 * 서버 시작 직후와 하루마다 다시 받으며, 실패하면 마지막으로 성공한 목록을 유지한다.
 * 첫 로딩이 끝나기 전에는 가상 예시 사업을 돌려준다. 인증키는 로그에 남기지 않는다.
 */
@Component
@Primary
@ConditionalOnExpression("!'${youth.api.key:}'.isBlank()")
public class YouthCenterProgramSource implements SupportProgramSource {
    private static final Logger log = LoggerFactory.getLogger(YouthCenterProgramSource.class);
    private static final int PAGE_SIZE = 500;
    private static final int MAX_PAGES = 50;

    private final String apiKey;
    private final String url;
    private final RestClient client;
    private final JsonMapper json;
    private final SupportProgramSource fallback;
    private final Clock clock;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "youth-policy-refresh");
        thread.setDaemon(true);
        return thread;
    });
    private volatile List<SupportProgram> cache = List.of();

    public YouthCenterProgramSource(@Value("${youth.api.key}") String apiKey,
                                    @Value("${youth.api.url:https://www.youthcenter.go.kr/go/ythip/getPlcy}") String url,
                                    JsonMapper json, SampleSupportProgramSource fallback) {
        this.apiKey = apiKey.strip();
        this.url = url;
        this.client = RestClient.create();
        this.json = json;
        this.fallback = fallback;
        this.clock = Clock.systemDefaultZone();
    }

    @EventListener(ApplicationReadyEvent.class)
    void start() {
        executor.scheduleWithFixedDelay(this::refreshQuietly, 0, 24, TimeUnit.HOURS);
    }

    @PreDestroy
    void stop() {
        executor.shutdownNow();
    }

    private void refreshQuietly() {
        try {
            List<SupportProgram> loaded = fetchAll();
            if (loaded.isEmpty()) {
                log.warn("온통청년 정책을 한 건도 받지 못해 이전 목록을 유지합니다.");
                return;
            }
            cache = loaded;
            log.info("온통청년 청년정책 {}건을 불러왔습니다.", loaded.size());
        } catch (RestClientResponseException e) {
            String body = e.getResponseBodyAsString();
            log.warn("온통청년 정책 갱신에 실패했습니다. HTTP {} {}", e.getStatusCode().value(),
                    body.length() > 200 ? body.substring(0, 200) : body);
        } catch (Exception e) {
            // 예외 메시지에 인증키가 포함된 주소가 들어갈 수 있어 종류만 기록한다.
            log.warn("온통청년 정책 갱신에 실패했습니다. {}", e.getClass().getSimpleName());
        }
    }

    private List<SupportProgram> fetchAll() throws Exception {
        List<SupportProgram> programs = new ArrayList<>();
        int total = Integer.MAX_VALUE;
        for (int page = 1; page <= MAX_PAGES && (page - 1) * PAGE_SIZE < total; page++) {
            int pageNum = page;
            // 인증키는 영숫자와 '-'뿐이라 인코딩이 필요 없다. 템플릿 확장 대신 완성된 URI를 그대로 보낸다.
            URI uri = URI.create(url + "?apiKeyNm=" + apiKey + "&pageNum=" + pageNum
                    + "&pageSize=" + PAGE_SIZE + "&rtnType=json");
            String body = client.get().uri(uri).retrieve().body(String.class);
            JsonNode root = json.readTree(body);
            if (root.path("resultCode").asInt(0) != 200) throw new IllegalStateException("unexpected result code");
            JsonNode result = root.path("result");
            total = result.path("pagging").path("totCount").asInt(0);
            for (JsonNode policy : result.path("youthPolicyList")) {
                YouthPolicyMapper.map(policy).ifPresent(programs::add);
            }
        }
        return programs;
    }

    @Override
    public Catalog catalog() {
        List<SupportProgram> loaded = cache;
        if (loaded.isEmpty()) return fallback.catalog();
        LocalDate today = LocalDate.now(clock);
        // 신청 마감이 지난 사업은 제외한다. 마감일을 모르는 사업(상시 등)은 남긴다.
        return new Catalog(loaded.stream().filter(p -> p.applyEnd() == null || !p.applyEnd().isBefore(today)).toList(),
                false);
    }
}
