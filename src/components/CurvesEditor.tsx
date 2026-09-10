import React, { useRef, useEffect, useState, useCallback } from 'react';
import { CurveChannel, CurvePoint } from '../types';
import { generateSplineLut256 } from '../engine/spline';
import { RotateCcw, Plus, Trash2, Sliders } from 'lucide-react';

interface CurvesEditorProps {
  curves: Record<CurveChannel, CurvePoint[]>;
  onChange: (channel: CurveChannel, points: CurvePoint[]) => void;
}

export const CurvesEditor: React.FC<CurvesEditorProps> = ({ curves, onChange }) => {
  const [activeChannel, setActiveChannel] = useState<CurveChannel>('rgb');
  const [selectedPointIndex, setSelectedPointIndex] = useState<number | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const isDraggingRef = useRef(false);

  const currentPoints = curves[activeChannel] || [
    { x: 0, y: 0 },
    { x: 1, y: 1 },
  ];

  const channelColors: Record<CurveChannel, { stroke: string; fill: string; label: string; text: string }> = {
    rgb: { stroke: '#f8fafc', fill: '#f8fafc33', label: 'RGB Master', text: 'text-slate-100' },
    red: { stroke: '#ef4444', fill: '#ef444433', label: 'Rojo', text: 'text-red-400' },
    green: { stroke: '#22c55e', fill: '#22c55e33', label: 'Verde', text: 'text-emerald-400' },
    blue: { stroke: '#3b82f6', fill: '#3b82f633', label: 'Azul', text: 'text-blue-400' },
  };

  const drawCanvas = useCallback(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const w = canvas.width;
    const h = canvas.height;

    // Dark grid background
    ctx.fillStyle = '#090d16';
    ctx.fillRect(0, 0, w, h);

    // 4x4 Grid lines
    ctx.strokeStyle = '#1e293b';
    ctx.lineWidth = 1;
    for (let i = 1; i < 4; i++) {
      const pos = (i / 4) * w;
      ctx.beginPath();
      ctx.moveTo(pos, 0);
      ctx.lineTo(pos, h);
      ctx.stroke();

      ctx.beginPath();
      ctx.moveTo(0, pos);
      ctx.lineTo(w, pos);
      ctx.stroke();
    }

    // Diagonal reference line (neutral transfer)
    ctx.strokeStyle = '#334155';
    ctx.setLineDash([4, 4]);
    ctx.beginPath();
    ctx.moveTo(0, h);
    ctx.lineTo(w, 0);
    ctx.stroke();
    ctx.setLineDash([]);

    // Draw Cubic Spline
    const lut = generateSplineLut256(currentPoints);
    ctx.strokeStyle = channelColors[activeChannel].stroke;
    ctx.lineWidth = 2.5;
    ctx.lineJoin = 'round';
    ctx.lineCap = 'round';

    ctx.beginPath();
    ctx.moveTo(0, h * (1 - lut[0]));
    for (let i = 1; i < 256; i++) {
      const x = (i / 255) * w;
      const y = (1 - lut[i]) * h;
      ctx.lineTo(x, y);
    }
    ctx.stroke();

    // Draw control points
    currentPoints.forEach((pt, idx) => {
      const px = pt.x * w;
      const py = (1 - pt.y) * h;
      const isSelected = idx === selectedPointIndex;

      ctx.beginPath();
      ctx.arc(px, py, isSelected ? 6.5 : 4.5, 0, Math.PI * 2);
      ctx.fillStyle = isSelected ? '#38bdf8' : '#ffffff';
      ctx.fill();
      ctx.strokeStyle = '#0284c7';
      ctx.lineWidth = 2;
      ctx.stroke();
    });
  }, [currentPoints, activeChannel, selectedPointIndex]);

  useEffect(() => {
    drawCanvas();
  }, [drawCanvas]);

  const handlePointerDown = (e: React.PointerEvent<HTMLCanvasElement>) => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const rect = canvas.getBoundingClientRect();
    const x = (e.clientX - rect.left) / rect.width;
    const y = 1 - (e.clientY - rect.top) / rect.height;

    // Check if clicked near an existing point
    const hitRadius = 16 / rect.width;
    const hitIndex = currentPoints.findIndex(
      (p) => Math.hypot(p.x - x, p.y - y) < hitRadius
    );

    if (hitIndex !== -1) {
      setSelectedPointIndex(hitIndex);
      isDraggingRef.current = true;
      canvas.setPointerCapture(e.pointerId);
    } else {
      // Add a new point if under 14 points
      if (currentPoints.length < 14) {
        const newPts = [...currentPoints, { x: Math.max(0, Math.min(1, x)), y: Math.max(0, Math.min(1, y)) }].sort(
          (a, b) => a.x - b.x
        );
        const newIdx = newPts.findIndex((p) => Math.abs(p.x - x) < 0.001);
        setSelectedPointIndex(newIdx);
        isDraggingRef.current = true;
        canvas.setPointerCapture(e.pointerId);
        onChange(activeChannel, newPts);
      }
    }
  };

  const handlePointerMove = (e: React.PointerEvent<HTMLCanvasElement>) => {
    if (!isDraggingRef.current || selectedPointIndex === null) return;
    const canvas = canvasRef.current;
    if (!canvas) return;
    const rect = canvas.getBoundingClientRect();
    let x = (e.clientX - rect.left) / rect.width;
    const y = 1 - (e.clientY - rect.top) / rect.height;

    // Boundary constraints: endpoints locked to x=0 and x=1
    if (selectedPointIndex === 0) x = 0;
    if (selectedPointIndex === currentPoints.length - 1) x = 1;

    const clampedX = Math.max(0, Math.min(1, x));
    const clampedY = Math.max(0, Math.min(1, y));

    const updated = [...currentPoints];
    updated[selectedPointIndex] = { x: clampedX, y: clampedY };
    onChange(activeChannel, updated);
  };

  const handlePointerUp = (e: React.PointerEvent<HTMLCanvasElement>) => {
    isDraggingRef.current = false;
    try {
      canvasRef.current?.releasePointerCapture(e.pointerId);
    } catch {}
  };

  const deleteSelectedPoint = () => {
    if (selectedPointIndex === null || currentPoints.length <= 2) return;
    if (selectedPointIndex === 0 || selectedPointIndex === currentPoints.length - 1) return; // Keep endpoints
    const updated = currentPoints.filter((_, idx) => idx !== selectedPointIndex);
    setSelectedPointIndex(null);
    onChange(activeChannel, updated);
  };

  const resetChannel = () => {
    onChange(activeChannel, [
      { x: 0, y: 0 },
      { x: 1, y: 1 },
    ]);
    setSelectedPointIndex(null);
  };

  const applyPreset = (preset: 'contrast' | 'matte' | 'film') => {
    if (preset === 'contrast') {
      onChange(activeChannel, [
        { x: 0, y: 0 },
        { x: 0.25, y: 0.18 },
        { x: 0.5, y: 0.5 },
        { x: 0.75, y: 0.82 },
        { x: 1, y: 1 },
      ]);
    } else if (preset === 'matte') {
      onChange(activeChannel, [
        { x: 0, y: 0.08 },
        { x: 0.25, y: 0.24 },
        { x: 0.75, y: 0.8 },
        { x: 1, y: 0.95 },
      ]);
    } else if (preset === 'film') {
      onChange(activeChannel, [
        { x: 0, y: 0.05 },
        { x: 0.2, y: 0.16 },
        { x: 0.45, y: 0.48 },
        { x: 0.78, y: 0.85 },
        { x: 1, y: 0.96 },
      ]);
    }
  };

  return (
    <div className="flex flex-col gap-3">
      {/* Channel Tabs */}
      <div className="flex items-center justify-between">
        <div className="flex rounded-lg bg-slate-800/80 p-0.5 border border-slate-700/60">
          {(['rgb', 'red', 'green', 'blue'] as CurveChannel[]).map((ch) => (
            <button
              key={ch}
              onClick={() => {
                setActiveChannel(ch);
                setSelectedPointIndex(null);
              }}
              className={`px-3 py-1 text-xs font-semibold rounded-md transition-all ${
                activeChannel === ch
                  ? 'bg-slate-700 text-white shadow-sm'
                  : 'text-slate-400 hover:text-slate-200'
              } ${channelColors[ch].text}`}
            >
              {channelColors[ch].label}
            </button>
          ))}
        </div>

        <div className="flex items-center gap-1.5 text-xs text-slate-400">
          <span className="font-mono bg-slate-800/80 px-2 py-0.5 rounded border border-slate-700/50">
            {currentPoints.length}/14 pts
          </span>
          <button
            onClick={resetChannel}
            title="Resetear curva"
            className="p-1 rounded hover:bg-slate-800 text-slate-400 hover:text-slate-200 transition-colors"
          >
            <RotateCcw className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>

      {/* Curve Canvas */}
      <div className="relative w-full aspect-square max-h-[260px] rounded-xl overflow-hidden border border-slate-700/60 shadow-inner bg-slate-950">
        <canvas
          ref={canvasRef}
          width={320}
          height={320}
          onPointerDown={handlePointerDown}
          onPointerMove={handlePointerMove}
          onPointerUp={handlePointerUp}
          className="w-full h-full cursor-crosshair touch-none"
        />

        {/* Selected Point Readout overlay */}
        {selectedPointIndex !== null && currentPoints[selectedPointIndex] && (
          <div className="absolute top-2 left-2 bg-slate-900/90 backdrop-blur-sm border border-slate-700 px-2 py-1 rounded text-[11px] font-mono text-sky-400 flex items-center gap-2">
            <span>In: {Math.round(currentPoints[selectedPointIndex].x * 255)}</span>
            <span>Out: {Math.round(currentPoints[selectedPointIndex].y * 255)}</span>
            {selectedPointIndex > 0 && selectedPointIndex < currentPoints.length - 1 && (
              <button
                onClick={deleteSelectedPoint}
                className="text-red-400 hover:text-red-300 ml-1"
                title="Eliminar punto"
              >
                <Trash2 className="w-3 h-3" />
              </button>
            )}
          </div>
        )}
      </div>

      {/* Quick Curve Presets */}
      <div className="flex items-center justify-between text-xs text-slate-400">
        <span className="text-[11px] font-medium text-slate-500 uppercase tracking-wider">Presets:</span>
        <div className="flex gap-1.5">
          <button
            onClick={() => applyPreset('contrast')}
            className="px-2 py-1 bg-slate-800/80 hover:bg-slate-700 rounded text-[11px] text-slate-300 transition-colors"
          >
            Contraste S
          </button>
          <button
            onClick={() => applyPreset('matte')}
            className="px-2 py-1 bg-slate-800/80 hover:bg-slate-700 rounded text-[11px] text-slate-300 transition-colors"
          >
            Mate
          </button>
          <button
            onClick={() => applyPreset('film')}
            className="px-2 py-1 bg-slate-800/80 hover:bg-slate-700 rounded text-[11px] text-slate-300 transition-colors"
          >
            Cine
          </button>
        </div>
      </div>
    </div>
  );
};
