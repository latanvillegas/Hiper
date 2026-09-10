import { CurvePoint } from '../types';

/**
 * Monotone Cubic Hermite Spline (Fritsch-Carlson algorithm).
 * Exactly identical to CubicSplineInterpolator.kt for Android.
 * Evaluates arbitrary control points (up to 14) and computes a 256-element LUT.
 */
export function generateSplineLut256(inputPoints: CurvePoint[]): Float32Array {
  const lut = new Float32Array(256);
  if (!inputPoints || inputPoints.length === 0) {
    for (let i = 0; i < 256; i++) lut[i] = i / 255.0;
    return lut;
  }

  // Sort and clamp
  const pts = [...inputPoints].sort((a, b) => a.x - b.x);

  // Ensure endpoints at 0.0 and 1.0
  if (pts[0].x > 0.0) {
    pts.unshift({ x: 0.0, y: pts[0].y });
  }
  if (pts[pts.length - 1].x < 1.0) {
    pts.push({ x: 1.0, y: pts[pts.length - 1].y });
  }

  // Deduplicate points with identical or near-identical X
  const clean: CurvePoint[] = [];
  for (const p of pts) {
    if (clean.length === 0 || Math.abs(p.x - clean[clean.length - 1].x) >= 0.001) {
      clean.push({ x: Math.max(0, Math.min(1, p.x)), y: Math.max(0, Math.min(1, p.y)) });
    }
  }

  const n = clean.length;
  if (n === 1) {
    lut.fill(clean[0].y);
    return lut;
  }

  const x = clean.map((p) => p.x);
  const y = clean.map((p) => p.y);

  const delta = new Float32Array(n - 1);
  const m = new Float32Array(n);

  for (let i = 0; i < n - 1; i++) {
    const h = x[i + 1] - x[i];
    delta[i] = h !== 0 ? (y[i + 1] - y[i]) / h : 0;
  }

  m[0] = delta[0];
  for (let i = 1; i < n - 1; i++) {
    m[i] = (delta[i - 1] + delta[i]) * 0.5;
  }
  m[n - 1] = delta[n - 2];

  // Enforce monotonicity
  for (let i = 0; i < n - 1; i++) {
    if (delta[i] === 0) {
      m[i] = 0;
      m[i + 1] = 0;
    } else {
      const alpha = m[i] / delta[i];
      const beta = m[i + 1] / delta[i];
      const s = alpha * alpha + beta * beta;
      if (s > 9.0) {
        const tau = 3.0 / Math.sqrt(s);
        m[i] = tau * alpha * delta[i];
        m[i + 1] = tau * beta * delta[i];
      }
    }
  }

  // Interpolate across 256 discrete bins
  let segIndex = 0;
  for (let i = 0; i < 256; i++) {
    const tX = i / 255.0;
    while (segIndex < n - 2 && tX > x[segIndex + 1]) {
      segIndex++;
    }

    const h = x[segIndex + 1] - x[segIndex];
    if (h <= 0.00001) {
      lut[i] = y[segIndex];
      continue;
    }

    const t = (tX - x[segIndex]) / h;
    const t2 = t * t;
    const t3 = t2 * t;

    const h00 = 2 * t3 - 3 * t2 + 1;
    const h10 = t3 - 2 * t2 + t;
    const h01 = -2 * t3 + 3 * t2;
    const h11 = t3 - t2;

    const val = h00 * y[segIndex] + h10 * h * m[segIndex] + h01 * y[segIndex + 1] + h11 * h * m[segIndex + 1];
    lut[i] = Math.max(0.0, Math.min(1.0, val));
  }

  return lut;
}
