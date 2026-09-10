export interface CurvePoint {
  x: number; // 0.0 to 1.0
  y: number; // 0.0 to 1.0
}

export type CurveChannel = 'rgb' | 'red' | 'green' | 'blue';

export interface HslChannelSetting {
  hueShift: number; // -180 to 180 deg
  saturation: number; // -1.0 to 1.0 (-100% to +100%)
  luminance: number; // -1.0 to 1.0 (-100% to +100%)
}

export type HslChannelName =
  | 'red'
  | 'yellow'
  | 'green'
  | 'cyan'
  | 'blue'
  | 'magenta'
  | 'shadows'
  | 'highlights';

export interface LevelsSettings {
  inputBlack: number; // 0 to 1
  inputGamma: number; // 0.1 to 3.0
  inputWhite: number; // 0 to 1
  outputBlack: number; // 0 to 1
  outputWhite: number; // 0 to 1
}

export interface PerspectiveSettings {
  verticalKeystone: number; // -45 to 45
  horizontalKeystone: number; // -45 to 45
  rotation: number; // -180 to 180
  scale: number; // 0.5 to 2.0
}

export interface OpticalEffectsSettings {
  halationIntensity: number; // 0 to 1
  halationThreshold: number; // 0.5 to 1.0
  halationRadius: number; // 1 to 30
  bloomIntensity: number; // 0 to 1
  bloomThreshold: number; // 0.5 to 1.0
  filmGrainAmount: number; // 0 to 1
  filmGrainSize: number; // 1 to 4
  lensBlurRadius: number; // 0 to 30
  lensBlurBlades: number; // 0 = circular, 5..8
  lensBlurSpecularBoost: number; // 0 to 3
}

export interface FaceAnalysisResult {
  gender: 'female' | 'male' | 'unisex';
  estimatedAge: number;
  ethnicity: 'fair' | 'light' | 'medium' | 'tan' | 'deep';
  skinUndertone: 'warm' | 'cool' | 'neutral';
  faceShape: 'oval' | 'round' | 'square' | 'heart' | 'diamond';
  eyeSymmetry: number; // 0-100%
  goldenRatioScore: number; // 0-100%
}

export interface FaceRetouchSettings {
  // Piel Profesional
  bilateralSpatialSigma: number; // 0 to 15
  bilateralRangeSigma: number; // 0.02 to 0.4
  texturePreservation: number; // 0 to 1
  skinSmooth: number; // 0 to 100
  poreRefining: number; // 0 to 100
  blemishRemoval: number; // 0 to 100 (acné, manchas)
  wrinkleSmoothing: number; // 0 to 100
  darkCirclesRemoval: number; // 0 to 100 (ojeras)
  eyeBagsReduction: number; // 0 to 100
  skinToneUniformity: number; // 0 to 100
  oilControl: number; // 0 to 100 (eliminación de brillo sebáceo)
  tanIntensity: number; // 0 to 100 (bronceado)

  // Ojos y Dientes
  teethWhitening: number; // 0 to 100
  teethAlignment: number; // 0 to 100
  teethVeneers: number; // 0 to 100
  teethBraces: boolean;
  eyeBrightening: number; // 0 to 100 (sclera whitening)
  irisEnhancement: number; // 0 to 100
  catchlightIntensity: number; // 0 to 100
  redEyeRemoval: number; // 0 to 100

  // Remodelación Facial (Warp Mesh 3D)
  jawSlimming: number; // 0 to 1
  cheekNarrowing: number; // 0 to 1
  chinSharpening: number; // 0 to 1
  chinLength: number; // -50 to +50
  noseSlimming: number; // 0 to 1
  noseBridge: number; // -50 to +50
  noseTip: number; // -50 to +50
  eyeEnlargement: number; // 0 to 1
  eyeDistance: number; // -50 to +50
  lipVolume: number; // 0 to 100
  cupidsBowDefinition: number; // 0 to 100
  cheekboneLift: number; // 0 to 100
  jawlineDefinition: number; // 0 to 100
  browLift: number; // 0 to 100
  neckSlimming: number; // 0 to 100 (papada)

  showFaceMesh: boolean;
}

export interface VirtualMakeupSettings {
  // Base / Foundation
  foundationIntensity: number; // 0 to 1
  foundationTone: string; // hex color
  foundationFinish: 'matte' | 'satin' | 'dewy';
  settingPowder: number; // 0 to 100
  settingSprayGlow: number; // 0 to 100

  // Contorno e Iluminador
  contourIntensity: number; // 0 to 1
  contourTone: string; // hex
  bronzerIntensity: number; // 0 to 100
  highlighterIntensity: number; // 0 to 100
  highlighterTone: string; // hex

  // Rubor
  blushIntensity: number; // 0 to 1
  blushColor: string; // hex
  blushFinish: 'matte' | 'shimmer';

  // Labios
  lipstickIntensity: number; // 0 to 1
  lipstickColor: string; // hex
  lipstickGloss: number; // 0 to 1
  lipstickFinish: 'matte' | 'satin' | 'gloss' | 'metallic' | 'gradient';
  lipLinerIntensity: number; // 0 to 100
  lipLinerColor: string; // hex

  // Ojos
  eyeshadowIntensity: number; // 0 to 1
  eyeshadowColor: string; // hex
  eyeshadowStyle: 'gradient' | 'cut_crease' | 'smokey';
  eyelinerIntensity: number; // 0 to 1
  eyelinerStyle: 'natural' | 'winged' | 'cat_eye' | 'tightline';
  mascaraIntensity: number; // 0 to 1
  falseLashesStyle: 'none' | 'natural' | 'glamour' | 'cat' | 'doll';
  falseLashesIntensity: number; // 0 to 100

  // Cejas
  eyebrowIntensity: number; // 0 to 1
  eyebrowColor: string; // hex
  eyebrowStyle: 'natural' | 'microblading' | 'laminated';
}

export interface BeautyExtraSettings {
  bodySlimming: number; // 0 to 100
  waistSlimming: number; // 0 to 100
  legLengthening: number; // 0 to 100
  heightAdjustment: number; // -50 to +50
  frecklesIntensity: number; // 0 to 100
  hairColorIntensity: number; // 0 to 100
  hairColor: string; // hex
  hairVolume: number; // 0 to 100
  smoothSkinBody: number; // 0 to 100
}

export interface HistogramBins {
  red: number[];
  green: number[];
  blue: number[];
  luma: number[];
  maxVal: number;
}

export type BlendMode =
  | 'Normal'
  | 'Multiply'
  | 'Screen'
  | 'Overlay'
  | 'Soft Light'
  | 'Hard Light'
  | 'Color Dodge'
  | 'Color Burn'
  | 'Difference'
  | 'Exclusion'
  | 'Linear Dodge (Add)';

export interface LayerItem {
  id: string;
  name: string;
  visible: boolean;
  opacity: number;
  blendMode: BlendMode;
}

export interface ColorWheelValue {
  hue: number; // 0 to 360 deg
  saturation: number; // 0 to 1
  luminance: number; // -1 to 1 (lift/gamma/gain luminance offset)
}

export interface ColorGradingSettings {
  shadows: ColorWheelValue;
  midtones: ColorWheelValue;
  highlights: ColorWheelValue;
  global: ColorWheelValue;
}

export interface BasicAdjustments {
  exposure: number; // -3 to +3 EV
  contrast: number; // -100 to +100
  highlights: number; // -100 to +100
  shadows: number; // -100 to +100
  whites: number; // -100 to +100
  blacks: number; // -100 to +100
  temperature: number; // -100 (cool) to +100 (warm)
  tint: number; // -100 (green) to +100 (magenta)
  clarity: number; // -100 to +100
  dehaze: number; // -100 to +100
  vibrance: number; // -100 to +100
  saturation: number; // -100 to +100
}

export interface TextOverlay {
  id: string;
  text: string;
  fontFamily: string;
  fontSize: number;
  color: string;
  opacity: number;
  x: number; // 0 to 100 percentage
  y: number; // 0 to 100 percentage
  bold: boolean;
  italic: boolean;
  shadow: boolean;
}

export type BeforeAfterMode = 'horizontal_slider' | 'vertical_slider' | 'split_50_50' | 'blink';

export interface HistorySnapshot {
  id: string;
  title: string;
  timestamp: string;
  thumbnail: string;
  state: {
    curves: Record<CurveChannel, CurvePoint[]>;
    levels: LevelsSettings;
    hslSettings: Record<HslChannelName, HslChannelSetting>;
    colorGrading: ColorGradingSettings;
    adjustments: BasicAdjustments;
    effects: OpticalEffectsSettings;
    faceRetouch: FaceRetouchSettings;
    makeup: VirtualMakeupSettings;
    perspective: PerspectiveSettings;
  };
}

export type DockPosition = 'left' | 'right' | 'bottom' | 'floating';

export interface DockablePanelState {
  id: string;
  title: string;
  position: DockPosition;
  visible: boolean;
  isCollapsed: boolean;
}

export interface KeyboardShortcut {
  id: string;
  key: string;
  label: string;
  category: 'Navegación' | 'Edición' | 'Visualización' | 'Herramientas';
}
