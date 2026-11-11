package com.example.demo.auth;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {SignupController.class, SigninController.class})
public class SignupExceptionHandler {
    public record SignupError(String message, Map<String, String> errors) {
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<SignupError> validation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new SignupError("입력 내용을 확인해 주세요.", errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<SignupError> malformedJson() {
        return ResponseEntity.badRequest().body(new SignupError("올바른 JSON 요청을 보내 주세요.", Map.of()));
    }

    @ExceptionHandler(SigninService.InvalidCredentialsException.class)
    ResponseEntity<SignupError> invalidCredentials() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new SignupError("이메일 또는 비밀번호가 올바르지 않습니다.", Map.of()));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    ResponseEntity<SignupError> duplicateEmail() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new SignupError("이미 가입된 이메일입니다.", Map.of()));
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<SignupError> databaseUnavailable() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new SignupError("회원가입을 처리할 수 없습니다. 잠시 후 다시 시도해 주세요.", Map.of()));
    }
}
