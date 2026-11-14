"use strict";

/** 담당 A: 계약서 파일·텍스트를 보내 추출 결과를 저장하고 확인 화면으로 이동한다. */
document.addEventListener("DOMContentLoaded", () => {
    const { errorText, notify } = EasytonUI;
    const form = document.getElementById("upload-form");

    const fileInput = document.getElementById("file");
    const manualFields = document.getElementById("manual-fields");
    const hours = document.getElementById("workday-hours");
    const startTime = EasytonUI.contractTime("startTime");
    const endTime = EasytonUI.contractTime("endTime");
    const dayButtons = [...document.querySelectorAll("#workday-buttons button")];
    const schedule = new Map();
    let activeDay = null;
    function showDay(day) {
        activeDay = day;
        const time = schedule.get(day);
        startTime.value = time?.startTime ?? "";
        endTime.value = time?.endTime ?? "";
        document.getElementById("workday-editor-status").textContent = day
            ? EasytonUI.DAY_LABELS[day] + "요일 근무 시간" : "요일을 선택해 주세요. (같은 요일을 두 번 누르면 선택이 취소돼요.)";
        for (const input of hours.querySelectorAll("select")) input.disabled = !day;
        for (const button of dayButtons) {
            button.setAttribute("aria-pressed", String(schedule.has(button.dataset.day)));
            button.classList.toggle("is-editing", button.dataset.day === day);
        }
    }
    for (const button of dayButtons) {
        button.addEventListener("click", () => {
            const day = button.dataset.day;
            if (day === activeDay) {
                schedule.delete(day);
                showDay([...schedule.keys()].pop() ?? null);
            } else {
                if (!schedule.has(day)) schedule.set(day, {});
                showDay(day);
            }
        });
    }
    hours.addEventListener("change", () => {
        if (activeDay) schedule.set(activeDay, { startTime: startTime.value || null, endTime: endTime.value || null });
    });
    showDay(null);
    const consent = document.getElementById("consent");
    /** AI 전송 동의는 사용자가 직접 체크한 선택만 이 브라우저에 기억한다. */
    try { consent.checked = localStorage.getItem("easyton.aiConsent") === "true"; } catch { /* 저장소를 못 쓰면 기본값 */ }
    consent.addEventListener("change", () => {
        try { localStorage.setItem("easyton.aiConsent", String(consent.checked)); } catch { /* 무시 */ }
    });
    const message = document.getElementById("message");
    const submit = document.getElementById("submit");
    const submitLabel = submit.textContent;
    const dropzone = document.getElementById("file-dropzone");
    const filePrompt = document.getElementById("file-prompt");
    const fileName = document.getElementById("file-name");
    const fileStatus = document.getElementById("file-status");
    let dragDepth = 0;
    let lastValidFiles = fileInput.files;

    const isImageFile = file => /\.(png|jpe?g|webp|gif)$/i.test(file.name)
        || /^image\/(png|jpe?g|webp|gif)$/i.test(file.type);

    function fileError(file) {
        if (!isImageFile(file) && !/\.txt$/i.test(file.name) && !file.type.startsWith("text/")) {
            return "이 파일 형식은 읽을 수 없어요. TXT, PNG, JPG, WebP, GIF 파일을 고르거나 아래에 계약서 내용을 붙여넣어 주세요.";
        }
        if (!file.size || file.size > 10 * 1024 * 1024) return "내용이 있는 10MB 이하 파일을 선택해 주세요.";
        return "";
    }

    function showFileStatus(text, error = false) {
        fileStatus.textContent = text;
        fileStatus.hidden = !text;
        fileStatus.classList.toggle("is-error", error);
        fileStatus.setAttribute("role", error ? "alert" : "status");
    }

    function renderFile(dragging = false) {
        const file = fileInput.files[0];
        dropzone.classList.toggle("is-dragging", dragging);
        filePrompt.textContent = dragging ? "여기에 드롭하세요" : file ? "계약서가 첨부되었어요" : "계약서를 끌어오거나 눌러서 첨부해 주세요";
        fileName.hidden = !file || dragging;
        fileName.textContent = file ? file.name + " · " + (file.size >= 1024 * 1024
            ? (file.size / (1024 * 1024)).toFixed(1) + "MB" : Math.max(1, Math.ceil(file.size / 1024)) + "KB") : "";
    }

    function resetDrag() {
        dragDepth = 0;
        renderFile();
    }

    const isFileDrag = event => Array.from(event.dataTransfer?.types || []).includes("Files");
    fileInput.addEventListener("change", () => {
        const file = fileInput.files[0];
        const error = file ? fileError(file) : "";
        if (error) fileInput.files = lastValidFiles;
        else lastValidFiles = fileInput.files;
        resetDrag();
        showFileStatus(error || (file ? "첨부한 계약서를 확인했어요. ‘다음’을 눌러 진행해 주세요." : ""), Boolean(error));
    });
    dropzone.addEventListener("dragenter", event => {
        if (!isFileDrag(event) || fileInput.disabled) return;
        event.preventDefault();
        dragDepth++;
        renderFile(true);
    });
    dropzone.addEventListener("dragover", event => {
        if (!isFileDrag(event) || fileInput.disabled) return;
        event.preventDefault();
        event.dataTransfer.dropEffect = "copy";
    });
    dropzone.addEventListener("dragleave", () => {
        dragDepth = Math.max(0, dragDepth - 1);
        if (!dragDepth) resetDrag();
    });
    dropzone.addEventListener("drop", event => {
        if (!isFileDrag(event)) return;
        event.preventDefault();
        resetDrag();
        if (fileInput.disabled) return;
        const files = event.dataTransfer.files;
        if (files.length !== 1) {
            showFileStatus("계약서 파일을 한 번에 하나씩 첨부해 주세요.", true);
            return;
        }
        const error = fileError(files[0]);
        if (error) {
            showFileStatus(error, true);
            return;
        }
        fileInput.files = files;
        fileInput.dispatchEvent(new Event("change", { bubbles: true }));
    });
    // 업로드 영역 밖에 놓아도 브라우저가 계약서 파일로 이동하지 않게 한다.
    for (const type of ["dragover", "drop"]) document.addEventListener(type, event => {
        if (!isFileDrag(event)) return;
        event.preventDefault();
        if (type === "drop") resetDrag();
    });
    document.addEventListener("dragleave", event => {
        if (!event.relatedTarget && (event.target === document.documentElement || event.target === document.body)) resetDrag();
    });
    window.addEventListener("dragend", resetDrag);
    window.addEventListener("blur", resetDrag);

    const picker = document.getElementById("sample-picker");
    const toggle = document.getElementById("sample-toggle");
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
    let previewHideTimer = null;

    function cancelPreviewHide() {
        clearTimeout(previewHideTimer);
        previewHideTimer = null;
    }

    function hidePreview() {
        cancelPreviewHide();
        preview.hidden = true;
        previewSample = null;
        previewSequence++;
        for (const option of options) option.dataset.previewActive = "false";
    }

    function schedulePreviewHide(event) {
        if (event.pointerType === "touch") return;
        cancelPreviewHide();
        // 항목과 미리보기 사이의 짧은 간격을 건너가며 읽고 스크롤할 수 있다.
        previewHideTimer = setTimeout(hidePreview, 120);
    }

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
        cancelPreviewHide();
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
        options[next].focus();
        showPreview(options[next]);
    }

    function openPopover(index = null) {
        popover.hidden = false;
        toggle.setAttribute("aria-expanded", "true");
        if (index !== null) focusOption(index);
    }

    function closePopover(restoreFocus = false) {
        popover.hidden = true;
        hidePreview();
        toggle.setAttribute("aria-expanded", "false");
        if (restoreFocus) toggle.focus();
    }

    toggle.addEventListener("click", () => {
        if (popover.hidden) openPopover();
        else closePopover();
    });
    toggle.addEventListener("keydown", event => {
        if (event.key !== "ArrowDown" && event.key !== "ArrowUp") return;
        event.preventDefault();
        openPopover(event.key === "ArrowUp" ? options.length - 1 : 0);
    });
    for (const option of options) {
        option.addEventListener("pointerenter", () => showPreview(option));
        option.addEventListener("pointerleave", schedulePreviewHide);
        option.addEventListener("focus", () => {
            if (option.matches(":focus-visible")) showPreview(option);
        });
        // 터치·키보드에서도 내용을 열 수 있지만 선택값은 저장하지 않는다.
        option.addEventListener("click", () => showPreview(option));
    }
    preview.addEventListener("pointerenter", cancelPreviewHide);
    preview.addEventListener("pointerleave", schedulePreviewHide);
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
    toggle.addEventListener("focus", hidePreview);
    document.addEventListener("pointerdown", event => {
        if (!picker.contains(event.target)) closePopover();
    });

    form.addEventListener("submit", async event => {
        event.preventDefault();
        if (submit.disabled) return;
        const file = fileInput.files[0] || null;
        const fields = {};
        // 첨부 파일을 우선 사용한다.
        // 직접 입력 항목만 있으면 서버나 AI로 보내지 않고 확인 화면에 전달한다.
        if (!file) {
            for (const input of manualFields.querySelectorAll("input, select")) {
                if (!input.reportValidity()) return;
            }
            for (const id of ["employerName", "workplace", "hourlyWage", "payDay"]) {
                const input = document.getElementById(id);
                const value = input.value.trim();
                if (value) fields[id] = input.type === "number" ? Number(value) : value;
            }
            if (schedule.size) {
                fields.workDays = EasytonUI.DAYS.filter(day => schedule.has(day));
                fields.workTimes = Object.fromEntries(schedule);
            }
            if (Object.keys(fields).length === 0) {
                notify(message, "파일을 선택하거나 계약 항목을 하나 이상 입력해 주세요.", "error");
                return;
            }
        }
        if (file && fileError(file)) {
            notify(message, fileError(file), "error");
            return;
        }
        if (file && isImageFile(file) && !consent.checked) {
            notify(message, "사진을 분석하려면 AI 전송에 동의해 주세요.", "error");
            consent.focus();
            return;
        }
        const useAi = consent.checked;
        const extracting = Boolean(file);
        submit.disabled = true;
        fileInput.disabled = true;
        consent.disabled = true;
        submit.textContent = extracting ? (useAi ? "AI가 읽는 중… (최대 몇 분)" : "추출하는 중…") : "입력 내용을 여는 중…";
        try {
            const contract = file
                ? await Easyton.extractContract({ file, consent: useAi })
                : fields;
            Easyton.saveContractInfo(contract);
            // 이전 계약의 리포트와 계약 일정은 더 이상 맞지 않으므로 지운다. 직접 추가한 일정은 남긴다.
            Easyton.saveCheckReport(null);
            EasytonUI.saveGeneratedEvents([]);
            location.href = extracting ? "/confirm.html?from=upload" : "/confirm.html";
        } catch (error) {
            notify(message, /^HTTP /.test(error.message) ? errorText(error) : error.message, "error");
            submit.disabled = false;
            fileInput.disabled = false;
            consent.disabled = false;
            submit.textContent = submitLabel;
        }
    });
});
