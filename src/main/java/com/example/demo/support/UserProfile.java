package com.example.demo.support;


/** 지원사업 매칭에 사용할 사용자 입력 정보를 전달한다. */
public record UserProfile(Integer age, String sido, String sigungu, String status,
                          String housing, String householdIncomeLevel) {}
