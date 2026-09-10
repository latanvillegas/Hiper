import React, { useRef, useState, useCallback } from 'react';
import { ZoomIn, ZoomOut, Maximize2, RotateCcw, Compass } from 'lucide-react';

interface ImageNavigatorProps {
  imageSrc: string;
  zoom: number;
  panX: number; // in pixels or normalized
  panY: number;
  rotation: number;
  onZoomChange: (zoom: number) => void;
  onPanChange: (panX: number, panY: number) => void;
  onRotationChange: (rot: number) => void;
  onResetView: () => void;
}

export const ImageNavigator: React.FC<ImageNavigatorProps> = ({
  imageSrc,
  zoom,
  panX,
  panY,
  rotation,
  onZoomChange,
  onPanChange,
  onRotationChange,
  onResetView,
}) => {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const [isDragging, setIsDragging] = useState(false);

  // Normalized viewport box width and height relative to zoom (clamp min 0.1, max 1.0)
  const viewWidthFraction = Math.min(1.0, 1.0 / zoom);
  const viewHeightFraction = Math.min(1.0, 1.0 / zoom);

  // Offset fraction normalized [-0.5, 0.5] mapped to center
  const centerXFraction = 0.5 - panX / (1200 * zoom);
  const centerYFraction = 0.5 - panY / (900 * zoom);

  const boxLeft = Math.max(0, Math.min(1 - viewWidthFraction, centerXFraction - viewWidthFraction / 2)) * 100;
  const boxTop = Math.max(0, Math.min(1 - viewHeightFraction, centerYFraction - viewHeightFraction / 2)) * 100;

  const handlePointer = useCallback(
    (clientX: number, clientY: number) => {
      if (!containerRef.current) return;
      const rect = containerRef.current.getBoundingClientRect();
      const clickX = (clientX - rect.left) / rect.width;
      const clickY = (clientY - rect.top) / rect.height;

      // Invert to pan coordinate
      const targetPanX = -(clickX - 0.5) * (1200 * zoom);
      const targetPanY = -(clickY - 0.5) * (900 * zoom);

      onPanChange(targetPanX, targetPanY);
    },
    [zoom, onPanChange]
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
    <div className="flex flex-col gap-2 p-2.5 bg-neutral-900/90 rounded-2xl border border-neutral-800 text-xs shadow-md">
      <div className="flex items-center justify-between">
        <span className="font-semibold text-neutral-200 flex items-center gap-1.5">
          <Compass className="w-3.5 h-3.5 text-sky-400" />
          Navegador de Imagen
        </span>
        <span className="font-mono text-[11px] text-sky-400 font-bold">
          {Math.round(zoom * 100)}%
        </span>
      </div>

      {/* Mini Thumbnail Container with Draggable Viewport Box */}
      <div
        ref={containerRef}
        onPointerDown={onPointerDown}
        onPointerMove={onPointerMove}
        onPointerUp={onPointerUp}
        className="relative w-full h-32 rounded-xl overflow-hidden bg-neutral-950 border border-neutral-700/80 cursor-grab active:cursor-grabbing select-none"
      >
        <img
          src={imageSrc}
          alt="Minimap"
          className="w-full h-full object-cover pointer-events-none opacity-80"
          style={{ transform: `rotate(${rotation}deg)` }}
        />

        {/* Viewport Boundary Rectangle */}
        <div
          className="absolute border-2 border-sky-400 bg-sky-400/20 shadow-lg pointer-events-none transition-all duration-75"
          style={{
            left: `${boxLeft}%`,
            top: `${boxTop}%`,
            width: `${viewWidthFraction * 100}%`,
            height: `${viewHeightFraction * 100}%`,
          }}
        >
          {/* Center Crosshair */}
          <div className="absolute inset-0 m-auto w-1.5 h-1.5 bg-sky-400 rounded-full" />
        </div>
      </div>

      {/* Quick Navigation / Zoom Bar */}
      <div className="flex items-center justify-between pt-1 gap-1">
        <div className="flex items-center gap-1">
          <button
            onClick={() => onZoomChange(Math.max(0.2, zoom - 0.25))}
            className="p-1.5 rounded-lg bg-neutral-800 hover:bg-neutral-700 text-neutral-300"
            title="Reducir Zoom (-)"
          >
            <ZoomOut className="w-3.5 h-3.5" />
          </button>
          <button
            onClick={() => onZoomChange(Math.min(5.0, zoom + 0.25))}
            className="p-1.5 rounded-lg bg-neutral-800 hover:bg-neutral-700 text-neutral-300"
            title="Aumentar Zoom (+)"
          >
            <ZoomIn className="w-3.5 h-3.5" />
          </button>
          <button
            onClick={() => {
              onZoomChange(1.0);
              onPanChange(0, 0);
            }}
            className="px-2 py-1 rounded-lg bg-neutral-800 hover:bg-neutral-700 text-[10px] font-mono text-neutral-300"
            title="Vista 1:1 (100%)"
          >
            100%
          </button>
          <button
            onClick={onResetView}
            className="p-1.5 rounded-lg bg-neutral-800 hover:bg-neutral-700 text-neutral-300"
            title="Ajustar a la pantalla (Fit)"
          >
            <Maximize2 className="w-3.5 h-3.5" />
          </button>
        </div>

        {/* Rotate & Reset */}
        <div className="flex items-center gap-1">
          <button
            onClick={() => onRotationChange((rotation + 90) % 360)}
            className="p-1.5 rounded-lg bg-neutral-800 hover:bg-neutral-700 text-neutral-300"
            title="Rotar 90°"
          >
            <RotateCcw className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>
    </div>
  );
};
