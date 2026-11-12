package com.example.demo.auth;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SignupControllerTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired JsonMapper json;

    @BeforeEach
    void cleanUsers() {
        jdbc.update("DELETE FROM app_users");
    }

    private String request(String email, String password) {
        return json.writeValueAsString(Map.of("name", "  홍길동  ", "email", email,
                "password", password, "passwordConfirm", password));
    }

    @Test
    void signupPersistsOnlySaltedHashAndReturnsPublicFields() throws Exception {
        String password = "안전한-password-123!";
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(request("  MEMBER@Example.com  ", password)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.email").value("member@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordConfirm").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        var user = jdbc.queryForMap("SELECT * FROM app_users WHERE email = ?", "member@example.com");
        assertEquals("홍길동", user.get("name"));
        assertNotNull(user.get("created_at"));
        String hash = (String) user.get("password_hash");
        assertNotEquals(password, hash);
        assertTrue(encoder.matches(password, hash));
        assertFalse(encoder.matches(password + "x", hash));

        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(request("another@example.com", password)))
                .andExpect(status().isCreated());
        String secondHash = jdbc.queryForObject("SELECT password_hash FROM app_users WHERE email = ?",
                String.class, "another@example.com");
        assertNotEquals(hash, secondHash, "같은 비밀번호라도 salt에 따라 다른 해시를 저장해야 한다.");
    }

    @Test
    void duplicateEmailIsRejectedRegardlessOfCaseOrSurroundingSpaces() throws Exception {
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(request("member@example.com", "password-123")))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(request(" MEMBER@EXAMPLE.COM ", "different-123")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("이미 가입된 이메일입니다."));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM app_users", Integer.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"name\":\"  \",\"email\":\"member@example.com\",\"password\":\"password1\",\"passwordConfirm\":\"password1\"}",
            "{\"name\":\"회원\",\"email\":\"invalid\",\"password\":\"password1\",\"passwordConfirm\":\"password1\"}",
            "{\"name\":\"회원\",\"email\":\"member@example.com\",\"password\":\"short\",\"passwordConfirm\":\"short\"}",
            "{\"name\":\"회원\",\"email\":\"member@example.com\",\"password\":\"password1\",\"passwordConfirm\":\"password2\"}",
            "{\"name\":\"회원\",\"email\":\"member@example.com\",\"password\":\"password1\"}",
            "{\"name\":null,\"email\":null,\"password\":null,\"passwordConfirm\":null}"
    })
    void invalidFieldsAreRejectedWithoutSaving(String body) throws Exception {
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("입력 내용을 확인해 주세요."))
                .andExpect(jsonPath("$.errors").isMap())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("password1"))));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM app_users", Integer.class));
    }

    @Test
    void passwordLengthBoundariesUseTheWholePassword() throws Exception {
        String password = "가".repeat(128);
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(request("long@example.com", password)))
                .andExpect(status().isCreated());
        String hash = jdbc.queryForObject("SELECT password_hash FROM app_users", String.class);
        assertTrue(encoder.matches(password, hash));
        assertFalse(encoder.matches("가".repeat(127) + "나", hash));
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(request("too-long@example.com", "a".repeat(129))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(request("min@example.com", "12345678")))
                .andExpect(status().isCreated());
    }

    @Test
    void oversizedNameOrEmailIsRejected() throws Exception {
        for (var body : new String[] {
                json.writeValueAsString(Map.of("name", "가".repeat(41), "email", "member@example.com",
                        "password", "password1", "passwordConfirm", "password1")),
                request("a".repeat(243) + "@example.com", "password1")
        }) {
            mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM app_users", Integer.class));
    }

    @Test
    void malformedJsonIsRejectedWithoutEchoingCredentials() throws Exception {
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"secret-value\","))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("올바른 JSON 요청을 보내 주세요."))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret-value"))));
    }

    @Test
    void existingPagesRemainAccessibleWithoutLogin() throws Exception {
        mvc.perform(get("/signup.html")).andExpect(status().isOk());
        mvc.perform(get("/index.html")).andExpect(status().isOk());
    }
}
