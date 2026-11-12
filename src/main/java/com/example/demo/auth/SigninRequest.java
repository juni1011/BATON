package com.example.demo.auth;

import java.util.Locale;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 이메일은 대소문자를 구분하지 않고, 비밀번호는 변형하지 않는다. */
public record SigninRequest(
        @NotBlank(message = "이메일을 입력해 주세요.")
        @Size(max = 254, message = "이메일은 254자 이내로 입력해 주세요.") String email,
        @NotBlank(message = "비밀번호를 입력해 주세요.")
        @Size(max = 128, message = "비밀번호는 128자 이내로 입력해 주세요.") String password) {

    public SigninRequest {
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return "SigninRequest[REDACTED]";
    }
}
