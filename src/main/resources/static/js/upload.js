"use strict";

/** 담당 A: 계약서 파일·텍스트를 보내 추출 결과를 저장하고 확인 화면으로 이동한다. */
document.addEventListener("DOMContentLoaded", () => {
    const { errorText, notify } = EasytonUI;
    const form = document.getElementById("upload-form");
    const fileInput = document.getElementById("file");
    const textInput = document.getElementById("text");
    const message = document.getElementById("message");
    const submit = document.getElementById("submit");

    // 모든 상호와 장소가 가상인 시연용 예시. 점검 항목이 여러 개 나오도록 구성했다.
    const EXAMPLE = [
        "[가상 근로계약서 예시]",
        "사용자: 가상햇살카페",
        "근무 장소: 가상시 상상구 예시로 12",
        "업무: 음료 제조 및 매장 정리",
        "계약 기간: 2026-11-16 ~ 2027-05-15",
        "근무 요일: 토요일, 일요일",
        "근무 시간: 10:00 ~ 18:30 / 주 소정근로시간: 16시간",
        "수습 기간은 3개월로 하며 수습 기간 시급은 90%를 지급한다.",
        "시급은 10,500원이다.",
        "근무 중 30분의 휴게시간을 부여한다.",
        "지각 3회 시 급여에서 1일분을 공제한다.",
        "계약 기간 중 퇴사할 경우 근로자는 위약금으로 300,000원을 지급한다."
    ].join("\n");

    document.getElementById("example").addEventListener("click", () => {
        textInput.value = EXAMPLE;
        fileInput.value = "";
        notify(message, "");
        textInput.focus();
    });

    form.addEventListener("submit", async event => {
        event.preventDefault();
        const file = fileInput.files[0] || null;
        const text = textInput.value.trim();
        if (!file && !text) {
            notify(message, "파일을 선택하거나 계약서 내용을 붙여넣어 주세요.", "error");
            return;
        }
        submit.disabled = true;
        submit.textContent = "추출하는 중…";
        try {
            const contract = await Easyton.extractContract(file ? { file } : { text });
            Easyton.saveContractInfo(contract);
            // 이전 계약의 리포트와 계약 일정은 더 이상 맞지 않으므로 지운다. 직접 추가한 일정은 남긴다.
            Easyton.saveCheckReport(null);
            EasytonUI.saveGeneratedEvents([]);
            location.href = "/confirm.html?from=upload";
        } catch (error) {
            notify(message, errorText(error), "error");
            submit.disabled = false;
            submit.textContent = "항목 추출하기";
        }
    });
});
