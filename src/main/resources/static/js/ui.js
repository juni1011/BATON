"use strict";

/** 화면들이 함께 쓰는 표시용 도구. 저장과 API 호출은 common.js(Easyton)를 사용한다. */
window.EasytonUI = (() => {
    const VERDICT_LABELS = { ELIGIBLE: "신청 가능", NEED_CHECK: "확인 필요", NOT_ELIGIBLE: "대상 아님" };
    /** 캘린더가 다루는 일정 종류. 이 밖의 종류로 저장된 일정은 표시하지 않는다. */
    const EVENT_LABELS = { SUPPORT_DEADLINE: "지원사업 마감", TODO: "할 일", CUSTOM: "내 일정" };

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
        if (status[1] === "400") return "입력 내용을 확인해 주세요.";
        return `요청을 처리하지 못했습니다. (오류 ${status[1]})`;
    }

    /** 알림 상자에 메시지를 표시한다. message가 없으면 숨긴다. */
    function notify(box, message, kind = "") {
        box.hidden = !message;
        box.className = "notice " + kind;
        box.textContent = message || "";
    }

    document.addEventListener("DOMContentLoaded", () => {
        const path = location.pathname === "/" ? "/index.html" : location.pathname;
        for (const link of document.querySelectorAll("nav a")) {
            if (link.getAttribute("href") === path) link.setAttribute("aria-current", "page");
        }
    });

    return Object.freeze({ VERDICT_LABELS, EVENT_LABELS, h, dateLabel, todayIso, errorText, notify });
})();
