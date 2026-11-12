"use strict";

/** 홈 화면에 추천 결과 요약과 다가오는 일정을 보여 준다. */
document.addEventListener("DOMContentLoaded", () => {
    const { VERDICT_LABELS, EVENT_LABELS, h, dateLabel, todayIso } = EasytonUI;
    const matches = Easyton.getSupportMatches();
    const events = Easyton.getCalendarEvents().filter(event => event.type in EVENT_LABELS);

    const summary = document.getElementById("summary");
    if (!matches.length) {
        summary.append(h("p", { class: "muted", text: "내 정보를 입력하면 추천 결과를 여기에서 볼 수 있어요." }),
            h("a", { class: "btn primary", href: "/signin.html?next=%2Fsupport.html", "data-auth-destination": "/support.html", text: "추천 받기" }));
    } else {
        summary.append(...matches.slice(0, 5).map(match => h("div", { class: "event" },
            h("span", { class: "badge " + match.verdict, text: VERDICT_LABELS[match.verdict] || match.verdict }),
            h("div", { class: "body" }, h("strong", { text: match.name }),
                h("div", { class: "small muted", text: `마감 ${dateLabel(match.applyEnd)}` })))));
    }

    const upcoming = document.getElementById("upcoming");
    const today = todayIso();
    const next = events.filter(event => event.date && event.date >= today)
        .sort((a, b) => (a.date + (a.startTime || "")).localeCompare(b.date + (b.startTime || ""))).slice(0, 5);
    if (!next.length) {
        upcoming.append(h("p", { class: "muted", text: events.length ? "예정된 일정이 없습니다." : "계약서를 점검하거나 마감일·개인 일정을 추가하면 여기에 표시됩니다." }));
    } else {
        upcoming.append(...next.map(event => h("div", { class: "event" },
            h("span", { class: "badge " + event.type, text: EVENT_LABELS[event.type] }),
            h("div", { class: "body" }, h("a", { href: "/calendar.html?date=" + encodeURIComponent(event.date), text: event.title }),
                h("div", { class: "small muted", text: dateLabel(event.date) + (event.startTime ? ` ${event.startTime}` : "") })))));
    }

    const protectedLinks = [...document.querySelectorAll("a[data-auth-destination]")];
    const signinUrl = destination => "/signin.html?next=" + encodeURIComponent(destination);
    let authCheck;

    function refreshAuthLinks() {
        authCheck = fetch("/api/auth/me", { credentials: "same-origin", cache: "no-store" })
            .then(response => response.ok, () => false);
        authCheck.then(signedIn => {
            for (const link of protectedLinks) {
                const destination = link.dataset.authDestination;
                link.href = signedIn ? destination : signinUrl(destination);
            }
        });
        return authCheck;
    }

    for (const link of protectedLinks) {
        link.addEventListener("click", async event => {
            if (event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
            event.preventDefault();
            const signedIn = await authCheck;
            const destination = link.dataset.authDestination;
            location.assign(signedIn ? destination : signinUrl(destination));
        });
    }
    refreshAuthLinks();
    window.addEventListener("focus", refreshAuthLinks);
    window.addEventListener("pageshow", event => { if (event.persisted) refreshAuthLinks(); });

    document.getElementById("reset").addEventListener("click", () => {
        if (!confirm("이 브라우저에 저장된 프로필, 추천 결과, 일정을 모두 지울까요?")) return;
        Easyton.saveCalendarEvents(null);
        Easyton.saveUserProfile(null);
        Easyton.saveSupportMatches(null);
        // 이전 버전(계약서 기능)이 남긴 값도 함께 지운다.
        localStorage.removeItem("easyton.contractInfo");
        localStorage.removeItem("easyton.checkReport");
        location.reload();
    });
});
