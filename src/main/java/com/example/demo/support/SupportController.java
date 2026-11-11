package com.example.demo.support;

import java.util.List;
import org.springframework.web.bind.annotation.*;

/** 담당 C: 가상 지원사업 매칭 API. 실제 제도 데이터는 연동하지 않는다. */
@RestController
@RequestMapping("/api/supports")
public class SupportController {
    private final SupportMatcher matcher;

    public SupportController(SupportMatcher matcher) {
        this.matcher = matcher;
    }

    /** 프로필을 가상 사업 조건과 비교해 판정 목록을 반환한다. */
    @PostMapping("/match")
    public List<SupportMatch> match(@RequestBody MatchRequest request) {
        return matcher.match(request);
    }
}
