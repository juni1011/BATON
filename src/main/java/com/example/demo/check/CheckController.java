package com.example.demo.check;

import com.example.demo.common.ContractInfo;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** 담당 B: 조항 점검 API의 고정 응답을 제공한다. */
@RestController
@RequestMapping("/api/checks")
public class CheckController {
    /** 계약 정보의 내용과 관계없이 정상 항목 한 개를 포함한 예시 리포트를 반환한다. */
    @PostMapping
    public CheckReport check(@RequestBody ContractInfo contract) {
        return new CheckReport(0, 0, 0, 1560000, List.of(new CheckItem(
                "check-example-1", "주휴수당 별도 지급 (스텁)", CheckLevel.OK,
                "시급은 15,000원이며 주휴수당은 시급에 포함하지 않고 별도로 지급한다.",
                "정상 표시를 위한 고정 예시이며 실제 계약을 점검하지 않습니다.",
                "개발용 예시: 법률 근거 검토 미구현", "주휴수당 산정 내역을 확인할 수 있나요?")));
    }
}
