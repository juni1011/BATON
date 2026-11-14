"use strict";

/** 공통 계약 데이터와 리포트, 일정의 저장 및 API 호출 도구를 제공한다. */
window.Easyton = (() => {
    const keys = Object.freeze({
        contract: "easyton.contractInfo", report: "easyton.checkReport", events: "easyton.calendarEvents",
        profile: "easyton.userProfile", matches: "easyton.supportMatches"
    });

    const OWNER_KEY = "easyton.owner";
    /** localStorage 키 → 서버 저장 키. */
    const serverKeys = Object.freeze(Object.fromEntries(Object.entries(keys).map(([name, key]) => [key, name])));

    function owner() {
        try { return localStorage.getItem(OWNER_KEY); } catch { return null; }
    }

    /** 로그인한 계정의 데이터를 서버에도 반영한다. 실패해도 화면 동작은 막지 않는다. */
    function pushToServer(key, value) {
        const name = serverKeys[key];
        if (!name || owner() === null) return;
        fetch(`/api/me/data/${name}`, value === null
            ? { method: "DELETE", keepalive: true }
            : { method: "PUT", headers: { "Content-Type": "application/json" }, body: JSON.stringify(value), keepalive: true })
            .catch(() => { /* 다음 저장 때 다시 시도된다 */ });
    }

    /** 값을 JSON으로 저장한다. null이면 저장 값을 삭제하며 저장소 오류는 호출자에게 전달한다. 로그인 중이면 서버에도 저장한다. */
    function save(key, value) {
        if (value === null) localStorage.removeItem(key);
        else localStorage.setItem(key, JSON.stringify(value));
        pushToServer(key, value);
    }

    /** 이 브라우저에 남은 계정 데이터를 지운다(로그아웃, 세션 만료 시). */
    function clearLocal() {
        for (const key of Object.values(keys)) localStorage.removeItem(key);
        localStorage.removeItem(OWNER_KEY);
    }

    /** 로그인해야 쓸 수 있는 화면이다. 홈(index)과 로그인·회원가입 화면은 열어 둔다. */
    const PROTECTED_PAGES = ["/upload.html", "/confirm.html", "/report.html", "/calendar.html", "/support.html"];

    /** 로그인하지 않았으면 로그인 화면으로 보내고, 로그인 뒤 원래 화면으로 돌아오게 한다. */
    function requireLogin() {
        if (!PROTECTED_PAGES.includes(location.pathname)) return;
        location.replace("/signin.html?next=" + encodeURIComponent(location.pathname));
    }

    /**
     * 페이지 스크립트가 localStorage를 읽기 전에 계정 데이터를 맞춘다. 동기 요청이라 로컬 서버의 작은 응답만 기다린다.
     * - 로그인 중: 서버 데이터로 덮어쓴다. 서버가 비어 있으면 로그인 전에 쓰던 이 브라우저 데이터를 계정으로 옮긴다.
     * - 로그아웃 상태인데 이전 계정 데이터가 남아 있으면 지운다.
     * - 서버에 연결할 수 없으면 아무것도 바꾸지 않는다.
     */
    function syncWithAccount() {
        try {
            const me = new XMLHttpRequest();
            me.open("GET", "/api/auth/me", false);
            me.send();
            if (me.status === 401) {
                if (owner() !== null) clearLocal();
                requireLogin();
                return;
            }
            if (me.status !== 200) return;
            const user = JSON.parse(me.responseText);
            const data = new XMLHttpRequest();
            data.open("GET", "/api/me/data", false);
            data.send();
            if (data.status !== 200) return;
            const remote = JSON.parse(data.responseText);
            const previousOwner = owner();
            const adopt = previousOwner === null && Object.keys(remote).length === 0;
            const pending = [];
            for (const [key, name] of Object.entries(serverKeys)) {
                if (adopt) {
                    const raw = localStorage.getItem(key);
                    if (raw !== null) pending.push([name, raw]);
                } else if (name in remote) localStorage.setItem(key, JSON.stringify(remote[name]));
                else localStorage.removeItem(key);
            }
            localStorage.setItem(OWNER_KEY, String(user.id));
            for (const [name, raw] of pending) {
                fetch(`/api/me/data/${name}`, { method: "PUT", headers: { "Content-Type": "application/json" }, body: raw })
                    .catch(() => { /* 다음 저장 때 다시 시도된다 */ });
            }
        } catch { /* 저장소나 네트워크 오류 시 기존 동작을 유지한다 */ }
    }

    syncWithAccount();

    /** 저장 값을 조회한다. 미저장 또는 손상된 JSON이면 기본값을 반환한다. */
    function load(key, fallback = null) {
        const raw = localStorage.getItem(key);
        if (raw === null) return fallback;
        try { return JSON.parse(raw); }
        catch { return fallback; }
    }

    /** ContractInfo를 저장하며 미기재 필드의 null을 그대로 유지한다. */
    function saveContractInfo(value) { save(keys.contract, value); }
    /** 저장된 ContractInfo를 반환하며 미저장 상태는 null이다. */
    function getContractInfo() { return load(keys.contract); }
    /** CheckReport를 저장한다. */
    function saveCheckReport(value) { save(keys.report, value); }
    /** 저장된 CheckReport를 반환하며 미저장 상태는 null이다. */
    function getCheckReport() { return load(keys.report); }
    /** CalendarEvent 배열을 저장한다. */
    function saveCalendarEvents(value) { save(keys.events, value); }
    /** 저장된 CalendarEvent 배열을 반환하며 미저장 상태는 빈 배열이다. */
    function getCalendarEvents() { return load(keys.events, []); }
    /** 지원사업 추천에 쓰는 UserProfile을 저장한다. */
    function saveUserProfile(value) { save(keys.profile, value); }
    /** 저장된 UserProfile을 반환하며 미저장 상태는 null이다. */
    function getUserProfile() { return load(keys.profile); }
    /** SupportMatch 배열을 저장한다. */
    function saveSupportMatches(value) { save(keys.matches, value); }
    /** 저장된 SupportMatch 배열을 반환하며 미저장 상태는 빈 배열이다. */
    function getSupportMatches() { return load(keys.matches, []); }

    /** fetch를 실행하고 JSON 응답을 반환한다. HTTP 오류는 본문과 상태를 포함한 Error로 전달한다. */
    async function apiFetch(url, options = {}) {
        const response = await fetch(url, options);
        if (!response.ok) throw new Error(`HTTP ${response.status}: ${await response.text()}`);
        if (response.status === 204) return null;
        return response.json();
    }

    /** 객체를 JSON 본문으로 전송한다. 날짜와 시간은 API 문자열 형식을 그대로 사용한다. */
    function postJson(url, body) {
        return apiFetch(url, {
            method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body)
        });
    }

    /** 파일 또는 텍스트를 multipart로 전송한다. boundary는 브라우저가 지정한다. */
    function extractContract({ file = null, text = null, consent = false } = {}) {
        const body = new FormData();
        if (file !== null) body.append("file", file);
        if (consent) body.append("consent", "true");
        if (text !== null) body.append("text", text);
        return apiFetch("/api/contracts/extract", { method: "POST", body });
    }

    return Object.freeze({ saveContractInfo, getContractInfo, saveCheckReport, getCheckReport,
        saveCalendarEvents, getCalendarEvents, saveUserProfile, getUserProfile,
        saveSupportMatches, getSupportMatches, clearLocal, apiFetch, postJson, extractContract });
})();
