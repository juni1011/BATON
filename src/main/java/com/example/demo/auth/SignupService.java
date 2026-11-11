package com.example.demo.auth;

import java.sql.PreparedStatement;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SignupService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    public SignupService(JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        String passwordHash = passwordEncoder.encode(request.password());
        var keyHolder = new GeneratedKeyHolder();
        // UNIQUE 제약으로 동시에 같은 이메일이 가입하는 경우도 차단한다.
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO app_users (name, email, password_hash) VALUES (?, ?, ?)",
                    new String[] {"id"});
            statement.setString(1, request.name());
            statement.setString(2, request.email());
            statement.setString(3, passwordHash);
            return statement;
        }, keyHolder);
        Number id = keyHolder.getKey();
        if (id == null) throw new IllegalStateException("회원 ID를 생성하지 못했습니다.");
        return new SignupResponse(id.longValue(), request.name(), request.email());
    }
}
