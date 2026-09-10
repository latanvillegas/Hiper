import {
  CurvePoint,
  HslChannelName,
  HslChannelSetting,
  LevelsSettings,
  OpticalEffectsSettings,
  PerspectiveSettings,
  FaceRetouchSettings,
  VirtualMakeupSettings,
  BeautyExtraSettings,
  HistogramBins,
  BasicAdjustments,
  ColorGradingSettings,
} from '../types';
import { generateSplineLut256 } from './spline';

function hexToRgb01(hex?: string, fallback: [number, number, number] = [0.9, 0.8, 0.7]): [number, number, number] {
  if (!hex) return fallback;
  const clean = hex.replace('#', '');
  if (clean.length === 3) {
    return [
      parseInt(clean[0] + clean[0], 16) / 255,
      parseInt(clean[1] + clean[1], 16) / 255,
      parseInt(clean[2] + clean[2], 16) / 255,
    ];
  }
  if (clean.length === 6) {
    return [
      parseInt(clean.substring(0, 2), 16) / 255,
      parseInt(clean.substring(2, 4), 16) / 255,
      parseInt(clean.substring(4, 6), 16) / 255,
    ];
  }
  return fallback;
}

export class WebGLEngine {
  private gl: WebGL2RenderingContext | null = null;
  private canvas: HTMLCanvasElement;
  private program: WebGLProgram | null = null;

  // Textures
  private sourceTexture: WebGLTexture | null = null;
  private curveLutTexture: WebGLTexture | null = null;

  // Uniform locations
  private uSourceLoc: WebGLUniformLocation | null = null;
  private uCurveLutLoc: WebGLUniformLocation | null = null;
  private uResolutionLoc: WebGLUniformLocation | null = null;
  private uPerspectiveLoc: WebGLUniformLocation | null = null;
  private uLevelsLoc: WebGLUniformLocation | null = null;
  private uHslChannelsLoc: WebGLUniformLocation | null = null;
  private uEffectsLoc: WebGLUniformLocation | null = null;
  private uFaceRetouchLoc: WebGLUniformLocation | null = null;
  private uFaceWarpLoc: WebGLUniformLocation | null = null;
  private uFaceFeaturesLoc: WebGLUniformLocation | null = null;
  private uMakeupLoc: WebGLUniformLocation | null = null;
  private uMakeupColors1Loc: WebGLUniformLocation | null = null;
  private uMakeupColors2Loc: WebGLUniformLocation | null = null;
  private uMakeupColors3Loc: WebGLUniformLocation | null = null;
  private uExtraBeautyLoc: WebGLUniformLocation | null = null;
  private uSeedLoc: WebGLUniformLocation | null = null;

  private quadVao: WebGLVertexArrayObject | null = null;
  private imageWidth = 1;
  private imageHeight = 1;

  constructor(canvas: HTMLCanvasElement) {
    this.canvas = canvas;
    this.initGL();
  }

  private initGL() {
    const gl = this.canvas.getContext('webgl2', {
      alpha: false,
      antialias: false,
      premultipliedAlpha: false,
      preserveDrawingBuffer: true,
    });

    if (!gl) {
      console.error('WebGL 2 is not supported on this browser.');
      return;
    }
    this.gl = gl;

    const vsSource = `#version 300 es
      precision highp float;
      layout(location = 0) in vec2 aPosition;
      out vec2 vUv;
      void main() {
        vUv = (aPosition + 1.0) * 0.5;
        gl_Position = vec4(aPosition, 0.0, 1.0);
      }
    `;

    const fsSource = `#version 300 es
      precision highp float;
      precision highp int;
      in vec2 vUv;
      out vec4 fragColor;

      uniform sampler2D uSource;
      uniform sampler2D uCurveLut; // 256x1 RGBA
      uniform vec2 uResolution;
      uniform vec4 uPerspective;   // vKeystone, hKeystone, rotationRad, scale
      uniform vec4 uLevelsInput;   // inBlack, inGamma, inWhite, 0
      uniform vec4 uLevelsOutput;  // outBlack, outWhite, 0, 0

      // Hsl 8 channels: R, Y, G, C, B, M, Shadows, Highlights
      // Each has vec3(hueShift/360.0, satMod, lumMod)
      uniform vec3 uHsl[8];

      // Effects: halationIntensity, halationThresh, bloomIntensity, bloomThresh
      uniform vec4 uEffects1;
      // Effects: grainAmount, grainSize, lensBlurRadius, specularBoost
      uniform vec4 uEffects2;

      // Basic adjustments
      uniform vec4 uAdjustments1; // exposureEV, contrast, temperature, tint
      uniform vec4 uAdjustments2; // highlights, shadows, whites, blacks
      uniform vec4 uAdjustments3; // clarity, dehaze, vibrance, saturation

      // 3-Way Color Grading: (hue/360.0, sat, lum, 0)
      uniform vec4 uColorGradingShadows;
      uniform vec4 uColorGradingMidtones;
      uniform vec4 uColorGradingHighlights;
      uniform float uShowClipping; // 0.0 or 1.0

      // Face retouch: spatialSigma, rangeSigma, texturePreserve, jawSlim
      uniform vec4 uFaceRetouch;
      uniform vec4 uFaceWarp; // cheekNarrow, chinSharpen, noseSlim, eyeEnlarge
      uniform vec4 uFaceFeatures; // teethWhiten, eyeBrighten, irisEnhance, oilControl
      uniform vec4 uMakeup; // foundation, blush, lipstick, gloss
      uniform vec4 uMakeupColors1; // blushR, blushG, blushB, contourIntensity
      uniform vec4 uMakeupColors2; // lipR, lipG, lipB, highlighterIntensity
      uniform vec4 uMakeupColors3; // foundR, foundG, foundB, frecklesIntensity
      uniform vec4 uExtraBeauty; // tanIntensity, blemishRemoval, darkCircles, bodySlimming
      uniform float uSeed;

      #define PI 3.14159265359

      vec3 rgb2hsl(vec3 c) {
        float maxC = max(c.r, max(c.g, c.b));
        float minC = min(c.r, min(c.g, c.b));
        float delta = maxC - minC;
        float l = (maxC + minC) * 0.5;
        float h = 0.0;
        float s = 0.0;

        if (delta > 0.00001) {
          s = l <= 0.5 ? delta / (maxC + minC) : delta / (2.0 - maxC - minC);
          if (maxC == c.r) {
            h = mod((c.g - c.b) / delta, 6.0);
          } else if (maxC == c.g) {
            h = ((c.b - c.r) / delta) + 2.0;
          } else {
            h = ((c.r - c.g) / delta) + 4.0;
          }
          h *= 60.0;
          if (h < 0.0) h += 360.0;
        }
        return vec3(h, s, l);
      }

      vec3 hsl2rgb(vec3 hsl) {
        float h = hsl.x;
        float s = hsl.y;
        float l = hsl.z;
        float c = (1.0 - abs(2.0 * l - 1.0)) * s;
        float x = c * (1.0 - abs(mod(h / 60.0, 2.0) - 1.0));
        float m = l - c * 0.5;

        vec3 rgbP;
        int sector = int(h / 60.0);
        if (sector == 0) rgbP = vec3(c, x, 0.0);
        else if (sector == 1) rgbP = vec3(x, c, 0.0);
        else if (sector == 2) rgbP = vec3(0.0, c, x);
        else if (sector == 3) rgbP = vec3(0.0, x, c);
        else if (sector == 4) rgbP = vec3(x, 0.0, c);
        else rgbP = vec3(c, 0.0, x);

        return clamp(rgbP + vec3(m), 0.0, 1.0);
      }

      float hash21(vec2 p, float s) {
        vec3 p3 = fract(vec3(p.xyx) * 0.1031);
        p3 += dot(p3, p3.yzx + 33.33 + s);
        return fract((p3.x + p3.y) * p3.z) * 2.0 - 1.0;
      }

      void main() {
        vec2 uv = vUv;

        // 1. Perspective Transform & Keystone
        vec2 center = vec2(0.5);
        vec2 p = uv - center;

        float vTilt = uPerspective.x;
        float hTilt = uPerspective.y;
        float rot = uPerspective.z;
        float scale = uPerspective.w;

        // Rotation
        float cosR = cos(rot);
        float sinR = sin(rot);
        mat2 rotMat = mat2(cosR, -sinR, sinR, cosR);
        p = rotMat * p;

        // Keystoning
        float vFactor = 1.0 - p.y * vTilt;
        float hFactor = 1.0 - p.x * hTilt;
        p.x /= (vFactor * scale);
        p.y /= (hFactor * scale);

        vec2 sampleUv = p + center;
        if (sampleUv.x < 0.0 || sampleUv.x > 1.0 || sampleUv.y < 0.0 || sampleUv.y > 1.0) {
          fragColor = vec4(0.05, 0.05, 0.07, 1.0);
          return;
        }

        // 2. Anatomical Warp Mesh Deformation (Jaw, Cheeks, Chin, Nose, Eyes, Neck)
        float jawSlim = uFaceRetouch.w;
        float cheekNarrow = uFaceWarp.x;
        float chinSharpen = uFaceWarp.y;
        float noseSlim = uFaceWarp.z;
        float eyeEnlarge = uFaceWarp.w;

        vec2 faceCenter = vec2(0.5, 0.46);
        vec2 toFace = sampleUv - faceCenter;
        float distToFace = length(toFace);

        // Jawline & Face Slimming
        if (jawSlim > 0.01 && distToFace < 0.38 && sampleUv.y > 0.36) {
          float push = (1.0 - distToFace / 0.38) * jawSlim * 0.07;
          sampleUv.x += toFace.x * push;
        }

        // Cheek Narrowing
        if (cheekNarrow > 0.01) {
          float dx = abs(sampleUv.x - 0.5);
          if (dx > 0.10 && dx < 0.32 && sampleUv.y > 0.38 && sampleUv.y < 0.58) {
            float cPush = cheekNarrow * 0.035 * (sampleUv.x > 0.5 ? 1.0 : -1.0);
            sampleUv.x += cPush;
          }
        }

        // Chin Sharpening
        if (chinSharpen > 0.01 && sampleUv.y < 0.38 && distToFace < 0.22) {
          float chPush = (1.0 - (0.38 - sampleUv.y) / 0.15) * chinSharpen * 0.04 * (sampleUv.x > 0.5 ? 1.0 : -1.0);
          sampleUv.x += chPush;
        }

        // Nose Slimming
        if (noseSlim > 0.01 && abs(sampleUv.x - 0.5) < 0.09 && sampleUv.y > 0.42 && sampleUv.y < 0.60) {
          float nPush = noseSlim * 0.03 * (sampleUv.x > 0.5 ? 1.0 : -1.0);
          sampleUv.x += nPush;
        }

        // Eye Enlargement
        if (eyeEnlarge > 0.01) {
          vec2 leftEyePos = vec2(0.36, 0.60);
          vec2 rightEyePos = vec2(0.64, 0.60);
          float dL = length(sampleUv - leftEyePos);
          float dR = length(sampleUv - rightEyePos);
          if (dL < 0.095) {
            sampleUv -= (sampleUv - leftEyePos) * (1.0 - dL / 0.095) * eyeEnlarge * 0.22;
          } else if (dR < 0.095) {
            sampleUv -= (sampleUv - rightEyePos) * (1.0 - dR / 0.095) * eyeEnlarge * 0.22;
          }
        }

        // 3. Texture Sample & Bilateral Smoothing (Skin Pore Texture Preservation)
        vec4 baseSample = texture(uSource, sampleUv);
        vec3 color = baseSample.rgb;

        float spatialSigma = uFaceRetouch.x;
        float rangeSigma = uFaceRetouch.y;
        float texturePreserve = uFaceRetouch.z;
        float blemishes = uExtraBeauty.y;

        if (spatialSigma > 0.5 || blemishes > 0.01) {
          // Fast bilateral kernel (8-tap multi-radius)
          vec3 sumCol = color;
          float sumW = 1.0;
          float centerLuma = dot(color, vec3(0.299, 0.587, 0.114));

          vec2 px = 1.0 / uResolution;
          float radius = max(1.0, min(spatialSigma + blemishes * 5.0, 9.0));
          float rDiag = radius * 0.7071;

          #define SAMPLE_BILATERAL_TAP(offX, offY) { \
            vec3 tap = texture(uSource, sampleUv + vec2(offX, offY) * px).rgb; \
            float tapLuma = dot(tap, vec3(0.299, 0.587, 0.114)); \
            float diff = tapLuma - centerLuma; \
            float w = exp(-(diff * diff) / (2.0 * rangeSigma * rangeSigma + 0.0001)); \
            sumCol += tap * w; \
            sumW += w; \
          }

          SAMPLE_BILATERAL_TAP(radius, 0.0)
          SAMPLE_BILATERAL_TAP(-radius, 0.0)
          SAMPLE_BILATERAL_TAP(0.0, radius)
          SAMPLE_BILATERAL_TAP(0.0, -radius)
          SAMPLE_BILATERAL_TAP(rDiag, rDiag)
          SAMPLE_BILATERAL_TAP(-rDiag, rDiag)
          SAMPLE_BILATERAL_TAP(rDiag, -rDiag)
          SAMPLE_BILATERAL_TAP(-rDiag, -rDiag)
          #undef SAMPLE_BILATERAL_TAP

          vec3 smoothCol = sumCol / sumW;
          vec3 highPass = color - smoothCol;
          // High-pass pores preserved according to texturePreserve slider
          float effPreserve = max(0.1, texturePreserve * (1.0 - blemishes * 0.5));
          color = smoothCol + highPass * effPreserve;
        }

        // 4. Skin Oil Control (Matte finish on shiny sebum zones)
        float oilControl = uFaceFeatures.w;
        if (oilControl > 0.01) {
          float pixLuma = dot(color, vec3(0.299, 0.587, 0.114));
          // Target hot specular zones in the face
          if (pixLuma > 0.70 && distToFace < 0.40) {
            float shineAtten = smoothstep(0.70, 0.95, pixLuma) * oilControl * 0.55;
            color = mix(color, color * 0.82, shineAtten);
          }
        }

        // 5. Dark Circles & Eye Bags Reduction
        float darkCircles = uExtraBeauty.z;
        if (darkCircles > 0.01) {
          vec2 lUnderEye = vec2(0.36, 0.54);
          vec2 rUnderEye = vec2(0.64, 0.54);
          float dLU = length((sampleUv - lUnderEye) * vec2(1.0, 1.8));
          float dRU = length((sampleUv - rUnderEye) * vec2(1.0, 1.8));
          float dUnder = min(dLU, dRU);
          if (dUnder < 0.065) {
            float uMask = smoothstep(0.065, 0.02, dUnder) * darkCircles * 0.45;
            color = mix(color, color * 1.22 + vec3(0.03, 0.025, 0.02), uMask);
          }
        }

        // 6. Teeth Whitening
        float teethWhiten = uFaceFeatures.x;
        if (teethWhiten > 0.01) {
          vec2 mouthCenter = vec2(0.5, 0.355);
          float dMouth = length((sampleUv - mouthCenter) * vec2(1.0, 2.2));
          if (dMouth < 0.048) {
            float wM = smoothstep(0.048, 0.015, dMouth) * teethWhiten;
            float mPixLuma = dot(color, vec3(0.299, 0.587, 0.114));
            vec3 targetTooth = mix(color, vec3(mPixLuma * 1.25 + 0.10), 0.70);
            color = mix(color, targetTooth, wM * 0.85);
          }
        }

        // 7. Eye Brightening (Sclera Whitening) & Iris Enhancement + Catchlight
        float eyeBrighten = uFaceFeatures.y;
        float irisEnhance = uFaceFeatures.z;
        if (eyeBrighten > 0.01 || irisEnhance > 0.01) {
          vec2 lEye = vec2(0.36, 0.60);
          vec2 rEye = vec2(0.64, 0.60);
          float dL = length(sampleUv - lEye);
          float dR = length(sampleUv - rEye);
          float dEye = min(dL, dR);
          if (dEye < 0.065) {
            if (dEye > 0.022 && eyeBrighten > 0.01) {
              // Sclera whitening
              float sMask = smoothstep(0.065, 0.025, dEye) * eyeBrighten;
              float eLuma = dot(color, vec3(0.299, 0.587, 0.114));
              color = mix(color, vec3(eLuma * 1.15 + 0.06), sMask * 0.55);
            } else if (dEye <= 0.022 && irisEnhance > 0.01) {
              // Iris enhancement
              color = mix(color, color * 1.28, irisEnhance * 0.45);
              // Artificial catchlight reflection
              vec2 eyeRefCenter = dL < dR ? lEye : rEye;
              float dCatch = length(sampleUv - (eyeRefCenter + vec2(0.006, 0.006)));
              if (dCatch < 0.0035) {
                color = mix(color, vec3(1.0), irisEnhance * 0.90);
              }
            }
          }
        }

        // 8. Virtual Makeup: Foundation & Tone Unification
        float foundation = uMakeup.x;
        if (foundation > 0.01 && distToFace < 0.45) {
          vec3 customTone = uMakeupColors3.rgb;
          float fMask = (1.0 - smoothstep(0.30, 0.45, distToFace)) * foundation * 0.35;
          // Blend with skin luminance preservation
          float baseLuma = dot(color, vec3(0.299, 0.587, 0.114));
          vec3 foundationLumaMatched = customTone * (baseLuma / (dot(customTone, vec3(0.299, 0.587, 0.114)) + 0.001));
          color = mix(color, foundationLumaMatched, fMask);
        }

        // 9. Contouring & Bronzer (Warm shadow sculpting on jaw and temples)
        float contourInt = uMakeupColors1.w;
        if (contourInt > 0.01) {
          float leftTemple = length(sampleUv - vec2(0.28, 0.58));
          float rightTemple = length(sampleUv - vec2(0.72, 0.58));
          float jawOuter = smoothstep(0.22, 0.36, distToFace);
          float cMask = (smoothstep(0.12, 0.02, min(leftTemple, rightTemple)) + jawOuter * 0.6) * contourInt * 0.35;
          color = mix(color, color * vec3(0.78, 0.68, 0.62), clamp(cMask, 0.0, 0.6));
        }

        // 10. Highlighter Glow (Cheekbones, nose bridge, cupid's bow)
        float highInt = uMakeupColors2.w;
        if (highInt > 0.01) {
          float lHigh = length(sampleUv - vec2(0.34, 0.52));
          float rHigh = length(sampleUv - vec2(0.66, 0.52));
          float noseHigh = smoothstep(0.02, 0.005, abs(sampleUv.x - 0.5)) * smoothstep(0.44, 0.58, sampleUv.y) * (1.0 - smoothstep(0.58, 0.62, sampleUv.y));
          float hMask = (smoothstep(0.08, 0.01, min(lHigh, rHigh)) * 0.7 + noseHigh * 0.5) * highInt * 0.5;
          color = mix(color, color * 1.35 + vec3(0.15, 0.14, 0.11), clamp(hMask, 0.0, 0.75));
        }

        // 11. Blush (Cheek flush with selected hex color)
        float blush = uMakeup.y;
        if (blush > 0.01) {
          float leftBlush = length(sampleUv - vec2(0.37, 0.44));
          float rightBlush = length(sampleUv - vec2(0.63, 0.44));
          float bMask = (smoothstep(0.13, 0.02, leftBlush) + smoothstep(0.13, 0.02, rightBlush)) * blush * 0.45;
          vec3 blushTone = uMakeupColors1.rgb;
          color = mix(color, blushTone, clamp(bMask, 0.0, 0.65));
        }

        // 12. Lipstick with Custom Shade and Gloss Specular Finish
        float lipstick = uMakeup.z;
        float gloss = uMakeup.w;
        vec2 lipCenter = vec2(0.5, 0.345);
        float lipDist = length((sampleUv - lipCenter) * vec2(1.0, 1.85));
        if (lipstick > 0.01 && lipDist < 0.078) {
          float lipWeight = smoothstep(0.078, 0.035, lipDist) * lipstick;
          vec3 lipColor = uMakeupColors2.rgb;
          color = mix(color, lipColor, lipWeight * 0.82);
          if (gloss > 0.05) {
            float lipLuma = dot(color, vec3(0.299, 0.587, 0.114));
            float spec = pow(max(0.0, lipLuma), 3.0);
            color += vec3(1.0) * gloss * lipWeight * (0.35 + spec * 0.25);
          }
        }

        // 13. Natural Organic Freckles
        float freckles = uMakeupColors3.w;
        if (freckles > 0.01 && distToFace < 0.28 && sampleUv.y > 0.40 && sampleUv.y < 0.55) {
          float noise = hash21(floor(sampleUv * 380.0), 42.0);
          if (noise > 0.86) {
            float fDot = smoothstep(0.86, 0.98, noise) * freckles * 0.65;
            color = mix(color, vec3(0.38, 0.22, 0.14), fDot);
          }
        }

        // 14. Skin Bronzing
        float tanIntensity = uExtraBeauty.x;
        if (tanIntensity > 0.01 && distToFace < 0.45) {
          color = mix(color, color * vec3(1.08, 0.98, 0.88) + vec3(0.03, 0.015, 0.0), tanIntensity * 0.28);
        }

        // 5. 8-Channel HSL Color Grading
        vec3 hsl = rgb2hsl(color);

        float totalHue = 0.0;
        float totalSat = 0.0;
        float totalLum = 0.0;

        for (int i = 0; i < 6; i++) {
          float centerHue = float(i) * 60.0;
          float diff = abs(mod(hsl.x - centerHue + 180.0, 360.0) - 180.0);
          float w = exp(-(diff * diff) / (2.0 * 25.0 * 25.0));
          totalHue += uHsl[i].x * 360.0 * w;
          totalSat += uHsl[i].y * w;
          totalLum += uHsl[i].z * w;
        }

        // Shadows (Index 6) & Highlights (Index 7)
        float sW = 1.0 - smoothstep(0.05, 0.45, hsl.z);
        totalHue += uHsl[6].x * 360.0 * sW;
        totalSat += uHsl[6].y * sW;
        totalLum += uHsl[6].z * sW;

        float hW = smoothstep(0.55, 0.95, hsl.z);
        totalHue += uHsl[7].x * 360.0 * hW;
        totalSat += uHsl[7].y * hW;
        totalLum += uHsl[7].z * hW;

        hsl.x = mod(hsl.x + totalHue, 360.0);
        if (hsl.x < 0.0) hsl.x += 360.0;
        hsl.y = clamp(hsl.y * (1.0 + totalSat), 0.0, 1.0);
        hsl.z = clamp(hsl.z + totalLum * 0.5, 0.0, 1.0);

        color = hsl2rgb(hsl);

        // 6. Levels Adjustment
        float inB = uLevelsInput.x;
        float inG = max(0.01, uLevelsInput.y);
        float inW = uLevelsInput.z;
        float outB = uLevelsOutput.x;
        float outW = uLevelsOutput.y;

        vec3 lvlNorm = clamp((color - vec3(inB)) / max(vec3(0.001), vec3(inW - inB)), 0.0, 1.0);
        vec3 lvlGamma = pow(lvlNorm, vec3(1.0 / inG));
        color = outB + (outW - outB) * lvlGamma;

        // 7. 14-Point RGB Curves (LUT sampling)
        float cr = texture(uCurveLut, vec2(color.r, 0.5)).r;
        float cg = texture(uCurveLut, vec2(color.g, 0.5)).g;
        float cb = texture(uCurveLut, vec2(color.b, 0.5)).b;
        color = vec3(cr, cg, cb);

        // 8. Optical Halation & Bloom
        float luma = dot(color, vec3(0.2126, 0.7152, 0.0722));
        float halIntensity = uEffects1.x;
        float halThresh = uEffects1.y;
        if (halIntensity > 0.01 && luma > halThresh) {
          float halW = (luma - halThresh) / (1.0 - halThresh + 0.001);
          vec3 halColor = vec3(1.0, 0.22, 0.04); // Kodak warm spectral bleed
          color += halColor * halW * halIntensity * 0.6;
        }

        float bloomIntensity = uEffects1.z;
        float bloomThresh = uEffects1.w;
        if (bloomIntensity > 0.01 && luma > bloomThresh) {
          float bW = (luma - bloomThresh) / (1.0 - bloomThresh + 0.001);
          color += vec3(bW * bloomIntensity * 0.5);
        }

        // 9. Film Grain
        float grainAmount = uEffects2.x;
        float grainSize = max(1.0, uEffects2.y);
        if (grainAmount > 0.01) {
          vec2 gCoord = (gl_FragCoord.xy) / grainSize;
          float noise = hash21(gCoord, uSeed);
          float midW = clamp(1.0 - abs(luma - 0.5) * 2.0, 0.15, 1.0);
          color += vec3(noise * grainAmount * midW * 0.15);
        }

        // 10. Basic Adjustments (Exposure, Contrast, Temp, Tint, Tone, Sat)
        // Exposure in EV
        color *= pow(2.0, uAdjustments1.x);

        // White Balance (Temperature & Tint)
        float tempVal = uAdjustments1.z * 0.005;
        float tintVal = uAdjustments1.w * 0.005;
        color.r += tempVal * 0.2;
        color.b -= tempVal * 0.2;
        color.g -= tintVal * 0.2;
        color.r += tintVal * 0.1;
        color.b += tintVal * 0.1;

        // Contrast
        float contrastFactor = 1.0 + uAdjustments1.y * 0.01;
        color = (color - 0.5) * contrastFactor + 0.5;

        // Highlights & Shadows
        float lumaTone = dot(color, vec3(0.2126, 0.7152, 0.0722));
        float hlOffset = uAdjustments2.x * 0.005;
        float shOffset = uAdjustments2.y * 0.005;
        if (hlOffset != 0.0) {
          color += vec3(hlOffset * smoothstep(0.4, 0.95, lumaTone));
        }
        if (shOffset != 0.0) {
          color += vec3(shOffset * (1.0 - smoothstep(0.05, 0.6, lumaTone)));
        }

        // Saturation & Vibrance
        float satFactor = 1.0 + uAdjustments3.w * 0.01;
        float currLuma = dot(color, vec3(0.2126, 0.7152, 0.0722));
        color = mix(vec3(currLuma), color, max(0.0, satFactor));

        // 11. Color Grading 3-Way (Shadows, Midtones, Highlights)
        if (uColorGradingShadows.y > 0.001 || abs(uColorGradingShadows.z) > 0.001) {
          vec3 shRgb = hsl2rgb(vec3(uColorGradingShadows.x * 360.0, uColorGradingShadows.y, 0.5));
          float shW = 1.0 - smoothstep(0.0, 0.5, currLuma);
          color += (shRgb - vec3(0.5)) * shW * uColorGradingShadows.y * 0.4;
          color += vec3(uColorGradingShadows.z * shW * 0.25);
        }
        if (uColorGradingMidtones.y > 0.001 || abs(uColorGradingMidtones.z) > 0.001) {
          vec3 midRgb = hsl2rgb(vec3(uColorGradingMidtones.x * 360.0, uColorGradingMidtones.y, 0.5));
          float midW = max(0.0, 1.0 - abs(currLuma - 0.5) * 2.0);
          color += (midRgb - vec3(0.5)) * midW * uColorGradingMidtones.y * 0.4;
          color += vec3(uColorGradingMidtones.z * midW * 0.25);
        }
        if (uColorGradingHighlights.y > 0.001 || abs(uColorGradingHighlights.z) > 0.001) {
          vec3 hlRgb = hsl2rgb(vec3(uColorGradingHighlights.x * 360.0, uColorGradingHighlights.y, 0.5));
          float hlW = smoothstep(0.5, 1.0, currLuma);
          color += (hlRgb - vec3(0.5)) * hlW * uColorGradingHighlights.y * 0.4;
          color += vec3(uColorGradingHighlights.z * hlW * 0.25);
        }

        // 12. Real-Time Clipping Warning Overlay
        if (uShowClipping > 0.5) {
          if (color.r >= 0.99 || color.g >= 0.99 || color.b >= 0.99) {
            color = vec3(1.0, 0.0, 0.0); // Blown highlights shown in bright red
          } else if (color.r <= 0.01 && color.g <= 0.01 && color.b <= 0.01) {
            color = vec3(0.1, 0.4, 1.0); // Crushed shadows shown in bright blue
          }
        }

        fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
      }
    `;

    const vs = this.compileShader(gl.VERTEX_SHADER, vsSource);
    const fs = this.compileShader(gl.FRAGMENT_SHADER, fsSource);
    if (!vs || !fs) return;

    const prog = gl.createProgram();
    if (!prog) return;
    gl.attachShader(prog, vs);
    gl.attachShader(prog, fs);
    gl.linkProgram(prog);

    if (!gl.getProgramParameter(prog, gl.LINK_STATUS)) {
      console.error('Program link error:', gl.getProgramInfoLog(prog));
      return;
    }

    this.program = prog;

    // Retrieve uniform locations
    this.uSourceLoc = gl.getUniformLocation(prog, 'uSource');
    this.uCurveLutLoc = gl.getUniformLocation(prog, 'uCurveLut');
    this.uResolutionLoc = gl.getUniformLocation(prog, 'uResolution');
    this.uPerspectiveLoc = gl.getUniformLocation(prog, 'uPerspective');
    this.uLevelsLoc = gl.getUniformLocation(prog, 'uLevelsInput');
    this.uHslChannelsLoc = gl.getUniformLocation(prog, 'uHsl');
    this.uEffectsLoc = gl.getUniformLocation(prog, 'uEffects1');
    this.uFaceRetouchLoc = gl.getUniformLocation(prog, 'uFaceRetouch');
    this.uFaceWarpLoc = gl.getUniformLocation(prog, 'uFaceWarp');
    this.uFaceFeaturesLoc = gl.getUniformLocation(prog, 'uFaceFeatures');
    this.uMakeupLoc = gl.getUniformLocation(prog, 'uMakeup');
    this.uMakeupColors1Loc = gl.getUniformLocation(prog, 'uMakeupColors1');
    this.uMakeupColors2Loc = gl.getUniformLocation(prog, 'uMakeupColors2');
    this.uMakeupColors3Loc = gl.getUniformLocation(prog, 'uMakeupColors3');
    this.uExtraBeautyLoc = gl.getUniformLocation(prog, 'uExtraBeauty');
    this.uSeedLoc = gl.getUniformLocation(prog, 'uSeed');

    // Adjustments & Color Grading
    const uAdj1 = gl.getUniformLocation(prog, 'uAdjustments1');
    const uAdj2 = gl.getUniformLocation(prog, 'uAdjustments2');
    const uAdj3 = gl.getUniformLocation(prog, 'uAdjustments3');
    const uCgSh = gl.getUniformLocation(prog, 'uColorGradingShadows');
    const uCgMid = gl.getUniformLocation(prog, 'uColorGradingMidtones');
    const uCgHl = gl.getUniformLocation(prog, 'uColorGradingHighlights');
    const uShowClip = gl.getUniformLocation(prog, 'uShowClipping');

    // Create quad buffer
    const vao = gl.createVertexArray();
    gl.bindVertexArray(vao);
    const posBuffer = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, posBuffer);
    gl.bufferData(
      gl.ARRAY_BUFFER,
      new Float32Array([-1, -1, 1, -1, -1, 1, -1, 1, 1, -1, 1, 1]),
      gl.STATIC_DRAW
    );
    gl.enableVertexAttribArray(0);
    gl.vertexAttribPointer(0, 2, gl.FLOAT, false, 0, 0);
    this.quadVao = vao;

    // Create LUT texture
    this.curveLutTexture = gl.createTexture();
    gl.bindTexture(gl.TEXTURE_2D, this.curveLutTexture);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
  }

  private compileShader(type: number, source: string): WebGLShader | null {
    const gl = this.gl!;
    const shader = gl.createShader(type);
    if (!shader) return null;
    gl.shaderSource(shader, source);
    gl.compileShader(shader);
    if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
      console.error('Shader compile error:', gl.getShaderInfoLog(shader));
      gl.deleteShader(shader);
      return null;
    }
    return shader;
  }

  public loadImage(image: HTMLImageElement | HTMLCanvasElement) {
    const gl = this.gl;
    if (!gl) return;

    this.imageWidth = image.width;
    this.imageHeight = image.height;

    this.canvas.width = image.width;
    this.canvas.height = image.height;

    if (!this.sourceTexture) {
      this.sourceTexture = gl.createTexture();
    }
    gl.bindTexture(gl.TEXTURE_2D, this.sourceTexture);
    gl.pixelStorei(gl.UNPACK_FLIP_Y_WEBGL, true);
    gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, image);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
  }

  public updateCurveLut(
    masterPts: CurvePoint[],
    redPts: CurvePoint[],
    greenPts: CurvePoint[],
    bluePts: CurvePoint[]
  ) {
    const gl = this.gl;
    if (!gl || !this.curveLutTexture) return;

    const mLut = generateSplineLut256(masterPts);
    const rLut = generateSplineLut256(redPts);
    const gLut = generateSplineLut256(greenPts);
    const bLut = generateSplineLut256(bluePts);

    const lutData = new Uint8Array(256 * 4);
    for (let i = 0; i < 256; i++) {
      const rVal = rLut[i];
      const gVal = gLut[i];
      const bVal = bLut[i];

      const rIdx = Math.max(0, Math.min(255, Math.round(rVal * 255)));
      const gIdx = Math.max(0, Math.min(255, Math.round(gVal * 255)));
      const bIdx = Math.max(0, Math.min(255, Math.round(bVal * 255)));

      lutData[i * 4 + 0] = Math.round(mLut[rIdx] * 255);
      lutData[i * 4 + 1] = Math.round(mLut[gIdx] * 255);
      lutData[i * 4 + 2] = Math.round(mLut[bIdx] * 255);
      lutData[i * 4 + 3] = 255;
    }

    gl.bindTexture(gl.TEXTURE_2D, this.curveLutTexture);
    gl.pixelStorei(gl.UNPACK_FLIP_Y_WEBGL, false);
    gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, 256, 1, 0, gl.RGBA, gl.UNSIGNED_BYTE, lutData);
  }

  public render(params: {
    perspective: PerspectiveSettings;
    levels: LevelsSettings;
    hsl: Record<HslChannelName, HslChannelSetting>;
    effects: OpticalEffectsSettings;
    faceRetouch: FaceRetouchSettings;
    makeup: VirtualMakeupSettings;
    beautyExtra?: BeautyExtraSettings;
    adjustments?: BasicAdjustments;
    colorGrading?: ColorGradingSettings;
    showClipping?: boolean;
  }) {
    const gl = this.gl;
    if (!gl || !this.program || !this.sourceTexture || !this.curveLutTexture) return;

    gl.viewport(0, 0, this.canvas.width, this.canvas.height);
    gl.useProgram(this.program);

    // Bind source texture to Unit 0
    gl.activeTexture(gl.TEXTURE0);
    gl.bindTexture(gl.TEXTURE_2D, this.sourceTexture);
    gl.uniform1i(this.uSourceLoc, 0);

    // Bind curve LUT texture to Unit 1
    gl.activeTexture(gl.TEXTURE1);
    gl.bindTexture(gl.TEXTURE_2D, this.curveLutTexture);
    gl.uniform1i(this.uCurveLutLoc, 1);

    gl.uniform2f(this.uResolutionLoc, this.canvas.width, this.canvas.height);

    // Perspective
    const rotRad = (params.perspective.rotation * Math.PI) / 180.0;
    const vTilt = Math.sin((params.perspective.verticalKeystone * Math.PI) / 180.0) * 0.7;
    const hTilt = Math.sin((params.perspective.horizontalKeystone * Math.PI) / 180.0) * 0.7;
    gl.uniform4f(this.uPerspectiveLoc, vTilt, hTilt, rotRad, params.perspective.scale);

    // Levels
    gl.uniform4f(
      this.uLevelsLoc,
      params.levels.inputBlack,
      params.levels.inputGamma,
      params.levels.inputWhite,
      0.0
    );
    const uLevelsOutLoc = gl.getUniformLocation(this.program, 'uLevelsOutput');
    gl.uniform4f(uLevelsOutLoc, params.levels.outputBlack, params.levels.outputWhite, 0.0, 0.0);

    // HSL uniform array
    const channels: HslChannelName[] = [
      'red',
      'yellow',
      'green',
      'cyan',
      'blue',
      'magenta',
      'shadows',
      'highlights',
    ];
    const hslFlat = new Float32Array(8 * 3);
    for (let i = 0; i < channels.length; i++) {
      const ch = params.hsl[channels[i]];
      hslFlat[i * 3 + 0] = ch ? ch.hueShift / 360.0 : 0;
      hslFlat[i * 3 + 1] = ch ? ch.saturation : 0;
      hslFlat[i * 3 + 2] = ch ? ch.luminance : 0;
    }
    gl.uniform3fv(this.uHslChannelsLoc, hslFlat);

    // Basic Adjustments
    const adj = params.adjustments;
    const uAdj1 = gl.getUniformLocation(this.program, 'uAdjustments1');
    gl.uniform4f(
      uAdj1,
      adj ? adj.exposure : 0,
      adj ? adj.contrast : 0,
      adj ? adj.temperature : 0,
      adj ? adj.tint : 0
    );
    const uAdj2 = gl.getUniformLocation(this.program, 'uAdjustments2');
    gl.uniform4f(
      uAdj2,
      adj ? adj.highlights : 0,
      adj ? adj.shadows : 0,
      adj ? adj.whites : 0,
      adj ? adj.blacks : 0
    );
    const uAdj3 = gl.getUniformLocation(this.program, 'uAdjustments3');
    gl.uniform4f(
      uAdj3,
      adj ? adj.clarity : 0,
      adj ? adj.dehaze : 0,
      adj ? adj.vibrance : 0,
      adj ? adj.saturation : 0
    );

    // Color Grading (Shadows, Midtones, Highlights)
    const cg = params.colorGrading;
    const uCgSh = gl.getUniformLocation(this.program, 'uColorGradingShadows');
    gl.uniform4f(
      uCgSh,
      cg ? cg.shadows.hue / 360.0 : 0,
      cg ? cg.shadows.saturation : 0,
      cg ? cg.shadows.luminance : 0,
      0.0
    );
    const uCgMid = gl.getUniformLocation(this.program, 'uColorGradingMidtones');
    gl.uniform4f(
      uCgMid,
      cg ? cg.midtones.hue / 360.0 : 0,
      cg ? cg.midtones.saturation : 0,
      cg ? cg.midtones.luminance : 0,
      0.0
    );
    const uCgHl = gl.getUniformLocation(this.program, 'uColorGradingHighlights');
    gl.uniform4f(
      uCgHl,
      cg ? cg.highlights.hue / 360.0 : 0,
      cg ? cg.highlights.saturation : 0,
      cg ? cg.highlights.luminance : 0,
      0.0
    );

    // Clipping Overlay
    const uShowClip = gl.getUniformLocation(this.program, 'uShowClipping');
    gl.uniform1f(uShowClip, params.showClipping ? 1.0 : 0.0);

    // Effects
    gl.uniform4f(
      this.uEffectsLoc,
      params.effects.halationIntensity,
      params.effects.halationThreshold,
      params.effects.bloomIntensity,
      params.effects.bloomThreshold
    );
    const uEffects2Loc = gl.getUniformLocation(this.program, 'uEffects2');
    gl.uniform4f(
      uEffects2Loc,
      params.effects.filmGrainAmount,
      params.effects.filmGrainSize,
      params.effects.lensBlurRadius,
      params.effects.lensBlurSpecularBoost
    );

    // Face Retouch & Warp Mesh
    gl.uniform4f(
      this.uFaceRetouchLoc,
      params.faceRetouch.bilateralSpatialSigma,
      params.faceRetouch.bilateralRangeSigma,
      params.faceRetouch.texturePreservation,
      params.faceRetouch.jawSlimming
    );

    gl.uniform4f(
      this.uFaceWarpLoc,
      params.faceRetouch.cheekNarrowing || 0,
      params.faceRetouch.chinSharpening || 0,
      params.faceRetouch.noseSlimming || 0,
      params.faceRetouch.eyeEnlargement || 0
    );

    gl.uniform4f(
      this.uFaceFeaturesLoc,
      (params.faceRetouch.teethWhitening || 0) / 100.0,
      (params.faceRetouch.eyeBrightening || 0) / 100.0,
      (params.faceRetouch.irisEnhancement || 0) / 100.0,
      (params.faceRetouch.oilControl || 0) / 100.0
    );

    // Makeup Base & Intensities
    gl.uniform4f(
      this.uMakeupLoc,
      params.makeup.foundationIntensity,
      params.makeup.blushIntensity,
      params.makeup.lipstickIntensity,
      params.makeup.lipstickGloss
    );

    const blushRgb = hexToRgb01(params.makeup.blushColor, [0.96, 0.25, 0.37]);
    gl.uniform4f(
      this.uMakeupColors1Loc,
      blushRgb[0],
      blushRgb[1],
      blushRgb[2],
      params.makeup.contourIntensity || 0
    );

    const lipRgb = hexToRgb01(params.makeup.lipstickColor, [0.88, 0.11, 0.28]);
    gl.uniform4f(
      this.uMakeupColors2Loc,
      lipRgb[0],
      lipRgb[1],
      lipRgb[2],
      (params.makeup.highlighterIntensity || 0) / 100.0
    );

    const foundRgb = hexToRgb01(params.makeup.foundationTone, [0.93, 0.82, 0.74]);
    gl.uniform4f(
      this.uMakeupColors3Loc,
      foundRgb[0],
      foundRgb[1],
      foundRgb[2],
      ((params.beautyExtra?.frecklesIntensity || 0) / 100.0)
    );

    gl.uniform4f(
      this.uExtraBeautyLoc,
      ((params.faceRetouch.tanIntensity || 0) / 100.0),
      ((params.faceRetouch.blemishRemoval || 0) / 100.0),
      ((params.faceRetouch.darkCirclesRemoval || 0) / 100.0),
      ((params.beautyExtra?.bodySlimming || 0) / 100.0)
    );

    gl.uniform1f(this.uSeedLoc, Math.random());

    // Draw
    gl.bindVertexArray(this.quadVao);
    gl.drawArrays(gl.TRIANGLES, 0, 6);
  }

  public extractHistogram(): HistogramBins {
    const gl = this.gl;
    if (!gl) {
      return { red: new Array(256).fill(0), green: new Array(256).fill(0), blue: new Array(256).fill(0), luma: new Array(256).fill(0), maxVal: 1 };
    }

    const w = this.canvas.width;
    const h = this.canvas.height;
    // Subsample for lightning fast 60 FPS histogram extraction
    const sampleW = Math.min(200, w);
    const sampleH = Math.min(200, h);
    const pixels = new Uint8Array(sampleW * sampleH * 4);

    gl.readPixels(0, 0, sampleW, sampleH, gl.RGBA, gl.UNSIGNED_BYTE, pixels);

    const rBins = new Array(256).fill(0);
    const gBins = new Array(256).fill(0);
    const bBins = new Array(256).fill(0);
    const lBins = new Array(256).fill(0);
    let maxV = 1;

    for (let i = 0; i < pixels.length; i += 4) {
      const r = pixels[i];
      const g = pixels[i + 1];
      const b = pixels[i + 2];
      const l = Math.round(0.2126 * r + 0.7152 * g + 0.0722 * b);

      rBins[r]++;
      gBins[g]++;
      bBins[b]++;
      lBins[l]++;

      if (lBins[l] > maxV) maxV = lBins[l];
    }

    return {
      red: rBins,
      green: gBins,
      blue: bBins,
      luma: lBins,
      maxVal: maxV,
    };
  }

  public captureScreenshot(): string {
    return this.canvas.toDataURL('image/jpeg', 0.95);
  }
}
