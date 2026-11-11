package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 계약서 점검 서비스의 스프링 부트 애플리케이션을 구성한다. */
@SpringBootApplication
public class DemoApplication {
    /** 내장 서버를 시작하고 API와 정적 화면을 제공한다. */
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
