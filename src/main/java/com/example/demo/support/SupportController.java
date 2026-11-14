package com.example.demo.support;

import java.util.List;
import org.springframework.web.bind.annotation.*;

/** 담당 C: 지원사업 매칭 API. 온통청년 청년정책 데이터로 판정한다. */
@RestController
@RequestMapping("/api/supports")
public class SupportController {
    private final SupportMatcher matcher;

    public SupportController(SupportMatcher matcher) {
        this.matcher = matcher;
    }

    /** 프로필을 사업 조건과 비교해 판정 목록을 반환한다. */
    @PostMapping("/match")
    public List<SupportMatch> match(@RequestBody MatchRequest request) {
        return matcher.match(request);
    }
}
