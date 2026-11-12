"use strict";

/** 담당 A: 계약서 파일·텍스트를 보내 추출 결과를 저장하고 확인 화면으로 이동한다. */
document.addEventListener("DOMContentLoaded", () => {
    const { errorText, notify } = EasytonUI;
    const form = document.getElementById("upload-form");
    const fileInput = document.getElementById("file");
    const textInput = document.getElementById("text");
    const message = document.getElementById("message");
    const submit = document.getElementById("submit");
    const consent = document.getElementById("consent");
    const consentBox = document.getElementById("image-consent");

    /** 사진을 고르면 AI 전송 동의 칸을 보여 준다. 동의는 파일을 바꿀 때마다 다시 받는다. */
    fileInput.addEventListener("change", () => {
        const file = fileInput.files[0];
        consentBox.hidden = !(file && file.type.startsWith("image/"));
        consent.checked = false;
    });

    const picker = document.getElementById("sample-picker");
    const sample = document.getElementById("sample");
    const toggle = document.getElementById("sample-toggle");
    const current = document.getElementById("sample-current");
    const popover = document.getElementById("sample-popover");
    const menu = document.getElementById("sample-options");
    const options = [...menu.querySelectorAll("[data-sample]")];
    const preview = document.getElementById("sample-preview");
    const previewTitle = document.getElementById("sample-preview-title");
    const previewStatus = document.getElementById("sample-preview-status");
    const previewText = document.getElementById("sample-preview-text");
    const sampleCache = new Map();
    let previewSequence = 0;
    let previewSample = null;
    let previewFailed = false;

    function sampleLabel(option) {
        return option.querySelector(".sample-option-label").textContent;
    }

    /** 샘플 원문을 캐시하며, 실패한 요청은 다시 시도할 수 있다. */
    function getSample(number) {
        if (!sampleCache.has(number)) {
            const request = fetch("/api/contracts/samples/" + encodeURIComponent(number))
                .then(response => {
                    if (!response.ok) throw new Error("HTTP " + response.status);
                    return response.text();
                })
                .catch(error => {
                    sampleCache.delete(number);
                    throw error;
                });
            sampleCache.set(number, request);
        }
        return sampleCache.get(number);
    }

    /** 닫힌 창 또는 이전 항목의 늦은 응답이 현재 미리보기를 덮어쓰지 않게 한다. */
    async function showPreview(option) {
        if (popover.hidden) return;
        const number = option.dataset.sample;
        if (previewSample === number && !preview.hidden && !previewFailed) return;
        const sequence = ++previewSequence;
        previewSample = number;
        previewFailed = false;
        preview.hidden = false;
        previewTitle.textContent = sampleLabel(option) + " 미리보기";
        previewStatus.textContent = "샘플을 불러오는 중…";
        previewText.textContent = "";
        previewText.setAttribute("aria-busy", "true");
        for (const item of options) item.dataset.previewActive = String(item === option);
        try {
            const text = await getSample(number);
            if (sequence !== previewSequence || popover.hidden || previewSample !== number) return;
            previewText.textContent = text;
            previewText.scrollTop = 0;
            previewStatus.textContent = "미리보기는 입력 중인 내용을 바꾸지 않습니다.";
        } catch (error) {
            if (sequence !== previewSequence || popover.hidden || previewSample !== number) return;
            previewFailed = true;
            previewStatus.textContent = "미리보기를 불러오지 못했어요. " + errorText(error) + " 항목에 다시 마우스를 올리면 재시도합니다.";
        } finally {
            if (sequence === previewSequence) previewText.setAttribute("aria-busy", "false");
        }
    }

    function focusOption(index) {
        const next = (index + options.length) % options.length;
        options.forEach((option, position) => { option.tabIndex = position === next ? 0 : -1; });
        options[next].focus();
    }

    function openPopover(index = options.findIndex(option => option.dataset.sample === sample.value)) {
        popover.hidden = false;
        toggle.setAttribute("aria-expanded", "true");
        focusOption(index);
    }

    function closePopover(restoreFocus = false) {
        popover.hidden = true;
        preview.hidden = true;
        previewSample = null;
        previewSequence++;
        toggle.setAttribute("aria-expanded", "false");
        for (const option of options) option.dataset.previewActive = "false";
        if (restoreFocus) toggle.focus();
    }

    toggle.addEventListener("click", () => {
        if (popover.hidden) openPopover();
        else closePopover();
    });
    toggle.addEventListener("keydown", event => {
        if (event.key !== "ArrowDown" && event.key !== "ArrowUp") return;
        event.preventDefault();
        openPopover(event.key === "ArrowUp" ? options.length - 1 : undefined);
    });
    for (const option of options) {
        option.addEventListener("pointerenter", () => showPreview(option));
        option.addEventListener("focus", () => showPreview(option));
        option.addEventListener("click", () => {
            sample.value = option.dataset.sample;
            current.textContent = sampleLabel(option);
            for (const item of options) item.setAttribute("aria-selected", String(item === option));
            // 터치 화면에서도 선택한 계약서를 충분히 읽을 수 있도록 창을 유지한다.
            focusOption(options.indexOf(option));
            showPreview(option);
        });
    }
    menu.addEventListener("keydown", event => {
        const index = options.indexOf(document.activeElement);
        const positions = { ArrowDown: index + 1, ArrowUp: index - 1, Home: 0, End: options.length - 1 };
        if (!(event.key in positions)) return;
        event.preventDefault();
        focusOption(positions[event.key]);
    });
    document.addEventListener("keydown", event => {
        if (event.key !== "Escape" || popover.hidden) return;
        event.preventDefault();
        closePopover(true);
    });
    picker.addEventListener("focusout", event => {
        if (event.relatedTarget && !picker.contains(event.relatedTarget)) closePopover();
    });
    document.addEventListener("pointerdown", event => {
        if (!picker.contains(event.target)) closePopover();
    });
    document.getElementById("sample-preview-close").addEventListener("click", () => closePopover(true));

    form.addEventListener("submit", async event => {
        event.preventDefault();
        const file = fileInput.files[0] || null;
        const text = textInput.value.trim();
        if (!file && !text) {
            notify(message, "파일을 선택하거나 계약서 내용을 붙여넣어 주세요.", "error");
            return;
        }
        const isImage = Boolean(file && file.type.startsWith("image/"));
        if (isImage && !consent.checked) {
            notify(message, "사진을 분석하려면 AI 전송에 동의해 주세요.", "error");
            consent.focus();
            return;
        }
        submit.disabled = true;
        submit.textContent = isImage ? "사진을 분석하는 중… (최대 몇 분)" : "추출하는 중…";
        try {
            const contract = await Easyton.extractContract(file ? { file, consent: isImage && consent.checked } : { text });
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
