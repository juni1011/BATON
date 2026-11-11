package com.example.demo.contract;

import com.example.demo.common.ContractInfo;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 담당 A: 계약서 업로드와 추출 API의 고정 응답을 제공한다. */
@RestController
@RequestMapping("/api/contracts")
public class ContractController {
    /** 파일 또는 텍스트 입력을 받되 분석하지 않고 샘플 1번 정보를 반환한다. */
    @PostMapping("/extract")
    public ContractInfo extract(@RequestParam(name = "file", required = false) MultipartFile file,
                                @RequestParam(name = "text", required = false) String text) {
        return new ContractInfo("가상별빛상점", "가상시 상상구 예시로 가상건물", "상품 정리 및 고객 안내",
                15000, 20.0, List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY), LocalTime.of(14, 0), LocalTime.of(18, 30),
                30, LocalDate.of(2026, 11, 1), LocalDate.of(2027, 2, 28), 10, false, false,
                null, null, true, List.of("시급은 15,000원이며 주휴수당은 시급에 포함하지 않고 별도로 지급한다.",
                "임금은 매월 10일에 전월분을 지급한다.", "근무 중 30분의 휴게시간을 자유롭게 이용한다.",
                "사회보험은 관계 법령에 따라 가입한다."));
    }
}
