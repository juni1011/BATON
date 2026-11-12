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
class UserDataControllerTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper json;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM app_users");
    }

    private MockHttpSession login(String email) throws Exception {
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("name", "회원", "email", email,
                        "password", "password-123!", "passwordConfirm", "password-123!"))))
                .andExpect(status().isCreated());
        var session = new MockHttpSession();
        mvc.perform(post("/api/auth/signin").session(session).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", "password-123!"))))
                .andExpect(status().isOk());
        return session;
    }

    @Test
    void dataIsStoredPerAccount() throws Exception {
        var a = login("a@example.com");
        var b = login("b@example.com");
        mvc.perform(put("/api/me/data/events").session(a).contentType(MediaType.APPLICATION_JSON)
                .content("[{\"id\":\"x\",\"title\":\"A의 일정\"}]")).andExpect(status().isNoContent());
        mvc.perform(put("/api/me/data/events").session(a).contentType(MediaType.APPLICATION_JSON)
                .content("[{\"id\":\"y\",\"title\":\"덮어쓴 일정\"}]")).andExpect(status().isNoContent());
        mvc.perform(get("/api/me/data").session(a)).andExpect(status().isOk())
                .andExpect(jsonPath("$.events[0].title").value("덮어쓴 일정"));
        mvc.perform(get("/api/me/data").session(b)).andExpect(status().isOk())
                .andExpect(jsonPath("$.events").doesNotExist());
        mvc.perform(delete("/api/me/data/events").session(a)).andExpect(status().isNoContent());
        mvc.perform(get("/api/me/data").session(a)).andExpect(jsonPath("$.events").doesNotExist());
    }

    @Test
    void rejectsAnonymousUnknownKeyAndInvalidJson() throws Exception {
        mvc.perform(get("/api/me/data")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/me/data/events").contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isUnauthorized());
        var a = login("a@example.com");
        mvc.perform(put("/api/me/data/secret").session(a).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/me/data/events").session(a).contentType(MediaType.APPLICATION_JSON).content("{oops"))
                .andExpect(status().isBadRequest());
    }
}
