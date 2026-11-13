"use strict";

/** 저장된 일정을 월 달력으로 보여 주고 직접 일정 추가·삭제를 지원한다. */
document.addEventListener("DOMContentLoaded", () => {
    const { EVENT_LABELS, h, dateLabel, todayIso, notify } = EasytonUI;
    const grid = document.getElementById("grid");
    const message = document.getElementById("message");
    const addForm = document.getElementById("add-form");
    const field = id => document.getElementById(id);
    const pad = value => String(value).padStart(2, "0");
    const today = todayIso();
    const hidden = new Set();
    let messageTimer = null;
    let messageAnimation = null;

    let events = Easyton.getCalendarEvents().filter(event => event.type in EVENT_LABELS);
    // 오늘이 있는 달에 일정이 없으면 가장 가까운 다음 일정(없으면 첫 일정)의 달부터 보여 준다.
    const sortedDates = events.map(event => event.date).filter(Boolean).sort();
    let selected = today;
    if (sortedDates.length && !sortedDates.some(date => date.slice(0, 7) === today.slice(0, 7))) {
        selected = sortedDates.find(date => date >= today) || sortedDates[0];
    }
    const linkedDate = new URLSearchParams(location.search).get("date");
    if (linkedDate && /^[1-9]\d{3}-\d{2}-\d{2}$/.test(linkedDate)) {
        const date = new Date(linkedDate + "T00:00:00Z");
        if (Number.isFinite(date.getTime()) && date.toISOString().slice(0, 10) === linkedDate) {
            selected = linkedDate;
        }
    }
    let year = Number(selected.slice(0, 4));
    let month = Number(selected.slice(5, 7));

    function eventsOn(date) {
        return events.filter(event => event.date === date && !hidden.has(event.type))
            .sort((a, b) => (a.startTime || "").localeCompare(b.startTime || ""));
    }

    function renderFilters() {
        const used = Object.keys(EVENT_LABELS).filter(type => events.some(event => event.type === type));
        field("filters").replaceChildren(...used.map(type => h("label", {},
            h("input", { type: "checkbox", checked: !hidden.has(type), onchange: event => {
                if (event.currentTarget.checked) hidden.delete(type); else hidden.add(type);
                renderGrid();
                renderDay();
            } }),
            h("span", { class: "dot " + type, "aria-hidden": "true" }), EVENT_LABELS[type])));
    }

    function renderGrid() {
        field("month-title").textContent = `${year}년 ${month}월`;
        const lead = new Date(Date.UTC(year, month - 1, 1)).getUTCDay();
        const length = new Date(Date.UTC(year, month, 0)).getUTCDate();
        const cells = [];
        for (let i = 0; i < lead; i++) cells.push(h("div", { class: "day out", "aria-hidden": "true" }));
        for (let day = 1; day <= length; day++) {
            const date = `${year}-${pad(month)}-${pad(day)}`;
            const list = eventsOn(date);
            cells.push(h("button", {
                type: "button",
                class: "day" + (date === today ? " today" : "") + (date === selected ? " selected" : ""),
                "aria-label": `${month}월 ${day}일, 일정 ${list.length}건`,
                "aria-pressed": String(date === selected),
                onclick: () => {
                    selected = date;
                    renderGrid();
                    renderDay();
                    grid.querySelector(".day.selected").focus();
                }
            },
                h("span", { class: "date", text: String(day) }),
                list.slice(0, 3).map(event => h("span", { class: "chip " + event.type, text: event.title })),
                list.length > 3 && h("span", { class: "chip more", text: `+${list.length - 3}` })));
        }
        grid.replaceChildren(...cells);
    }

    function renderDay() {
        field("day-title").textContent = dateLabel(selected);
        field("date").value = selected;
        const list = eventsOn(selected);
        const box = field("day-events");
        if (!list.length) {
            box.replaceChildren(h("p", { class: "muted", text: "이 날은 일정이 없습니다." }));
            return;
        }
        box.replaceChildren(...list.map(event => h("div", { class: "event" },
            h("span", { class: "badge " + event.type, text: EVENT_LABELS[event.type] || event.type }),
            h("div", { class: "body" },
                h("strong", { text: event.title }),
                h("div", { class: "small muted", text: event.startTime
                    ? `${event.startTime}${event.endTime ? " ~ " + event.endTime : ""}` : "종일" }),
                event.description && h("div", { class: "small", text: event.description }),
                event.sourceLink && event.sourceLink.startsWith("/") && event.sourceLink !== "/calendar.html"
                    && h("a", { class: "small", href: event.sourceLink, text: "출처 보기" })),
            h("button", { type: "button", class: "btn small danger",
                "aria-label": `${event.title} 삭제`, onclick: () => remove(event.id) }, "삭제"))));
    }

    function remove(id) {
        events = events.filter(event => event.id !== id);
        Easyton.saveCalendarEvents(events);
        renderAll();
    }

    function renderAll() {
        renderFilters();
        renderGrid();
        renderDay();
    }

    function move(step) {
        month += step;
        if (month < 1) { month = 12; year--; }
        if (month > 12) { month = 1; year++; }
        renderGrid();
    }

    field("prev").addEventListener("click", () => move(-1));
    field("next").addEventListener("click", () => move(1));
    field("today").addEventListener("click", () => {
        selected = today;
        year = Number(today.slice(0, 4));
        month = Number(today.slice(5, 7));
        renderGrid();
        renderDay();
    });

    field("new-event").addEventListener("click", event => {
        event.preventDefault();
        field("title").focus();
    });

    const startHour = field("startHour");
    const endHour = field("endHour");
    for (const prefix of ["start", "end"]) {
        const hour = field(prefix + "Hour");
        const minute = field(prefix + "Minute");
        for (let value = 0; value < 24; value++) {
            hour.add(new Option(pad(value) + "시", pad(value)));
        }
        for (let value = 0; value < 60; value++) {
            minute.add(new Option(pad(value) + "분", pad(value)));
        }
        hour.addEventListener("change", () => {
            minute.disabled = hour.value === "";
            if (minute.disabled) minute.value = "00";
        });
    }

    function readTime(prefix) {
        const hour = field(prefix + "Hour").value;
        return hour === "" ? null : hour + ":" + field(prefix + "Minute").value;
    }

    addForm.addEventListener("submit", event => {
        event.preventDefault();
        clearTimeout(messageTimer);
        messageTimer = null;
        messageAnimation?.cancel();
        messageAnimation = null;
        const title = field("title").value.trim();
        const date = field("date").value;
        if (!title) {
            notify(message, "일정 제목을 입력해 주세요.", "error");
            field("title").focus();
            return;
        }
        const start = readTime("start");
        const end = readTime("end");
        if (end && !start) {
            notify(message, "종료 시간을 저장하려면 시작 시간도 입력해 주세요.", "error");
            startHour.focus();
            return;
        }
        const type = "CUSTOM";
        if (start && end && end <= start) {
            notify(message, "종료 시각은 시작 시각보다 늦어야 합니다.", "error");
            endHour.focus();
            return;
        }
        events.push({
            id: "custom-" + Date.now().toString(36) + Math.random().toString(36).slice(2, 6),
            title, type, date, startTime: start, endTime: end,
            description: field("description").value.trim() || null, sourceLink: "/calendar.html"
        });
        Easyton.saveCalendarEvents(events);
        notify(message, "일정을 저장했어요.");
        if (!window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
            messageAnimation = message.animate([
                { opacity: 0, offset: 0, easing: "ease-out" },
                { opacity: 1, offset: 0.1 },
                { opacity: 1, offset: 0.9, easing: "ease-in" },
                { opacity: 0, offset: 1 }
            ], { duration: 3000, fill: "both" });
        }
        messageTimer = setTimeout(() => {
            notify(message, "");
            messageAnimation?.cancel();
            messageAnimation = null;
            messageTimer = null;
        }, 3000);
        hidden.delete(type);
        selected = date;
        year = Number(date.slice(0, 4));
        month = Number(date.slice(5, 7));
        addForm.reset();
        field("startMinute").disabled = true;
        field("endMinute").disabled = true;
        renderAll();
    });

    renderAll();
});
