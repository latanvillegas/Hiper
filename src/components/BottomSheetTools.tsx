import React, { useState } from 'react';
import {
  BasicAdjustments,
  TextOverlay,
  LayerItem,
  OpticalEffectsSettings,
  FaceRetouchSettings,
  VirtualMakeupSettings,
} from '../types';
import { ProfessionalSlider } from './ProfessionalSlider';
import {
  Sliders,
  Brush,
  Smile,
  Sparkles,
  Layers,
  Type,
  ChevronDown,
  ChevronUp,
  RotateCcw,
  Plus,
  Trash2,
} from 'lucide-react';

export type BottomSheetCategory = 'adjustments' | 'selective' | 'beauty' | 'filters' | 'layers' | 'text';

export interface BottomSheetToolsProps {
  adjustments: BasicAdjustments;
  onAdjustmentsChange: (adj: BasicAdjustments) => void;
  textOverlays: TextOverlay[];
  onTextOverlaysChange: (texts: TextOverlay[]) => void;
  activeCategory: BottomSheetCategory;
  onCategoryChange: (cat: BottomSheetCategory) => void;
  isExpanded: boolean;
  onToggleExpand: () => void;
  // Sub-panel triggers or direct controls
  onOpenMaskTools?: () => void;
  onOpenFaceTools?: () => void;
  onOpenEffectsTools?: () => void;
  onOpenLayersTools?: () => void;
  onResetCategory?: () => void;
}

export const BottomSheetTools: React.FC<BottomSheetToolsProps> = ({
  adjustments,
  onAdjustmentsChange,
  textOverlays,
  onTextOverlaysChange,
  activeCategory,
  onCategoryChange,
  isExpanded,
  onToggleExpand,
}) => {
  const [newText, setNewText] = useState('PhotoEngine Pro');

  const CATEGORIES = [
    { id: 'adjustments', label: 'Ajustes', icon: Sliders },
    { id: 'selective', label: 'Selectivo', icon: Brush },
    { id: 'beauty', label: 'Belleza', icon: Smile },
    { id: 'filters', label: 'Filtros', icon: Sparkles },
    { id: 'layers', label: 'Capas', icon: Layers },
    { id: 'text', label: 'Texto', icon: Type },
  ];

  const resetAdjustments = () => {
    onAdjustmentsChange({
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
    });
  };

  const addTextOverlay = () => {
    if (!newText.trim()) return;
    const newOverlay: TextOverlay = {
      id: Date.now().toString(),
      text: newText,
      fontFamily: 'sans-serif',
      fontSize: 28,
      color: '#ffffff',
      opacity: 0.9,
      x: 50,
      y: 85,
      bold: true,
      italic: false,
      shadow: true,
    };
    onTextOverlaysChange([...textOverlays, newOverlay]);
    setNewText('');
  };

  const removeTextOverlay = (id: string) => {
    onTextOverlaysChange(textOverlays.filter((t) => t.id !== id));
  };

  return (
    <div className="w-full bg-neutral-900/95 backdrop-blur-md border-t border-neutral-800 shadow-2xl flex flex-col z-20 transition-all duration-300">
      {/* Draggable Header & Category Tabs (Material Design 3) */}
      <div className="flex items-center justify-between px-3 py-1.5 border-b border-neutral-800/80">
        {/* Category Pill Tabs */}
        <div className="flex items-center gap-1 overflow-x-auto scrollbar-none py-0.5">
          {CATEGORIES.map((cat) => {
            const Icon = cat.icon;
            const isSelected = activeCategory === cat.id;
            return (
              <button
                key={cat.id}
                onClick={() => {
                  onCategoryChange(cat.id);
                  if (!isExpanded) onToggleExpand();
                }}
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap transition-all ${
                  isSelected
                    ? 'bg-sky-500/20 text-sky-400 border border-sky-500/40 shadow-sm'
                    : 'text-neutral-400 hover:text-neutral-200 hover:bg-neutral-800/60'
                }`}
              >
                <Icon className={`w-3.5 h-3.5 ${isSelected ? 'text-sky-400' : 'text-neutral-400'}`} />
                <span>{cat.label}</span>
              </button>
            );
          })}
        </div>

        {/* Expand / Collapse Drawer Toggle */}
        <button
          onClick={onToggleExpand}
          className="p-1.5 rounded-lg text-neutral-400 hover:text-neutral-200 hover:bg-neutral-800 transition-colors ml-2"
          title={isExpanded ? 'Colapsar panel inferior' : 'Expandir panel inferior'}
        >
          {isExpanded ? <ChevronDown className="w-4 h-4" /> : <ChevronUp className="w-4 h-4" />}
        </button>
      </div>

      {/* Expandable Body */}
      {isExpanded && (
        <div className="h-56 overflow-y-auto p-3 bg-neutral-950/70 scrollbar-thin">
          {/* TAB 1: AJUSTES BASICOS */}
          {activeCategory === 'adjustments' && (
            <div className="flex flex-col gap-2 max-w-4xl mx-auto">
              <div className="flex items-center justify-between mb-1">
                <span className="text-xs font-bold text-neutral-200 uppercase tracking-wider">
                  Ajustes Tonal & Colorimetría
                </span>
                <button
                  onClick={resetAdjustments}
                  className="flex items-center gap-1 px-2 py-0.5 rounded text-[11px] text-neutral-400 hover:text-white"
                >
                  <RotateCcw className="w-3 h-3" /> Reset Todos
                </button>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-x-4 gap-y-1">
                <ProfessionalSlider
                  label="Exposición (EV)"
                  value={parseFloat(adjustments.exposure.toFixed(2))}
                  min={-3.0}
                  max={3.0}
                  step={0.05}
                  unit=" EV"
                  defaultValue={0}
                  onChange={(val) => onAdjustmentsChange({ ...adjustments, exposure: val })}
                />
                <ProfessionalSlider
                  label="Contraste"
                  value={adjustments.contrast}
                  min={-100}
                  max={100}
                  defaultValue={0}
                  onChange={(val) => onAdjustmentsChange({ ...adjustments, contrast: val })}
                />
                <ProfessionalSlider
                  label="Altas Luces"
                  value={adjustments.highlights}
                  min={-100}
                  max={100}
                  defaultValue={0}
                  onChange={(val) => onAdjustmentsChange({ ...adjustments, highlights: val })}
                />
                <ProfessionalSlider
                  label="Sombras"
                  value={adjustments.shadows}
                  min={-100}
                  max={100}
                  defaultValue={0}
                  onChange={(val) => onAdjustmentsChange({ ...adjustments, shadows: val })}
                />
                <ProfessionalSlider
                  label="Blancos"
                  value={adjustments.whites}
                  min={-100}
                  max={100}
                  defaultValue={0}
                  onChange={(val) => onAdjustmentsChange({ ...adjustments, whites: val })}
                />
                <ProfessionalSlider
                  label="Negros"
                  value={adjustments.blacks}
                  min={-100}
                  max={100}
                  defaultValue={0}
                  onChange={(val) => onAdjustmentsChange({ ...adjustments, blacks: val })}
                />
                <ProfessionalSlider
                  label="Temperatura (WB)"
                  value={adjustments.temperature}
                  min={-100}
                  max={100}
                  defaultValue={0}
                  accentColor={adjustments.temperature > 0 ? 'text-amber-400' : 'text-sky-400'}
                  onChange={(val) => onAdjustmentsChange({ ...adjustments, temperature: val })}
                />
                <ProfessionalSlider
                  label="Tinte"
                  value={adjustments.tint}
                  min={-100}
                  max={100}
                  defaultValue={0}
                  accentColor={adjustments.tint > 0 ? 'text-rose-400' : 'text-emerald-400'}
                  onChange={(val) => onAdjustmentsChange({ ...adjustments, tint: val })}
                />
                <ProfessionalSlider
                  label="Claridad"
                  value={adjustments.clarity}
                  min={-100}
                  max={100}
                  defaultValue={0}
                  onChange={(val) => onAdjustmentsChange({ ...adjustments, clarity: val })}
                />
                <ProfessionalSlider
                  label="Dehaze (Neblina)"
                  value={adjustments.dehaze}
                  min={-100}
                  max={100}
                  defaultValue={0}
                  onChange={(val) => onAdjustmentsChange({ ...adjustments, dehaze: val })}
                />
                <ProfessionalSlider
                  label="Intensidad (Vibrance)"
                  value={adjustments.vibrance}
                  min={-100}
                  max={100}
                  defaultValue={0}
                  onChange={(val) => onAdjustmentsChange({ ...adjustments, vibrance: val })}
                />
                <ProfessionalSlider
                  label="Saturación"
                  value={adjustments.saturation}
                  min={-100}
                  max={100}
                  defaultValue={0}
                  onChange={(val) => onAdjustmentsChange({ ...adjustments, saturation: val })}
                />
              </div>
            </div>
          )}

          {/* TAB 6: TEXTO Y TIPOGRAFÍA */}
          {activeCategory === 'text' && (
            <div className="flex flex-col gap-3 max-w-2xl mx-auto">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-neutral-200 uppercase tracking-wider">
                  Superposición de Texto & Marca de Agua
                </span>
                <span className="text-[11px] text-neutral-400 font-mono">
                  {textOverlays.length} elementos
                </span>
              </div>

              {/* Add Text Input */}
              <div className="flex items-center gap-2">
                <input
                  type="text"
                  value={newText}
                  onChange={(e) => setNewText(e.target.value)}
                  placeholder="Escribe texto o copyright..."
                  className="flex-1 px-3 py-1.5 rounded-xl bg-neutral-900 border border-neutral-700 text-xs text-neutral-200 outline-none focus:border-sky-500"
                />
                <button
                  onClick={addTextOverlay}
                  className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-sky-600 hover:bg-sky-500 text-white text-xs font-semibold shadow-md transition-colors"
                >
                  <Plus className="w-3.5 h-3.5" />
                  <span>Añadir Texto</span>
                </button>
              </div>

              {/* Text Overlays list */}
              <div className="space-y-2">
                {textOverlays.map((item) => (
                  <div
                    key={item.id}
                    className="flex items-center justify-between p-2 rounded-xl bg-neutral-900 border border-neutral-800"
                  >
                    <div className="flex items-center gap-2 flex-1 mr-2">
                      <span className="font-semibold text-xs text-neutral-200 truncate">
                        "{item.text}"
                      </span>
                      <span className="text-[10px] text-neutral-400 font-mono">
                        {item.fontSize}px • {item.fontFamily}
                      </span>
                    </div>

                    <div className="flex items-center gap-2">
                      <input
                        type="color"
                        value={item.color}
                        onChange={(e) =>
                          onTextOverlaysChange(
                            textOverlays.map((t) =>
                              t.id === item.id ? { ...t, color: e.target.value } : t
                            )
                          )
                        }
                        className="w-5 h-5 rounded border border-neutral-700 cursor-pointer"
                      />
                      <button
                        onClick={() => removeTextOverlay(item.id)}
                        className="p-1 text-neutral-500 hover:text-rose-400 transition-colors"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* OTHER TABS HINTS (Opened in main right drawer) */}
          {(activeCategory === 'selective' ||
            activeCategory === 'beauty' ||
            activeCategory === 'filters' ||
            activeCategory === 'layers') && (
            <div className="flex flex-col items-center justify-center h-full text-center py-4">
              <p className="text-xs text-neutral-300 font-medium mb-1">
                Herramientas de {CATEGORIES.find((c) => c.id === activeCategory)?.label} activas
              </p>
              <p className="text-[11px] text-neutral-500 max-w-md">
                Interactúa con los paneles en el lateral derecho o utiliza las herramientas táctiles en el lienzo directamente.
              </p>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
