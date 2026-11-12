package com.example.demo.auth;

/** 비밀번호나 해시를 응답에 포함하지 않는다. */
public record SignupResponse(long id, String name, String email) {
}
