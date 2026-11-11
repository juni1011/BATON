package com.example.demo.auth;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SigninControllerTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper json;

    @BeforeEach
    void signup() throws Exception {
        jdbc.update("DELETE FROM app_users");
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("name", "홍길동", "email", "member@example.com",
                        "password", "password-123!", "passwordConfirm", "password-123!"))))
                .andExpect(status().isCreated());
    }

    private String body(String email, String password) {
        return json.writeValueAsString(Map.of("email", email, "password", password));
    }

    @Test
    void signinCreatesSessionAndSignoutEndsIt() throws Exception {
        var session = new MockHttpSession();
        mvc.perform(post("/api/auth/signin").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content(body(" MEMBER@example.com ", "password-123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.email").value("member@example.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("홍길동"));
        mvc.perform(post("/api/auth/signout").session(session)).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveSameUnauthorizedResponse() throws Exception {
        for (String[] c : new String[][] {{"member@example.com", "wrong-password"}, {"nobody@example.com", "password-123!"}}) {
            mvc.perform(post("/api/auth/signin").contentType(MediaType.APPLICATION_JSON).content(body(c[0], c[1])))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 올바르지 않습니다."));
        }
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void blankFieldsAreRejected() throws Exception {
        mvc.perform(post("/api/auth/signin").contentType(MediaType.APPLICATION_JSON).content(body("", "")))
                .andExpect(status().isBadRequest());
    }
}
