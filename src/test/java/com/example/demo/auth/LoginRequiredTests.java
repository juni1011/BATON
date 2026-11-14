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

/** 로그인하지 않으면 기능 API가 401이고, 로그인하면 열리는지 검증한다. */
@SpringBootTest(properties = "app.auth.required=true")
@AutoConfigureMockMvc
class LoginRequiredTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper json;

    @BeforeEach
    void resetUsers() {
        jdbc.update("DELETE FROM app_users");
    }

    @Test
    void featureApisRejectAnonymousRequests() throws Exception {
        mvc.perform(post("/api/supports/match").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/supports/ai/recommend").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/contracts/extract").param("text", "x")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/checks").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/calendar/events").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authEndpointsStayOpenAndSigninUnlocksFeatures() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("name", "홍길동", "email", "member@example.com",
                        "password", "password-123!", "passwordConfirm", "password-123!"))))
                .andExpect(status().isCreated());
        var session = (MockHttpSession) mvc.perform(post("/api/auth/signin").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", "member@example.com", "password", "password-123!"))))
                .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
        mvc.perform(post("/api/supports/match").session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"profile\":{\"age\":24},\"category\":null}")).andExpect(status().isOk());
    }
}
