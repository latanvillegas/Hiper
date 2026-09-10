import React from 'react';
import { PerspectiveSettings } from '../types';
import { Compass, RotateCw, RefreshCw, Grid, Wand2 } from 'lucide-react';

interface PerspectivePanelProps {
  perspective: PerspectiveSettings;
  onChange: (settings: PerspectiveSettings) => void;
  onReset: () => void;
}

export const PerspectivePanel: React.FC<PerspectivePanelProps> = ({
  perspective,
  onChange,
  onReset,
}) => {
  const autoStraighten = () => {
    // Automatic Hough line / vanishing point estimation simulation
    onChange({
      verticalKeystone: -4.5,
      horizontalKeystone: 2.1,
      rotation: -1.2,
      scale: 1.06,
    });
  };

  return (
    <div className="flex flex-col gap-3">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Compass className="w-4 h-4 text-emerald-400" />
          <span className="text-xs font-semibold text-slate-200">Geometría & Perspectiva</span>
        </div>
        <button
          onClick={autoStraighten}
          className="px-2 py-0.5 rounded text-[11px] font-medium bg-emerald-950 border border-emerald-600 text-emerald-300 hover:bg-emerald-900 transition-colors flex items-center gap-1"
        >
          <Wand2 className="w-3 h-3" /> Auto-Enderezar
        </button>
      </div>

      <div className="bg-slate-900/80 rounded-xl p-3 border border-slate-800/80 space-y-3.5">
        {/* Vertical Keystone */}
        <div className="space-y-1">
          <div className="flex justify-between text-[11px] text-slate-400">
            <span>Keystone Vertical (Inclinación)</span>
            <span className="font-mono text-slate-300">{perspective.verticalKeystone.toFixed(1)}°</span>
          </div>
          <input
            type="range"
            min={-30}
            max={30}
            step={0.5}
            value={perspective.verticalKeystone}
            onChange={(e) => onChange({ ...perspective, verticalKeystone: parseFloat(e.target.value) })}
            className="w-full accent-emerald-400 h-1.5 bg-slate-800 rounded-lg cursor-pointer"
          />
        </div>

        {/* Horizontal Keystone */}
        <div className="space-y-1">
          <div className="flex justify-between text-[11px] text-slate-400">
            <span>Keystone Horizontal (Ángulo Lateral)</span>
            <span className="font-mono text-slate-300">{perspective.horizontalKeystone.toFixed(1)}°</span>
          </div>
          <input
            type="range"
            min={-30}
            max={30}
            step={0.5}
            value={perspective.horizontalKeystone}
            onChange={(e) => onChange({ ...perspective, horizontalKeystone: parseFloat(e.target.value) })}
            className="w-full accent-emerald-400 h-1.5 bg-slate-800 rounded-lg cursor-pointer"
          />
        </div>

        {/* Rotation */}
        <div className="space-y-1">
          <div className="flex justify-between text-[11px] text-slate-400">
            <span>Rotación (Nivelación de Horizonte)</span>
            <span className="font-mono text-slate-300">{perspective.rotation.toFixed(1)}°</span>
          </div>
          <input
            type="range"
            min={-45}
            max={45}
            step={0.2}
            value={perspective.rotation}
            onChange={(e) => onChange({ ...perspective, rotation: parseFloat(e.target.value) })}
            className="w-full accent-emerald-400 h-1.5 bg-slate-800 rounded-lg cursor-pointer"
          />
        </div>

        {/* Scale (Recorte para evitar bordes vacíos) */}
        <div className="space-y-1">
          <div className="flex justify-between text-[11px] text-slate-400">
            <span>Escalado / Compensación de Bordes</span>
            <span className="font-mono text-slate-300">{perspective.scale.toFixed(2)}x</span>
          </div>
          <input
            type="range"
            min={0.8}
            max={1.5}
            step={0.02}
            value={perspective.scale}
            onChange={(e) => onChange({ ...perspective, scale: parseFloat(e.target.value) })}
            className="w-full accent-emerald-400 h-1.5 bg-slate-800 rounded-lg cursor-pointer"
          />
        </div>

        <button
          onClick={onReset}
          className="w-full py-1 text-xs text-slate-400 hover:text-slate-200 border border-slate-700/60 rounded-lg hover:bg-slate-800 transition-colors"
        >
          Resetear Perspectiva
        </button>
      </div>
    </div>
  );
};
