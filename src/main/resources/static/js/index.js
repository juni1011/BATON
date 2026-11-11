"use strict";

/** 담당 D: 홈 화면에 진행 상태, 계약 요약, 다가오는 일정을 보여 준다. */
document.addEventListener("DOMContentLoaded", () => {
    const { DAY_LABELS, EVENT_LABELS, h, won, dateLabel, todayIso } = EasytonUI;
    const contract = Easyton.getContractInfo();
    const report = Easyton.getCheckReport();
    const matches = Easyton.getSupportMatches();
    const events = Easyton.getCalendarEvents();

    document.getElementById("continue").hidden = !report;

    const issueCount = report ? (report.dangerCount || 0) + (report.warningCount || 0) + (report.missingCount || 0) : 0;
    const steps = [
        ["/upload.html", "계약서 업로드", contract ? "완료" : "텍스트 파일 또는 붙여넣기", !!contract],
        ["/confirm.html", "추출 값 확인", report ? "완료" : contract ? "값을 확인하고 점검하세요" : "읽은 값을 직접 고칠 수 있어요", !!report],
        ["/report.html", "점검 리포트", report ? `확인 필요 ${issueCount}건` : "위험·주의·누락 조항 안내", !!report],
        ["/support.html", "지원사업 추천", matches.length ? `추천 결과 ${matches.length}건` : "내 조건에 맞는 사업 찾기", matches.length > 0],
        ["/calendar.html", "캘린더", events.length ? `일정 ${events.length}건` : "근무일·급여일 한눈에", events.length > 0]
    ];
    document.getElementById("steps").append(...steps.map(([href, title, state, done], index) =>
        h("li", { class: done ? "done" : "" }, h("a", { href },
            h("span", { class: "num", text: done ? "✓" : String(index + 1) }),
            h("strong", { text: title }), h("span", { class: "state", text: state })))));

    const summary = document.getElementById("summary");
    if (!contract) {
        summary.append(h("p", { class: "muted", text: "아직 저장된 계약서가 없습니다." }),
            h("a", { class: "btn primary", href: "/upload.html", text: "계약서 업로드하기" }));
    } else {
        const days = (contract.workDays || []).map(day => DAY_LABELS[day]).join("·");
        const time = contract.startTime && contract.endTime ? `${contract.startTime} ~ ${contract.endTime}` : "";
        const rows = [
            ["사용자", contract.employerName],
            ["시급", contract.hourlyWage == null ? null : won(contract.hourlyWage)],
            ["근무", [days, time].filter(Boolean).join(" ") || null],
            ["기간", contract.contractStart ? `${dateLabel(contract.contractStart)} ~ ${contract.contractEnd ? dateLabel(contract.contractEnd) : "종료일 미기재"}` : null],
            ["예상 월급", report && report.estimatedMonthlyPay != null ? won(report.estimatedMonthlyPay) + " (세전)" : null]
        ];
        summary.append(h("dl", { class: "meta" }, rows.flatMap(([name, value]) =>
            [h("dt", { text: name }), h("dd", { text: value ?? "미기재" })])));
        if (report) {
            summary.append(h("div", { class: "row" },
                h("span", { class: "badge DANGER", text: `위험 ${report.dangerCount || 0}` }),
                h("span", { class: "badge WARNING", text: `주의 ${report.warningCount || 0}` }),
                h("span", { class: "badge MISSING", text: `누락 ${report.missingCount || 0}` })));
        }
        summary.append(h("a", { class: "btn small", href: report ? "/report.html" : "/confirm.html",
            text: report ? "리포트 보기" : "값 확인하고 점검하기" }));
    }

    const upcoming = document.getElementById("upcoming");
    const today = todayIso();
    const next = events.filter(event => event.date && event.date >= today)
        .sort((a, b) => (a.date + (a.startTime || "")).localeCompare(b.date + (b.startTime || ""))).slice(0, 5);
    if (!next.length) {
        upcoming.append(h("p", { class: "muted", text: events.length ? "예정된 일정이 없습니다." : "계약서를 점검하면 근무일과 급여일이 여기에 표시됩니다." }));
    } else {
        upcoming.append(...next.map(event => h("div", { class: "event" },
            h("span", { class: "badge " + event.type, text: EVENT_LABELS[event.type] || event.type }),
            h("div", { class: "body" }, h("strong", { text: event.title }),
                h("div", { class: "small muted", text: dateLabel(event.date) + (event.startTime ? ` ${event.startTime}` : "") })))),
            h("a", { class: "btn small", href: "/calendar.html", text: "캘린더 열기", style: "margin-top:8px" }));
    }

    document.getElementById("reset").addEventListener("click", () => {
        if (!confirm("이 브라우저에 저장된 계약 정보, 리포트, 일정, 프로필을 모두 지울까요?")) return;
        Easyton.saveContractInfo(null);
        Easyton.saveCheckReport(null);
        Easyton.saveCalendarEvents(null);
        Easyton.saveUserProfile(null);
        Easyton.saveSupportMatches(null);
        location.reload();
    });
});
