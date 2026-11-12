package com.example.demo.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 세션 쿠키 기반 로그인. 응답에는 비밀번호나 해시를 포함하지 않는다. */
@RestController
@RequestMapping("/api/auth")
public class SigninController {
    static final String SESSION_USER = "easyton.user";

    private final SigninService service;

    public SigninController(SigninService service) {
        this.service = service;
    }

    @PostMapping("/signin")
    public SignupResponse signin(@Valid @RequestBody SigninRequest request, HttpServletRequest http) {
        SignupResponse user = service.authenticate(request);
        // 세션 고정 공격을 막기 위해 로그인 시 기존 세션 ID를 교체한다.
        if (http.getSession(false) != null) http.changeSessionId();
        http.getSession(true).setAttribute(SESSION_USER, user);
        return user;
    }

    @GetMapping("/me")
    public ResponseEntity<SignupResponse> me(HttpServletRequest http) {
        HttpSession session = http.getSession(false);
        Object user = session == null ? null : session.getAttribute(SESSION_USER);
        if (user instanceof SignupResponse response) return ResponseEntity.ok(response);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @PostMapping("/signout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void signout(HttpServletRequest http) {
        HttpSession session = http.getSession(false);
        if (session != null) session.invalidate();
    }
}
