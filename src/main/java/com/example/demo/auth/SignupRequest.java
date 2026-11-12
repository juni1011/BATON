package com.example.demo.auth;

import java.util.Locale;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 이메일은 대소문자를 구분하지 않고 사용한다. 비밀번호는 변형하지 않는다. */
public record SignupRequest(
        @NotBlank(message = "이름을 입력해 주세요.")
        @Size(max = 40, message = "이름은 40자 이내로 입력해 주세요.") String name,
        @NotBlank(message = "이메일을 입력해 주세요.")
        @Email(message = "올바른 이메일을 입력해 주세요.")
        @Size(max = 254, message = "이메일은 254자 이내로 입력해 주세요.") String email,
        @NotBlank(message = "비밀번호를 입력해 주세요.")
        @Size(min = 8, max = 128, message = "비밀번호는 8~128자로 입력해 주세요.") String password,
        @NotBlank(message = "비밀번호 확인을 입력해 주세요.")
        @Size(max = 128, message = "비밀번호 확인은 128자 이내로 입력해 주세요.") String passwordConfirm) {

    public SignupRequest {
        name = name == null ? null : name.strip();
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }

    @AssertTrue(message = "비밀번호가 일치하지 않습니다.")
    public boolean isPasswordConfirmed() {
        return password != null && password.equals(passwordConfirm);
    }

    @Override
    public String toString() {
        return "SignupRequest[REDACTED]";
    }
}
