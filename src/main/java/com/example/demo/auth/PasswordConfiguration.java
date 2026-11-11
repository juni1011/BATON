package com.example.demo.auth;

import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;

@Configuration
public class PasswordConfiguration {
    @Bean
    PasswordEncoder passwordEncoder() {
        // 알고리즘 식별자를 함께 저장해 향후 로그인 및 해시 업그레이드에 사용한다.
        // PBKDF2는 화면의 최대 길이(128자)를 잘라내지 않고 모두 해시한다.
        String id = "pbkdf2@SpringSecurity_v5_8";
        return new DelegatingPasswordEncoder(id,
                Map.of(id, Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8()));
    }
}
