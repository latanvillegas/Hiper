import React, { useState } from 'react';
import { Brush, Sparkles, Sun, Droplets, Trees, User, CircleDot, Bandage, Shield } from 'lucide-react';

export type MaskMode = 'none' | 'brush' | 'linear' | 'radial' | 'healing' | 'ai_subject' | 'ai_sky' | 'ai_water' | 'ai_veg' | 'ai_skin';

interface MaskToolsPanelProps {
  currentMode: MaskMode;
  onModeChange: (mode: MaskMode) => void;
  brushRadius: number;
  brushFeather: number;
  onBrushParamsChange: (radius: number, feather: number) => void;
  onRunAiSegmentation: (category: string) => void;
  isProcessingAi: boolean;
}

export const MaskToolsPanel: React.FC<MaskToolsPanelProps> = ({
  currentMode,
  onModeChange,
  brushRadius,
  brushFeather,
  onBrushParamsChange,
  onRunAiSegmentation,
  isProcessingAi,
}) => {
  return (
    <div className="flex flex-col gap-3">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Brush className="w-4 h-4 text-cyan-400" />
          <span className="text-xs font-semibold text-slate-200">Máscaras AI & Retoque Local</span>
        </div>
        {currentMode !== 'none' && (
          <button
            onClick={() => onModeChange('none')}
            className="text-[11px] text-slate-400 hover:text-slate-200"
          >
            Desactivar
          </button>
        )}
      </div>

      {/* AI Smart Segmenter Categories (ML Kit Segmenter) */}
      <div className="space-y-1.5">
        <span className="text-[10px] font-semibold text-slate-400 uppercase tracking-wider">
          Segmentación AI (ML Kit Neural)
        </span>
        <div className="grid grid-cols-5 gap-1.5">
          {[
            { id: 'ai_subject', label: 'Sujeto', icon: User, color: 'text-indigo-400' },
            { id: 'ai_sky', label: 'Cielo', icon: Sun, color: 'text-sky-400' },
            { id: 'ai_water', label: 'Agua', icon: Droplets, color: 'text-cyan-400' },
            { id: 'ai_veg', label: 'Vegetación', icon: Trees, color: 'text-emerald-400' },
            { id: 'ai_skin', label: 'Piel', icon: Shield, color: 'text-amber-400' },
          ].map((item) => {
            const Icon = item.icon;
            const isActive = currentMode === item.id;
            return (
              <button
                key={item.id}
                onClick={() => {
                  onModeChange(item.id as MaskMode);
                  onRunAiSegmentation(item.label);
                }}
                disabled={isProcessingAi}
                className={`flex flex-col items-center justify-center p-2 rounded-lg border text-center transition-all ${
                  isActive
                    ? 'bg-sky-950 border-sky-500 shadow-sm'
                    : 'bg-slate-900/60 border-slate-800 hover:bg-slate-800 text-slate-400'
                }`}
              >
                <Icon className={`w-4 h-4 mb-1 ${item.color}`} />
                <span className="text-[10px] font-medium leading-tight">{item.label}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Manual Local Tools */}
      <div className="space-y-1.5 pt-1">
        <span className="text-[10px] font-semibold text-slate-400 uppercase tracking-wider">
          Herramientas Manuales & Healing
        </span>
        <div className="grid grid-cols-4 gap-1.5">
          {[
            { id: 'brush', label: 'Pincel', icon: Brush },
            { id: 'linear', label: 'Grad. Lineal', icon: CircleDot },
            { id: 'radial', label: 'Grad. Radial', icon: CircleDot },
            { id: 'healing', label: 'Healing Brush', icon: Bandage },
          ].map((tool) => {
            const Icon = tool.icon;
            const isActive = currentMode === tool.id;
            return (
              <button
                key={tool.id}
                onClick={() => onModeChange(tool.id as MaskMode)}
                className={`flex flex-col items-center justify-center p-2 rounded-lg border text-center transition-all ${
                  isActive
                    ? 'bg-sky-950 border-sky-500 text-sky-200'
                    : 'bg-slate-900/60 border-slate-800 hover:bg-slate-800 text-slate-400'
                }`}
              >
                <Icon className="w-4 h-4 mb-1 text-slate-300" />
                <span className="text-[10px] font-medium leading-tight">{tool.label}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Tool Parameters (Size & Feather) */}
      {(currentMode === 'brush' || currentMode === 'radial' || currentMode === 'linear' || currentMode === 'healing') && (
        <div className="bg-slate-900/80 rounded-xl p-3 border border-slate-800/80 space-y-2.5">
          <div className="space-y-1">
            <div className="flex justify-between text-[11px] text-slate-400">
              <span>Tamaño / Radio</span>
              <span className="font-mono text-slate-300">{brushRadius} px</span>
            </div>
            <input
              type="range"
              min={10}
              max={150}
              step={2}
              value={brushRadius}
              onChange={(e) => onBrushParamsChange(parseInt(e.target.value), brushFeather)}
              className="w-full accent-cyan-400 h-1.5 bg-slate-800 rounded-lg cursor-pointer"
            />
          </div>

          <div className="space-y-1">
            <div className="flex justify-between text-[11px] text-slate-400">
              <span>Difuminado (Feather)</span>
              <span className="font-mono text-slate-300">{Math.round(brushFeather * 100)}%</span>
            </div>
            <input
              type="range"
              min={0}
              max={1}
              step={0.05}
              value={brushFeather}
              onChange={(e) => onBrushParamsChange(brushRadius, parseFloat(e.target.value))}
              className="w-full accent-cyan-400 h-1.5 bg-slate-800 rounded-lg cursor-pointer"
            />
          </div>

          {currentMode === 'healing' && (
            <div className="text-[11px] text-emerald-400/90 bg-emerald-950/40 border border-emerald-800/50 p-2 rounded-lg">
              ✨ <strong>Muestreo Automático:</strong> Haz clic sobre cualquier imperfección para buscar la mejor textura donante y clonar conservando iluminación natural.
            </div>
          )}
        </div>
      )}
    </div>
  );
};
