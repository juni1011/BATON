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

    /** 서버에 아직 반영하지 못한 저장 키 이름 목록. 다음에 페이지를 열 때 서버 값으로 덮어쓰지 않고 다시 보낸다. */
    const PENDING_KEY = "easyton.pendingSync";

    function pendingNames() {
        try {
            const list = JSON.parse(localStorage.getItem(PENDING_KEY) || "[]");
            return Array.isArray(list) ? list : [];
        } catch { return []; }
    }

    function markPending(name, pending) {
        try {
            const rest = pendingNames().filter(item => item !== name);
            if (pending) rest.push(name);
            if (rest.length) localStorage.setItem(PENDING_KEY, JSON.stringify(rest));
            else localStorage.removeItem(PENDING_KEY);
        } catch { /* 저장소 오류는 무시한다 */ }
    }

    /** 로컬 값(raw JSON 문자열, 삭제면 null)을 서버에 보낸다. 성공하면 true. */
    async function sendToServer(name, raw) {
        const options = raw === null ? { method: "DELETE" }
            : { method: "PUT", headers: { "Content-Type": "application/json" }, body: raw };
        // keepalive 요청은 본문이 64KB를 넘으면 거부되므로 작은 값에만 쓴다.
        if (raw === null || raw.length < 30000) options.keepalive = true;
        try {
            const response = await fetch(`/api/me/data/${name}`, options);
            if (!response.ok) return false;
            markPending(name, false);
            return true;
        } catch { return false; }
    }

    /**
     * 로그인한 계정의 데이터를 서버에도 반영한다. 화면 동작은 막지 않는다.
     * 결과는 "server"(계정에 저장됨), "local"(로그인 전이라 이 브라우저에만 저장), "failed"(서버 저장 실패, 다음에 다시 시도)이다.
     */
    function pushToServer(key, value) {
        const name = serverKeys[key];
        if (!name || owner() === null) return Promise.resolve("local");
        markPending(name, true);
        return sendToServer(name, value === null ? null : JSON.stringify(value))
            .then(ok => ok ? "server" : "failed");
    }

    /** 값을 JSON으로 저장한다. null이면 저장 값을 삭제하며 저장소 오류는 호출자에게 전달한다. 로그인 중이면 서버에도 저장하고 그 결과를 약속으로 돌려준다. */
    function save(key, value) {
        if (value === null) localStorage.removeItem(key);
        else localStorage.setItem(key, JSON.stringify(value));
        return pushToServer(key, value);
    }

    /** 이 브라우저에 남은 계정 데이터를 지운다(로그아웃, 세션 만료 시). */
    function clearLocal() {
        for (const key of Object.values(keys)) localStorage.removeItem(key);
        localStorage.removeItem(OWNER_KEY);
        localStorage.removeItem(PENDING_KEY);
    }

    /** 로그인해야 쓸 수 있는 화면이다. 홈(index)과 로그인·회원가입 화면은 열어 둔다. */
    const PROTECTED_PAGES = ["/upload.html", "/confirm.html", "/report.html", "/calendar.html", "/support.html"];

    /** 로그인하지 않았으면 로그인 화면으로 보내고, 로그인 뒤 원래 화면으로 돌아오게 한다. */
    function requireLogin() {
        if (!PROTECTED_PAGES.includes(location.pathname)) return;
        location.replace("/signin.html?next=" + encodeURIComponent(location.pathname));
    }

    /** 계정 일정과 이 브라우저의 일정을 id 기준으로 합친다. */
    function mergeEvents(remoteEvents, localRaw) {
        let local = [];
        try { local = JSON.parse(localRaw); } catch { /* 손상된 값은 무시한다 */ }
        const merged = Array.isArray(remoteEvents) ? [...remoteEvents] : [];
        const ids = new Set(merged.map(event => event && event.id));
        for (const event of Array.isArray(local) ? local : []) {
            if (event && !ids.has(event.id)) merged.push(event);
        }
        return merged;
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
            // 같은 계정이 서버 저장에 실패해 남긴 값은 서버 값으로 덮어쓰지 않고 다시 보낸다.
            const retry = previousOwner === String(user.id) ? pendingNames() : [];
            const pending = [];
            for (const [key, name] of Object.entries(serverKeys)) {
                const raw = localStorage.getItem(key);
                if (retry.includes(name)) pending.push([name, raw]);
                else if (adopt) {
                    if (raw !== null) pending.push([name, raw]);
                } else if (name === "events" && previousOwner === null && raw !== null) {
                    // 로그인 전에 이 브라우저에 넣어 둔 일정은 계정 일정과 합친다.
                    const merged = mergeEvents(remote.events, raw);
                    localStorage.setItem(key, JSON.stringify(merged));
                    pending.push([name, JSON.stringify(merged)]);
                } else if (name in remote) localStorage.setItem(key, JSON.stringify(remote[name]));
                else localStorage.removeItem(key);
            }
            localStorage.setItem(OWNER_KEY, String(user.id));
            for (const [name, raw] of pending) {
                markPending(name, true);
                sendToServer(name, raw);
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
    function saveContractInfo(value) { return save(keys.contract, value); }
    /** 저장된 ContractInfo를 반환하며 미저장 상태는 null이다. */
    function getContractInfo() { return load(keys.contract); }
    /** CheckReport를 저장한다. */
    function saveCheckReport(value) { return save(keys.report, value); }
    /** 저장된 CheckReport를 반환하며 미저장 상태는 null이다. */
    function getCheckReport() { return load(keys.report); }
    /** CalendarEvent 배열을 저장한다. */
    function saveCalendarEvents(value) { return save(keys.events, value); }
    /** 저장된 CalendarEvent 배열을 반환하며 미저장 상태는 빈 배열이다. */
    function getCalendarEvents() { return load(keys.events, []); }
    /** 지원사업 추천에 쓰는 UserProfile을 저장한다. */
    function saveUserProfile(value) { return save(keys.profile, value); }
    /** 저장된 UserProfile을 반환하며 미저장 상태는 null이다. */
    function getUserProfile() { return load(keys.profile); }
    /** SupportMatch 배열을 저장한다. */
    function saveSupportMatches(value) { return save(keys.matches, value); }
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
