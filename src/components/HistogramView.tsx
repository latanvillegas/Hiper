import React, { useRef, useEffect, useState } from 'react';
import { HistogramBins, LevelsSettings } from '../types';
import { Activity, SlidersHorizontal, RotateCcw, AlertTriangle, Eye } from 'lucide-react';
import { ProfessionalSlider } from './ProfessionalSlider';

interface HistogramViewProps {
  histogram: HistogramBins;
  levels: LevelsSettings;
  onLevelsChange: (levels: LevelsSettings) => void;
  showClippingOverlay?: boolean;
  onToggleClippingOverlay?: () => void;
}

export const HistogramView: React.FC<HistogramViewProps> = ({
  histogram,
  levels,
  onLevelsChange,
  showClippingOverlay = false,
  onToggleClippingOverlay,
}) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const [activeChannel, setActiveChannel] = useState<'all' | 'red' | 'green' | 'blue' | 'luma'>('all');

  // Compute clipping stats
  // Bin 0 & 1 = Crushed Shadows (Blue warning)
  const shadowClipR = (histogram.red[0] || 0) + (histogram.red[1] || 0);
  const shadowClipG = (histogram.green[0] || 0) + (histogram.green[1] || 0);
  const shadowClipB = (histogram.blue[0] || 0) + (histogram.blue[1] || 0);
  const shadowTotal = shadowClipR + shadowClipG + shadowClipB;

  // Bin 254 & 255 = Blown Highlights (Red warning)
  const highlightClipR = (histogram.red[254] || 0) + (histogram.red[255] || 0);
  const highlightClipG = (histogram.green[254] || 0) + (histogram.green[255] || 0);
  const highlightClipB = (histogram.blue[254] || 0) + (histogram.blue[255] || 0);
  const highlightTotal = highlightClipR + highlightClipG + highlightClipB;

  // Total samples approximation
  const totalSamples = Math.max(1, histogram.luma.reduce((acc, v) => acc + v, 0));
  const shadowPercent = Math.min(100, (shadowTotal / (totalSamples * 3)) * 100);
  const highlightPercent = Math.min(100, (highlightTotal / (totalSamples * 3)) * 100);

  const hasShadowClipping = shadowPercent > 0.4;
  const hasHighlightClipping = highlightPercent > 0.4;

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const w = canvas.width;
    const h = canvas.height;

    // Strict neutral dark background
    ctx.fillStyle = '#121214';
    ctx.fillRect(0, 0, w, h);

    // Subtle grid lines
    ctx.strokeStyle = '#27272a';
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(w * 0.25, 0); ctx.lineTo(w * 0.25, h);
    ctx.moveTo(w * 0.5, 0); ctx.lineTo(w * 0.5, h);
    ctx.moveTo(w * 0.75, 0); ctx.lineTo(w * 0.75, h);
    ctx.moveTo(0, h * 0.5); ctx.lineTo(w, h * 0.5);
    ctx.stroke();

    const maxCount = Math.max(1, histogram.maxVal);
    const barW = w / 256;

    const drawChannel = (bins: number[], color: string, composite: GlobalCompositeOperation = 'source-over') => {
      ctx.globalCompositeOperation = composite;
      ctx.fillStyle = color;
      ctx.beginPath();
      ctx.moveTo(0, h);
      for (let i = 0; i < 256; i++) {
        const val = (bins[i] / maxCount) * (h * 0.92);
        const x = i * barW;
        const y = h - val;
        ctx.lineTo(x, y);
      }
      ctx.lineTo(w, h);
      ctx.closePath();
      ctx.fill();
    };

    if (activeChannel === 'all') {
      drawChannel(histogram.red, 'rgba(239, 68, 68, 0.45)', 'lighter');
      drawChannel(histogram.green, 'rgba(34, 197, 94, 0.45)', 'lighter');
      drawChannel(histogram.blue, 'rgba(59, 130, 246, 0.45)', 'lighter');
    } else if (activeChannel === 'red') {
      drawChannel(histogram.red, 'rgba(239, 68, 68, 0.85)');
    } else if (activeChannel === 'green') {
      drawChannel(histogram.green, 'rgba(34, 197, 94, 0.85)');
    } else if (activeChannel === 'blue') {
      drawChannel(histogram.blue, 'rgba(59, 130, 246, 0.85)');
    } else {
      drawChannel(histogram.luma, 'rgba(244, 244, 245, 0.85)');
    }

    ctx.globalCompositeOperation = 'source-over';

    // Highlight Levels Black and White points on the histogram
    const inBlackX = levels.inputBlack * w;
    const inWhiteX = levels.inputWhite * w;

    ctx.strokeStyle = '#38bdf8';
    ctx.lineWidth = 1.5;
    ctx.beginPath();
    ctx.moveTo(inBlackX, 0);
    ctx.lineTo(inBlackX, h);
    ctx.moveTo(inWhiteX, 0);
    ctx.lineTo(inWhiteX, h);
    ctx.stroke();

    // Draw clipping indicators directly on the canvas borders
    if (hasShadowClipping) {
      ctx.fillStyle = '#3b82f6'; // Blue warning for crushed blacks
      ctx.fillRect(0, 0, 4, h);
    }
    if (hasHighlightClipping) {
      ctx.fillStyle = '#ef4444'; // Red warning for blown whites
      ctx.fillRect(w - 4, 0, 4, h);
    }
  }, [histogram, activeChannel, levels, hasShadowClipping, hasHighlightClipping]);

  const resetLevels = () => {
    onLevelsChange({
      inputBlack: 0.0,
      inputGamma: 1.0,
      inputWhite: 1.0,
      outputBlack: 0.0,
      outputWhite: 1.0,
    });
  };

  return (
    <div className="flex flex-col gap-3">
      {/* Histogram Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-1.5">
          <Activity className="w-4 h-4 text-sky-400" />
          <span className="text-xs font-bold text-neutral-200">
            Histograma RGB en Tiempo Real
          </span>
        </div>
        <div className="flex gap-0.5 text-[10px] bg-neutral-900 p-0.5 rounded-lg border border-neutral-800">
          {(['all', 'red', 'green', 'blue', 'luma'] as const).map((ch) => (
            <button
              key={ch}
              onClick={() => setActiveChannel(ch)}
              className={`px-1.5 py-0.5 rounded font-medium transition-colors ${
                activeChannel === ch
                  ? 'bg-neutral-800 text-sky-300 font-semibold shadow-sm'
                  : 'text-neutral-400 hover:text-neutral-200'
              }`}
            >
              {ch.toUpperCase()}
            </button>
          ))}
        </div>
      </div>

      {/* Real-time Clipping Warning Indicators Header */}
      <div className="flex items-center justify-between text-[11px] px-1">
        {/* Shadow clipping badge (Blue) */}
        <div
          className={`flex items-center gap-1 px-2 py-0.5 rounded-md font-mono transition-colors ${
            hasShadowClipping
              ? 'bg-blue-950/80 text-blue-400 border border-blue-800 animate-pulse'
              : 'text-neutral-500 bg-neutral-900/50'
          }`}
          title="Píxeles empastados en negros (< 2 Luma). Se muestran en azul si activas la máscara."
        >
          <span className="w-2 h-2 rounded-full bg-blue-500" />
          <span>Sombras: {shadowPercent.toFixed(1)}%</span>
        </div>

        {/* Toggle clipping overlay button */}
        {onToggleClippingOverlay && (
          <button
            onClick={onToggleClippingOverlay}
            className={`flex items-center gap-1 px-2 py-0.5 rounded-md text-[10px] font-medium border transition-colors ${
              showClippingOverlay
                ? 'bg-amber-950/80 border-amber-600 text-amber-300 shadow-sm'
                : 'bg-neutral-900 border-neutral-800 text-neutral-400 hover:text-neutral-200'
            }`}
            title="Destacar píxeles quemados (rojo) y empastados (azul) en el lienzo"
          >
            <Eye className="w-3 h-3" />
            <span>Overlay Zebra</span>
          </button>
        )}

        {/* Highlight clipping badge (Red) */}
        <div
          className={`flex items-center gap-1 px-2 py-0.5 rounded-md font-mono transition-colors ${
            hasHighlightClipping
              ? 'bg-rose-950/80 text-rose-400 border border-rose-800 animate-pulse'
              : 'text-neutral-500 bg-neutral-900/50'
          }`}
          title="Píxeles quemados en altas luces (> 253 Luma). Se muestran en rojo si activas la máscara."
        >
          <span>Luces: {highlightPercent.toFixed(1)}%</span>
          <span className="w-2 h-2 rounded-full bg-rose-500" />
        </div>
      </div>

      {/* Histogram Canvas */}
      <div className="w-full h-28 rounded-xl overflow-hidden border border-neutral-800 bg-neutral-950 relative shadow-inner">
        <canvas ref={canvasRef} width={256} height={112} className="w-full h-full block" />
        <div className="absolute bottom-1 right-2 text-[9px] font-mono text-neutral-500">
          Vulkan / GLSL 60 FPS
        </div>
      </div>

      {/* Levels Controls with Professional Precision Sliders */}
      <div className="flex flex-col gap-1.5 pt-1">
        <div className="flex items-center justify-between text-xs px-1">
          <span className="font-semibold text-neutral-200 flex items-center gap-1.5">
            <SlidersHorizontal className="w-3.5 h-3.5 text-sky-400" />
            Niveles Tonal
          </span>
          <button
            onClick={resetLevels}
            className="text-[11px] text-neutral-400 hover:text-neutral-200 flex items-center gap-1 hover:underline"
          >
            <RotateCcw className="w-3 h-3" /> Reset
          </button>
        </div>

        <ProfessionalSlider
          label="Punto Negro (Sombras)"
          value={Math.round(levels.inputBlack * 255)}
          min={0}
          max={100}
          defaultValue={0}
          onChange={(val) =>
            onLevelsChange({ ...levels, inputBlack: val / 255 })
          }
          tooltip="Ajusta el umbral de negro absoluto. Píxeles por debajo se convierten en negro puro."
        />

        <ProfessionalSlider
          label="Medios Tonos (Gamma)"
          value={parseFloat(levels.inputGamma.toFixed(2))}
          min={0.3}
          max={2.5}
          step={0.05}
          defaultValue={1.0}
          onChange={(val) => onLevelsChange({ ...levels, inputGamma: val })}
          tooltip="Ajusta la curva de respuesta de luminancia intermedia sin recortar blancos ni negros."
        />

        <ProfessionalSlider
          label="Punto Blanco (Altas Luces)"
          value={Math.round(levels.inputWhite * 255)}
          min={150}
          max={255}
          defaultValue={255}
          onChange={(val) =>
            onLevelsChange({ ...levels, inputWhite: val / 255 })
          }
          tooltip="Ajusta el umbral de blanco absoluto. Píxeles por encima se convierten en blanco puro."
        />
      </div>
    </div>
  );
};
