package com.example.demo.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import java.io.IOException;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 로그인하지 않은 요청은 계약서 점검, 일정, 지원사업 추천(AI 포함) API를 쓸 수 없게 막는다.
 * 로그인·회원가입·로그아웃·내 정보 확인({@code /api/auth/**})만 비로그인으로 열어 둔다.
 * {@code app.auth.required=false}로 끌 수 있다(테스트용).
 */
@Configuration
public class LoginRequiredConfiguration implements WebMvcConfigurer {
    private final boolean required;

    public LoginRequiredConfiguration(@Value("${app.auth.required:true}") boolean required) {
        this.required = required;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (!required) return;
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
                HttpSession session = request.getSession(false);
                if (session != null && session.getAttribute(SigninController.SESSION_USER) instanceof SignupResponse) return true;
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"message\":\"로그인이 필요합니다.\"}");
                return false;
            }
        }).addPathPatterns("/api/**").excludePathPatterns("/api/auth/**");
    }
}
