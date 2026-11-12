package com.example.demo.support;

/** 프로필을 전달하는 매칭 요청이다. */
public record MatchRequest(UserProfile profile, String category) {}
