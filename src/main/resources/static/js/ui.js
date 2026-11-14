"use strict";

/** 화면들이 함께 쓰는 표시용 도구. 저장과 API 호출은 common.js(Easyton)를 사용한다. */
window.EasytonUI = (() => {
    const DAYS = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];
    const DAY_LABELS = { MONDAY: "월", TUESDAY: "화", WEDNESDAY: "수", THURSDAY: "목", FRIDAY: "금", SATURDAY: "토", SUNDAY: "일" };
    const LEVEL_LABELS = { DANGER: "위험", WARNING: "주의", MISSING: "누락", OK: "정상" };
    const VERDICT_LABELS = { ELIGIBLE: "신청 가능", NEED_CHECK: "확인 필요", NOT_ELIGIBLE: "대상 아님" };
    const EVENT_LABELS = {
        WORK: "근무", PAYDAY: "급여일", CONTRACT_END: "계약 종료", PROBATION_END: "수습 종료",
        SUPPORT_DEADLINE: "지원사업 마감", TODO: "할 일", CUSTOM: "내 일정"
    };
    /** 사용자가 직접 추가해 계약 일정 재생성 때 지우지 않는 종류. */
    const USER_EVENT_TYPES = ["SUPPORT_DEADLINE", "TODO", "CUSTOM"];

    /** 요소를 만든다. props의 class·text·on* 외 값은 속성으로 넣고, 자식 문자열은 텍스트로만 넣는다. */
    function h(tag, props = {}, ...children) {
        const node = document.createElement(tag);
        for (const [key, value] of Object.entries(props || {})) {
            if (value === null || value === undefined || value === false) continue;
            if (key === "class") node.className = value;
            else if (key === "text") node.textContent = value;
            else if (key.startsWith("on")) node.addEventListener(key.slice(2), value);
            else node.setAttribute(key, value === true ? "" : value);
        }
        for (const child of children.flat()) {
            if (child === null || child === undefined || child === false) continue;
            node.append(child);
        }
        return node;
    }

    function won(value) {
        return value === null || value === undefined ? "-" : Number(value).toLocaleString("ko-KR") + "원";
    }

    /** "2026-11-01" → "2026.11.01 (일)". 시간대 영향을 받지 않도록 UTC로만 계산한다. */
    function dateLabel(iso) {
        if (!iso) return "-";
        const [y, m, d] = iso.split("-").map(Number);
        const day = "일월화수목금토"[new Date(Date.UTC(y, m - 1, d)).getUTCDay()];
        return `${y}.${String(m).padStart(2, "0")}.${String(d).padStart(2, "0")} (${day})`;
    }

    /** 브라우저 현지 날짜를 yyyy-MM-dd로 돌려준다. */
    function todayIso() {
        const now = new Date();
        return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
    }

    /** apiFetch 오류를 사용자에게 보여 줄 문장으로 바꾼다. */
    function errorText(error) {
        const status = /^HTTP (\d{3})/.exec(error && error.message ? error.message : "");
        if (!status) return "서버에 연결할 수 없습니다. 서버가 실행 중인지 확인해 주세요.";
        if (status[1] === "415") return "텍스트(.txt) 파일이나 PNG·JPG·WEBP·GIF 사진만 읽을 수 있습니다. PDF는 내용을 붙여넣어 주세요.";
        if (status[1] === "413") return "파일이나 내용이 너무 큽니다. 사진은 10MB 이하, 텍스트는 3만 자 이하로 줄여 주세요.";
        if (status[1] === "422") return "AI가 계약서 내용을 읽지 못했어요. 사진이라면 더 선명한 사진으로 다시 올리거나, 내용을 붙여넣어 주세요.";
        if (status[1] === "429") return "지금 AI 분석 요청이 많아요. 잠시 뒤 다시 시도해 주세요.";
        if (status[1] === "502") return "AI 분석에 실패했어요. 잠시 뒤 다시 시도하거나, AI 체크를 풀고 항목이 줄마다 적힌 형식으로 읽어 보세요.";
        if (status[1] === "503") return "AI 분석을 사용할 수 없어요. 이 서버에서 Codex를 쓸 수 없는 상태입니다. AI 체크를 풀고 다시 시도해 주세요.";
        if (status[1] === "504") return "AI 분석 시간이 너무 오래 걸렸어요. 다시 시도해 주세요.";
        if (status[1] === "400") return "입력 내용을 확인해 주세요.";
        return `요청을 처리하지 못했습니다. (오류 ${status[1]})`;
    }

    /** 알림 상자에 메시지를 표시한다. message가 없으면 숨긴다. */
    function notify(box, message, kind = "") {
        box.hidden = !message;
        box.className = "notice " + kind;
        box.textContent = message || "";
    }

    /** 새로 생성한 계약 일정과 기존에 사용자가 추가한 일정을 합쳐 저장한다. */
    function saveGeneratedEvents(generated) {
        const kept = Easyton.getCalendarEvents().filter(event => USER_EVENT_TYPES.includes(event.type));
        const events = [...generated, ...kept];
        Easyton.saveCalendarEvents(events);
        return events;
    }

    /** 계약 정보로 점검과 일정 생성을 함께 실행하고 결과를 저장한다. */
    async function analyze(contract) {
        const [report, events] = await Promise.all([
            Easyton.postJson("/api/checks", contract),
            Easyton.postJson("/api/calendar/events", contract)
        ]);
        Easyton.saveCheckReport(report);
        saveGeneratedEvents(events);
        return report;
    }

    document.addEventListener("DOMContentLoaded", () => {
        const path = location.pathname === "/" ? "/index.html" : location.pathname;
        for (const link of document.querySelectorAll("nav a")) {
            if (link.pathname === path && link.origin === location.origin) link.setAttribute("aria-current", "page");
        }
    });

    function contractTime(id) {
        const [hour, minute] = document.getElementById(id).querySelectorAll("select");
        return {
            get value() { return hour.value ? hour.value + ":" + minute.value : ""; },
            set value(time) {
                const [hours, minutes] = (time || "").split(":");
                hour.value = hours?.padStart(2, "0") || "";
                minute.value = minutes || "00";
            }
        };
    }

    return Object.freeze({ contractTime, DAYS, DAY_LABELS, LEVEL_LABELS, VERDICT_LABELS, EVENT_LABELS, USER_EVENT_TYPES,
        h, won, dateLabel, todayIso, errorText, notify, saveGeneratedEvents, analyze });
})();

/** 로그인 상태이면 메뉴의 "로그인" 링크를 "이름님 · 로그아웃"으로 바꾼다. 서버에 연결할 수 없으면 그대로 둔다. */
document.addEventListener("DOMContentLoaded", async () => {
    const account = document.querySelector(".account-nav");
    if (!account) return;
    try {
        const response = await fetch("/api/auth/me");
        if (!response.ok) return;
        const user = await response.json();
        const logout = EasytonUI.h("a", {
            href: "#", text: "로그아웃",
            onclick: async event => {
                event.preventDefault();
                try {
                    const result = await fetch("/api/auth/signout", { method: "POST" });
                    if (!result.ok) throw new Error("Signout failed");
                } catch {
                    const status = account.querySelector("[role=status]");
                    if (!status) account.append(EasytonUI.h("span", { role: "status", text: "로그아웃을 다시 시도해 주세요." }));
                    return;
                }
                if (window.Easyton) Easyton.clearLocal();
                location.assign("/index.html");
            }
        });
        account.replaceChildren(EasytonUI.h("span", { class: "nav-user", text: `${user.name}님` }), logout);
    } catch { /* 비로그인 상태로 둔다 */ }
});
