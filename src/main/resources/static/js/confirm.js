"use strict";

/** 담당 A: 추출 값을 고치고 저장한다. 비워 둔 값은 추정하지 않고 null로 보낸다. */
document.addEventListener("DOMContentLoaded", () => {
    const { DAYS, DAY_LABELS, h, errorText, notify } = EasytonUI;
    const form = document.getElementById("confirm-form");
    const message = document.getElementById("message");
    const origin = document.getElementById("origin");
    const submit = document.getElementById("submit");
    const times = {
        startTime: EasytonUI.contractTime("startTime"),
        endTime: EasytonUI.contractTime("endTime")
    };
    const field = id => times[id] || document.getElementById(id);

    const TEXT = ["employerName", "workplace", "jobDescription", "startTime", "endTime", "contractStart", "contractEnd"];
    const INTEGER = ["hourlyWage", "breakMinutes", "payDay", "probationMonths", "probationWageRate"];
    const BOOLEAN = ["weeklyHolidayPayIncluded", "probation", "socialInsuranceMentioned"];

    const dayBox = field("workDays");
    for (const day of DAYS) {
        dayBox.append(h("label", {}, h("input", { type: "checkbox", value: day }), DAY_LABELS[day]));
    }

    /** 값이 비어 있는(미기재) 입력을 점선으로 표시한다. */
    function markEmpty() {
        for (const id of [...TEXT, ...INTEGER, ...BOOLEAN, "weeklyHours", "clauses"]) {
            document.getElementById(id).classList.toggle("is-null", field(id).value === "");
        }
    }

    let workTimes;

    function fill(contract) {
        workTimes = contract.workTimes;
        if (workTimes) {
            dayBox.after(h("p", { class: "hint", text: DAYS.filter(day => workTimes[day]).map(day => {
                const time = workTimes[day];
                return DAY_LABELS[day] + " " + (time.startTime ?? "미입력") + "–" + (time.endTime ?? "미입력");
            }).join(" · ") }));
            for (const id of ["startTime", "endTime"]) document.getElementById(id).parentElement.hidden = true;
        }
        for (const id of [...TEXT, ...INTEGER, "weeklyHours"]) field(id).value = contract[id] ?? "";
        for (const id of BOOLEAN) field(id).value = contract[id] === null || contract[id] === undefined ? "" : String(contract[id]);
        for (const box of dayBox.querySelectorAll("input")) box.checked = (contract.workDays || []).includes(box.value);
        field("clauses").value = (contract.clauses || []).join("\n");
        markEmpty();
    }

    /** 폼에서 18개 키를 모두 가진 ContractInfo를 만든다. */
    function read() {
        const contract = {};
        for (const id of TEXT) contract[id] = field(id).value.trim() || null;
        for (const id of INTEGER) contract[id] = field(id).value === "" ? null : Math.round(Number(field(id).value));
        contract.weeklyHours = field("weeklyHours").value === "" ? null : Number(field("weeklyHours").value);
        for (const id of BOOLEAN) contract[id] = field(id).value === "" ? null : field(id).value === "true";
        const days = [...dayBox.querySelectorAll("input:checked")].map(box => box.value);
        contract.workDays = days.length ? days : null;
        if (workTimes) contract.workTimes = Object.fromEntries(days.map(day => [day, workTimes[day] ?? {}]));
        const clauses = field("clauses").value.split("\n").map(line => line.trim()).filter(Boolean);
        contract.clauses = clauses.length ? clauses : null;
        return contract;
    }

    function validate(contract) {
        for (const id of INTEGER) {
            if (contract[id] !== null && (!Number.isFinite(contract[id]) || contract[id] < 0)) return "숫자 항목을 확인해 주세요.";
        }
        if (contract.weeklyHours !== null && (!Number.isFinite(contract.weeklyHours) || contract.weeklyHours < 0)) {
            return "주 소정근로시간을 확인해 주세요.";
        }
        if (contract.payDay !== null && (contract.payDay < 1 || contract.payDay > 31)) return "임금 지급일은 1~31 사이로 입력해 주세요.";
        if (contract.contractStart && contract.contractEnd && contract.contractEnd < contract.contractStart) {
            return "계약 종료일이 시작일보다 빠릅니다.";
        }
        return null;
    }

    const saved = Easyton.getContractInfo();
    if (saved) {
        fill(saved);
        if (new URLSearchParams(location.search).get("from") === "upload") {
            const found = Object.values(saved).filter(value => value !== null).length;
            notify(origin, found === 0
                ? "계약서에서 읽어 낸 항목이 없습니다. 아래에 직접 입력해 주세요."
                : `18개 항목 중 ${found}개를 읽었습니다. 점선으로 표시된 칸은 계약서에서 찾지 못한 항목입니다.`,
                found === 0 ? "warn" : "");
        }
    } else {
        markEmpty();
        notify(origin, "저장된 계약서가 없습니다. 계약서를 보면서 직접 입력할 수 있습니다.");
    }

    form.addEventListener("input", markEmpty);
    form.addEventListener("submit", async event => {
        event.preventDefault();
        const contract = read();
        if (!Object.values(contract).some(value => value !== null)) {
            notify(message, "점검할 계약 내용을 하나 이상 입력해 주세요.", "error");
            field("employerName").focus();
            return;
        }
        const problem = validate(contract);
        if (problem) {
            notify(message, problem, "error");
            return;
        }
        submit.disabled = true;
        submit.textContent = "점검하는 중…";
        try {
            await EasytonUI.analyze(contract);
            Easyton.saveContractInfo(contract);
            location.href = "/report.html";
        } catch (error) {
            const detail = /^HTTP 404\b/.test(error.message)
                ? "현재 서버에 계약서 점검 기능이 연결되어 있지 않아요. 입력한 내용은 이 화면에 남아 있어요."
                : errorText(error);
            notify(message, detail, "error");
            submit.disabled = false;
            submit.textContent = "저장하고 점검하기";
        }
    });
});
