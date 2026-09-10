import React, { useState, useEffect, useRef, useCallback } from 'react';
import {
  CurveChannel,
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
  LayerItem,
  BasicAdjustments,
  ColorGradingSettings,
  BeforeAfterMode,
  HistorySnapshot,
  DockablePanelState,
  DockPosition,
  TextOverlay,
} from './types';
import { WebGLEngine } from './engine/webglEngine';
import { SAMPLE_PHOTOS, SamplePhoto } from './data/samplePhotos';
import { Header } from './components/Header';
import { CurvesEditor } from './components/CurvesEditor';
import { HistogramView } from './components/HistogramView';
import { HslPanel } from './components/HslPanel';
import { FaceRetouchPanel } from './components/FaceRetouchPanel';
import { EffectsPanel } from './components/EffectsPanel';
import { PerspectivePanel } from './components/PerspectivePanel';
import { MaskToolsPanel, MaskMode } from './components/MaskToolsPanel';
import { LayersPanel } from './components/LayersPanel';
import { AndroidCodeExplorer } from './components/AndroidCodeExplorer';
import { FaceMeshOverlay } from './components/FaceMeshOverlay';
import { HealingOverlay } from './components/HealingOverlay';
import { ColorGradingWheels } from './components/ColorGradingWheels';
import { ColorPalettePicker } from './components/ColorPalettePicker';
import { ImageNavigator } from './components/ImageNavigator';
import { BeforeAfterOverlay } from './components/BeforeAfterOverlay';
import { HistoryPanel } from './components/HistoryPanel';
import { BottomSheetTools, BottomSheetCategory } from './components/BottomSheetTools';
import { KeyboardShortcutsModal } from './components/KeyboardShortcutsModal';
import { OnboardingTutorial } from './components/OnboardingTutorial';
import { DockablePanels } from './components/DockablePanels';
import {
  LineChart as CurvesIcon,
  BarChart3 as HistogramIcon,
  Palette as HslIcon,
  Smile as FaceIcon,
  Sparkles as EffectsIcon,
  Compass as PerspectiveIcon,
  Brush as MaskIcon,
  Layers as LayersIcon,
  SlidersHorizontal,
  History,
  Navigation,
  Pipette,
  Maximize2,
  ZoomIn,
  ZoomOut,
  RotateCw,
  AlertTriangle,
  Eye,
} from 'lucide-react';

const INITIAL_CURVE_POINTS: Record<CurveChannel, CurvePoint[]> = {
  rgb: [
    { x: 0, y: 0 },
    { x: 1, y: 1 },
  ],
  red: [
    { x: 0, y: 0 },
    { x: 1, y: 1 },
  ],
  green: [
    { x: 0, y: 0 },
    { x: 1, y: 1 },
  ],
  blue: [
    { x: 0, y: 0 },
    { x: 1, y: 1 },
  ],
};

const INITIAL_HSL: Record<HslChannelName, HslChannelSetting> = {
  red: { hueShift: 0, saturation: 0, luminance: 0 },
  yellow: { hueShift: 0, saturation: 0, luminance: 0 },
  green: { hueShift: 0, saturation: 0, luminance: 0 },
  cyan: { hueShift: 0, saturation: 0, luminance: 0 },
  blue: { hueShift: 0, saturation: 0, luminance: 0 },
  magenta: { hueShift: 0, saturation: 0, luminance: 0 },
  shadows: { hueShift: 0, saturation: 0, luminance: 0 },
  highlights: { hueShift: 0, saturation: 0, luminance: 0 },
};

const INITIAL_ADJUSTMENTS: BasicAdjustments = {
  exposure: 0,
  contrast: 0,
  highlights: 0,
  shadows: 0,
  whites: 0,
  blacks: 0,
  temperature: 0,
  tint: 0,
  clarity: 0,
  dehaze: 0,
  vibrance: 0,
  saturation: 0,
};

const INITIAL_COLOR_GRADING: ColorGradingSettings = {
  shadows: { hue: 215, saturation: 0, luminance: 0 },
  midtones: { hue: 45, saturation: 0, luminance: 0 },
  highlights: { hue: 35, saturation: 0, luminance: 0 },
  global: { hue: 0, saturation: 0, luminance: 0 },
};

export default function App() {
  // Navigation & View
  const [activeView, setActiveView] = useState<'editor' | 'code'>('editor');
  const [currentPhoto, setCurrentPhoto] = useState<SamplePhoto>(SAMPLE_PHOTOS[0]);
  const [fps] = useState(60);

  // Before / After View
  const [isCompareMode, setIsCompareMode] = useState(false);
  const [compareMode, setCompareMode] = useState<BeforeAfterMode>('horizontal_slider');
  const [compareSplit, setCompareSplit] = useState(50);

  // Viewport Zoom & Pan
  const [zoom, setZoom] = useState(1.0);
  const [pan, setPan] = useState({ x: 0, y: 0 });
  const [canvasRotation, setCanvasRotation] = useState(0);

  // Real-Time Clipping Warnings
  const [showClippingOverlay, setShowClippingOverlay] = useState(false);

  // Bottom Sheet Drawer
  const [bottomSheetCategory, setBottomSheetCategory] = useState<BottomSheetCategory>('adjustments');
  const [isBottomSheetExpanded, setIsBottomSheetExpanded] = useState(false);

  // Modals & Tutorials
  const [isShortcutsOpen, setIsShortcutsOpen] = useState(false);
  const [isOnboardingOpen, setIsOnboardingOpen] = useState(false);

  // Text Overlays
  const [textOverlays, setTextOverlays] = useState<TextOverlay[]>([]);

  // Engine Parameters
  const [curves, setCurves] = useState<Record<CurveChannel, CurvePoint[]>>(INITIAL_CURVE_POINTS);
  const [levels, setLevels] = useState<LevelsSettings>({
    inputBlack: 0,
    inputGamma: 1.0,
    inputWhite: 1.0,
    outputBlack: 0,
    outputWhite: 1.0,
  });
  const [hslSettings, setHslSettings] = useState<Record<HslChannelName, HslChannelSetting>>(INITIAL_HSL);
  const [basicAdjustments, setBasicAdjustments] = useState<BasicAdjustments>(INITIAL_ADJUSTMENTS);
  const [colorGrading, setColorGrading] = useState<ColorGradingSettings>(INITIAL_COLOR_GRADING);
  const [selectedPaletteColor, setSelectedPaletteColor] = useState('#38bdf8');

  const [perspective, setPerspective] = useState<PerspectiveSettings>({
    verticalKeystone: 0,
    horizontalKeystone: 0,
    rotation: 0,
    scale: 1.0,
  });
  const [effects, setEffects] = useState<OpticalEffectsSettings>({
    halationIntensity: 0,
    halationThreshold: 0.7,
    halationRadius: 12,
    bloomIntensity: 0,
    bloomThreshold: 0.75,
    filmGrainAmount: 0,
    filmGrainSize: 1.8,
    lensBlurRadius: 0,
    lensBlurBlades: 0,
    lensBlurSpecularBoost: 0,
  });
  const [faceRetouch, setFaceRetouch] = useState<FaceRetouchSettings>({
    bilateralSpatialSigma: 0,
    bilateralRangeSigma: 0.12,
    texturePreservation: 0.45,
    jawSlimming: 0,
    cheekNarrowing: 0,
    chinSharpening: 0,
    noseSlimming: 0,
    eyeEnlargement: 0,
    showFaceMesh: false,
  });
  const [makeup, setMakeup] = useState<VirtualMakeupSettings>({
    foundationIntensity: 0,
    contourIntensity: 0,
    blushIntensity: 0,
    blushColor: '#f43f5e',
    lipstickIntensity: 0,
    lipstickColor: '#e11d48',
    lipstickGloss: 0,
    eyeshadowIntensity: 0,
    eyeshadowColor: '#6366f1',
    eyelinerIntensity: 0,
    mascaraIntensity: 0,
    eyebrowIntensity: 0,
    eyebrowColor: '#451a03',
  });
  const [beautyExtra, setBeautyExtra] = useState<BeautyExtraSettings>({
    bodySlimming: 0,
    heightAdjustment: 0,
    frecklesIntensity: 0,
    hairColor: '#3d2314',
    hairColorIntensity: 0,
    hairVolume: 0,
    fullBodySmoothing: 0,
  });

  // Layers
  const [layers, setLayers] = useState<LayerItem[]>([
    { id: '1', name: 'Capa Base', visible: true, opacity: 1.0, blendMode: 'Normal' },
  ]);
  const [selectedLayerId, setSelectedLayerId] = useState<string>('1');

  // Masks & Healing
  const [maskMode, setMaskMode] = useState<MaskMode>('none');
  const [brushRadius, setBrushRadius] = useState(40);
  const [brushFeather, setBrushFeather] = useState(0.5);
  const [isProcessingAi, setIsProcessingAi] = useState(false);
  const [aiMaskStatus, setAiMaskStatus] = useState<string | null>(null);
  const [healingPoints, setHealingPoints] = useState<
    Array<{ target: { x: number; y: number }; donor: { x: number; y: number }; radius: number }>
  >([]);

  // Histogram
  const [histogram, setHistogram] = useState<HistogramBins>({
    red: new Array(256).fill(0),
    green: new Array(256).fill(0),
    blue: new Array(256).fill(0),
    luma: new Array(256).fill(0),
    maxVal: 1,
  });

  // Action History
  const [history, setHistory] = useState<HistorySnapshot[]>([]);
  const [historyIndex, setHistoryIndex] = useState(0);

  // Dockable Panels Configuration
  const [dockPanels, setDockPanels] = useState<DockablePanelState[]>([
    { id: 'navigator', title: 'Navegador de Imagen', position: 'left', visible: true, isCollapsed: false },
    { id: 'history', title: 'Historial de Snapshots', position: 'left', visible: true, isCollapsed: false },
    { id: 'palette', title: 'Paleta de Tonos', position: 'left', visible: false, isCollapsed: false },
    { id: 'histogram', title: 'Histograma RGB & Clipping', position: 'right', visible: true, isCollapsed: false },
    { id: 'curves', title: 'Curvas RGB (14 Pts)', position: 'right', visible: true, isCollapsed: false },
    { id: 'grading', title: 'Ruedas de Color 3-Way', position: 'right', visible: false, isCollapsed: false },
    { id: 'retouch', title: 'Retoque Facial & Belleza', position: 'right', visible: false, isCollapsed: false },
    { id: 'effects', title: 'Efectos Ópticos & Cine', position: 'right', visible: false, isCollapsed: false },
    { id: 'masks', title: 'Máscaras Selectivas AI', position: 'right', visible: false, isCollapsed: false },
    { id: 'layers', title: 'Capas y Modos de Fusión', position: 'right', visible: false, isCollapsed: false },
  ]);

  // Canvas and WebGL References
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const engineRef = useRef<WebGLEngine | null>(null);
  const originalImageRef = useRef<HTMLImageElement | null>(null);

  // Re-render when parameters change
  const renderScene = useCallback(() => {
    const engine = engineRef.current;
    if (!engine) return;

    engine.render({
      perspective,
      levels,
      hsl: hslSettings,
      effects,
      faceRetouch,
      makeup,
      beautyExtra,
      adjustments: basicAdjustments,
      colorGrading,
      showClipping: showClippingOverlay,
    });

    const h = engine.extractHistogram();
    setHistogram(h);
  }, [perspective, levels, hslSettings, effects, faceRetouch, makeup, beautyExtra, basicAdjustments, colorGrading, showClippingOverlay]);

  // Initialize WebGL2 engine
  useEffect(() => {
    if (!canvasRef.current) return;
    const engine = new WebGLEngine(canvasRef.current);
    engineRef.current = engine;

    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.src = currentPhoto.url;
    img.onload = () => {
      originalImageRef.current = img;
      engine.loadImage(img);
      engine.updateCurveLut(curves.rgb, curves.red, curves.green, curves.blue);
      renderScene();

      // Initial Snapshot
      const initialThumb = engine.captureScreenshot() || currentPhoto.url;
      const initialSnap: HistorySnapshot = {
        id: 'initial',
        title: 'Estado Original',
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
        thumbnail: initialThumb,
        state: {
          curves,
          levels,
          hslSettings,
          colorGrading,
          adjustments: basicAdjustments,
          effects,
          faceRetouch,
          makeup,
          perspective,
        },
      };
      setHistory([initialSnap]);
      setHistoryIndex(0);
    };
  }, [currentPhoto]);

  // Update curve LUT and render whenever curves change
  useEffect(() => {
    const engine = engineRef.current;
    if (!engine) return;
    engine.updateCurveLut(curves.rgb, curves.red, curves.green, curves.blue);
    renderScene();
  }, [curves, renderScene]);

  // Re-render on any changes
  useEffect(() => {
    renderScene();
  }, [renderScene]);

  // Record a Snapshot in History
  const pushSnapshot = useCallback((title: string) => {
    const engine = engineRef.current;
    const thumb = engine?.captureScreenshot() || currentPhoto.url;

    const newSnapshot: HistorySnapshot = {
      id: Date.now().toString(),
      title,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
      thumbnail: thumb,
      state: {
        curves,
        levels,
        hslSettings,
        colorGrading,
        adjustments: basicAdjustments,
        effects,
        faceRetouch,
        makeup,
        perspective,
      },
    };

    setHistory((prev) => {
      const truncated = prev.slice(0, historyIndex + 1);
      return [...truncated, newSnapshot];
    });
    setHistoryIndex((prev) => prev + 1);
  }, [currentPhoto, curves, levels, hslSettings, colorGrading, basicAdjustments, effects, faceRetouch, makeup, perspective, historyIndex]);

  // Restore snapshot
  const handleSelectSnapshot = (index: number) => {
    const snap = history[index];
    if (!snap) return;
    setCurves(snap.state.curves);
    setLevels(snap.state.levels);
    setHslSettings(snap.state.hslSettings);
    setColorGrading(snap.state.colorGrading);
    setBasicAdjustments(snap.state.adjustments);
    setEffects(snap.state.effects);
    setFaceRetouch(snap.state.faceRetouch);
    setMakeup(snap.state.makeup);
    setPerspective(snap.state.perspective);
    setHistoryIndex(index);
  };

  const handleUndo = () => {
    if (historyIndex > 0) {
      handleSelectSnapshot(historyIndex - 1);
    }
  };

  const handleRedo = () => {
    if (historyIndex < history.length - 1) {
      handleSelectSnapshot(historyIndex + 1);
    }
  };

  // Photo Switch
  const handleSelectPhoto = (photo: SamplePhoto) => {
    setCurrentPhoto(photo);
    setHealingPoints([]);
    setPan({ x: 0, y: 0 });
    setZoom(1.0);
  };

  // Custom File Upload or RAW DNG
  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (event) => {
      const img = new Image();
      img.onload = () => {
        originalImageRef.current = img;
        engineRef.current?.loadImage(img);
        renderScene();
        pushSnapshot(`Carga: ${file.name}`);
      };
      img.src = event.target?.result as string;
    };
    reader.readAsDataURL(file);
  };

  // Reset All Edits
  const handleResetAll = () => {
    setCurves(INITIAL_CURVE_POINTS);
    setLevels({
      inputBlack: 0,
      inputGamma: 1.0,
      inputWhite: 1.0,
      outputBlack: 0,
      outputWhite: 1.0,
    });
    setHslSettings(INITIAL_HSL);
    setBasicAdjustments(INITIAL_ADJUSTMENTS);
    setColorGrading(INITIAL_COLOR_GRADING);
    setPerspective({
      verticalKeystone: 0,
      horizontalKeystone: 0,
      rotation: 0,
      scale: 1.0,
    });
    setEffects({
      halationIntensity: 0,
      halationThreshold: 0.7,
      halationRadius: 12,
      bloomIntensity: 0,
      bloomThreshold: 0.75,
      filmGrainAmount: 0,
      filmGrainSize: 1.8,
      lensBlurRadius: 0,
      lensBlurBlades: 0,
      lensBlurSpecularBoost: 0,
    });
    setFaceRetouch({
      bilateralSpatialSigma: 0,
      bilateralRangeSigma: 0.12,
      texturePreservation: 0.45,
      jawSlimming: 0,
      cheekNarrowing: 0,
      chinSharpening: 0,
      noseSlimming: 0,
      eyeEnlargement: 0,
      showFaceMesh: false,
    });
    setMakeup({
      foundationIntensity: 0,
      contourIntensity: 0,
      blushIntensity: 0,
      blushColor: '#f43f5e',
      lipstickIntensity: 0,
      lipstickColor: '#e11d48',
      lipstickGloss: 0,
      eyeshadowIntensity: 0,
      eyeshadowColor: '#6366f1',
      eyelinerIntensity: 0,
      mascaraIntensity: 0,
      eyebrowIntensity: 0,
      eyebrowColor: '#451a03',
    });
    setHealingPoints([]);
    setShowClippingOverlay(false);
    pushSnapshot('Resetear Todo');
  };

  // Export processed image
  const handleExportImage = () => {
    const dataUrl = engineRef.current?.captureScreenshot();
    if (!dataUrl) return;
    const a = document.createElement('a');
    a.href = dataUrl;
    a.download = `photoengine_pro_${Date.now()}.jpg`;
    a.click();
  };

  // Color Palette application
  const handleApplyPaletteColor = (target: 'tint' | 'blush' | 'lipstick' | 'shadows' | 'highlights') => {
    if (target === 'blush') {
      setMakeup((prev) => ({ ...prev, blushColor: selectedPaletteColor }));
      pushSnapshot(`Color Colorete: ${selectedPaletteColor}`);
    } else if (target === 'lipstick') {
      setMakeup((prev) => ({ ...prev, lipstickColor: selectedPaletteColor }));
      pushSnapshot(`Color Labial: ${selectedPaletteColor}`);
    }
  };

  // AI Segmentation simulation
  const handleRunAiSegmentation = (category: string) => {
    setIsProcessingAi(true);
    setAiMaskStatus(`Segmentando ${category} con ML Kit Neural...`);
    setTimeout(() => {
      setIsProcessingAi(false);
      setAiMaskStatus(`Máscara de ${category} generada con éxito (Precisión 98.4%).`);
      pushSnapshot(`Máscara AI: ${category}`);
      setTimeout(() => setAiMaskStatus(null), 3000);
    }, 600);
  };

  // Canvas Click for Healing Brush auto-sampling
  const handleCanvasClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (maskMode !== 'healing') return;
    const rect = e.currentTarget.getBoundingClientRect();
    const clickX = e.clientX - rect.left;
    const clickY = e.clientY - rect.top;

    const donorX = clickX + 45;
    const donorY = clickY - 25;

    setHealingPoints((prev) => [
      ...prev,
      {
        target: { x: clickX, y: clickY },
        donor: { x: donorX, y: donorY },
        radius: brushRadius * 0.4,
      },
    ]);
    pushSnapshot('Pincel Corrector Healing');
  };

  // Touch & Gesture Handling
  const touchStateRef = useRef<{
    initialPointers: Map<number, { x: number; y: number }>;
    initialDistance: number;
    initialZoom: number;
    initialPan: { x: number; y: number };
    lastTapTime: number;
  }>({
    initialPointers: new Map(),
    initialDistance: 0,
    initialZoom: 1.0,
    initialPan: { x: 0, y: 0 },
    lastTapTime: 0,
  });

  const handlePointerDown = (e: React.PointerEvent<HTMLDivElement>) => {
    e.currentTarget.setPointerCapture(e.pointerId);
    touchStateRef.current.initialPointers.set(e.pointerId, { x: e.clientX, y: e.clientY });

    // Double tap check
    const now = Date.now();
    if (now - touchStateRef.current.lastTapTime < 300) {
      // Toggle 100% (2x) vs Fit (1x)
      if (zoom > 1.05) {
        setZoom(1.0);
        setPan({ x: 0, y: 0 });
      } else {
        setZoom(2.0);
      }
      touchStateRef.current.lastTapTime = 0;
      return;
    }
    touchStateRef.current.lastTapTime = now;

    if (touchStateRef.current.initialPointers.size === 2) {
      const pts: Array<{ x: number; y: number }> = Array.from(touchStateRef.current.initialPointers.values());
      const dist = Math.hypot(pts[0].x - pts[1].x, pts[0].y - pts[1].y);
      touchStateRef.current.initialDistance = dist;
      touchStateRef.current.initialZoom = zoom;
      touchStateRef.current.initialPan = { ...pan };
    } else if (touchStateRef.current.initialPointers.size === 1) {
      touchStateRef.current.initialPan = { ...pan };
    }
  };

  const handlePointerMove = (e: React.PointerEvent<HTMLDivElement>) => {
    if (!touchStateRef.current.initialPointers.has(e.pointerId)) return;
    touchStateRef.current.initialPointers.set(e.pointerId, { x: e.clientX, y: e.clientY });

    const pointers: Array<{ x: number; y: number }> = Array.from(touchStateRef.current.initialPointers.values());

    // Two-finger gesture: pinch zoom & pan
    if (pointers.length === 2 && touchStateRef.current.initialDistance > 0) {
      const dist = Math.hypot(pointers[0].x - pointers[1].x, pointers[0].y - pointers[1].y);
      const scaleFactor = dist / touchStateRef.current.initialDistance;
      const newZoom = Math.max(0.2, Math.min(5.0, touchStateRef.current.initialZoom * scaleFactor));
      setZoom(newZoom);

      const midX = (pointers[0].x + pointers[1].x) / 2;
      const midY = (pointers[0].y + pointers[1].y) / 2;
      // Slight smooth pan adjustment
    } else if (pointers.length === 1 && (e.buttons === 1 || e.buttons === 4)) {
      // Single finger / mouse pan when zoomed in or with middle click
      if (zoom > 1.05 || e.shiftKey || e.altKey || e.buttons === 4) {
        const dx = e.movementX;
        const dy = e.movementY;
        setPan((prev) => ({ x: prev.x + dx, y: prev.y + dy }));
      }
    }
  };

  const handlePointerUp = (e: React.PointerEvent<HTMLDivElement>) => {
    touchStateRef.current.initialPointers.delete(e.pointerId);
    try {
      e.currentTarget.releasePointerCapture(e.pointerId);
    } catch {
      // Ignored
    }
  };

  // Wheel Zoom
  const handleWheel = (e: React.WheelEvent<HTMLDivElement>) => {
    e.preventDefault();
    const zoomDelta = e.deltaY < 0 ? 1.15 : 0.87;
    setZoom((prev) => Math.max(0.2, Math.min(5.0, prev * zoomDelta)));
  };

  // Global Keyboard Shortcuts Listener
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // Ignore if typing in inputs
      if (['INPUT', 'TEXTAREA', 'SELECT'].includes((e.target as HTMLElement)?.tagName)) return;

      if (e.key === '\\') {
        e.preventDefault();
        setIsCompareMode((prev) => !prev);
      } else if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'z') {
        e.preventDefault();
        if (e.shiftKey) {
          handleRedo();
        } else {
          handleUndo();
        }
      } else if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'y') {
        e.preventDefault();
        handleRedo();
      } else if (e.key === '0' && (e.ctrlKey || e.metaKey)) {
        e.preventDefault();
        setZoom(1.0);
        setPan({ x: 0, y: 0 });
      } else if (e.key === '1' && (e.ctrlKey || e.metaKey)) {
        e.preventDefault();
        setZoom(2.0);
      } else if (e.key.toLowerCase() === 'j') {
        setShowClippingOverlay((prev) => !prev);
      } else if (e.key === '?') {
        setIsShortcutsOpen((prev) => !prev);
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [historyIndex, history]);

  // Dockable Panel Actions
  const handleToggleCollapsePanel = (id: string) => {
    setDockPanels((prev) =>
      prev.map((p) => (p.id === id ? { ...p, isCollapsed: !p.isCollapsed } : p))
    );
  };

  const handleClosePanel = (id: string) => {
    setDockPanels((prev) =>
      prev.map((p) => (p.id === id ? { ...p, visible: false } : p))
    );
  };

  const handleDockChange = (id: string, newPos: DockPosition) => {
    setDockPanels((prev) =>
      prev.map((p) => (p.id === id ? { ...p, position: newPos } : p))
    );
  };

  const handleTogglePanelVisibility = (id: string) => {
    setDockPanels((prev) =>
      prev.map((p) => (p.id === id ? { ...p, visible: !p.visible, isCollapsed: false } : p))
    );
  };

  // Render individual panel contents
  const renderPanelContent = (id: string) => {
    switch (id) {
      case 'navigator':
        return (
          <ImageNavigator
            imageUrl={currentPhoto.url}
            zoom={zoom}
            pan={pan}
            onPanChange={setPan}
            onZoomChange={setZoom}
            onResetView={() => {
              setZoom(1.0);
              setPan({ x: 0, y: 0 });
            }}
            rotation={canvasRotation}
            onRotate={() => setCanvasRotation((prev) => (prev + 90) % 360)}
          />
        );
      case 'history':
        return (
          <HistoryPanel
            snapshots={history}
            currentIndex={historyIndex}
            onSelectSnapshot={handleSelectSnapshot}
            onUndo={handleUndo}
            onRedo={handleRedo}
            onCreateSnapshot={() => pushSnapshot(`Snapshot manual #${history.length + 1}`)}
            canUndo={historyIndex > 0}
            canRedo={historyIndex < history.length - 1}
          />
        );
      case 'palette':
        return (
          <ColorPalettePicker
            selectedColor={selectedPaletteColor}
            onSelectColor={setSelectedPaletteColor}
            onApplyColorTo={handleApplyPaletteColor}
          />
        );
      case 'histogram':
        return (
          <HistogramView
            bins={histogram}
            levels={levels}
            onChangeLevels={(newLevels) => {
              setLevels(newLevels);
            }}
            showClippingWarning={showClippingOverlay}
            onToggleClippingWarning={() => setShowClippingOverlay(!showClippingOverlay)}
          />
        );
      case 'curves':
        return (
          <CurvesEditor
            curves={curves}
            onChange={(newCurves) => {
              setCurves(newCurves);
            }}
          />
        );
      case 'grading':
        return (
          <ColorGradingWheels
            colorGrading={colorGrading}
            onChange={setColorGrading}
            onReset={() => setColorGrading(INITIAL_COLOR_GRADING)}
          />
        );
      case 'retouch':
        return (
          <FaceRetouchPanel
            faceRetouch={faceRetouch}
            makeup={makeup}
            beautyExtra={beautyExtra}
            onChangeFace={setFaceRetouch}
            onChangeMakeup={setMakeup}
            onChangeBeautyExtra={setBeautyExtra}
          />
        );
      case 'effects':
        return <EffectsPanel effects={effects} onChange={setEffects} />;
      case 'masks':
        return (
          <MaskToolsPanel
            maskMode={maskMode}
            onSelectMaskMode={setMaskMode}
            brushRadius={brushRadius}
            onChangeBrushRadius={setBrushRadius}
            brushFeather={brushFeather}
            onChangeBrushFeather={setBrushFeather}
            onRunAiSegmentation={handleRunAiSegmentation}
            isProcessingAi={isProcessingAi}
            statusMessage={aiMaskStatus}
          />
        );
      case 'layers':
        return (
          <LayersPanel
            layers={layers}
            selectedLayerId={selectedLayerId}
            onSelectLayer={setSelectedLayerId}
            onToggleVisibility={(layerId) =>
              setLayers((prev) =>
                prev.map((l) => (l.id === layerId ? { ...l, visible: !l.visible } : l))
              )
            }
            onChangeOpacity={(layerId, op) =>
              setLayers((prev) =>
                prev.map((l) => (l.id === layerId ? { ...l, opacity: op } : l))
              )
            }
            onChangeBlendMode={(layerId, mode) =>
              setLayers((prev) =>
                prev.map((l) => (l.id === layerId ? { ...l, blendMode: mode } : l))
              )
            }
            onAddLayer={() => {
              const newL: LayerItem = {
                id: Date.now().toString(),
                name: `Capa ${layers.length + 1}`,
                visible: true,
                opacity: 1.0,
                blendMode: 'Normal',
              };
              setLayers((prev) => [...prev, newL]);
            }}
            onDeleteLayer={(layerId) =>
              setLayers((prev) => prev.filter((l) => l.id !== layerId))
            }
          />
        );
      default:
        return null;
    }
  };

  return (
    <div className="flex flex-col h-screen w-screen bg-neutral-950 text-neutral-100 overflow-hidden select-none font-sans antialiased">
      {/* Top Application Header */}
      <Header
        currentPhoto={currentPhoto}
        onSelectPhoto={handleSelectPhoto}
        onFileUpload={handleFileUpload}
        isCompareMode={isCompareMode}
        onToggleCompare={() => setIsCompareMode(!isCompareMode)}
        onResetAll={handleResetAll}
        onExportImage={handleExportImage}
        activeView={activeView}
        onToggleView={setActiveView}
        onOpenShortcuts={() => setIsShortcutsOpen(true)}
        onOpenOnboarding={() => setIsOnboardingOpen(true)}
        fps={fps}
      />

      {/* Main Workspace Body */}
      {activeView === 'code' ? (
        <div className="flex-1 p-4 overflow-hidden">
          <AndroidCodeExplorer />
        </div>
      ) : (
        <div className="flex flex-1 overflow-hidden relative">
          {/* Quick Panel Toggle Sidebar (Left) */}
          <div className="w-12 bg-neutral-900 border-r border-neutral-800 flex flex-col items-center py-2 gap-2 z-10">
            <button
              onClick={() => handleTogglePanelVisibility('navigator')}
              className={`p-2.5 rounded-xl text-xs transition-colors ${
                dockPanels.find((p) => p.id === 'navigator')?.visible
                  ? 'bg-sky-600/20 text-sky-400 border border-sky-500/30'
                  : 'text-neutral-400 hover:text-neutral-200 hover:bg-neutral-800'
              }`}
              title="Navegador de Imagen"
            >
              <Navigation className="w-4 h-4" />
            </button>
            <button
              onClick={() => handleTogglePanelVisibility('history')}
              className={`p-2.5 rounded-xl text-xs transition-colors ${
                dockPanels.find((p) => p.id === 'history')?.visible
                  ? 'bg-sky-600/20 text-sky-400 border border-sky-500/30'
                  : 'text-neutral-400 hover:text-neutral-200 hover:bg-neutral-800'
              }`}
              title="Historial de Acciones"
            >
              <History className="w-4 h-4" />
            </button>
            <button
              onClick={() => handleTogglePanelVisibility('palette')}
              className={`p-2.5 rounded-xl text-xs transition-colors ${
                dockPanels.find((p) => p.id === 'palette')?.visible
                  ? 'bg-sky-600/20 text-sky-400 border border-sky-500/30'
                  : 'text-neutral-400 hover:text-neutral-200 hover:bg-neutral-800'
              }`}
              title="Paleta de Colores Exactos"
            >
              <Pipette className="w-4 h-4" />
            </button>

            <div className="w-6 h-px bg-neutral-800 my-1" />

            <button
              onClick={() => handleTogglePanelVisibility('histogram')}
              className={`p-2.5 rounded-xl text-xs transition-colors ${
                dockPanels.find((p) => p.id === 'histogram')?.visible
                  ? 'bg-sky-600/20 text-sky-400 border border-sky-500/30'
                  : 'text-neutral-400 hover:text-neutral-200 hover:bg-neutral-800'
              }`}
              title="Histograma & Clipping"
            >
              <HistogramIcon className="w-4 h-4" />
            </button>
            <button
              onClick={() => handleTogglePanelVisibility('curves')}
              className={`p-2.5 rounded-xl text-xs transition-colors ${
                dockPanels.find((p) => p.id === 'curves')?.visible
                  ? 'bg-sky-600/20 text-sky-400 border border-sky-500/30'
                  : 'text-neutral-400 hover:text-neutral-200 hover:bg-neutral-800'
              }`}
              title="Curvas RGB (14 Puntos)"
            >
              <CurvesIcon className="w-4 h-4" />
            </button>
            <button
              onClick={() => handleTogglePanelVisibility('grading')}
              className={`p-2.5 rounded-xl text-xs transition-colors ${
                dockPanels.find((p) => p.id === 'grading')?.visible
                  ? 'bg-sky-600/20 text-sky-400 border border-sky-500/30'
                  : 'text-neutral-400 hover:text-neutral-200 hover:bg-neutral-800'
              }`}
              title="Ruedas de Color 3-Way"
            >
              <HslIcon className="w-4 h-4" />
            </button>
            <button
              onClick={() => handleTogglePanelVisibility('retouch')}
              className={`p-2.5 rounded-xl text-xs transition-colors ${
                dockPanels.find((p) => p.id === 'retouch')?.visible
                  ? 'bg-sky-600/20 text-sky-400 border border-sky-500/30'
                  : 'text-neutral-400 hover:text-neutral-200 hover:bg-neutral-800'
              }`}
              title="Retoque Facial & Belleza"
            >
              <FaceIcon className="w-4 h-4" />
            </button>
            <button
              onClick={() => handleTogglePanelVisibility('effects')}
              className={`p-2.5 rounded-xl text-xs transition-colors ${
                dockPanels.find((p) => p.id === 'effects')?.visible
                  ? 'bg-sky-600/20 text-sky-400 border border-sky-500/30'
                  : 'text-neutral-400 hover:text-neutral-200 hover:bg-neutral-800'
              }`}
              title="Efectos Ópticos"
            >
              <EffectsIcon className="w-4 h-4" />
            </button>
            <button
              onClick={() => handleTogglePanelVisibility('masks')}
              className={`p-2.5 rounded-xl text-xs transition-colors ${
                dockPanels.find((p) => p.id === 'masks')?.visible
                  ? 'bg-sky-600/20 text-sky-400 border border-sky-500/30'
                  : 'text-neutral-400 hover:text-neutral-200 hover:bg-neutral-800'
              }`}
              title="Máscaras Selectivas AI"
            >
              <MaskIcon className="w-4 h-4" />
            </button>
            <button
              onClick={() => handleTogglePanelVisibility('layers')}
              className={`p-2.5 rounded-xl text-xs transition-colors ${
                dockPanels.find((p) => p.id === 'layers')?.visible
                  ? 'bg-sky-600/20 text-sky-400 border border-sky-500/30'
                  : 'text-neutral-400 hover:text-neutral-200 hover:bg-neutral-800'
              }`}
              title="Capas y Fusión"
            >
              <LayersIcon className="w-4 h-4" />
            </button>
          </div>

          {/* Left Dockable Column */}
          <div className="hidden md:flex">
            <DockablePanels
              panels={dockPanels}
              position="left"
              onToggleCollapse={handleToggleCollapsePanel}
              onClosePanel={handleClosePanel}
              onDockChange={handleDockChange}
              renderPanelContent={renderPanelContent}
            />
          </div>

          {/* Center: Live GPU Stage with Gestures and Before/After Overlay */}
          <div
            className="flex-1 flex flex-col items-center justify-center p-3 bg-neutral-950 relative overflow-hidden"
            onPointerDown={handlePointerDown}
            onPointerMove={handlePointerMove}
            onPointerUp={handlePointerUp}
            onPointerCancel={handlePointerUp}
            onWheel={handleWheel}
          >
            {/* Viewport Floating HUD */}
            <div className="absolute top-4 left-4 z-20 flex items-center gap-1.5 bg-neutral-900/80 backdrop-blur-md px-3 py-1.5 rounded-xl border border-neutral-800 text-xs font-mono text-neutral-300">
              <span className="font-semibold text-sky-400">{Math.round(zoom * 100)}%</span>
              <span className="text-neutral-600">|</span>
              <button
                onClick={() => setZoom((z) => Math.min(5.0, z * 1.25))}
                className="p-1 hover:text-white rounded"
                title="Acercar (Zoom In)"
              >
                <ZoomIn className="w-3.5 h-3.5" />
              </button>
              <button
                onClick={() => setZoom((z) => Math.max(0.2, z / 1.25))}
                className="p-1 hover:text-white rounded"
                title="Alejar (Zoom Out)"
              >
                <ZoomOut className="w-3.5 h-3.5" />
              </button>
              <button
                onClick={() => {
                  setZoom(1.0);
                  setPan({ x: 0, y: 0 });
                }}
                className="p-1 hover:text-white rounded"
                title="Ajustar a Pantalla (100%)"
              >
                <Maximize2 className="w-3.5 h-3.5" />
              </button>
              <button
                onClick={() => setCanvasRotation((r) => (r + 90) % 360)}
                className="p-1 hover:text-white rounded"
                title="Rotar Lienzo 90°"
              >
                <RotateCw className="w-3.5 h-3.5" />
              </button>
              <span className="text-neutral-600">|</span>
              {/* Clipping alert indicator */}
              <button
                onClick={() => setShowClippingOverlay(!showClippingOverlay)}
                className={`flex items-center gap-1 px-1.5 py-0.5 rounded text-[11px] font-sans transition-colors ${
                  showClippingOverlay
                    ? 'bg-amber-950 text-amber-300 border border-amber-800'
                    : 'text-neutral-400 hover:text-neutral-200'
                }`}
                title="Alerta de Clipping en tiempo real (Rojo=Altas Luces, Azul=Sombras)"
              >
                <AlertTriangle className="w-3 h-3 text-amber-400" />
                <span>Clipping {showClippingOverlay ? 'ON' : 'OFF'}</span>
              </button>
            </div>

            {/* Viewport Canvas Container */}
            <div
              onClick={handleCanvasClick}
              className="relative max-w-full max-h-full rounded-2xl overflow-hidden shadow-2xl border border-neutral-800/80 bg-neutral-900 flex items-center justify-center transition-transform duration-75"
              style={{
                transform: `translate(${pan.x}px, ${pan.y}px) scale(${zoom}) rotate(${canvasRotation}deg)`,
                cursor: zoom > 1.05 ? 'grab' : maskMode === 'healing' ? 'crosshair' : 'default',
              }}
            >
              {/* WebGL2 Main Canvas */}
              <canvas
                ref={canvasRef}
                className="max-w-full max-h-[calc(100vh-140px)] object-contain block select-none"
              />

              {/* Text Overlays */}
              {textOverlays.map((to) => (
                <div
                  key={to.id}
                  className="absolute pointer-events-none select-none"
                  style={{
                    left: `${to.x}%`,
                    top: `${to.y}%`,
                    fontFamily: to.fontFamily,
                    fontSize: `${to.fontSize}px`,
                    color: to.color,
                    opacity: to.opacity,
                    fontWeight: to.bold ? 'bold' : 'normal',
                    fontStyle: to.italic ? 'italic' : 'normal',
                    textShadow: to.shadow ? '0 2px 8px rgba(0,0,0,0.8)' : 'none',
                  }}
                >
                  {to.text}
                </div>
              ))}

              {/* Split Comparison Mode Overlay */}
              {isCompareMode && originalImageRef.current && (
                <BeforeAfterOverlay
                  originalUrl={originalImageRef.current.src}
                  mode={compareMode}
                  splitPercent={compareSplit}
                  onChangeSplit={setCompareSplit}
                  onChangeMode={setCompareMode}
                  onClose={() => setIsCompareMode(false)}
                />
              )}

              {/* Face Mesh Landmark Overlay */}
              {faceRetouch.showFaceMesh && (
                <FaceMeshOverlay
                  width={canvasRef.current?.width || 800}
                  height={canvasRef.current?.height || 600}
                  visible={faceRetouch.showFaceMesh}
                />
              )}

              {/* Healing Brush Points Overlay */}
              {maskMode === 'healing' && <HealingOverlay points={healingPoints} />}
            </div>

            {/* Material Design 3 Bottom Sheet / Categorized Tools */}
            <div className="w-full max-w-3xl z-20 mt-2">
              <BottomSheetTools
                activeCategory={bottomSheetCategory}
                onCategoryChange={(cat) => {
                  setBottomSheetCategory(cat);
                  setIsBottomSheetExpanded(true);
                }}
                isExpanded={isBottomSheetExpanded}
                onToggleExpand={() => setIsBottomSheetExpanded(!isBottomSheetExpanded)}
                adjustments={basicAdjustments}
                onAdjustmentsChange={(newAdj) => {
                  setBasicAdjustments(newAdj);
                }}
                textOverlays={textOverlays}
                onTextOverlaysChange={(texts) => {
                  setTextOverlays(texts);
                  pushSnapshot('Actualizar Textos');
                }}
                onResetCategory={() => {
                  setBasicAdjustments(INITIAL_ADJUSTMENTS);
                  pushSnapshot('Resetear Ajustes Básicos');
                }}
              />
            </div>
          </div>

          {/* Right Dockable Column */}
          <div className="hidden lg:flex">
            <DockablePanels
              panels={dockPanels}
              position="right"
              onToggleCollapse={handleToggleCollapsePanel}
              onClosePanel={handleClosePanel}
              onDockChange={handleDockChange}
              renderPanelContent={renderPanelContent}
            />
          </div>
        </div>
      )}

      {/* Keyboard Shortcuts Dialog */}
      <KeyboardShortcutsModal
        isOpen={isShortcutsOpen}
        onClose={() => setIsShortcutsOpen(false)}
      />

      {/* Onboarding Interactive Tutorial Walkthrough */}
      <OnboardingTutorial
        isOpen={isOnboardingOpen}
        onClose={() => setIsOnboardingOpen(false)}
      />
    </div>
  );
}
