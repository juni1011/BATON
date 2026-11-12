package com.example.demo.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** 로그인한 회원의 화면 데이터(계약 정보, 리포트, 일정, 프로필, 추천 결과)를 계정별로 저장한다. */
@RestController
@RequestMapping("/api/me/data")
public class UserDataController {
    /** 화면(common.js)이 쓰는 저장 키. 그 밖의 키는 거부한다. */
    static final Set<String> KEYS = Set.of("contract", "report", "events", "profile", "matches");
    static final int MAX_BYTES = 1_000_000;

    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    public UserDataController(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    private Long userId(HttpServletRequest http) {
        HttpSession session = http.getSession(false);
        Object user = session == null ? null : session.getAttribute(SigninController.SESSION_USER);
        return user instanceof SignupResponse response ? response.id() : null;
    }

    @GetMapping
    public ResponseEntity<Map<String, JsonNode>> all(HttpServletRequest http) {
        Long id = userId(http);
        if (id == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Map<String, JsonNode> result = new LinkedHashMap<>();
        jdbc.query("SELECT data_key, data_value FROM user_data WHERE user_id = ?", rs -> {
            result.put(rs.getString("data_key"), json.readTree(rs.getString("data_value")));
        }, id);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{key}")
    public ResponseEntity<Void> put(@PathVariable String key, @RequestBody String body, HttpServletRequest http) {
        Long id = userId(http);
        if (id == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!KEYS.contains(key)) return ResponseEntity.notFound().build();
        if (body.length() > MAX_BYTES) return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).build();
        try {
            json.readTree(body);
        } catch (JacksonException e) {
            return ResponseEntity.badRequest().build();
        }
        jdbc.update("INSERT INTO user_data (user_id, data_key, data_value) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE data_value = VALUES(data_value), updated_at = CURRENT_TIMESTAMP(6)",
                id, key, body);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> delete(@PathVariable String key, HttpServletRequest http) {
        Long id = userId(http);
        if (id == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!KEYS.contains(key)) return ResponseEntity.notFound().build();
        jdbc.update("DELETE FROM user_data WHERE user_id = ? AND data_key = ?", id, key);
        return ResponseEntity.noContent().build();
    }
}
