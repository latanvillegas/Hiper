import React, { useState } from 'react';
import {
  FaceRetouchSettings,
  VirtualMakeupSettings,
  BeautyExtraSettings,
  FaceAnalysisResult,
} from '../types';
import {
  Smile,
  Sparkles,
  Wand2,
  Eye,
  Sliders,
  Palette,
  User,
  Zap,
  RotateCcw,
  Check,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';
import {
  FOUNDATION_SHADES,
  BLUSH_PALETTES,
  LIPSTICK_COLLECTIONS,
  HIGHLIGHTER_PALETTES,
  CONTOURING_PALETTES,
  EYESHADOW_PRESETS,
  DEFAULT_FACE_RETOUCH,
  DEFAULT_VIRTUAL_MAKEUP,
  DEFAULT_BEAUTY_EXTRA,
} from '../data/beautyPresets';

interface FaceRetouchPanelProps {
  faceSettings?: FaceRetouchSettings;
  faceRetouch?: FaceRetouchSettings;
  makeupSettings?: VirtualMakeupSettings;
  makeup?: VirtualMakeupSettings;
  beautyExtra?: BeautyExtraSettings;
  onFaceChange?: (settings: FaceRetouchSettings) => void;
  onChangeFace?: (settings: FaceRetouchSettings) => void;
  onMakeupChange?: (settings: VirtualMakeupSettings) => void;
  onChangeMakeup?: (settings: VirtualMakeupSettings) => void;
  onBeautyExtraChange?: (settings: BeautyExtraSettings) => void;
  onChangeBeautyExtra?: (settings: BeautyExtraSettings) => void;
}

export const FaceRetouchPanel: React.FC<FaceRetouchPanelProps> = ({
  faceSettings,
  faceRetouch,
  makeupSettings,
  makeup,
  beautyExtra,
  onFaceChange,
  onChangeFace,
  onMakeupChange,
  onChangeMakeup,
  onBeautyExtraChange,
  onChangeBeautyExtra,
}) => {
  const currentFace: FaceRetouchSettings = faceRetouch || faceSettings || DEFAULT_FACE_RETOUCH;
  const currentMakeup: VirtualMakeupSettings = makeup || makeupSettings || DEFAULT_VIRTUAL_MAKEUP;
  const currentExtra: BeautyExtraSettings = beautyExtra || DEFAULT_BEAUTY_EXTRA;

  const updateFace = (next: FaceRetouchSettings) => {
    if (onChangeFace) onChangeFace(next);
    if (onFaceChange) onFaceChange(next);
  };

  const updateMakeup = (next: VirtualMakeupSettings) => {
    if (onChangeMakeup) onChangeMakeup(next);
    if (onMakeupChange) onMakeupChange(next);
  };

  const updateExtra = (next: BeautyExtraSettings) => {
    if (onChangeBeautyExtra) onChangeBeautyExtra(next);
    if (onBeautyExtraChange) onBeautyExtraChange(next);
  };

  const [activeTab, setActiveTab] = useState<
    'skin' | 'warp' | 'features' | 'makeup' | 'extra' | 'biometrics'
  >('skin');

  // Makeup category filter
  const [selectedFoundationCat, setSelectedFoundationCat] = useState<string>('Light');
  const [selectedLipFamily, setSelectedLipFamily] = useState<string>('Rojos Icónicos & Carmesí (20)');

  // Biometric Analysis Mock State
  const [faceAnalysis] = useState<FaceAnalysisResult>({
    landmarksCount: 468,
    estimatedAge: 26,
    gender: 'Femenino',
    skinToneUndertone: 'Neutro Cálido',
    faceShape: 'Ovalado Armónico',
    skinType: 'Mixta / Zona T Brillante',
    goldenRatioScore: 94.2,
    symmetryScore: 92.8,
  });

  const handleAutoTune = () => {
    updateFace({
      ...currentFace,
      bilateralSpatialSigma: 3.5,
      bilateralRangeSigma: 0.14,
      texturePreservation: 0.55,
      blemishRemoval: 45,
      darkCirclesRemoval: 35,
      poreRefining: 40,
      oilControl: 30,
      jawSlimming: 0.22,
      cheekNarrowing: 0.18,
      chinSharpening: 0.15,
      noseSlimming: 0.18,
      eyeEnlargement: 0.15,
      teethWhitening: 35,
      eyeBrightening: 30,
      irisEnhancement: 25,
      showFaceMesh: false,
    });

    updateMakeup({
      ...currentMakeup,
      foundationIntensity: 0.25,
      foundationTone: '#fae7d7',
      contourIntensity: 0.2,
      blushIntensity: 0.25,
      blushColor: '#f472b6',
      lipstickIntensity: 0.35,
      lipstickColor: '#c44d44',
      lipstickGloss: 0.25,
      highlighterIntensity: 30,
    });
  };

  const handleReset = () => {
    updateFace(DEFAULT_FACE_RETOUCH);
    updateMakeup(DEFAULT_VIRTUAL_MAKEUP);
    updateExtra(DEFAULT_BEAUTY_EXTRA);
  };

  return (
    <div className="flex flex-col gap-2.5 text-slate-200">
      {/* Top Header with AI Badge & Quick Action Buttons */}
      <div className="flex items-center justify-between bg-slate-900/90 p-2.5 rounded-xl border border-slate-800">
        <div className="flex items-center gap-2">
          <div className="w-6 h-6 rounded-lg bg-pink-500/20 border border-pink-500/40 flex items-center justify-center text-pink-400">
            <Sparkles className="w-3.5 h-3.5" />
          </div>
          <div>
            <div className="text-xs font-bold text-slate-100 flex items-center gap-1.5">
              <span>BeautyPlus AI Pro</span>
              <span className="bg-pink-500/20 text-pink-400 text-[9px] px-1.5 py-0.2 rounded font-semibold border border-pink-500/30">
                468 MESH
              </span>
            </div>
            <div className="text-[10px] text-slate-400">Retoque Facial & Maquillaje de Estudio</div>
          </div>
        </div>

        <div className="flex items-center gap-1.5">
          <button
            onClick={() => updateFace({ ...currentFace, showFaceMesh: !currentFace.showFaceMesh })}
            className={`px-2 py-1 rounded-lg text-[11px] font-semibold border transition-all flex items-center gap-1 ${
              currentFace.showFaceMesh
                ? 'bg-sky-500/20 border-sky-400 text-sky-300'
                : 'bg-slate-800 border-slate-700 text-slate-400 hover:text-slate-200'
            }`}
            title="Visualizar Malla 3D ML Kit (468 landmarks)"
          >
            <Eye className="w-3 h-3" />
            <span>Mesh</span>
          </button>

          <button
            onClick={handleAutoTune}
            className="px-2.5 py-1 rounded-lg text-[11px] font-semibold bg-gradient-to-r from-pink-600 to-purple-600 hover:from-pink-500 hover:to-purple-500 text-white shadow-sm transition-all flex items-center gap-1 border border-pink-400/30"
            title="Auto-embellecer con ratio áureo natural"
          >
            <Wand2 className="w-3 h-3" />
            <span>1-Tap AI</span>
          </button>

          <button
            onClick={handleReset}
            className="p-1 rounded-lg bg-slate-800 border border-slate-700 text-slate-400 hover:text-slate-200 hover:bg-slate-700 transition-colors"
            title="Restablecer todos los ajustes de belleza"
          >
            <RotateCcw className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>

      {/* Main Navigation Subtabs */}
      <div className="grid grid-cols-6 gap-1 bg-slate-900/80 p-1 rounded-xl border border-slate-800 text-[11px] font-medium">
        <button
          onClick={() => setActiveTab('skin')}
          className={`py-1.5 px-1 rounded-lg transition-all text-center truncate ${
            activeTab === 'skin'
              ? 'bg-gradient-to-b from-pink-500 to-rose-600 text-white font-semibold shadow'
              : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800'
          }`}
        >
          Piel AI
        </button>
        <button
          onClick={() => setActiveTab('warp')}
          className={`py-1.5 px-1 rounded-lg transition-all text-center truncate ${
            activeTab === 'warp'
              ? 'bg-gradient-to-b from-purple-500 to-indigo-600 text-white font-semibold shadow'
              : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800'
          }`}
        >
          Warp
        </button>
        <button
          onClick={() => setActiveTab('features')}
          className={`py-1.5 px-1 rounded-lg transition-all text-center truncate ${
            activeTab === 'features'
              ? 'bg-gradient-to-b from-sky-500 to-cyan-600 text-white font-semibold shadow'
              : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800'
          }`}
        >
          Ojos/Dientes
        </button>
        <button
          onClick={() => setActiveTab('makeup')}
          className={`py-1.5 px-1 rounded-lg transition-all text-center truncate ${
            activeTab === 'makeup'
              ? 'bg-gradient-to-b from-amber-500 to-orange-600 text-white font-semibold shadow'
              : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800'
          }`}
        >
          Makeup
        </button>
        <button
          onClick={() => setActiveTab('extra')}
          className={`py-1.5 px-1 rounded-lg transition-all text-center truncate ${
            activeTab === 'extra'
              ? 'bg-gradient-to-b from-emerald-500 to-teal-600 text-white font-semibold shadow'
              : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800'
          }`}
        >
          Cuerpo
        </button>
        <button
          onClick={() => setActiveTab('biometrics')}
          className={`py-1.5 px-1 rounded-lg transition-all text-center truncate ${
            activeTab === 'biometrics'
              ? 'bg-gradient-to-b from-fuchsia-600 to-pink-700 text-white font-semibold shadow'
              : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800'
          }`}
        >
          Biometría
        </button>
      </div>

      {/* Panel Body Content */}
      <div className="bg-slate-900/80 rounded-xl p-3 border border-slate-800 space-y-3.5 max-h-[520px] overflow-y-auto pr-2 custom-scrollbar">
        {/* ============================================================ */}
        {/* TAB 1: PIEL PROFESIONAL BILATERAL & AI IMPERFECTIONS */}
        {/* ============================================================ */}
        {activeTab === 'skin' && (
          <div className="space-y-3">
            <div className="text-[11px] font-semibold text-pink-300 flex items-center justify-between pb-1 border-b border-slate-800">
              <span>Retoque y Textura Natural de Piel</span>
              <span className="text-[10px] text-slate-500">Kernel Bilateral + High-Pass</span>
            </div>

            {/* Suavizado Bilateral */}
            <div className="space-y-1">
              <div className="flex justify-between text-[11px] text-slate-300">
                <span>Suavizado Bilateral (Radio)</span>
                <span className="font-mono text-pink-300">
                  {currentFace.bilateralSpatialSigma.toFixed(1)} px
                </span>
              </div>
              <input
                type="range"
                min={0}
                max={12}
                step={0.2}
                value={currentFace.bilateralSpatialSigma}
                onChange={(e) =>
                  updateFace({ ...currentFace, bilateralSpatialSigma: parseFloat(e.target.value) })
                }
                className="w-full accent-pink-400 h-1.5 bg-slate-800 rounded-lg cursor-pointer"
              />
            </div>

            {/* Rango de color */}
            <div className="space-y-1">
              <div className="flex justify-between text-[11px] text-slate-300">
                <span>Sensibilidad de Tono (Sigma Rango)</span>
                <span className="font-mono text-pink-300">
                  {currentFace.bilateralRangeSigma.toFixed(2)}
                </span>
              </div>
              <input
                type="range"
                min={0.03}
                max={0.35}
                step={0.01}
                value={currentFace.bilateralRangeSigma}
                onChange={(e) =>
                  updateFace({ ...currentFace, bilateralRangeSigma: parseFloat(e.target.value) })
                }
                className="w-full accent-pink-400 h-1.5 bg-slate-800 rounded-lg cursor-pointer"
              />
            </div>

            {/* Preservación de textura de poros */}
            <div className="space-y-1">
              <div className="flex justify-between text-[11px] text-slate-300">
                <span>Preservación de Poros y Nitidez</span>
                <span className="font-mono text-pink-300">
                  {Math.round(currentFace.texturePreservation * 100)}%
                </span>
              </div>
              <input
                type="range"
                min={0}
                max={1}
                step={0.05}
                value={currentFace.texturePreservation}
                onChange={(e) =>
                  updateFace({ ...currentFace, texturePreservation: parseFloat(e.target.value) })
                }
                className="w-full accent-pink-400 h-1.5 bg-slate-800 rounded-lg cursor-pointer"
              />
            </div>

            <div className="pt-2 border-t border-slate-800">
              <div className="text-[11px] font-semibold text-purple-300 pb-1.5">
                Corrección Inteligente de Imperfecciones
              </div>

              <div className="grid grid-cols-2 gap-2.5">
                {/* Eliminación de imperfecciones AI */}
                <div className="space-y-1">
                  <div className="flex justify-between text-[10px] text-slate-300">
                    <span>Acné y Manchas</span>
                    <span className="font-mono text-purple-300">
                      {Math.round(currentFace.blemishRemoval || 0)}%
                    </span>
                  </div>
                  <input
                    type="range"
                    min={0}
                    max={100}
                    value={currentFace.blemishRemoval || 0}
                    onChange={(e) =>
                      updateFace({ ...currentFace, blemishRemoval: parseFloat(e.target.value) })
                    }
                    className="w-full accent-purple-400 h-1 bg-slate-800 rounded-lg cursor-pointer"
                  />
                </div>

                {/* Ojeras y Bolsas */}
                <div className="space-y-1">
                  <div className="flex justify-between text-[10px] text-slate-300">
                    <span>Ojeras y Bolsas</span>
                    <span className="font-mono text-purple-300">
                      {Math.round(currentFace.darkCirclesRemoval || 0)}%
                    </span>
                  </div>
                  <input
                    type="range"
                    min={0}
                    max={100}
                    value={currentFace.darkCirclesRemoval || 0}
                    onChange={(e) =>
                      updateFace({ ...currentFace, darkCirclesRemoval: parseFloat(e.target.value) })
                    }
                    className="w-full accent-purple-400 h-1 bg-slate-800 rounded-lg cursor-pointer"
                  />
                </div>

                {/* Arrugas y líneas */}
                <div className="space-y-1">
                  <div className="flex justify-between text-[10px] text-slate-300">
                    <span>Arrugas y Líneas</span>
                    <span className="font-mono text-purple-300">
                      {Math.round(currentFace.wrinkleSmoothing || 0)}%
                    </span>
                  </div>
                  <input
                    type="range"
                    min={0}
                    max={100}
                    value={currentFace.wrinkleSmoothing || 0}
                    onChange={(e) =>
                      updateFace({ ...currentFace, wrinkleSmoothing: parseFloat(e.target.value) })
                    }
                    className="w-full accent-purple-400 h-1 bg-slate-800 rounded-lg cursor-pointer"
                  />
                </div>

                {/* Poros dilatados */}
                <div className="space-y-1">
                  <div className="flex justify-between text-[10px] text-slate-300">
                    <span>Refinar Poros</span>
                    <span className="font-mono text-purple-300">
                      {Math.round(currentFace.poreRefining || 0)}%
                    </span>
                  </div>
                  <input
                    type="range"
                    min={0}
                    max={100}
                    value={currentFace.poreRefining || 0}
                    onChange={(e) =>
                      updateFace({ ...currentFace, poreRefining: parseFloat(e.target.value) })
                    }
                    className="w-full accent-purple-400 h-1 bg-slate-800 rounded-lg cursor-pointer"
                  />
                </div>

                {/* Oil Control / Antibrillo */}
                <div className="space-y-1">
                  <div className="flex justify-between text-[10px] text-slate-300">
                    <span>Control de Sebo (Oil)</span>
                    <span className="font-mono text-purple-300">
                      {Math.round(currentFace.oilControl || 0)}%
                    </span>
                  </div>
                  <input
                    type="range"
                    min={0}
                    max={100}
                    value={currentFace.oilControl || 0}
                    onChange={(e) =>
                      updateFace({ ...currentFace, oilControl: parseFloat(e.target.value) })
                    }
                    className="w-full accent-purple-400 h-1 bg-slate-800 rounded-lg cursor-pointer"
                  />
                </div>

                {/* Bronceado */}
                <div className="space-y-1">
                  <div className="flex justify-between text-[10px] text-slate-300">
                    <span>Bronceado Uniforme</span>
                    <span className="font-mono text-purple-300">
                      {Math.round(currentFace.tanIntensity || 0)}%
                    </span>
                  </div>
                  <input
                    type="range"
                    min={0}
                    max={100}
                    value={currentFace.tanIntensity || 0}
                    onChange={(e) =>
                      updateFace({ ...currentFace, tanIntensity: parseFloat(e.target.value) })
                    }
                    className="w-full accent-purple-400 h-1 bg-slate-800 rounded-lg cursor-pointer"
                  />
                </div>
              </div>
            </div>
          </div>
        )}

        {/* ============================================================ */}
        {/* TAB 2: WARP MESH - REMODELACIÓN ANATÓMICA 3D */}
        {/* ============================================================ */}
        {activeTab === 'warp' && (
          <div className="space-y-3">
            <div className="text-[11px] font-semibold text-purple-300 flex items-center justify-between pb-1 border-b border-slate-800">
              <span>Deformación de Malla Anatómica (Warp Mesh)</span>
              <span className="text-[10px] text-slate-500">Morfología Natural</span>
            </div>

            <div className="grid grid-cols-2 gap-2.5">
              {/* Jaw Slimming */}
              <div className="space-y-1">
                <div className="flex justify-between text-[10px] text-slate-300">
                  <span>Mandíbula (Jawline)</span>
                  <span className="font-mono text-purple-300">
                    {Math.round(currentFace.jawSlimming * 100)}%
                  </span>
                </div>
                <input
                  type="range"
                  min={0}
                  max={1}
                  step={0.02}
                  value={currentFace.jawSlimming}
                  onChange={(e) =>
                    updateFace({ ...currentFace, jawSlimming: parseFloat(e.target.value) })
                  }
                  className="w-full accent-purple-400 h-1.5 bg-slate-800 rounded-lg cursor-pointer"
                />
              </div>

              {/* Cheek Narrowing */}
              <div className="space-y-1">
                <div className="flex justify-between text-[10px] text-slate-300">
                  <span>Mejillas (Cheeks)</span>
                  <span className="font-mono text-purple-300">
                    {Math.round(currentFace.cheekNarrowing * 100)}%
                  </span>
                </div>
                <input
                  type="range"
                  min={0}
                  max={1}
                  step={0.02}
                  value={currentFace.cheekNarrowing}
                  onChange={(e) =>
                    updateFace({ ...currentFace, cheekNarrowing: parseFloat(e.target.value) })
                  }
                  className="w-full accent-purple-400 h-1.5 bg-slate-800 rounded-lg cursor-pointer"
                />
              </div>

              {/* Chin Sharpening */}
              <div className="space-y-1">
                <div className="flex justify-between text-[10px] text-slate-300">
                  <span>Mentón / Barbilla</span>
                  <span className="font-mono text-purple-300">
                    {Math.round(currentFace.chinSharpening * 100)}%
                  </span>
                </div>
                <input
                  type="range"
                  min={0}
                  max={1}
                  step={0.02}
                  value={currentFace.chinSharpening}
                  onChange={(e) =>
                    updateFace({ ...currentFace, chinSharpening: parseFloat(e.target.value) })
                  }
                  className="w-full accent-purple-400 h-1.5 bg-slate-800 rounded-lg cursor-pointer"
                />
              </div>

              {/* Nose Slimming */}
              <div className="space-y-1">
                <div className="flex justify-between text-[10px] text-slate-300">
                  <span>Afinar Nariz (Nose)</span>
                  <span className="font-mono text-purple-300">
                    {Math.round(currentFace.noseSlimming * 100)}%
                  </span>
                </div>
                <input
                  type="range"
                  min={0}
                  max={1}
                  step={0.02}
                  value={currentFace.noseSlimming}
                  onChange={(e) =>
                    updateFace({ ...currentFace, noseSlimming: parseFloat(e.target.value) })
                  }
                  className="w-full accent-purple-400 h-1.5 bg-slate-800 rounded-lg cursor-pointer"
                />
              </div>

              {/* Eye Enlargement */}
              <div className="space-y-1">
                <div className="flex justify-between text-[10px] text-slate-300">
                  <span>Agrandar Ojos</span>
                  <span className="font-mono text-purple-300">
                    {Math.round(currentFace.eyeEnlargement * 100)}%
                  </span>
                </div>
                <input
                  type="range"
                  min={0}
                  max={1}
                  step={0.02}
                  value={currentFace.eyeEnlargement}
                  onChange={(e) =>
                    updateFace({ ...currentFace, eyeEnlargement: parseFloat(e.target.value) })
                  }
                  className="w-full accent-purple-400 h-1.5 bg-slate-800 rounded-lg cursor-pointer"
                />
              </div>

              {/* Pómulos */}
              <div className="space-y-1">
                <div className="flex justify-between text-[10px] text-slate-300">
                  <span>Elevar Pómulos</span>
                  <span className="font-mono text-purple-300">
                    {Math.round((currentFace.cheekboneLift || 0))} %
                  </span>
                </div>
                <input
                  type="range"
                  min={0}
                  max={100}
                  value={currentFace.cheekboneLift || 0}
                  onChange={(e) =>
                    updateFace({ ...currentFace, cheekboneLift: parseFloat(e.target.value) })
                  }
                  className="w-full accent-purple-400 h-1.5 bg-slate-800 rounded-lg cursor-pointer"
                />
              </div>

              {/* Adelgazamiento de cuello */}
              <div className="space-y-1 col-span-2">
                <div className="flex justify-between text-[10px] text-slate-300">
                  <span>Adelgazar Cuello & Reducir Papada</span>
                  <span className="font-mono text-purple-300">
                    {Math.round(currentFace.neckSlimming || 0)}%
                  </span>
                </div>
                <input
                  type="range"
                  min={0}
                  max={100}
                  value={currentFace.neckSlimming || 0}
                  onChange={(e) =>
                    updateFace({ ...currentFace, neckSlimming: parseFloat(e.target.value) })
                  }
                  className="w-full accent-purple-400 h-1.5 bg-slate-800 rounded-lg cursor-pointer"
                />
              </div>
            </div>
          </div>
        )}

        {/* ============================================================ */}
        {/* TAB 3: DIENTES & OJOS PERFECTOS */}
        {/* ============================================================ */}
        {activeTab === 'features' && (
          <div className="space-y-3">
            <div className="text-[11px] font-semibold text-cyan-300 flex items-center justify-between pb-1 border-b border-slate-800">
              <span>Odontología Cosmética & Mirada Brillante</span>
              <span className="text-[10px] text-slate-500">Segmentación Diente / Iris</span>
            </div>

            <div className="space-y-2.5">
              {/* Blanqueamiento Dental */}
              <div className="space-y-1">
                <div className="flex justify-between text-[11px] text-slate-300">
                  <span>Blanqueamiento Dental AI (Teeth Whitening)</span>
                  <span className="font-mono text-cyan-300">
                    {Math.round(currentFace.teethWhitening || 0)}%
                  </span>
                </div>
                <input
                  type="range"
                  min={0}
                  max={100}
                  value={currentFace.teethWhitening || 0}
                  onChange={(e) =>
                    updateFace({ ...currentFace, teethWhitening: parseFloat(e.target.value) })
                  }
                  className="w-full accent-cyan-400 h-1.5 bg-slate-800 rounded-lg cursor-pointer"
                />
              </div>

              {/* Alineación Dental */}
              <div className="space-y-1">
                <div className="flex justify-between text-[11px] text-slate-300">
                  <span>Alineación Virtual & Carillas</span>
                  <span className="font-mono text-cyan-300">
                    {Math.round(currentFace.teethAlignment || 0)}%
                  </span>
                </div>
                <input
                  type="range"
                  min={0}
                  max={100}
                  value={currentFace.teethAlignment || 0}
                  onChange={(e) =>
                    updateFace({ ...currentFace, teethAlignment: parseFloat(e.target.value) })
                  }
                  className="w-full accent-cyan-400 h-1.5 bg-slate-800 rounded-lg cursor-pointer"
                />
              </div>

              {/* Blanqueamiento de Esclera */}
              <div className="space-y-1">
                <div className="flex justify-between text-[11px] text-slate-300">
                  <span>Blanqueamiento de Esclera (Ojos Claros)</span>
                  <span className="font-mono text-cyan-300">
                    {Math.round(currentFace.eyeBrightening || 0)}%
                  </span>
                </div>
                <input
                  type="range"
                  min={0}
                  max={100}
                  value={currentFace.eyeBrightening || 0}
                  onChange={(e) =>
                    updateFace({ ...currentFace, eyeBrightening: parseFloat(e.target.value) })
                  }
                  className="w-full accent-cyan-400 h-1.5 bg-slate-800 rounded-lg cursor-pointer"
                />
              </div>

              {/* Realce de Iris */}
              <div className="space-y-1">
                <div className="flex justify-between text-[11px] text-slate-300">
                  <span>Realce de Color de Iris & Profundidad</span>
                  <span className="font-mono text-cyan-300">
                    {Math.round(currentFace.irisEnhancement || 0)}%
                  </span>
                </div>
                <input
                  type="range"
                  min={0}
                  max={100}
                  value={currentFace.irisEnhancement || 0}
                  onChange={(e) =>
                    updateFace({ ...currentFace, irisEnhancement: parseFloat(e.target.value) })
                  }
                  className="w-full accent-cyan-400 h-1.5 bg-slate-800 rounded-lg cursor-pointer"
                />
              </div>

              {/* Catchlight & Eliminación Ojos Rojos */}
              <div className="grid grid-cols-2 gap-2 pt-1">
                <div className="space-y-1">
                  <div className="flex justify-between text-[10px] text-slate-300">
                    <span>Catchlight (Brillo)</span>
                    <span className="font-mono text-cyan-300">
                      {Math.round(currentFace.catchlightIntensity || 0)}%
                    </span>
                  </div>
                  <input
                    type="range"
                    min={0}
                    max={100}
                    value={currentFace.catchlightIntensity || 0}
                    onChange={(e) =>
                      updateFace({ ...currentFace, catchlightIntensity: parseFloat(e.target.value) })
                    }
                    className="w-full accent-cyan-400 h-1 bg-slate-800 rounded-lg cursor-pointer"
                  />
                </div>

                <div className="space-y-1">
                  <div className="flex justify-between text-[10px] text-slate-300">
                    <span>Quitar Ojos Rojos</span>
                    <span className="font-mono text-cyan-300">
                      {Math.round(currentFace.redEyeRemoval || 0)}%
                    </span>
                  </div>
                  <input
                    type="range"
                    min={0}
                    max={100}
                    value={currentFace.redEyeRemoval || 0}
                    onChange={(e) =>
                      updateFace({ ...currentFace, redEyeRemoval: parseFloat(e.target.value) })
                    }
                    className="w-full accent-cyan-400 h-1 bg-slate-800 rounded-lg cursor-pointer"
                  />
                </div>
              </div>
            </div>
          </div>
        )}

        {/* ============================================================ */}
        {/* TAB 4: MAQUILLAJE VIRTUAL COMPLETO (STUDIO MAKEUP) */}
        {/* ============================================================ */}
        {activeTab === 'makeup' && (
          <div className="space-y-3.5">
            {/* Foundation Section */}
            <div className="space-y-2 pb-2 border-b border-slate-800">
              <div className="flex items-center justify-between">
                <span className="text-[11px] font-semibold text-amber-300">Base / Foundation (50+ Tonos)</span>
                <span className="font-mono text-xs text-amber-300">
                  {Math.round(currentMakeup.foundationIntensity * 100)}%
                </span>
              </div>
              <input
                type="range"
                min={0}
                max={1}
                step={0.02}
                value={currentMakeup.foundationIntensity}
                onChange={(e) =>
                  updateMakeup({ ...currentMakeup, foundationIntensity: parseFloat(e.target.value) })
                }
                className="w-full accent-amber-400 h-1.5 bg-slate-800 rounded-lg cursor-pointer"
              />

              {/* Categorías de piel */}
              <div className="flex gap-1 overflow-x-auto pb-1 text-[10px]">
                {(['Fair', 'Light', 'Medium', 'Tan', 'Deep'] as const).map((cat) => (
                  <button
                    key={cat}
                    onClick={() => setSelectedFoundationCat(cat)}
                    className={`px-2 py-0.5 rounded transition-colors whitespace-nowrap ${
                      selectedFoundationCat === cat
                        ? 'bg-amber-500 text-slate-950 font-bold'
                        : 'bg-slate-800 text-slate-400 hover:text-slate-200'
                    }`}
                  >
                    {cat}
                  </button>
                ))}
              </div>

              {/* Muestrario de Tonos de Base */}
              <div className="flex gap-1.5 overflow-x-auto py-1 custom-scrollbar">
                {FOUNDATION_SHADES.filter((s) => s.category === selectedFoundationCat).map((shade) => (
                  <button
                    key={shade.id}
                    onClick={() => updateMakeup({ ...currentMakeup, foundationTone: shade.hex })}
                    className={`flex-shrink-0 w-6 h-6 rounded-full border-2 transition-transform ${
                      currentMakeup.foundationTone === shade.hex
                        ? 'border-white scale-110 shadow-md ring-2 ring-amber-400'
                        : 'border-slate-700 hover:scale-105'
                    }`}
                    style={{ backgroundColor: shade.hex }}
                    title={`${shade.name} (${shade.undertone})`}
                  />
                ))}
              </div>
            </div>

            {/* Lipstick & Gloss Section */}
            <div className="space-y-2 pb-2 border-b border-slate-800">
              <div className="flex items-center justify-between">
                <span className="text-[11px] font-semibold text-rose-300">Lápiz Labial (100+ Tonos)</span>
                <span className="font-mono text-xs text-rose-300">
                  {Math.round(currentMakeup.lipstickIntensity * 100)}%
                </span>
              </div>
              <input
                type="range"
                min={0}
                max={1}
                step={0.02}
                value={currentMakeup.lipstickIntensity}
                onChange={(e) =>
                  updateMakeup({ ...currentMakeup, lipstickIntensity: parseFloat(e.target.value) })
                }
                className="w-full accent-rose-500 h-1.5 bg-slate-800 rounded-lg cursor-pointer"
              />

              {/* Brillo Especular Gloss */}
              <div className="flex items-center justify-between text-[10px] text-slate-300 pt-1">
                <span>Brillo Especular (Lip Gloss)</span>
                <span className="font-mono text-rose-300">
                  {Math.round(currentMakeup.lipstickGloss * 100)}%
                </span>
              </div>
              <input
                type="range"
                min={0}
                max={1}
                step={0.05}
                value={currentMakeup.lipstickGloss}
                onChange={(e) =>
                  updateMakeup({ ...currentMakeup, lipstickGloss: parseFloat(e.target.value) })
                }
                className="w-full accent-rose-400 h-1 bg-slate-800 rounded-lg cursor-pointer"
              />

              {/* Familias de Labial */}
              <div className="flex gap-1 overflow-x-auto pb-1 text-[10px]">
                {LIPSTICK_COLLECTIONS.map((col) => (
                  <button
                    key={col.family}
                    onClick={() => setSelectedLipFamily(col.family)}
                    className={`px-2 py-0.5 rounded transition-colors whitespace-nowrap ${
                      selectedLipFamily === col.family
                        ? 'bg-rose-500 text-white font-bold'
                        : 'bg-slate-800 text-slate-400 hover:text-slate-200'
                    }`}
                  >
                    {col.family.split(' ')[0]}
                  </button>
                ))}
              </div>

              {/* Swatches de Labial */}
              <div className="flex gap-1.5 overflow-x-auto py-1 custom-scrollbar">
                {LIPSTICK_COLLECTIONS.find((c) => c.family === selectedLipFamily)?.items.map((lip) => (
                  <button
                    key={lip.id}
                    onClick={() => updateMakeup({ ...currentMakeup, lipstickColor: lip.hex })}
                    className={`flex-shrink-0 w-6 h-6 rounded-full border-2 transition-transform ${
                      currentMakeup.lipstickColor === lip.hex
                        ? 'border-white scale-110 shadow-md ring-2 ring-rose-400'
                        : 'border-slate-700 hover:scale-105'
                    }`}
                    style={{ backgroundColor: lip.hex }}
                    title={lip.name}
                  />
                ))}
              </div>
            </div>

            {/* Rubor / Blush Section */}
            <div className="space-y-2 pb-2 border-b border-slate-800">
              <div className="flex items-center justify-between">
                <span className="text-[11px] font-semibold text-pink-300">Rubor en Mejillas (Blush)</span>
                <span className="font-mono text-xs text-pink-300">
                  {Math.round(currentMakeup.blushIntensity * 100)}%
                </span>
              </div>
              <input
                type="range"
                min={0}
                max={1}
                step={0.02}
                value={currentMakeup.blushIntensity}
                onChange={(e) =>
                  updateMakeup({ ...currentMakeup, blushIntensity: parseFloat(e.target.value) })
                }
                className="w-full accent-pink-500 h-1.5 bg-slate-800 rounded-lg cursor-pointer"
              />

              <div className="flex gap-1.5 overflow-x-auto py-1 custom-scrollbar">
                {BLUSH_PALETTES.map((blush) => (
                  <button
                    key={blush.id}
                    onClick={() => updateMakeup({ ...currentMakeup, blushColor: blush.hex })}
                    className={`flex-shrink-0 w-6 h-6 rounded-full border-2 transition-transform ${
                      currentMakeup.blushColor === blush.hex
                        ? 'border-white scale-110 shadow-md ring-2 ring-pink-400'
                        : 'border-slate-700 hover:scale-105'
                    }`}
                    style={{ backgroundColor: blush.hex }}
                    title={blush.name}
                  />
                ))}
              </div>
            </div>

            {/* Contorno & Iluminador */}
            <div className="grid grid-cols-2 gap-3 pt-1">
              {/* Contouring */}
              <div className="space-y-1.5">
                <div className="flex justify-between text-[10px] text-slate-300">
                  <span className="font-semibold text-stone-300">Contorno AI</span>
                  <span className="font-mono text-stone-300">
                    {Math.round(currentMakeup.contourIntensity * 100)}%
                  </span>
                </div>
                <input
                  type="range"
                  min={0}
                  max={1}
                  step={0.05}
                  value={currentMakeup.contourIntensity}
                  onChange={(e) =>
                    updateMakeup({ ...currentMakeup, contourIntensity: parseFloat(e.target.value) })
                  }
                  className="w-full accent-stone-400 h-1 bg-slate-800 rounded-lg cursor-pointer"
                />

                <div className="flex gap-1 overflow-x-auto py-0.5 custom-scrollbar">
                  {CONTOURING_PALETTES.slice(0, 8).map((c) => (
                    <button
                      key={c.id}
                      onClick={() => updateMakeup({ ...currentMakeup, contourTone: c.hex })}
                      className="w-4 h-4 rounded-full flex-shrink-0 border border-slate-700"
                      style={{ backgroundColor: c.hex }}
                      title={c.name}
                    />
                  ))}
                </div>
              </div>

              {/* Highlighter */}
              <div className="space-y-1.5">
                <div className="flex justify-between text-[10px] text-slate-300">
                  <span className="font-semibold text-yellow-200">Iluminador Glow</span>
                  <span className="font-mono text-yellow-200">
                    {Math.round(currentMakeup.highlighterIntensity || 0)}%
                  </span>
                </div>
                <input
                  type="range"
                  min={0}
                  max={100}
                  value={currentMakeup.highlighterIntensity || 0}
                  onChange={(e) =>
                    updateMakeup({ ...currentMakeup, highlighterIntensity: parseFloat(e.target.value) })
                  }
                  className="w-full accent-yellow-300 h-1 bg-slate-800 rounded-lg cursor-pointer"
                />

                <div className="flex gap-1 overflow-x-auto py-0.5 custom-scrollbar">
                  {HIGHLIGHTER_PALETTES.slice(0, 8).map((h) => (
                    <button
                      key={h.id}
                      onClick={() => updateMakeup({ ...currentMakeup, highlighterTone: h.hex })}
                      className="w-4 h-4 rounded-full flex-shrink-0 border border-slate-700"
                      style={{ backgroundColor: h.hex }}
                      title={h.name}
                    />
                  ))}
                </div>
              </div>
            </div>

            {/* Ojos y Cejas */}
            <div className="grid grid-cols-2 gap-3 pt-2 border-t border-slate-800">
              <div className="space-y-1">
                <div className="flex justify-between text-[10px] text-slate-300">
                  <span>Delineador (Eyeliner)</span>
                  <span className="font-mono text-indigo-300">
                    {Math.round(currentMakeup.eyelinerIntensity * 100)}%
                  </span>
                </div>
                <input
                  type="range"
                  min={0}
                  max={1}
                  step={0.05}
                  value={currentMakeup.eyelinerIntensity}
                  onChange={(e) =>
                    updateMakeup({ ...currentMakeup, eyelinerIntensity: parseFloat(e.target.value) })
                  }
                  className="w-full accent-indigo-400 h-1 bg-slate-800 rounded-lg cursor-pointer"
                />
              </div>

              <div className="space-y-1">
                <div className="flex justify-between text-[10px] text-slate-300">
                  <span>Máscara de Pestañas</span>
                  <span className="font-mono text-indigo-300">
                    {Math.round(currentMakeup.mascaraIntensity * 100)}%
                  </span>
                </div>
                <input
                  type="range"
                  min={0}
                  max={1}
                  step={0.05}
                  value={currentMakeup.mascaraIntensity}
                  onChange={(e) =>
                    updateMakeup({ ...currentMakeup, mascaraIntensity: parseFloat(e.target.value) })
                  }
                  className="w-full accent-indigo-400 h-1 bg-slate-800 rounded-lg cursor-pointer"
                />
              </div>
            </div>
          </div>
        )}

        {/* ============================================================ */}
        {/* TAB 5: BELLEZA EXTRA (BODY & HAIR) */}
        {/* ============================================================ */}
        {activeTab === 'extra' && (
          <div className="space-y-3">
            <div className="text-[11px] font-semibold text-emerald-300 flex items-center justify-between pb-1 border-b border-slate-800">
              <span>Remodelación Corporal & Cabello</span>
              <span className="text-[10px] text-slate-500">Body Reshaping AI</span>
            </div>

            <div className="space-y-2.5">
              {/* Body Slimming */}
              <div className="space-y-1">
                <div className="flex justify-between text-[11px] text-slate-300">
                  <span>Adelgazar Cintura y Brazos</span>
                  <span className="font-mono text-emerald-300">
                    {Math.round(currentExtra.bodySlimming)}%
                  </span>
                </div>
                <input
                  type="range"
                  min={0}
                  max={100}
                  value={currentExtra.bodySlimming}
                  onChange={(e) =>
                    updateExtra({ ...currentExtra, bodySlimming: parseFloat(e.target.value) })
                  }
                  className="w-full accent-emerald-400 h-1.5 bg-slate-800 rounded-lg cursor-pointer"
                />
              </div>

              {/* Height Adjustment */}
              <div className="space-y-1">
                <div className="flex justify-between text-[11px] text-slate-300">
                  <span>Estilizar Altura / Silueta</span>
                  <span className="font-mono text-emerald-300">
                    {currentExtra.heightAdjustment > 0 ? `+${currentExtra.heightAdjustment}` : currentExtra.heightAdjustment}%
                  </span>
                </div>
                <input
                  type="range"
                  min={-50}
                  max={50}
                  value={currentExtra.heightAdjustment}
                  onChange={(e) =>
                    updateExtra({ ...currentExtra, heightAdjustment: parseFloat(e.target.value) })
                  }
                  className="w-full accent-emerald-400 h-1.5 bg-slate-800 rounded-lg cursor-pointer"
                />
              </div>

              {/* Pecas Orgánicas */}
              <div className="space-y-1">
                <div className="flex justify-between text-[11px] text-slate-300">
                  <span>Pecas Naturales Orgánicas</span>
                  <span className="font-mono text-emerald-300">
                    {Math.round(currentExtra.frecklesIntensity)}%
                  </span>
                </div>
                <input
                  type="range"
                  min={0}
                  max={100}
                  value={currentExtra.frecklesIntensity}
                  onChange={(e) =>
                    updateExtra({ ...currentExtra, frecklesIntensity: parseFloat(e.target.value) })
                  }
                  className="w-full accent-emerald-400 h-1.5 bg-slate-800 rounded-lg cursor-pointer"
                />
              </div>

              {/* Tinte de Cabello & Volumen */}
              <div className="grid grid-cols-2 gap-2 pt-1 border-t border-slate-800">
                <div className="space-y-1">
                  <div className="flex justify-between text-[10px] text-slate-300">
                    <span>Volumen de Cabello</span>
                    <span className="font-mono text-emerald-300">
                      {Math.round(currentExtra.hairVolume)}%
                    </span>
                  </div>
                  <input
                    type="range"
                    min={0}
                    max={100}
                    value={currentExtra.hairVolume}
                    onChange={(e) =>
                      updateExtra({ ...currentExtra, hairVolume: parseFloat(e.target.value) })
                    }
                    className="w-full accent-emerald-400 h-1 bg-slate-800 rounded-lg cursor-pointer"
                  />
                </div>

                <div className="space-y-1">
                  <div className="flex justify-between text-[10px] text-slate-300">
                    <span>Suavizado Corporal</span>
                    <span className="font-mono text-emerald-300">
                      {Math.round(currentExtra.smoothSkinBody || 0)}%
                    </span>
                  </div>
                  <input
                    type="range"
                    min={0}
                    max={100}
                    value={currentExtra.smoothSkinBody || 0}
                    onChange={(e) =>
                      updateExtra({ ...currentExtra, smoothSkinBody: parseFloat(e.target.value) })
                    }
                    className="w-full accent-emerald-400 h-1 bg-slate-800 rounded-lg cursor-pointer"
                  />
                </div>
              </div>
            </div>
          </div>
        )}

        {/* ============================================================ */}
        {/* TAB 6: BIOMETRÍA FACIAL 3D & DIAGNÓSTICO ML KIT */}
        {/* ============================================================ */}
        {activeTab === 'biometrics' && (
          <div className="space-y-3">
            <div className="text-[11px] font-semibold text-fuchsia-300 flex items-center justify-between pb-1 border-b border-slate-800">
              <span>Diagnóstico Biométrico ML Kit</span>
              <span className="text-[10px] text-emerald-400 font-mono">468 Puntos OK</span>
            </div>

            <div className="grid grid-cols-2 gap-2 text-xs">
              <div className="bg-slate-800/80 p-2 rounded-lg border border-slate-700/60">
                <div className="text-[10px] text-slate-400">Género Estimado</div>
                <div className="font-semibold text-slate-100">{faceAnalysis.gender}</div>
              </div>

              <div className="bg-slate-800/80 p-2 rounded-lg border border-slate-700/60">
                <div className="text-[10px] text-slate-400">Edad Estimada</div>
                <div className="font-semibold text-slate-100">{faceAnalysis.estimatedAge} años</div>
              </div>

              <div className="bg-slate-800/80 p-2 rounded-lg border border-slate-700/60">
                <div className="text-[10px] text-slate-400">Subtono de Piel</div>
                <div className="font-semibold text-slate-100">{faceAnalysis.skinToneUndertone}</div>
              </div>

              <div className="bg-slate-800/80 p-2 rounded-lg border border-slate-700/60">
                <div className="text-[10px] text-slate-400">Forma Facial</div>
                <div className="font-semibold text-slate-100">{faceAnalysis.faceShape}</div>
              </div>

              <div className="bg-slate-800/80 p-2 rounded-lg border border-slate-700/60">
                <div className="text-[10px] text-slate-400">Ratio Áureo Facial</div>
                <div className="font-semibold text-amber-300 font-mono">
                  {faceAnalysis.goldenRatioScore}%
                </div>
              </div>

              <div className="bg-slate-800/80 p-2 rounded-lg border border-slate-700/60">
                <div className="text-[10px] text-slate-400">Simetría Ocular</div>
                <div className="font-semibold text-sky-300 font-mono">
                  {faceAnalysis.symmetryScore}%
                </div>
              </div>
            </div>

            <div className="bg-gradient-to-r from-pink-950/40 to-purple-950/40 p-2.5 rounded-lg border border-pink-500/20 text-[11px] text-slate-300">
              <div className="flex items-center gap-1.5 font-semibold text-pink-300 pb-1">
                <Sparkles className="w-3.5 h-3.5" />
                <span>Recomendación Personalizada AI</span>
              </div>
              <div>
                Recomendamos base en tono natural <span className="text-amber-300 font-mono">Ivory 110</span>{' '}
                con un toque suave de rubor satinado en pómulos y un 25% de estrechamiento sutil mandibular para acentuar el arco áureo sin alterar tu expresión.
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
