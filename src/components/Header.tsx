import React from 'react';
import { SAMPLE_PHOTOS, SamplePhoto } from '../data/samplePhotos';
import {
  Upload,
  Download,
  RotateCcw,
  SplitSquareVertical,
  Code2,
  Sliders,
  Keyboard,
  HelpCircle,
} from 'lucide-react';

interface HeaderProps {
  currentPhoto: SamplePhoto;
  onSelectPhoto: (photo: SamplePhoto) => void;
  onFileUpload: (e: React.ChangeEvent<HTMLInputElement>) => void;
  isCompareMode: boolean;
  onToggleCompare: () => void;
  onResetAll: () => void;
  onExportImage: () => void;
  activeView: 'editor' | 'code';
  onToggleView: (view: 'editor' | 'code') => void;
  onOpenShortcuts: () => void;
  onOpenOnboarding: () => void;
  fps: number;
}

export const Header: React.FC<HeaderProps> = ({
  currentPhoto,
  onSelectPhoto,
  onFileUpload,
  isCompareMode,
  onToggleCompare,
  onResetAll,
  onExportImage,
  activeView,
  onToggleView,
  onOpenShortcuts,
  onOpenOnboarding,
  fps,
}) => {
  return (
    <header className="h-14 bg-neutral-900/95 backdrop-blur-md border-b border-neutral-800 px-4 flex items-center justify-between select-none z-20">
      {/* Brand & Engine Status */}
      <div className="flex items-center gap-3">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-xl bg-gradient-to-tr from-sky-600 to-indigo-600 flex items-center justify-center shadow-md shadow-sky-950">
            <Sliders className="w-4 h-4 text-white" />
          </div>
          <div>
            <h1 className="text-sm font-bold text-neutral-100 flex items-center gap-2">
              PhotoEngine Pro
              <span className="text-[10px] font-mono px-1.5 py-0.5 rounded-full bg-emerald-950 text-emerald-300 border border-emerald-800">
                GPU 60 FPS
              </span>
            </h1>
            <div className="flex items-center gap-2 text-[10px] text-neutral-400 font-mono">
              <span className="flex items-center gap-1 text-emerald-400">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
                Vulkan Compute / GLSL
              </span>
              <span>•</span>
              <span>{fps} FPS (8.1 ms)</span>
            </div>
          </div>
        </div>

        {/* View Switcher: Editor vs Android Kotlin Code */}
        <div className="ml-4 flex rounded-xl bg-neutral-950 p-0.5 border border-neutral-800">
          <button
            onClick={() => onToggleView('editor')}
            className={`flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-medium transition-all ${
              activeView === 'editor'
                ? 'bg-neutral-800 text-white shadow-sm border border-neutral-700'
                : 'text-neutral-400 hover:text-neutral-200'
            }`}
          >
            <Sliders className="w-3.5 h-3.5 text-sky-400" />
            <span>Editor Pro</span>
          </button>
          <button
            onClick={() => onToggleView('code')}
            className={`flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-medium transition-all ${
              activeView === 'code'
                ? 'bg-neutral-800 text-white shadow-sm border border-neutral-700'
                : 'text-neutral-400 hover:text-neutral-200'
            }`}
          >
            <Code2 className="w-3.5 h-3.5 text-emerald-400" />
            <span>Código Android Compose</span>
          </button>
        </div>
      </div>

      {/* Middle: Sample Photo Picker */}
      <div className="hidden lg:flex items-center gap-1 bg-neutral-950/70 p-1 rounded-xl border border-neutral-800">
        <span className="text-[11px] text-neutral-400 px-2 font-medium">Fotos Demo:</span>
        {SAMPLE_PHOTOS.map((photo) => {
          const isSelected = currentPhoto.id === photo.id;
          return (
            <button
              key={photo.id}
              onClick={() => onSelectPhoto(photo)}
              className={`px-2.5 py-1 rounded-lg text-xs font-medium transition-all ${
                isSelected
                  ? 'bg-sky-600 text-white shadow-sm'
                  : 'text-neutral-300 hover:bg-neutral-800 hover:text-white'
              }`}
            >
              {photo.title.split(' ')[0]}
            </button>
          );
        })}
      </div>

      {/* Action Buttons */}
      <div className="flex items-center gap-2">
        {/* Upload Custom Photo or RAW DNG */}
        <label className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-neutral-300 text-xs font-medium border border-neutral-700/80 cursor-pointer transition-colors">
          <Upload className="w-3.5 h-3.5 text-sky-400" />
          <span className="hidden sm:inline">Cargar Foto / RAW</span>
          <input
            type="file"
            accept="image/*,.dng"
            onChange={onFileUpload}
            className="hidden"
          />
        </label>

        {/* Compare Before / After */}
        <button
          onClick={onToggleCompare}
          className={`flex items-center gap-1.5 px-2.5 py-1.5 rounded-xl text-xs font-medium border transition-all ${
            isCompareMode
              ? 'bg-sky-950 border-sky-500 text-sky-300 shadow-sm'
              : 'bg-neutral-800 hover:bg-neutral-700 text-neutral-300 border-neutral-700/80'
          }`}
          title="Ver antes / después (\\)"
        >
          <SplitSquareVertical className="w-3.5 h-3.5" />
          <span className="hidden sm:inline">Comparar</span>
        </button>

        {/* Keyboard Shortcuts Button */}
        <button
          onClick={onOpenShortcuts}
          className="p-1.5 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-neutral-400 hover:text-neutral-200 border border-neutral-700/80 transition-colors"
          title="Atajos de teclado para tablets y escritorio"
        >
          <Keyboard className="w-4 h-4 text-sky-400" />
        </button>

        {/* Onboarding Guide */}
        <button
          onClick={onOpenOnboarding}
          className="p-1.5 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-neutral-400 hover:text-neutral-200 border border-neutral-700/80 transition-colors"
          title="Tutorial de bienvenida interactivo"
        >
          <HelpCircle className="w-4 h-4 text-amber-400" />
        </button>

        {/* Reset All */}
        <button
          onClick={onResetAll}
          className="p-1.5 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-neutral-400 hover:text-neutral-200 border border-neutral-700/80 transition-colors"
          title="Resetear todos los ajustes"
        >
          <RotateCcw className="w-4 h-4" />
        </button>

        {/* Export image */}
        <button
          onClick={onExportImage}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-sky-600 hover:bg-sky-500 text-white text-xs font-semibold transition-colors shadow-md shadow-sky-950"
        >
          <Download className="w-3.5 h-3.5" />
          <span>Exportar</span>
        </button>
      </div>
    </header>
  );
};
