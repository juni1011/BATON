import React from "react";

const COLORS = {
  burgundy: "#9B3F3F",
  mustard: "#C69A33",
  ivory: "#FFFCF4",
  soft: "#F6EBDD",
  line: "#D8C8B8",
};

const outline = {
  stroke: COLORS.burgundy,
  strokeWidth: 2.4,
  strokeLinecap: "round",
  strokeLinejoin: "round",
};

function IllustrationFrame({ kind, state, children }) {
  return (
    <svg
      className={`support-illustration support-illustration--${kind}`}
      viewBox="0 0 180 150"
      width="180"
      height="150"
      aria-hidden="true"
      focusable="false"
      data-illustration={state}
    >
      <circle cx="90" cy="74" r="63" fill={COLORS.ivory} />
      <circle cx="30" cy="47" r="4" fill={COLORS.mustard} opacity=".2" />
      <path d="M147 29v10m-5-5h10" stroke={COLORS.mustard} strokeWidth="2" strokeLinecap="round" opacity=".55" />
      <ellipse cx="90" cy="139" rx="61" ry="4" fill={COLORS.burgundy} opacity=".07" />
      <g className="support-illustration__scene">{children}</g>
    </svg>
  );
}

function Portrait({ variant = "neutral", children }) {
  const shirt = variant === "youth" ? COLORS.mustard : COLORS.soft;
  return (
    <g>
      {variant === "minor" && <rect x="47" y="88" width="19" height="40" rx="8" fill={COLORS.mustard} {...outline} />}
      <path d="M54 134v-13c0-23 15-37 36-37s36 14 36 37v13Z" fill={shirt} {...outline} />
      <path d="M77 84v9l13 9 13-9v-9" fill={COLORS.ivory} {...outline} />
      <path d="M69 103l-4 25m46-25 4 25" fill="none" stroke={COLORS.burgundy} strokeWidth="2" strokeLinecap="round" opacity=".5" />
      <ellipse cx="90" cy="58" rx="24" ry="28" fill={COLORS.ivory} {...outline} />
      <path d="M66 57c-5-22 9-34 25-34 18 0 29 13 24 32-6-1-10-9-12-15-8 9-21 12-37 11Z" fill={COLORS.burgundy} />
      {variant === "senior" && <path d="m69 43 3 9m37-10-3 10" stroke={COLORS.ivory} strokeWidth="4" strokeLinecap="round" />}
      <circle cx="81" cy="59" r="1.65" fill={COLORS.burgundy} />
      <circle cx="100" cy="59" r="1.65" fill={COLORS.burgundy} />
      <path d="M84 71q6 5 12 0" fill="none" {...outline} strokeWidth="1.8" />
      {variant === "senior" && (
        <g fill="none" {...outline} strokeWidth="1.8">
          <rect x="72" y="53" width="17" height="13" rx="5" />
          <rect x="93" y="53" width="17" height="13" rx="5" />
          <path d="M89 58h4m-21-1-5-2m43 2 4-2" />
        </g>
      )}
      {variant === "middle" && (
        <g fill="none" {...outline}>
          <path d="m68 113 19 11q4 2 8-1l17-10" />
          <path d="m75 120 14-4q4-1 7 2" />
        </g>
      )}
      {variant === "senior" && <path d="M68 116h44m-38 7h32" stroke={COLORS.mustard} strokeWidth="3" strokeLinecap="round" />}
      {children}
    </g>
  );
}

function AgeScene({ stage }) {
  if (stage === "minor") {
    return (
      <Portrait variant="minor">
        <g {...outline}>
          <path d="M63 103q14-4 27 4 13-8 27-4v30q-14-3-27 3-13-6-27-3Z" fill={COLORS.ivory} />
          <path d="M90 107v29" fill="none" />
          <path d="m70 113 13 2m-13 6 13 2m14-8 13-2m-13 10 13-2" stroke={COLORS.mustard} strokeWidth="1.8" />
        </g>
      </Portrait>
    );
  }
  if (stage === "youth") {
    return (
      <Portrait variant="youth">
        <path d="m111 106 15-13" fill="none" {...outline} strokeWidth="7" />
        <circle cx="130" cy="89" r="6" fill={COLORS.ivory} {...outline} />
        <path d="M139 110q13-11 10-26m-6 3 6-5 4 6" fill="none" {...outline} />
        <path d="m137 59 3 7 7 3-7 3-3 7-3-7-7-3 7-3Z" fill={COLORS.mustard} />
      </Portrait>
    );
  }
  if (stage === "middle") {
    return (
      <>
        <path d="M33 126q-3-19 7-32m-6 17q-12-1-11-12 11 0 11 12m3-9q-2-12 9-17 4 11-9 17" fill={COLORS.soft} stroke={COLORS.mustard} strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
        <Portrait variant="middle" />
      </>
    );
  }
  if (stage === "senior") return <Portrait variant="senior" />;
  return <Portrait />;
}

/** stage is supplied by the parent; this module does not infer age boundaries. */
export function AgeIllustration({ stage }) {
  const state = ["minor", "youth", "middle", "senior"].includes(stage) ? stage : "neutral";
  return <IllustrationFrame kind="age" state={state}><AgeScene stage={state} /></IllustrationFrame>;
}

const STATUS = {
  대학생: "university",
  구직자: "jobseeker",
  재직자: "employed",
  기타: "other",
  university: "university",
  jobseeker: "jobseeker",
  employed: "employed",
  other: "other",
};

function NeutralCard() {
  return (
    <g fill={COLORS.ivory} stroke={COLORS.line} strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="57" y="31" width="66" height="99" rx="10" />
      <circle cx="90" cy="58" r="12" />
      <path d="M74 87h32m-32 12h32m-32 12h21" />
    </g>
  );
}

export function StatusIllustration({ value }) {
  const state = STATUS[String(value ?? "").trim()] ?? "neutral";
  let scene = <NeutralCard />;
  if (state === "university") {
    scene = (
      <g {...outline}>
        <path d="M58 59v17q32 17 64 0V59" fill={COLORS.mustard} />
        <path d="m43 51 47-22 47 22-47 22Z" fill={COLORS.burgundy} />
        <path d="M137 52v32" fill="none" stroke={COLORS.mustard} />
        <circle cx="137" cy="88" r="4" fill={COLORS.mustard} stroke="none" />
        <path d="M48 91q21-6 42 4 21-10 42-4v38q-21-5-42 5-21-10-42-5Z" fill={COLORS.ivory} />
        <path d="M90 95v39" fill="none" />
        <path d="m57 103 24 3m-24 8 24 3m18-11 24-3m-24 14 24-3" fill="none" stroke={COLORS.mustard} strokeWidth="1.8" />
      </g>
    );
  } else if (state === "jobseeker") {
    scene = (
      <g {...outline}>
        <rect x="48" y="28" width="74" height="102" rx="10" fill={COLORS.ivory} />
        <circle cx="85" cy="54" r="11" fill={COLORS.soft} />
        <path d="M69 76q16-15 32 0m-38 13h40m-40 11h29m-29 11h24" fill="none" />
        <circle cx="124" cy="104" r="21" fill={COLORS.ivory} stroke={COLORS.mustard} strokeWidth="5" />
        <path d="m139 120 14 14" fill="none" strokeWidth="6" />
      </g>
    );
  } else if (state === "employed") {
    scene = (
      <g {...outline}>
        <path d="M69 62V47a7 7 0 0 1 7-7h28a7 7 0 0 1 7 7v15" fill="none" />
        <rect x="42" y="61" width="96" height="69" rx="10" fill={COLORS.soft} />
        <path d="M42 85q48 20 96 0" fill="none" />
        <rect x="82" y="86" width="16" height="17" rx="3" fill={COLORS.mustard} />
        <path d="M52 119h76" fill="none" stroke={COLORS.mustard} strokeWidth="2" />
      </g>
    );
  } else if (state === "other") {
    scene = (
      <g {...outline}>
        <g transform="rotate(-10 66 78)"><rect x="42" y="42" width="48" height="70" rx="8" fill={COLORS.ivory} /><circle cx="66" cy="67" r="10" fill={COLORS.mustard} stroke="none" /><path d="M56 90h20m-20 9h13" fill="none" /></g>
        <g transform="rotate(10 116 91)"><rect x="92" y="56" width="48" height="70" rx="8" fill={COLORS.ivory} /><path d="m116 72 11 11-11 11-11-11Z" fill={COLORS.soft} stroke={COLORS.mustard} /><path d="M106 108h20" fill="none" /></g>
        <path d="M53 127q33 18 68 4" fill="none" stroke={COLORS.mustard} strokeDasharray="3 6" />
      </g>
    );
  }
  return <IllustrationFrame kind="status" state={state}>{scene}</IllustrationFrame>;
}

const HOUSING = {
  월세: "rental",
  전세: "jeonse",
  자가: "own",
  "가족과 거주": "family",
  "기숙사·기타": "dorm",
  rental: "rental",
  jeonse: "jeonse",
  own: "own",
  family: "family",
  dorm: "dorm",
};

function House({ family = false, neutral = false }) {
  const stroke = neutral ? COLORS.line : COLORS.burgundy;
  return (
    <g fill={COLORS.ivory} stroke={stroke} strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round">
      <path d="M53 69v62h74V69" />
      <path d="m42 75 48-42 48 42-9 10-39-34-39 34Z" fill={neutral ? COLORS.ivory : COLORS.soft} />
      {family ? (
        <g>
          {[70, 90, 110].map((x) => <g key={x}><circle cx={x} cy="88" r="6" fill={COLORS.ivory} /><path d={`M${x - 8} 113v-10q0-10 8-10t8 10v10Z`} fill={x === 90 ? COLORS.soft : COLORS.ivory} /></g>)}
        </g>
      ) : (
        <><rect x="81" y="97" width="19" height="34" rx="3" /><rect x="64" y="80" width="15" height="14" rx="2" /><path d="M71.5 80v14m-7.5-7h15m14 27h1" /></>
      )}
      <path d="M43 133h94" fill="none" />
    </g>
  );
}

export function HousingIllustration({ value }) {
  const state = HOUSING[String(value ?? "").trim()] ?? "neutral";
  let scene = <House neutral />;
  if (state === "rental") {
    scene = <><House /><g {...outline}><rect x="116" y="86" width="39" height="46" rx="6" fill={COLORS.ivory} /><path d="M116 100h39m-29-19v11m18-11v11" fill="none" /><rect x="124" y="107" width="7" height="7" rx="1" fill={COLORS.mustard} stroke="none" /><path d="M138 110h8m-22 12h7m7 0h8" fill="none" /></g></>;
  } else if (state === "jeonse") {
    scene = <><House /><g {...outline}><circle cx="125" cy="102" r="12" fill={COLORS.mustard} /><circle cx="123" cy="100" r="3" fill={COLORS.ivory} stroke="none" /><path d="m134 111 20 20m-11-11-5 5m12-1-5 5" fill="none" strokeWidth="5" /></g></>;
  } else if (state === "own") {
    scene = <><House /><circle cx="133" cy="115" r="19" fill={COLORS.mustard} {...outline} /><path d="m124 115 6 6 12-13" fill="none" {...outline} strokeWidth="3.5" /></>;
  } else if (state === "family") {
    scene = <House family />;
  } else if (state === "dorm") {
    scene = <g {...outline}><rect x="51" y="38" width="78" height="95" rx="7" fill={COLORS.ivory} /><path d="M51 54h78" stroke={COLORS.mustard} strokeWidth="7" />{[65, 101].flatMap((x) => [69, 92].map((y) => <rect key={`${x}-${y}`} x={x} y={y} width="14" height="14" rx="2" fill={COLORS.soft} />))}<path d="M82 133v-23h16v23m-57 1h98" fill="none" /></g>;
  }
  return <IllustrationFrame kind="housing" state={state}>{scene}</IllustrationFrame>;
}

const INCOME_LEVEL = {
  "중위소득50%이하": 1,
  "중위소득100%이하": 2,
  "중위소득150%이하": 3,
  "중위소득150%초과": 4,
};
const BAR_HEIGHTS = [[26, 43, 61], [18, 28, 38], [28, 43, 58], [38, 58, 80], [48, 76, 100]];

export function IncomeIllustration({ value }) {
  const normalized = String(value ?? "").replace(/\s+/g, "");
  const level = INCOME_LEVEL[normalized] ?? 0;
  const unknown = normalized === "미확인";
  const heights = BAR_HEIGHTS[level];
  return (
    <IllustrationFrame kind="income" state={level ? `level-${level}` : unknown ? "unknown" : "neutral"}>
      <g fill="none" stroke={COLORS.line} strokeWidth="1.5" strokeLinecap="round">
        <path d="M36 29v100h111" />
        <path d="M42 53h102m-102 27h102m-102 27h102" opacity=".5" />
      </g>
      {heights.map((height, index) => (
        <rect
          key={index}
          className="support-illustration__bar"
          x={49 + index * 30}
          y={128 - height}
          width="21"
          height={height}
          rx="4"
          fill={level ? index === 0 ? COLORS.mustard : COLORS.burgundy : "none"}
          fillOpacity={level && index === 1 ? ".55" : "1"}
          stroke={level ? "none" : COLORS.line}
          strokeWidth="2"
          strokeDasharray={level ? undefined : "4 4"}
        />
      ))}
      {unknown && <g><circle cx="140" cy="41" r="15" fill={COLORS.ivory} stroke={COLORS.line} strokeWidth="2" />{[134, 140, 146].map((x) => <circle key={x} cx={x} cy="41" r="1.8" fill={COLORS.burgundy} />)}</g>}
    </IllustrationFrame>
  );
}
