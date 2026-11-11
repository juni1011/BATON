"use strict";

/** 홈 화면에 프로필 상태, 추천 결과 요약, 다가오는 일정을 보여 준다. */
document.addEventListener("DOMContentLoaded", () => {
    const { VERDICT_LABELS, EVENT_LABELS, h, dateLabel, todayIso } = EasytonUI;
    const profile = Easyton.getUserProfile();
    const matches = Easyton.getSupportMatches();
    const events = Easyton.getCalendarEvents().filter(event => event.type in EVENT_LABELS);

    const steps = [
        ["/support.html", "내 정보 입력", profile ? "완료" : "나이, 지역, 상태 등", !!profile],
        ["/support.html", "지원사업 추천", matches.length ? `추천 결과 ${matches.length}건` : "내 조건에 맞는 사업 찾기", matches.length > 0],
        ["/calendar.html", "캘린더", events.length ? `일정 ${events.length}건` : "신청 마감일과 내 일정 관리", events.length > 0]
    ];
    document.getElementById("steps").append(...steps.map(([href, title, state, done], index) =>
        h("li", { class: done ? "done" : "" }, h("a", { href },
            h("span", { class: "num", text: done ? "✓" : String(index + 1) }),
            h("strong", { text: title }), h("span", { class: "state", text: state })))));

    const summary = document.getElementById("summary");
    if (!matches.length) {
        summary.append(h("p", { class: "muted", text: "아직 추천 결과가 없습니다." }),
            h("a", { class: "btn primary", href: "/support.html", text: "추천 받기" }));
    } else {
        summary.append(...matches.slice(0, 5).map(match => h("div", { class: "event" },
            h("span", { class: "badge " + match.verdict, text: VERDICT_LABELS[match.verdict] || match.verdict }),
            h("div", { class: "body" }, h("strong", { text: match.name }),
                h("div", { class: "small muted", text: `마감 ${dateLabel(match.applyEnd)}` })))),
            h("a", { class: "btn small", href: "/support.html", text: "추천 결과 보기", style: "margin-top:8px" }));
    }

    const upcoming = document.getElementById("upcoming");
    const today = todayIso();
    const next = events.filter(event => event.date && event.date >= today)
        .sort((a, b) => (a.date + (a.startTime || "")).localeCompare(b.date + (b.startTime || ""))).slice(0, 5);
    if (!next.length) {
        upcoming.append(h("p", { class: "muted", text: events.length ? "예정된 일정이 없습니다." : "추천 결과에서 마감일을 추가하면 여기에 표시됩니다." }));
    } else {
        upcoming.append(...next.map(event => h("div", { class: "event" },
            h("span", { class: "badge " + event.type, text: EVENT_LABELS[event.type] }),
            h("div", { class: "body" }, h("strong", { text: event.title }),
                h("div", { class: "small muted", text: dateLabel(event.date) + (event.startTime ? ` ${event.startTime}` : "") })))),
            h("a", { class: "btn small", href: "/calendar.html", text: "캘린더 열기", style: "margin-top:8px" }));
    }

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
