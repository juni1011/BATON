"use strict";

/** 일정, 프로필, 추천 결과의 저장 및 API 호출 도구를 제공한다. */
window.Easyton = (() => {
    const keys = Object.freeze({
        events: "easyton.calendarEvents", profile: "easyton.userProfile", matches: "easyton.supportMatches"
    });

    /** 값을 JSON으로 저장한다. null이면 저장 값을 삭제하며 저장소 오류는 호출자에게 전달한다. */
    function save(key, value) {
        if (value === null) localStorage.removeItem(key);
        else localStorage.setItem(key, JSON.stringify(value));
    }

    /** 저장 값을 조회한다. 미저장 또는 손상된 JSON이면 기본값을 반환한다. */
    function load(key, fallback = null) {
        const raw = localStorage.getItem(key);
        if (raw === null) return fallback;
        try { return JSON.parse(raw); }
        catch { return fallback; }
    }

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

    return Object.freeze({ saveCalendarEvents, getCalendarEvents, saveUserProfile, getUserProfile,
        saveSupportMatches, getSupportMatches, apiFetch, postJson });
})();
