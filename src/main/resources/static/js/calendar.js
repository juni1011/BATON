"use strict";

/** 담당 D: 저장된 일정을 월 달력으로 보여 주고 직접 일정 추가·삭제를 지원한다. */
document.addEventListener("DOMContentLoaded", () => {
    const { EVENT_LABELS, USER_EVENT_TYPES, h, dateLabel, todayIso, errorText, notify } = EasytonUI;
    const grid = document.getElementById("grid");
    const message = document.getElementById("message");
    const addForm = document.getElementById("add-form");
    const regenerate = document.getElementById("regenerate");
    const field = id => document.getElementById(id);
    const pad = value => String(value).padStart(2, "0");
    const today = todayIso();
    const hidden = new Set();

    let events = Easyton.getCalendarEvents();
    // 오늘이 있는 달에 일정이 없으면 가장 가까운 다음 일정(없으면 첫 일정)의 달부터 보여 준다.
    const sortedDates = events.map(event => event.date).filter(Boolean).sort();
    let selected = today;
    if (sortedDates.length && !sortedDates.some(date => date.slice(0, 7) === today.slice(0, 7))) {
        selected = sortedDates.find(date => date >= today) || sortedDates[0];
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
                onclick: () => { selected = date; renderGrid(); renderDay(); }
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
            USER_EVENT_TYPES.includes(event.type) && h("button", { type: "button", class: "btn small danger",
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

    addForm.addEventListener("submit", event => {
        event.preventDefault();
        const title = field("title").value.trim();
        const date = field("date").value;
        if (!title || !date) return;
        const start = field("startTime").value || null;
        const end = field("endTime").value || null;
        if (start && end && end <= start) {
            notify(message, "종료 시각은 시작 시각보다 늦어야 합니다.", "error");
            return;
        }
        events.push({
            id: "custom-" + Date.now().toString(36) + Math.random().toString(36).slice(2, 6),
            title, type: field("type").value, date, startTime: start, endTime: start ? end : null,
            description: field("description").value.trim() || null, sourceLink: "/calendar.html"
        });
        Easyton.saveCalendarEvents(events);
        notify(message, "");
        selected = date;
        year = Number(date.slice(0, 4));
        month = Number(date.slice(5, 7));
        for (const id of ["title", "startTime", "endTime", "description"]) field(id).value = "";
        renderAll();
    });

    const contract = Easyton.getContractInfo();
    regenerate.hidden = !contract;
    regenerate.addEventListener("click", async () => {
        regenerate.disabled = true;
        try {
            const generated = await Easyton.postJson("/api/calendar/events", contract);
            events = EasytonUI.saveGeneratedEvents(generated);
            notify(message, generated.length
                ? `계약 일정 ${generated.length}건을 다시 만들었습니다.`
                : "계약 시작일이 없어 일정을 만들지 못했습니다. 추출 값 확인에서 시작일을 입력해 주세요.", generated.length ? "" : "warn");
            renderAll();
        } catch (error) {
            notify(message, errorText(error), "error");
        } finally {
            regenerate.disabled = false;
        }
    });

    if (!events.length) {
        notify(message, contract
            ? "아직 계약 일정이 없습니다. '계약 일정 다시 만들기'를 누르거나 직접 일정을 추가해 보세요."
            : "저장된 계약서가 없습니다. 계약서를 업로드하면 근무일과 급여일이 자동으로 표시됩니다. 직접 일정을 추가할 수도 있습니다.");
    }
    renderAll();
});
