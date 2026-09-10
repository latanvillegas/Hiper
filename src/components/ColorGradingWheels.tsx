import React, { useRef, useState, useCallback } from 'react';
import { ColorGradingSettings, ColorWheelValue } from '../types';
import { RotateCcw } from 'lucide-react';

interface ColorGradingWheelsProps {
  colorGrading: ColorGradingSettings;
  onChange: (grading: ColorGradingSettings) => void;
  onReset: () => void;
}

interface SingleWheelProps {
  label: string;
  value: ColorWheelValue;
  onChange: (val: ColorWheelValue) => void;
  onReset: () => void;
}

const SingleColorWheel: React.FC<SingleWheelProps> = ({
  label,
  value,
  onChange,
  onReset,
}) => {
  const wheelRef = useRef<HTMLDivElement | null>(null);
  const [isDragging, setIsDragging] = useState(false);

  // Convert Hue (0..360) and Saturation (0..1) to cartesian coordinates inside circle (R = 54)
  const radius = 54;
  const rad = (value.hue * Math.PI) / 180;
  const puckX = radius + Math.cos(rad) * (value.saturation * radius);
  const puckY = radius + Math.sin(rad) * (value.saturation * radius);

  const handlePointer = useCallback(
    (clientX: number, clientY: number) => {
      if (!wheelRef.current) return;
      const rect = wheelRef.current.getBoundingClientRect();
      const centerX = rect.left + rect.width / 2;
      const centerY = rect.top + rect.height / 2;

      const dx = clientX - centerX;
      const dy = clientY - centerY;
      const dist = Math.sqrt(dx * dx + dy * dy);

      // Angle in degrees [0, 360)
      let angle = (Math.atan2(dy, dx) * 180) / Math.PI;
      if (angle < 0) angle += 360;

      // Saturation clamped [0, 1]
      const maxDist = rect.width / 2;
      const sat = Math.min(1.0, dist / maxDist);

      onChange({
        ...value,
        hue: Math.round(angle),
        saturation: parseFloat(sat.toFixed(2)),
      });
    },
    [value, onChange]
  );

  const onPointerDown = (e: React.PointerEvent<HTMLDivElement>) => {
    setIsDragging(true);
    e.currentTarget.setPointerCapture(e.pointerId);
    handlePointer(e.clientX, e.clientY);
  };

  const onPointerMove = (e: React.PointerEvent<HTMLDivElement>) => {
    if (isDragging) {
      handlePointer(e.clientX, e.clientY);
    }
  };

  const onPointerUp = (e: React.PointerEvent<HTMLDivElement>) => {
    setIsDragging(false);
    try {
      e.currentTarget.releasePointerCapture(e.pointerId);
    } catch {}
  };

  return (
    <div className="flex flex-col items-center bg-neutral-900/80 p-3 rounded-2xl border border-neutral-800 shadow-sm w-full">
      {/* Header */}
      <div className="flex items-center justify-between w-full mb-2">
        <span className="text-xs font-semibold text-neutral-200 tracking-wide">
          {label}
        </span>
        <button
          onClick={onReset}
          className="p-1 text-neutral-400 hover:text-white rounded hover:bg-neutral-800 transition-colors"
          title="Resetear rueda"
        >
          <RotateCcw className="w-3 h-3" />
        </button>
      </div>

      {/* 2D Chromaticity Disk */}
      <div
        ref={wheelRef}
        onPointerDown={onPointerDown}
        onPointerMove={onPointerMove}
        onPointerUp={onPointerUp}
        className="relative w-28 h-28 rounded-full cursor-crosshair select-none shadow-inner border-2 border-neutral-700/60 overflow-hidden"
        style={{
          background:
            'radial-gradient(circle, #808080 0%, transparent 70%), conic-gradient(from 0deg, #ff0000, #ffff00, #00ff00, #00ffff, #0000ff, #ff00ff, #ff0000)',
        }}
      >
        {/* Subtle grid crosshair */}
        <div className="absolute inset-0 pointer-events-none opacity-25 flex items-center justify-center">
          <div className="w-full h-px bg-white" />
          <div className="h-full w-px bg-white absolute" />
          <div className="w-14 h-14 rounded-full border border-white" />
        </div>

        {/* Draggable Puck Indicator */}
        <div
          className="absolute -translate-x-1/2 -translate-y-1/2 w-4 h-4 rounded-full bg-white border-2 border-neutral-900 shadow-md pointer-events-none transition-transform"
          style={{
            left: `${puckX}px`,
            top: `${puckY}px`,
          }}
        />
      </div>

      {/* Numeric Readouts */}
      <div className="flex items-center justify-between w-full mt-2 text-[10px] font-mono text-neutral-400">
        <span>Tono: {value.hue}°</span>
        <span>Sat: {Math.round(value.saturation * 100)}%</span>
      </div>

      {/* Luminance / Lift-Gamma-Gain Slider */}
      <div className="w-full mt-2 flex flex-col gap-1">
        <div className="flex items-center justify-between text-[10px] text-neutral-400 font-mono">
          <span>Luminancia</span>
          <span className={value.luminance !== 0 ? 'text-sky-400 font-semibold' : ''}>
            {value.luminance > 0 ? `+${value.luminance.toFixed(2)}` : value.luminance.toFixed(2)}
          </span>
        </div>
        <input
          type="range"
          min={-1.0}
          max={1.0}
          step={0.02}
          value={value.luminance}
          onChange={(e) =>
            onChange({ ...value, luminance: parseFloat(e.target.value) })
          }
          className="w-full accent-sky-400 h-1.5 bg-neutral-800 rounded-full cursor-pointer"
        />
      </div>
    </div>
  );
};

export const ColorGradingWheels: React.FC<ColorGradingWheelsProps> = ({
  colorGrading,
  onChange,
  onReset,
}) => {
  const [activeTab, setActiveTab] = useState<'3way' | 'all'>('3way');

  const updateShadows = (val: ColorWheelValue) => {
    onChange({ ...colorGrading, shadows: val });
  };
  const updateMidtones = (val: ColorWheelValue) => {
    onChange({ ...colorGrading, midtones: val });
  };
  const updateHighlights = (val: ColorWheelValue) => {
    onChange({ ...colorGrading, highlights: val });
  };
  const updateGlobal = (val: ColorWheelValue) => {
    onChange({ ...colorGrading, global: val });
  };

  return (
    <div className="flex flex-col gap-3">
      {/* Header with Title & Reset */}
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-xs font-bold text-neutral-100 uppercase tracking-wider">
            Color Grading (3-Way Wheels)
          </h3>
          <p className="text-[11px] text-neutral-400">
            Gradación cromática cinematográfica estilo DaVinci Resolve
          </p>
        </div>
        <button
          onClick={onReset}
          className="flex items-center gap-1 px-2 py-1 rounded-lg bg-neutral-800 hover:bg-neutral-700 text-[11px] text-neutral-300 transition-colors"
        >
          <RotateCcw className="w-3 h-3" />
          <span>Reset</span>
        </button>
      </div>

      {/* Wheels Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <SingleColorWheel
          label="Sombras (Lift)"
          value={colorGrading.shadows}
          onChange={updateShadows}
          onReset={() =>
            updateShadows({ hue: 0, saturation: 0, luminance: 0 })
          }
        />
        <SingleColorWheel
          label="Medios (Gamma)"
          value={colorGrading.midtones}
          onChange={updateMidtones}
          onReset={() =>
            updateMidtones({ hue: 0, saturation: 0, luminance: 0 })
          }
        />
        <SingleColorWheel
          label="Luces (Gain)"
          value={colorGrading.highlights}
          onChange={updateHighlights}
          onReset={() =>
            updateHighlights({ hue: 0, saturation: 0, luminance: 0 })
          }
        />
      </div>

      {/* Global Offset Wheel */}
      <div className="bg-neutral-900/60 p-3 rounded-2xl border border-neutral-800/80">
        <div className="flex items-center justify-between mb-2">
          <span className="text-xs font-semibold text-neutral-200">
            Rueda Global / Offset Maestro
          </span>
          <span className="text-[10px] font-mono text-neutral-400">
            Tono: {colorGrading.global.hue}° | Sat: {Math.round(colorGrading.global.saturation * 100)}%
          </span>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 items-center">
          <div className="flex items-center gap-2">
            <span className="text-[11px] text-neutral-400 w-12">Tono</span>
            <input
              type="range"
              min={0}
              max={360}
              value={colorGrading.global.hue}
              onChange={(e) =>
                updateGlobal({ ...colorGrading.global, hue: parseInt(e.target.value) })
              }
              className="w-full accent-amber-400 h-1.5 bg-neutral-800 rounded-full"
            />
          </div>
          <div className="flex items-center gap-2">
            <span className="text-[11px] text-neutral-400 w-12">Sat</span>
            <input
              type="range"
              min={0}
              max={1}
              step={0.01}
              value={colorGrading.global.saturation}
              onChange={(e) =>
                updateGlobal({ ...colorGrading.global, saturation: parseFloat(e.target.value) })
              }
              className="w-full accent-amber-400 h-1.5 bg-neutral-800 rounded-full"
            />
          </div>
        </div>
      </div>
    </div>
  );
};
