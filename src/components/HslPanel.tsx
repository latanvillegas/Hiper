import React, { useState } from 'react';
import { HslChannelName, HslChannelSetting } from '../types';
import { Palette, RotateCcw } from 'lucide-react';

interface HslPanelProps {
  hslSettings: Record<HslChannelName, HslChannelSetting>;
  onChange: (channel: HslChannelName, setting: HslChannelSetting) => void;
  onReset: () => void;
}

const CHANNELS: { id: HslChannelName; label: string; color: string; indicator: string }[] = [
  { id: 'red', label: 'Rojo', color: '#ef4444', indicator: 'bg-red-500' },
  { id: 'yellow', label: 'Amarillo', color: '#eab308', indicator: 'bg-yellow-500' },
  { id: 'green', label: 'Verde', color: '#22c55e', indicator: 'bg-green-500' },
  { id: 'cyan', label: 'Cyan', color: '#06b6d4', indicator: 'bg-cyan-500' },
  { id: 'blue', label: 'Azul', color: '#3b82f6', indicator: 'bg-blue-500' },
  { id: 'magenta', label: 'Magenta', color: '#d946ef', indicator: 'bg-fuchsia-500' },
  { id: 'shadows', label: 'Sombras', color: '#64748b', indicator: 'bg-slate-600' },
  { id: 'highlights', label: 'Highlights', color: '#f8fafc', indicator: 'bg-slate-200' },
];

export const HslPanel: React.FC<HslPanelProps> = ({ hslSettings, onChange, onReset }) => {
  const [selectedChannel, setSelectedChannel] = useState<HslChannelName>('red');
  const current = hslSettings[selectedChannel] || { hueShift: 0, saturation: 0, luminance: 0 };

  const handleUpdate = (key: keyof HslChannelSetting, value: number) => {
    onChange(selectedChannel, {
      ...current,
      [key]: value,
    });
  };

  const isShadowOrHighlight = selectedChannel === 'shadows' || selectedChannel === 'highlights';

  return (
    <div className="flex flex-col gap-3">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Palette className="w-4 h-4 text-sky-400" />
          <span className="text-xs font-semibold text-slate-200">Color HSL (8 Canales)</span>
        </div>
        <button
          onClick={onReset}
          className="text-[11px] text-slate-400 hover:text-slate-200 flex items-center gap-1"
        >
          <RotateCcw className="w-3 h-3" /> Reset HSL
        </button>
      </div>

      {/* 8 Channels Color Selector Buttons */}
      <div className="grid grid-cols-4 gap-1.5">
        {CHANNELS.map((ch) => {
          const isSelected = selectedChannel === ch.id;
          const isModified =
            Math.abs(hslSettings[ch.id]?.hueShift || 0) > 0.1 ||
            Math.abs(hslSettings[ch.id]?.saturation || 0) > 0.01 ||
            Math.abs(hslSettings[ch.id]?.luminance || 0) > 0.01;

          return (
            <button
              key={ch.id}
              onClick={() => setSelectedChannel(ch.id)}
              className={`flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg border text-xs font-medium transition-all ${
                isSelected
                  ? 'bg-slate-800 border-sky-500/80 text-white shadow-sm'
                  : 'bg-slate-900/60 border-slate-800 text-slate-400 hover:bg-slate-800/80 hover:text-slate-200'
              }`}
            >
              <span className={`w-2.5 h-2.5 rounded-full ${ch.indicator} ${isModified ? 'ring-2 ring-sky-400 ring-offset-1 ring-offset-slate-900' : ''}`} />
              <span className="truncate">{ch.label}</span>
            </button>
          );
        })}
      </div>

      {/* Sliders for current active channel */}
      <div className="bg-slate-900/80 rounded-xl p-3 border border-slate-800/80 space-y-3">
        {/* Hue Shift (Tono) */}
        {!isShadowOrHighlight && (
          <div className="space-y-1">
            <div className="flex justify-between text-[11px] text-slate-400">
              <span>Tono (Hue Shift)</span>
              <span className="font-mono text-slate-300">
                {current.hueShift > 0 ? `+${Math.round(current.hueShift)}°` : `${Math.round(current.hueShift)}°`}
              </span>
            </div>
            <input
              type="range"
              min={-180}
              max={180}
              step={1}
              value={current.hueShift}
              onChange={(e) => handleUpdate('hueShift', parseFloat(e.target.value))}
              className="w-full accent-sky-400 h-1.5 bg-slate-800 rounded-lg cursor-pointer"
            />
          </div>
        )}

        {/* Saturation */}
        <div className="space-y-1">
          <div className="flex justify-between text-[11px] text-slate-400">
            <span>Saturación</span>
            <span className="font-mono text-slate-300">
              {current.saturation > 0 ? `+${Math.round(current.saturation * 100)}%` : `${Math.round(current.saturation * 100)}%`}
            </span>
          </div>
          <input
            type="range"
            min={-1.0}
            max={1.0}
            step={0.02}
            value={current.saturation}
            onChange={(e) => handleUpdate('saturation', parseFloat(e.target.value))}
            className="w-full accent-sky-400 h-1.5 bg-slate-800 rounded-lg cursor-pointer"
          />
        </div>

        {/* Luminance */}
        <div className="space-y-1">
          <div className="flex justify-between text-[11px] text-slate-400">
            <span>Luminancia</span>
            <span className="font-mono text-slate-300">
              {current.luminance > 0 ? `+${Math.round(current.luminance * 100)}%` : `${Math.round(current.luminance * 100)}%`}
            </span>
          </div>
          <input
            type="range"
            min={-1.0}
            max={1.0}
            step={0.02}
            value={current.luminance}
            onChange={(e) => handleUpdate('luminance', parseFloat(e.target.value))}
            className="w-full accent-sky-400 h-1.5 bg-slate-800 rounded-lg cursor-pointer"
          />
        </div>
      </div>
    </div>
  );
};
