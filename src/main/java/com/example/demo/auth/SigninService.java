package com.example.demo.auth;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class SigninService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    /** 없는 이메일도 같은 비용으로 검증해 응답 시간으로 가입 여부를 알 수 없게 한다. */
    private final String dummyHash;

    public SigninService(JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    private record Row(long id, String name, String email, String hash) {
    }

    /** 이메일 또는 비밀번호가 틀리면 어느 쪽인지 알리지 않고 같은 예외를 던진다. */
    public SignupResponse authenticate(SigninRequest request) {
        List<Row> rows = jdbc.query(
                "SELECT id, name, email, password_hash FROM app_users WHERE email = ?",
                (rs, n) -> new Row(rs.getLong("id"), rs.getString("name"),
                        rs.getString("email"), rs.getString("password_hash")),
                request.email());
        Row row = rows.isEmpty() ? null : rows.get(0);
        boolean ok = passwordEncoder.matches(request.password(), row == null ? dummyHash : row.hash());
        if (row == null || !ok) throw new InvalidCredentialsException();
        return new SignupResponse(row.id(), row.name(), row.email());
    }

    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException() {
            super(null, null, false, false);
        }
    }
}
