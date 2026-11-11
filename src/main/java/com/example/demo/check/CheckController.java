package com.example.demo.check;

import com.example.demo.common.ContractInfo;
import org.springframework.web.bind.annotation.*;

/** 담당 B: 조항 점검 API. 규칙 기반 점검 결과를 돌려준다. */
@RestController
@RequestMapping("/api/checks")
public class CheckController {
    private final ContractChecker checker;

    public CheckController(ContractChecker checker) {
        this.checker = checker;
    }

    /** 계약 정보를 점검해 수준별 건수, 예상 월급, 항목 목록을 반환한다. */
    @PostMapping
    public CheckReport check(@RequestBody ContractInfo contract) {
        return checker.check(contract);
    }
}
