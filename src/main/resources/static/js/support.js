"use strict";

/** 프로필을 입력받아 가상 지원사업 매칭 결과를 보여 주고 마감일을 캘린더에 추가한다. */
document.addEventListener("DOMContentLoaded", () => {
    const { VERDICT_LABELS, h, dateLabel, errorText, notify } = EasytonUI;
    const form = document.getElementById("profile-form");
    const message = document.getElementById("message");
    const results = document.getElementById("results");
    const submit = document.getElementById("submit");
    const field = id => document.getElementById(id);
    const FIELDS = ["sido", "sigungu", "status", "housing", "householdIncomeLevel"];
    const SIDO = ["서울특별시", "부산광역시", "대구광역시", "인천광역시", "광주광역시", "대전광역시", "울산광역시",
        "세종특별자치시", "경기도", "강원특별자치도", "충청북도", "충청남도", "전북특별자치도", "전라남도",
        "경상북도", "경상남도", "제주특별자치도", "가상시"];

    field("sido-list").append(...SIDO.map(name => h("option", { value: name })));

    const profile = Easyton.getUserProfile();
    if (profile) {
        field("age").value = profile.age ?? "";
        for (const id of FIELDS) field(id).value = profile[id] ?? "";
    }

    function hasDeadline(match) {
        return Easyton.getCalendarEvents().some(event => event.id === "support-" + match.id);
    }

    function addDeadline(match, button) {
        const events = Easyton.getCalendarEvents().filter(event => event.id !== "support-" + match.id);
        events.push({
            id: "support-" + match.id, title: match.name + " 신청 마감", type: "SUPPORT_DEADLINE",
            date: match.applyEnd, startTime: null, endTime: null,
            description: match.organization, sourceLink: "/support.html"
        });
        Easyton.saveCalendarEvents(events);
        button.textContent = "캘린더에 추가됨";
        button.disabled = true;
    }

    function safeUrl(url) {
        return /^https?:\/\//.test(url || "") ? url : null;
    }

    function render(matches) {
        results.replaceChildren();
        if (!matches.length) return;
        results.append(h("h2", { text: `추천 결과 ${matches.length}건` }));
        for (const match of matches) {
            const added = hasDeadline(match);
            const url = safeUrl(match.applyUrl);
            results.append(h("article", { class: "card stack" },
                h("div", { class: "row" },
                    h("span", { class: "badge " + match.verdict, text: VERDICT_LABELS[match.verdict] || match.verdict }),
                    h("h3", { text: match.name, style: "margin:0" })),
                h("p", { text: match.reason, style: "margin-bottom:0" }),
                h("dl", { class: "meta" },
                    h("dt", { text: "기관" }), h("dd", { text: match.organization || "-" }),
                    h("dt", { text: "신청 기간" }), h("dd", { text: `${dateLabel(match.applyStart)} ~ ${dateLabel(match.applyEnd)}` }),
                    h("dt", { text: "필요 서류" }), h("dd", { text: (match.requiredDocuments || []).join(", ") || "-" })),
                match.verdict !== "NOT_ELIGIBLE" && h("div", { class: "row" },
                    match.applyEnd && h("button", { type: "button", class: "btn small", disabled: added,
                        onclick: event => addDeadline(match, event.currentTarget) }, added ? "캘린더에 추가됨" : "마감일을 캘린더에 추가"),
                    url && h("a", { class: "btn small", href: url, target: "_blank", rel: "noopener noreferrer", text: "신청 페이지 (예시 링크)" }))));
        }
    }

    render(Easyton.getSupportMatches());

    form.addEventListener("submit", async event => {
        event.preventDefault();
        const next = { age: field("age").value === "" ? null : Math.round(Number(field("age").value)) };
        for (const id of FIELDS) next[id] = field(id).value.trim() || null;
        submit.disabled = true;
        try {
            Easyton.saveUserProfile(next);
            const matches = await Easyton.postJson("/api/supports/match", { profile: next });
            Easyton.saveSupportMatches(matches);
            notify(message, "");
            render(matches);
            results.scrollIntoView({ behavior: "smooth", block: "start" });
        } catch (error) {
            notify(message, errorText(error), "error");
        } finally {
            submit.disabled = false;
        }
    });
});
