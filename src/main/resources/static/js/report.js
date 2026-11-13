"use strict";

/** 담당 B: 점검 리포트를 수준별로 보여 주고 사장님께 물어볼 질문을 모아 준다. */
document.addEventListener("DOMContentLoaded", async () => {
    const { LEVEL_LABELS, h, won, errorText, notify } = EasytonUI;
    const message = document.getElementById("message");
    const contract = Easyton.getContractInfo();
    let report = Easyton.getCheckReport();

    if (!contract && !report) {
        document.getElementById("empty").hidden = false;
        return;
    }
    if (!report) {
        try {
            report = await EasytonUI.analyze(contract);
        } catch (error) {
            notify(message, errorText(error), "error");
            return;
        }
    }

    const items = report.items || [];
    const issues = ["DANGER", "MISSING", "WARNING"].flatMap(level => items.filter(item => item.level === level));
    const priorityQuestions = issues.filter(item => item.questionForEmployer).slice(0, 3);
    if (priorityQuestions.length) {
        document.getElementById("priority-questions").append(...priorityQuestions.map(item =>
            h("li", {}, h("strong", { text: item.title }), h("p", { text: item.questionForEmployer }))));
        document.getElementById("before-signing").hidden = false;
    }
    if (contract && contract.employerName) {
        document.getElementById("subtitle").textContent = `${contract.employerName} 계약서에서 확인이 필요한 부분을 정리했습니다.`;
    }

    document.getElementById("stats").append(
        ...[["DANGER", report.dangerCount], ["WARNING", report.warningCount], ["MISSING", report.missingCount]]
            .map(([level, count]) => h("div", { class: "stat " + level }, h("b", { text: `${count ?? 0}건` }), LEVEL_LABELS[level])),
        h("div", { class: "stat pay" }, h("b", { text: won(report.estimatedMonthlyPay) }), "예상 월급 (세전)"));
    document.getElementById("pay-note").textContent = report.estimatedMonthlyPay === null || report.estimatedMonthlyPay === undefined
        ? "시급 또는 주 근로시간이 없어 예상 월급을 계산하지 못했습니다."
        : "예상 월급은 시급 × 주 근로시간(주 15시간 이상이면 주휴수당 포함) × 월 평균 4.345주로 계산한 세전 금액입니다.";

    const list = document.getElementById("items");
    if (issues.length === 0) {
        list.append(h("div", { class: "notice" }, "점검 규칙에서 문제로 잡힌 항목이 없습니다. 그래도 서명 전에 계약서 원문을 한 번 더 읽어 보세요."));
    }
    const card = item => h("article", { class: "card item stack " + item.level },
        h("div", { class: "row" }, h("span", { class: "badge " + item.level, text: LEVEL_LABELS[item.level] || item.level }), h("h3", { text: item.title })),
        item.originalText && h("blockquote", { text: item.originalText }),
        h("p", { text: item.reason, style: "margin-bottom:0" }),
        item.legalBasis && h("p", { class: "muted small", text: "근거: " + item.legalBasis, style: "margin:4px 0 0" }),
        item.questionForEmployer && h("div", { class: "question" }, h("strong", { text: "이렇게 물어보세요  " }), item.questionForEmployer));
    list.append(...issues.map(card));

    const okItems = items.filter(item => item.level === "OK");
    if (okItems.length) {
        list.append(h("details", { class: "card" },
            h("summary", { text: `정상으로 확인된 항목 ${okItems.length}건`, style: "cursor:pointer;font-weight:700" }),
            h("div", { style: "margin-top:16px" }, okItems.map(card))));
    }

    const questions = issues.map(item => item.questionForEmployer).filter(Boolean);
    const copy = document.getElementById("copy");
    copy.hidden = questions.length === 0;
    copy.addEventListener("click", async () => {
        const text = questions.map((question, index) => `${index + 1}. ${question}`).join("\n");
        try {
            await navigator.clipboard.writeText(text);
            notify(message, `질문 ${questions.length}개를 복사했습니다.`);
        } catch {
            notify(message, "복사 권한이 없어 아래 질문을 직접 복사해 주세요.\n" + text, "warn");
            message.style.whiteSpace = "pre-wrap";
        }
    });

    document.getElementById("actions").hidden = false;
    document.getElementById("report").hidden = false;
});
