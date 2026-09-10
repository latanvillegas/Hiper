import React from 'react';
import { OpticalEffectsSettings } from '../types';
import { Sparkles, Sun, Film, Camera, RotateCcw } from 'lucide-react';

interface EffectsPanelProps {
  effects: OpticalEffectsSettings;
  onChange: (effects: OpticalEffectsSettings) => void;
  onReset: () => void;
}

export const EffectsPanel: React.FC<EffectsPanelProps> = ({ effects, onChange, onReset }) => {
  return (
    <div className="flex flex-col gap-3">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Sparkles className="w-4 h-4 text-amber-400" />
          <span className="text-xs font-semibold text-slate-200">Efectos Ópticos & Analógicos</span>
        </div>
        <button
          onClick={onReset}
          className="text-[11px] text-slate-400 hover:text-slate-200 flex items-center gap-1"
        >
          <RotateCcw className="w-3 h-3" /> Reset
        </button>
      </div>

      <div className="bg-slate-900/80 rounded-xl p-3 border border-slate-800/80 space-y-3.5">
        {/* Halation (Kodak 2383 Spectral Glow) */}
        <div className="space-y-1.5">
          <div className="flex items-center justify-between text-[11px]">
            <span className="text-slate-300 font-medium flex items-center gap-1.5">
              <Sun className="w-3.5 h-3.5 text-red-400" /> Halation Analógico (Cine 2383)
            </span>
            <span className="font-mono text-slate-300">{Math.round(effects.halationIntensity * 100)}%</span>
          </div>
          <input
            type="range"
            min={0}
            max={1}
            step={0.02}
            value={effects.halationIntensity}
            onChange={(e) => onChange({ ...effects, halationIntensity: parseFloat(e.target.value) })}
            className="w-full accent-red-500 h-1.5 bg-slate-800 rounded-lg cursor-pointer"
          />
          <div className="flex justify-between text-[10px] text-slate-500">
            <span>Umbral de altas luces: {Math.round(effects.halationThreshold * 100)}%</span>
            <button
              onClick={() => onChange({ ...effects, halationThreshold: effects.halationThreshold === 0.65 ? 0.8 : 0.65 })}
              className="hover:text-slate-400 underline"
            >
              {effects.halationThreshold < 0.75 ? 'Sensible' : 'Estricto'}
            </button>
          </div>
        </div>

        {/* Bloom */}
        <div className="space-y-1.5">
          <div className="flex items-center justify-between text-[11px]">
            <span className="text-slate-300 font-medium flex items-center gap-1.5">
              <Sparkles className="w-3.5 h-3.5 text-amber-300" /> Bloom Óptico (Difusión de Lente)
            </span>
            <span className="font-mono text-slate-300">{Math.round(effects.bloomIntensity * 100)}%</span>
          </div>
          <input
            type="range"
            min={0}
            max={1}
            step={0.02}
            value={effects.bloomIntensity}
            onChange={(e) => onChange({ ...effects, bloomIntensity: parseFloat(e.target.value) })}
            className="w-full accent-amber-400 h-1.5 bg-slate-800 rounded-lg cursor-pointer"
          />
        </div>

        {/* Film Grain */}
        <div className="space-y-1.5">
          <div className="flex items-center justify-between text-[11px]">
            <span className="text-slate-300 font-medium flex items-center gap-1.5">
              <Film className="w-3.5 h-3.5 text-slate-300" /> Grano de Película (Haluros de Plata)
            </span>
            <span className="font-mono text-slate-300">{Math.round(effects.filmGrainAmount * 100)}%</span>
          </div>
          <input
            type="range"
            min={0}
            max={1}
            step={0.02}
            value={effects.filmGrainAmount}
            onChange={(e) => onChange({ ...effects, filmGrainAmount: parseFloat(e.target.value) })}
            className="w-full accent-slate-300 h-1.5 bg-slate-800 rounded-lg cursor-pointer"
          />
          <div className="flex items-center justify-between text-[10px] text-slate-400">
            <span>Tamaño grano: {effects.filmGrainSize.toFixed(1)} px</span>
            <div className="flex gap-1">
              {[1.0, 1.8, 2.5, 3.2].map((sz) => (
                <button
                  key={sz}
                  onClick={() => onChange({ ...effects, filmGrainSize: sz })}
                  className={`px-1.5 py-0.5 rounded ${
                    Math.abs(effects.filmGrainSize - sz) < 0.2
                      ? 'bg-slate-700 text-white'
                      : 'text-slate-500 hover:text-slate-300'
                  }`}
                >
                  {sz < 2 ? 'Fino' : sz < 3 ? 'Medio' : 'Grueso'}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Lens Blur con Bokeh Real */}
        <div className="space-y-1.5">
          <div className="flex items-center justify-between text-[11px]">
            <span className="text-slate-300 font-medium flex items-center gap-1.5">
              <Camera className="w-3.5 h-3.5 text-sky-400" /> Lens Blur & Bokeh Real
            </span>
            <span className="font-mono text-slate-300">{effects.lensBlurRadius.toFixed(1)} px</span>
          </div>
          <input
            type="range"
            min={0}
            max={24}
            step={0.5}
            value={effects.lensBlurRadius}
            onChange={(e) => onChange({ ...effects, lensBlurRadius: parseFloat(e.target.value) })}
            className="w-full accent-sky-400 h-1.5 bg-slate-800 rounded-lg cursor-pointer"
          />

          {/* Aperture blades selector */}
          <div className="flex items-center justify-between text-[10px] text-slate-400 pt-1">
            <span>Apertura del diafragma:</span>
            <div className="flex gap-1">
              {[
                { val: 0, label: 'Circular' },
                { val: 6, label: 'Hex (6)' },
                { val: 8, label: 'Oct (8)' },
              ].map((blade) => (
                <button
                  key={blade.val}
                  onClick={() => onChange({ ...effects, lensBlurBlades: blade.val })}
                  className={`px-1.5 py-0.5 rounded ${
                    effects.lensBlurBlades === blade.val
                      ? 'bg-sky-900 border border-sky-600 text-sky-200'
                      : 'bg-slate-800 text-slate-400 hover:text-slate-200'
                  }`}
                >
                  {blade.label}
                </button>
              ))}
            </div>
          </div>

          {/* Specular Boost */}
          <div className="space-y-1 pt-1">
            <div className="flex justify-between text-[10px] text-slate-400">
              <span>Realce de destellos especulares (Bokeh Balls)</span>
              <span className="font-mono text-slate-300">{effects.lensBlurSpecularBoost.toFixed(1)}x</span>
            </div>
            <input
              type="range"
              min={0}
              max={3}
              step={0.1}
              value={effects.lensBlurSpecularBoost}
              onChange={(e) => onChange({ ...effects, lensBlurSpecularBoost: parseFloat(e.target.value) })}
              className="w-full accent-sky-400 h-1 bg-slate-800 rounded-lg cursor-pointer"
            />
          </div>
        </div>
      </div>
    </div>
  );
};
