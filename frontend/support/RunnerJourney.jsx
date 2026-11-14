import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react';
import { DEX_REFERENCE, sampleMotion } from './dex-running-reference.js';

const BURGUNDY = '#9B3F3F';
const GOLD = '#C69A33';
export const ENTRY_DURATION = 760;
export const MIN_RUN_DURATION = 4200;
export const JUMP_DURATION = 640;
export const CELEBRATION_DURATION = 2000;
export const RUN_CYCLE_DURATION = DEX_REFERENCE.cycleMs;
export const GROUND_CONTACT_RATIO = DEX_REFERENCE.contactRatio;
export const TRACK_CARD_DURATION = 500;
const clamp = (value, min = 0, max = 1) => Math.max(min, Math.min(max, value));
const mix = (a, b, amount) => a + (b - a) * amount;
const smooth = value => { const p = clamp(value); return p * p * (3 - 2 * p); };
const pointMix = (a, b, amount) => ({ x: mix(a.x, b.x, amount), y: mix(a.y, b.y, amount) });

export function RunnerMark({ className = '', ...props }) {
    return <svg viewBox="0 0 440 440" className={className} aria-hidden="true" focusable="false" {...props}>
        <g transform="translate(9 90)">
            <g fill={GOLD}>
                <path d="M0 236C35 178 65 152 121 152H157L146 171H121C84 171 61 184 37 218H76L64 236Z" />
                <path d="M35 205C45 191 58 185 74 185H140L129 200H106L97 210H32Z" />
                <path d="M94 209H132L119 233H79Z" />
            </g>
            <g fill={BURGUNDY}>
                <path d="M79 238H171C178 238 180 234 182 226L185 220H313L340 253H68Z" />
                <path d="M196 42C202 36 211 36 219 38L273 48C284 50 290 56 298 68H397C405 68 409 72 405 80L393 100C390 106 386 108 379 108H284L278 115L366 171C375 177 380 179 390 181L414 187C424 189 425 194 419 202L382 250C376 258 366 259 358 251L252 145L196 178L165 221L169 228C172 232 170 235 166 235H135C130 235 129 232 132 227L172 161C174 157 178 154 182 150L217 119C223 114 227 107 231 99L241 78L210 70L180 99C174 106 164 105 159 99C153 94 154 86 160 80Z" />
                <circle cx="308" cy="29" r="28" />
            </g>
        </g>
    </svg>;
}

export function getJourneyPhase(time, finishAt = null) {
    if (finishAt !== null && time >= finishAt + JUMP_DURATION) return { id: 'celebrate', title: '완주!!!' };
    if (time < ENTRY_DURATION) return { id: 'start', title: '출발!' };
    return { id: finishAt !== null ? 'jump' : 'run', title: '힘차게 달려나가는 중', dots: '.'.repeat(1 + Math.floor((time - ENTRY_DURATION) / 400) % 3) };
}

export function createJourneyLayout(width, height) {
    const w = Math.max(1, width);
    const h = Math.max(1, height);
    const mobile = w < 720;
    const cardWidth = mobile ? 154 : 172;
    const cardHeight = mobile ? 82 : 94;
    const cardPitch = cardWidth + (mobile ? 22 : 30);
    return { width: w, height: h, mobile, cardWidth, cardHeight, cardPitch,
        trackY: Math.max(230, h * .58), scale: mobile ? .92 : 1.16 };
}

// One full set always follows the previous one, including beyond both clipped edges.
export function getTrackCards(time, layout, finishAt = null, reduced = false) {
    const landing = finishAt === null ? Infinity : finishAt + JUMP_DURATION;
    const elapsed = Math.max(0, Math.min(time, landing) - ENTRY_DURATION);
    const distance = reduced ? 0 : elapsed * layout.cardPitch / TRACK_CARD_DURATION;
    const offset = distance % (layout.cardPitch * 5);
    const first = Math.floor((offset - layout.width / 2) / layout.cardPitch) - 1;
    const last = Math.ceil((offset + layout.width / 2) / layout.cardPitch) + 1;
    return Array.from({ length: last - first + 1 }, (_, i) => {
        const slot = first + i;
        return { slot, index: ((slot % 5) + 5) % 5,
            x: layout.width / 2 + slot * layout.cardPitch - offset };
    });
}

export function getStrideSpan(layout) {
    return layout.cardPitch / TRACK_CARD_DURATION * RUN_CYCLE_DURATION * GROUND_CONTACT_RATIO / layout.scale;
}

export function getJourneyPosition(time, layout, origin, finishAt = null, reduced = false) {
    const center = { x: layout.width / 2, y: layout.trackY };
    if (reduced) return { ...center, scale: 1, jump: 0 };
    if (time < ENTRY_DURATION) {
        const progress = smooth(time / ENTRY_DURATION);
        const source = { x: layout.width * clamp(origin?.x ?? .8, .03, .97),
            y: layout.height * clamp(origin?.y ?? .5, .15, .85) + 34 };
        const point = pointMix(source, center, progress);
        return { ...point, y: point.y - Math.sin(Math.PI * progress) * 42,
            scale: mix(.42, 1, progress), jump: 0 };
    }
    const jumpTime = finishAt === null ? -1 : time - finishAt;
    const airborne = jumpTime >= 120 && jumpTime < JUMP_DURATION;
    const jump = airborne ? Math.sin(Math.PI * (jumpTime - 120) / (JUMP_DURATION - 120)) ** 2 * (layout.mobile ? 43 : 56) : 0;
    return { ...center, y: center.y - jump, scale: 1, jump };
}

// Pivots sit inside the torso, so rotating limbs always retain a filled overlap.
// The bind pose and widths keep the original logo's broad, angular proportions.
const LOGO_SEGMENTS = {
    backArm: { root: { x: -3, y: -83 }, joint: { x: -30.82, y: -90.62 }, end: { x: -46.46, y: -74.52 }, widths: [13.5, 12.5, 10.5] },
    frontArm: { root: { x: 5, y: -76 }, joint: { x: 32.2, y: -75.9 }, end: { x: 55.66, y: -75.9 }, widths: [18.4, 17, 13.5] },
    backLeg: { root: { x: -12, y: -60 }, joint: { x: -39.56, y: -38.41 }, end: { x: -55.66, y: -9.2 }, widths: [15, 12, 8] },
    frontLeg: { root: { x: -2, y: -61 }, joint: { x: 23.92, y: -34.96 }, end: { x: 47.84, y: -1.38 }, widths: [22, 18, 11] }
};
const PELVIS = { x: -7, y: -60 };
function bodyPoint(point, shift, leanDegrees) {
    const angle = leanDegrees * Math.PI / 180;
    const x = point.x - PELVIS.x, y = point.y - PELVIS.y;
    return { x: PELVIS.x + x * Math.cos(angle) - y * Math.sin(angle),
        y: PELVIS.y + x * Math.sin(angle) + y * Math.cos(angle) + shift };
}
const transformLimb = (pose, shift, leanDegrees) => ({
    root: bodyPoint(pose.root, shift, leanDegrees),
    joint: bodyPoint(pose.joint, shift, leanDegrees),
    end: bodyPoint(pose.end, shift, leanDegrees)
});
const normal = (a, b) => {
    const length = Math.max(.001, Math.hypot(b.x - a.x, b.y - a.y));
    return { x: -(b.y - a.y) / length, y: (b.x - a.x) / length };
};
function polygonPath(points) {
    const area = points.reduce((sum, p, i) => {
        const next = points[(i + 1) % points.length];
        return sum + p.x * next.y - next.x * p.y;
    }, 0);
    // Identical winding lets overlapping subpaths form a union without holes.
    const contour = area < 0 ? [...points].reverse() : points;
    return contour.map((p, i) => `${i ? 'L' : 'M'}${p.x} ${p.y}`).join('') + 'Z';
}
function beveledSegment(start, end, startWidth, endWidth) {
    const n = normal(start, end), u = { x: n.y, y: -n.x };
    const overlap = 3;
    const a = { x: start.x - u.x * overlap, y: start.y - u.y * overlap };
    const b = { x: end.x + u.x * overlap, y: end.y + u.y * overlap };
    const point = (p, along, across) => ({ x: p.x + u.x * along + n.x * across, y: p.y + u.y * along + n.y * across });
    const w1 = startWidth / 2, w2 = endWidth / 2, cut = 1.5;
    return polygonPath([
        point(a, cut, w1), point(b, -cut, w2), point(b, 0, w2 - cut), point(b, 0, -w2 + cut),
        point(b, -cut, -w2), point(a, cut, -w1), point(a, 0, -w1 + cut), point(a, 0, w1 - cut)
    ]);
}
function jointCap(center, radius, rounded = true) {
    if (rounded) {
        const x = center.x, y = center.y, k = radius * .552284749831;
        return `M${x + radius} ${y}C${x + radius} ${y + k} ${x + k} ${y + radius} ${x} ${y + radius}`
            + `C${x - k} ${y + radius} ${x - radius} ${y + k} ${x - radius} ${y}`
            + `C${x - radius} ${y - k} ${x - k} ${y - radius} ${x} ${y - radius}`
            + `C${x + k} ${y - radius} ${x + radius} ${y - k} ${x + radius} ${y}Z`;
    }
    const corner = radius * .41421356237;
    return polygonPath([
        [radius, corner], [corner, radius], [-corner, radius], [-radius, corner],
        [-radius, -corner], [-corner, -radius], [corner, -radius], [radius, -corner]
    ].map(([x, y]) => ({ x: center.x + x, y: center.y + y })));
}
function footPath(pose, front) {
    const ground = front ? -4 : -1;
    const lift = Math.max(0, ground - pose.end.y);
    const angle = clamp((pose.end.x - pose.joint.x) * .7, -18, 25)
        * smooth(lift / 12) * Math.PI / 180;
    const outline = front ? [[-7, -4], [6, -4], [15, 2], [12, 5], [-8, 5]] : [[-6, -3], [5, -3], [12, 2], [10, 4], [-7, 4]];
    return polygonPath(outline.map(([x, y]) => ({
        x: pose.end.x + Math.cos(angle) * x - Math.sin(angle) * y,
        y: pose.end.y + Math.sin(angle) * x + Math.cos(angle) * y
    })));
}
export function getRunnerGeometry({ limbs, shift, leanDegrees = 0 }) {
    const p = (x, y) => { const point = bodyPoint({ x, y }, shift, leanDegrees); return `${point.x} ${point.y}`; };
    const torso = `M${p(1.38, -94.3)}C${p(6.44, -93.38)} ${p(9.2, -90.62)} ${p(12.88, -85.1)}`
        + `L${p(6.44, -66.7)}L${p(3.68, -63.48)}L${p(-8.28, -49.68)}L${p(-24.38, -61.64)}`
        + `C${p(-21.62, -63.94)} ${p(-19.78, -67.16)} ${p(-17.94, -70.84)}L${p(-13.34, -80.5)}Z`;
    const parts = Object.fromEntries(Object.entries(limbs).map(([name, pose]) => {
        const widths = LOGO_SEGMENTS[name].widths;
        return [name, {
            upper: beveledSegment(pose.root, pose.joint, widths[0], widths[1]),
            lower: beveledSegment(pose.joint, pose.end, widths[1], widths[2]),
            root: jointCap(pose.root, widths[0] / 2),
            joint: jointCap(pose.joint, widths[1] / 2),
            end: jointCap(pose.end, widths[2] / 2, false),
            ...(name.endsWith('Leg') ? { foot: footPath(pose, name.startsWith('front')) } : {})
        }];
    }));
    // A single fill is rasterized once: no antialiased seams between body pieces.
    return { torso, parts, body: torso + Object.values(parts).flatMap(part => Object.values(part)).join('') };
}
function solveKnee(root, foot) {
    const upper = 35, lower = 42;
    const dx = foot.x - root.x, dy = foot.y - root.y;
    const distance = Math.max(.01, Math.hypot(dx, dy));
    const reach = Math.min(distance, upper + lower - .2);
    const ux = dx / distance, uy = dy / distance;
    const along = (upper * upper - lower * lower + reach * reach) / (2 * reach);
    const bend = Math.sqrt(Math.max(0, upper * upper - along * along));
    return { root, joint: { x: root.x + ux * along + uy * bend, y: root.y + uy * along - ux * bend },
        end: { x: root.x + ux * reach, y: root.y + uy * reach } };
}
function armPose(root, swing, flex, upper, lower) {
    const radians = Math.PI / 180;
    const a = swing * radians, b = (swing + flex) * radians;
    const joint = { x: root.x + Math.sin(a) * upper, y: root.y + Math.cos(a) * upper };
    return { root, joint, end: { x: joint.x + Math.sin(b) * lower, y: joint.y + Math.cos(b) * lower } };
}
function poseMix(a, b, p) {
    const blendBone = (a1, a2, b1, b2, root) => {
        const angleA = Math.atan2(a2.y - a1.y, a2.x - a1.x);
        const angleB = Math.atan2(b2.y - b1.y, b2.x - b1.x);
        const turn = Math.atan2(Math.sin(angleB - angleA), Math.cos(angleB - angleA));
        const angle = angleA + turn * p;
        const length = mix(Math.hypot(a2.x - a1.x, a2.y - a1.y), Math.hypot(b2.x - b1.x, b2.y - b1.y), p);
        return { x: root.x + Math.cos(angle) * length, y: root.y + Math.sin(angle) * length };
    };
    const root = pointMix(a.root, b.root, p);
    const joint = blendBone(a.root, a.joint, b.root, b.joint, root);
    return { root, joint, end: blendBone(a.joint, a.end, b.joint, b.end, joint) };
}

export function getRunnerPose(time, finishAt = null, reduced = false, strideSpan = 36) {
    if (reduced) return {
        limbs: Object.fromEntries(Object.entries(LOGO_SEGMENTS).map(([name, part]) => [name, {
            root: part.root, joint: part.joint, end: part.end
        }])),
        shift: 0, leanDegrees: 0, head: { x: 17.48, y: -103.04 }
    };
    const jumpTime = finishAt === null ? -1 : time - finishAt;
    const deceleration = clamp(jumpTime / 160);
    const gaitTime = jumpTime < 0 ? time : finishAt + 160 * (deceleration - deceleration ** 3 + .5 * deceleration ** 4);
    const cycle = Math.max(0, gaitTime - ENTRY_DURATION) / RUN_CYCLE_DURATION;
    const sprintWeight = reduced ? 0 : smooth((time - ENTRY_DURATION) / 180) * (jumpTime < 0 ? 1 : 1 - smooth(jumpTime / 220));
    // The logo already leans ~23°. Apply the difference from the observed
    // projected trunk angle, rather than adding another full forward lean.
    const leanDegrees = sprintWeight * (sampleMotion(DEX_REFERENCE.lean, cycle) - DEX_REFERENCE.neutralLogoLean);
    const anticipation = jumpTime >= 0 && jumpTime < 120 ? Math.sin(Math.PI * jumpTime / 120) ** 2 * 10 : 0;
    const impact = jumpTime >= JUMP_DURATION ? Math.sin(Math.PI * clamp((jumpTime - JUMP_DURATION) / 230)) ** 2 * 9 : 0;
    const shift = sprintWeight * sampleMotion(DEX_REFERENCE.pelvis, cycle) + anticipation + impact;
    const footAt = (offset, ground, center) => {
        const beat = (cycle + offset) % 1;
        const contact = GROUND_CONTACT_RATIO;
        const front = strideSpan / 2 + center, back = -strideSpan / 2 + center;
        if (beat < contact) return { x: mix(front, back, beat / contact), y: ground };
        const p = (beat - contact) / (1 - contact);
        const half = strideSpan / 2;
        // Endpoint tangents exactly match the support-foot/belt velocity.
        const tangent = -2 * (1 - contact) / contact;
        return { x: center + half * sampleMotion(DEX_REFERENCE.footX, p, { periodic: false, startSlope: tangent, endSlope: tangent }),
            y: ground - sampleMotion(DEX_REFERENCE.footLift, p, { periodic: false, startSlope: 0, endSlope: 0 }) };
    };
    const flight = jumpTime < 120 || jumpTime >= JUMP_DURATION ? 0 : Math.sin(Math.PI * (jumpTime - 120) / (JUMP_DURATION - 120)) ** 2;
    const transition = jumpTime < 0 ? 0 : smooth(jumpTime / 160);
    const limbs = {};
    for (const [name, part] of Object.entries(LOGO_SEGMENTS)) {
        const root = bodyPoint(part.root, shift, leanDegrees);
        const bind = transformLimb(part, shift, leanDegrees);
        const isFront = name.startsWith('front');
        let gait, jump;
        if (name.endsWith('Leg')) {
            const ground = isFront ? -4 : -1;
            // Each foot sweeps about its own hip. Sharing the front hip's
            // center overextended the rear leg just before touchdown.
            const foot = footAt(isFront ? 0 : .5, ground, isFront ? -3 : -12);
            gait = solveKnee(root, foot);
            jump = solveKnee(root, { x: isFront ? 21 + flight * 5 : -24 - flight * 5, y: ground - flight * 9 });
        } else {
            const armPhase = cycle + DEX_REFERENCE.armPhaseLead + (isFront ? 0 : .5);
            // bodyPoint rotates SVG clockwise, so compensate before applying
            // the common body transform to preserve the measured screen angle.
            const swing = sampleMotion(DEX_REFERENCE.armSwing, armPhase) + leanDegrees;
            const elbow = sampleMotion(DEX_REFERENCE.elbowFlex, armPhase);
            gait = transformLimb(armPose(part.root, swing, elbow, isFront ? 24 : 25, 24), shift, leanDegrees);
            jump = transformLimb(armPose(part.root, (isFront ? 20 : -25) + flight * (isFront ? 70 : -65),
                isFront ? 55 : 75, isFront ? 24 : 25, 24), shift, leanDegrees);
        }
        const entryBlend = reduced ? 0 : smooth((time - ENTRY_DURATION) / 180);
        const moving = poseMix(bind, gait, entryBlend);
        limbs[name] = poseMix(moving, jump, transition);
    }
    return { limbs, shift, leanDegrees, head: bodyPoint({ x: 17.48, y: -103.04 }, shift, leanDegrees) };
}

export function RunnerFigure({ time, position, scale, finishAt, reduced, strideSpan }) {
    const pose = getRunnerPose(time, finishAt, reduced, strideSpan);
    const geometry = getRunnerGeometry(pose);
    return <g data-runner="true" transform={`translate(${position.x} ${position.y}) scale(${scale * position.scale})`} fill={BURGUNDY}>
        <path data-runner-body="true" d={geometry.body} fillRule="nonzero" />
        <circle data-runner-head="true" cx={pose.head.x} cy={pose.head.y} r="12.88" />
    </g>;
}

function wrap(value, maxLength, maxLines = 2) {
    const text = String(value ?? '').trim();
    const lines = [];
    let remaining = text;
    while (remaining && lines.length < maxLines) {
        if (remaining.length <= maxLength) { lines.push(remaining); break; }
        let end = remaining.lastIndexOf(' ', maxLength);
        if (end < maxLength / 2) end = maxLength;
        lines.push(remaining.slice(0, end).trim());
        remaining = remaining.slice(end).trim();
        if (lines.length === maxLines) lines[maxLines - 1] = `${lines[maxLines - 1].slice(0, Math.max(1, maxLength - 1))}…`;
    }
    return lines;
}

function TrackCard({ step, x, layout }) {
    const w = layout.cardWidth;
    const h = layout.cardHeight;
    const value = wrap(step.value, Math.floor((w - 24) / 12), 2);
    return <g data-track-card={step.label} transform={`translate(${x} ${layout.trackY + 13})`}>
        <rect x={-w / 2} y="7" width={w} height={h} rx="13" fill="#E9D8B7" />
        <rect x={-w / 2} width={w} height={h} rx="13" fill="#FFFAE8" stroke={GOLD} strokeWidth="1.5" />
        <path d={`M${-w / 2 + 12} 0H${w / 2 - 12}`} stroke={GOLD} strokeWidth="3" strokeLinecap="round" />
        <text x={-w / 2 + 14} y="23" className="rj-card-label">{step.label}</text>
        <text x={-w / 2 + 14} y="47" className="rj-card-value">
            {value.map((line, i) => <tspan key={i} x={-w / 2 + 14} dy={i ? 18 : 0}>{line}</tspan>)}
        </text>
    </g>;
}

// Seeded values keep the celebration stable through renders and resizes.
const CONFETTI = Array.from({ length: 100 }, (_, i) => ({
    x: ((i * 47 + 13) % 101) / 100, y: ((i * 31 + 7) % 97) / 96,
    angle: i * 2.39996, speed: 48 + i % 7 * 15, delay: i % 5 * 55,
    color: [BURGUNDY, GOLD, '#D77965', '#8DAD97', '#E4BB65'][i % 5]
}));
function Confetti({ elapsed, layout, reduced }) {
    if (elapsed < 0 || elapsed >= CELEBRATION_DURATION) return null;
    return <g data-confetti="true">{CONFETTI.map((piece, i) => {
        const t = clamp((elapsed - piece.delay) / 1750);
        const distance = reduced ? 0 : t * piece.speed;
        const x = piece.x * layout.width + Math.cos(piece.angle) * distance;
        const y = piece.y * layout.height + Math.sin(piece.angle) * distance + (reduced ? 0 : 85 * t * t);
        const opacity = reduced ? .65 : clamp((elapsed - piece.delay) / 100) * clamp((2000 - elapsed) / 350);
        return <g key={i} opacity={opacity} transform={`translate(${x} ${y}) rotate(${i * 29 + (reduced ? 0 : t * 300)})`}>
            {i % 4 === 0 ? <path d="M-4 0H4M0-4V4" stroke={piece.color} strokeWidth="2" strokeLinecap="round" />
                : <rect x="-3" y="-2" width={i % 2 ? 7 : 4} height="4" rx="1" fill={piece.color} />}
        </g>;
    })}</g>;
}

export function RunnerJourney({ steps = [], onComplete, origin, ready = false }) {
    const hostRef = useRef(null);
    const frameRef = useRef(null);
    const completeRef = useRef(false);
    const callbackRef = useRef(onComplete);
    const readyRef = useRef(ready);
    const originRef = useRef(origin);
    const skipRef = useRef(false);
    const [size, setSize] = useState({ width: 960, height: 560 });
    const [clock, setClock] = useState({ time: 0, finishAt: null, reduced: false });
    const [skipped, setSkipped] = useState(false);
    callbackRef.current = onComplete;
    readyRef.current = ready;
    const finish = useCallback(() => {
        if (completeRef.current) return;
        completeRef.current = true;
        cancelAnimationFrame(frameRef.current);
        frameRef.current = null;
        callbackRef.current?.();
    }, []);
    const skip = useCallback(() => {
        skipRef.current = true;
        setSkipped(true);
        if (readyRef.current) finish();
    }, [finish]);

    useLayoutEffect(() => {
        const host = hostRef.current;
        if (!host) return;
        const measure = () => {
            const { width, height } = host.getBoundingClientRect();
            if (width > 0 && height > 0) setSize(current => Math.abs(current.width - width) > .5 || Math.abs(current.height - height) > .5 ? { width, height } : current);
        };
        measure();
        const observer = typeof ResizeObserver === 'function' ? new ResizeObserver(measure) : null;
        observer?.observe(host);
        if (!observer) window.addEventListener('resize', measure);
        return () => { observer?.disconnect(); window.removeEventListener('resize', measure); };
    }, []);

    useEffect(() => {
        const preference = window.matchMedia('(prefers-reduced-motion: reduce)');
        let start = null;
        let finishAt = null;
        let disposed = false;
        const tick = now => {
            if (disposed || completeRef.current) return;
            if (start === null) start = now;
            const time = now - start;
            if (skipRef.current && readyRef.current) { finish(); return; }
            const minimum = preference.matches ? ENTRY_DURATION : ENTRY_DURATION + MIN_RUN_DURATION;
            if (readyRef.current && finishAt === null && time >= minimum) finishAt = time;
            setClock({ time, finishAt, reduced: preference.matches || skipRef.current });
            if (finishAt !== null && time >= finishAt + JUMP_DURATION + CELEBRATION_DURATION) { finish(); return; }
            frameRef.current = requestAnimationFrame(tick);
        };
        frameRef.current = requestAnimationFrame(tick);
        return () => { disposed = true; cancelAnimationFrame(frameRef.current); frameRef.current = null; };
    }, [finish]);

    const { time, finishAt, reduced } = clock;
    const inputSteps = Array.from({ length: 5 }, (_, i) => ({
        label: String(steps[i]?.label ?? `입력 ${i + 1}`),
        value: steps[i]?.value === '' || steps[i]?.value == null ? '선택 안 함' : String(steps[i].value)
    }));
    const layout = createJourneyLayout(size.width, size.height);
    const position = getJourneyPosition(time, layout, originRef.current, finishAt, reduced);
    const phase = getJourneyPhase(time, finishAt);
    const celebrationTime = finishAt === null ? -1 : time - finishAt - JUMP_DURATION;
    const cards = getTrackCards(time, layout, finishAt, reduced);
    return <div className="runner-journey" ref={hostRef} tabIndex={-1} role="group" aria-label="첫걸음 달리기 장면" data-runner-phase={phase.id}>
        <div className="rj-heading">
            <h2><span role="status" aria-live="polite" aria-atomic="true">{phase.title}</span>{phase.dots && <span className="rj-loading-dots" aria-hidden="true">{phase.dots}</span>}</h2>
        </div>
        <svg className="rj-scene" viewBox={`0 0 ${layout.width} ${layout.height}`} preserveAspectRatio="none" aria-hidden="true" focusable="false">
            <path d={`M-30 ${layout.trackY + 59}H${layout.width + 30}`} stroke="#F0E8D5" strokeWidth="108" />
            <path d={`M0 ${layout.trackY + 59}H${layout.width}`} stroke="#D8BB73" strokeWidth="1.5" strokeDasharray="4 10" />
            <g data-track="true">{cards.map(card => <TrackCard key={card.slot} step={inputSteps[card.index]} x={card.x} layout={layout} />)}</g>
            <ellipse cx={layout.width / 2} cy={layout.trackY + 5} rx={27 * layout.scale - position.jump * .15} ry="5" fill={BURGUNDY} opacity=".12" />
            <RunnerFigure time={time} position={position} scale={layout.scale} finishAt={finishAt} reduced={reduced} strideSpan={getStrideSpan(layout)} />
            <Confetti elapsed={celebrationTime} layout={layout} reduced={reduced} />
        </svg>
        {celebrationTime < 0 && !skipped && <button type="button" className="rj-skip" onClick={skip}>동작 건너뛰기 <span aria-hidden="true">↗</span></button>}
        <ol className="rj-sr-only" aria-label="선택한 다섯 가지 조건">{inputSteps.map((step, i) => <li key={i}>{step.label}: {step.value}</li>)}</ol>
    </div>;
}

export default RunnerJourney;
