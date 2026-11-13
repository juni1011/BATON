"use strict";

/** 홈 화면에 추천 결과 요약과 다가오는 일정을 보여 준다. */
document.addEventListener("DOMContentLoaded", () => {
    const { EVENT_LABELS, h, dateLabel, todayIso } = EasytonUI;
    const matches = Easyton.getSupportMatches();
    const events = Easyton.getCalendarEvents().filter(event => event.type in EVENT_LABELS);

    const summary = document.getElementById("summary");
    const sortControls = document.getElementById("program-sort");
    const today = todayIso();
    const openPrograms = matches.filter(match => !match.applyEnd || match.applyEnd >= today);

    function renderPrograms(sort) {
        const programs = sort === "deadline"
            ? openPrograms.filter(match => match.applyEnd).sort((a, b) => a.applyEnd.localeCompare(b.applyEnd))
            : [...openPrograms].sort((a, b) => (b.applyStart || "").localeCompare(a.applyStart || ""));
        summary.replaceChildren();
        if (!programs.length) {
            summary.append(h("p", { class: "muted", text: sort === "deadline"
                ? "마감일이 있는 공고가 없어요."
                : "지원사업 추천을 받으면 여기에서 신청 기간을 확인할 수 있어요." }));
            return;
        }
        summary.append(h("ul", { class: "home-program-list" }, programs.slice(0, 3).map(match =>
            h("li", { class: "event" },
                h("div", { class: "body" },
                    h("strong", { text: match.name }),
                    h("div", { class: "small muted", text: match.organization || "" })),
                h("span", { class: "small muted", text: match.applyEnd ? "마감 " + dateLabel(match.applyEnd) : "마감일 미정" })))));
    }

    document.getElementById("program-count").textContent = openPrograms.length.toLocaleString("ko-KR");
    for (const button of sortControls.querySelectorAll("button")) {
        button.addEventListener("click", () => {
            for (const option of sortControls.querySelectorAll("button")) {
                option.setAttribute("aria-pressed", String(option === button));
            }
            renderPrograms(button.dataset.sort);
        });
    }
    renderPrograms("recent");
    const upcoming = document.getElementById("upcoming");
    const next = events.filter(event => event.date && event.date >= today)
        .sort((a, b) => (a.date + (a.startTime || "")).localeCompare(b.date + (b.startTime || ""))).slice(0, 5);
    if (!next.length) {
        upcoming.append(h("p", { class: "muted", text: events.length ? "다가오는 일정이 없어요." : "캘린더에 일정을 추가하면 여기에 표시돼요." }));
    } else {
        upcoming.append(...next.map(event => h("div", { class: "event" },
            h("time", { class: "home-event-date", datetime: event.date, "aria-label": dateLabel(event.date),
                text: event.date.slice(5).replace("-", ".") }),
            h("div", { class: "body" },
                h("a", { href: "/calendar.html?date=" + encodeURIComponent(event.date), text: event.title }),
                h("div", { class: "small muted" },
                    h("span", { class: "badge " + event.type, text: EVENT_LABELS[event.type] }),
                    event.startTime || "종일")))));
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
