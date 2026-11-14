// Projected timing observed in rUpbvji8cK0, not a measurement of real cadence.
// Repeating near-foot contacts: ~0.167, 0.625, 1.083 s (±0.042 s/frame).
// Camera tracking, perspective, blur and the edited clip limit angle precision.
export const DEX_REFERENCE = Object.freeze({
    sourceUrl: 'https://www.youtube.com/watch?v=rUpbvji8cK0',
    cycleMs: 460,
    contactRatio: .20,
    neutralLogoLean: 22.83,
    armPhaseLead: .10,
    // These are logo adaptations of the observed rhythm, not motion capture.
    lean: [[0, 14], [.18, 16], [.36, 14], [.5, 14], [.68, 16], [.86, 14]],
    pelvis: [[0, -15], [.10, -13], [.29, -17], [.43, -16], [.5, -15], [.60, -13], [.79, -17], [.93, -16]],
    // Upper arm angle from screen-down; positive is forward. The opposing
    // arm uses half a cycle of offset, so it advances with the other knee.
    armSwing: [[0, -48], [.17, -12], [.34, 38], [.48, 26], [.65, -22], [.82, -65]],
    elbowFlex: [[0, 67], [.17, 88], [.34, 111], [.48, 103], [.65, 82], [.82, 60]],
    // Swing phase follows the brief support phase. Heel rises behind the hip,
    // then lowers before the thigh advances, preserving head/knee clearance.
    footX: [[0, -1], [.17, -1.82], [.35, -1.34], [.53, .43], [.73, 1.76], [.90, 1.45], [1, 1]],
    footLift: [[0, 0], [.17, 25], [.35, 56], [.53, 33], [.73, 16], [.90, 4], [1, 0]]
});

// Shape-preserving cubic Hermite interpolation. Tangents vanish at extrema;
// periodic channels share the same value and derivative across the loop seam.
export function sampleMotion(keys, phase, { periodic = true, startSlope, endSlope } = {}) {
    const p = periodic ? ((phase % 1) + 1) % 1 : Math.max(0, Math.min(1, phase));
    const points = periodic ? [
        [keys.at(-1)[0] - 1, keys.at(-1)[1]], ...keys,
        [keys[0][0] + 1, keys[0][1]], [keys[1][0] + 1, keys[1][1]]
    ] : keys;
    const secant = i => (points[i + 1][1] - points[i][1]) / (points[i + 1][0] - points[i][0]);
    const tangent = i => {
        if (i === 0) return startSlope ?? secant(0);
        if (i === points.length - 1) return endSlope ?? secant(i - 1);
        const before = secant(i - 1), after = secant(i);
        if (before * after <= 0) return 0;
        const h0 = points[i][0] - points[i - 1][0], h1 = points[i + 1][0] - points[i][0];
        const w0 = 2 * h1 + h0, w1 = h1 + 2 * h0;
        return (w0 + w1) / (w0 / before + w1 / after);
    };
    let i = 0;
    while (i < points.length - 2 && p > points[i + 1][0]) i++;
    const [x0, y0] = points[i], [x1, y1] = points[i + 1], h = x1 - x0;
    const t = (p - x0) / h, t2 = t * t, t3 = t2 * t;
    return (2 * t3 - 3 * t2 + 1) * y0 + (t3 - 2 * t2 + t) * h * tangent(i)
        + (-2 * t3 + 3 * t2) * y1 + (t3 - t2) * h * tangent(i + 1);
}
