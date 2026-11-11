package com.example.demo.support;

import com.example.demo.common.ContractInfo;

/** 프로필과 계약 정보를 함께 전달하는 매칭 요청이다. */
public record MatchRequest(UserProfile profile, ContractInfo contract) {}
