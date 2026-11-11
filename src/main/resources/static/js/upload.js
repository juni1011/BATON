"use strict";

/** 담당 A: 계약서 파일·텍스트를 보내 추출 결과를 저장하고 확인 화면으로 이동한다. */
document.addEventListener("DOMContentLoaded", () => {
    const { errorText, notify } = EasytonUI;
    const form = document.getElementById("upload-form");
    const fileInput = document.getElementById("file");
    const textInput = document.getElementById("text");
    const message = document.getElementById("message");
    const submit = document.getElementById("submit");

    const example = document.getElementById("example");
    example.addEventListener("click", async () => {
        const sample = document.getElementById("sample").value;
        example.disabled = true;
        try {
            const response = await fetch("/api/contracts/samples/" + encodeURIComponent(sample));
            if (!response.ok) throw new Error("HTTP " + response.status);
            textInput.value = await response.text();
            fileInput.value = "";
            notify(message, "가상 계약서 샘플을 불러왔어요.");
            textInput.focus();
        } catch (error) {
            notify(message, errorText(error), "error");
        } finally {
            example.disabled = false;
        }
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
