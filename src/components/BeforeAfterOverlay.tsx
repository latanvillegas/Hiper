import React, { useState, useEffect } from 'react';
import { BeforeAfterMode } from '../types';
import {
  SplitSquareVertical,
  SplitSquareHorizontal,
  Columns,
  Eye,
  Play,
  Pause,
} from 'lucide-react';

interface BeforeAfterOverlayProps {
  originalImageSrc: string;
  mode: BeforeAfterMode;
  onModeChange: (mode: BeforeAfterMode) => void;
  splitPercent: number;
  onSplitChange: (percent: number) => void;
}

export const BeforeAfterOverlay: React.FC<BeforeAfterOverlayProps> = ({
  originalImageSrc,
  mode,
  onModeChange,
  splitPercent,
  onSplitChange,
}) => {
  const [isHoldingBlink, setIsHoldingBlink] = useState(false);
  const [isAutoBlink, setIsAutoBlink] = useState(false);
  const [autoBlinkState, setAutoBlinkState] = useState<'before' | 'after'>('after');

  // Auto-blink timer
  useEffect(() => {
    if (!isAutoBlink || mode !== 'blink') return;
    const interval = setInterval(() => {
      setAutoBlinkState((prev) => (prev === 'before' ? 'after' : 'before'));
    }, 1200);
    return () => clearInterval(interval);
  }, [isAutoBlink, mode]);

  const showOriginalInBlink =
    mode === 'blink' && (isHoldingBlink || (isAutoBlink && autoBlinkState === 'before'));

  return (
    <>
      {/* Top Floating Comparison Mode Selector Bar */}
      <div className="absolute top-3 left-1/2 -translate-x-1/2 z-30 flex items-center gap-1 bg-neutral-900/90 backdrop-blur-md p-1 rounded-xl border border-neutral-800 shadow-xl select-none">
        <button
          onClick={() => onModeChange('horizontal_slider')}
          className={`flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-medium transition-all ${
            mode === 'horizontal_slider'
              ? 'bg-neutral-800 text-sky-400 font-semibold shadow-sm border border-neutral-700'
              : 'text-neutral-400 hover:text-neutral-200'
          }`}
          title="Slider Horizontal (Deslizar izquierda / derecha)"
        >
          <SplitSquareVertical className="w-3.5 h-3.5" />
          <span>Horizontal</span>
        </button>

        <button
          onClick={() => onModeChange('vertical_slider')}
          className={`flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-medium transition-all ${
            mode === 'vertical_slider'
              ? 'bg-neutral-800 text-sky-400 font-semibold shadow-sm border border-neutral-700'
              : 'text-neutral-400 hover:text-neutral-200'
          }`}
          title="Slider Vertical (Deslizar arriba / abajo)"
        >
          <SplitSquareHorizontal className="w-3.5 h-3.5" />
          <span>Vertical</span>
        </button>

        <button
          onClick={() => onModeChange('split_50_50')}
          className={`flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-medium transition-all ${
            mode === 'split_50_50'
              ? 'bg-neutral-800 text-sky-400 font-semibold shadow-sm border border-neutral-700'
              : 'text-neutral-400 hover:text-neutral-200'
          }`}
          title="Dividir 50 / 50 Fijo"
        >
          <Columns className="w-3.5 h-3.5" />
          <span>50 / 50</span>
        </button>

        <button
          onClick={() => onModeChange('blink')}
          className={`flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-medium transition-all ${
            mode === 'blink'
              ? 'bg-neutral-800 text-sky-400 font-semibold shadow-sm border border-neutral-700'
              : 'text-neutral-400 hover:text-neutral-200'
          }`}
          title="Modo Blink (Alternar antes / después instantáneo)"
        >
          <Eye className="w-3.5 h-3.5" />
          <span>Blink</span>
        </button>
      </div>

      {/* Comparison Overlays inside Viewport */}
      {/* 1. Horizontal Slider */}
      {mode === 'horizontal_slider' && (
        <>
          <div
            className="absolute inset-0 overflow-hidden pointer-events-none"
            style={{ width: `${splitPercent}%` }}
          >
            <img
              src={originalImageSrc}
              alt="Original"
              className="max-w-none w-full h-full object-contain"
            />
            <div className="absolute top-3 left-3 bg-black/75 px-2 py-0.5 rounded text-[10px] font-mono text-neutral-300 border border-neutral-800">
              Original RAW
            </div>
          </div>

          <div
            className="absolute top-0 bottom-0 w-0.5 bg-sky-400 z-20 flex items-center justify-center cursor-ew-resize"
            style={{ left: `${splitPercent}%` }}
          >
            <div className="w-6 h-6 rounded-full bg-neutral-900 border-2 border-sky-400 shadow-xl flex items-center justify-center">
              <div className="w-1.5 h-1.5 rounded-full bg-sky-400" />
            </div>
          </div>
        </>
      )}

      {/* 2. Vertical Slider */}
      {mode === 'vertical_slider' && (
        <>
          <div
            className="absolute inset-0 overflow-hidden pointer-events-none"
            style={{ height: `${splitPercent}%` }}
          >
            <img
              src={originalImageSrc}
              alt="Original"
              className="max-w-none w-full h-full object-contain"
            />
            <div className="absolute top-3 left-3 bg-black/75 px-2 py-0.5 rounded text-[10px] font-mono text-neutral-300 border border-neutral-800">
              Original (Arriba)
            </div>
          </div>

          <div
            className="absolute left-0 right-0 h-0.5 bg-sky-400 z-20 flex items-center justify-center cursor-ns-resize"
            style={{ top: `${splitPercent}%` }}
          >
            <div className="w-6 h-6 rounded-full bg-neutral-900 border-2 border-sky-400 shadow-xl flex items-center justify-center">
              <div className="w-1.5 h-1.5 rounded-full bg-sky-400" />
            </div>
          </div>
        </>
      )}

      {/* 3. Split 50/50 */}
      {mode === 'split_50_50' && (
        <>
          <div
            className="absolute inset-0 overflow-hidden pointer-events-none"
            style={{ width: '50%' }}
          >
            <img
              src={originalImageSrc}
              alt="Original"
              className="max-w-none w-full h-full object-contain"
            />
            <div className="absolute top-3 left-3 bg-black/75 px-2 py-0.5 rounded text-[10px] font-mono text-neutral-300 border border-neutral-800">
              Original (50%)
            </div>
          </div>

          <div
            className="absolute top-0 bottom-0 w-0.5 bg-neutral-400 z-20 pointer-events-none"
            style={{ left: '50%' }}
          />

          <div className="absolute top-3 right-3 bg-black/75 px-2 py-0.5 rounded text-[10px] font-mono text-sky-400 border border-neutral-800 pointer-events-none">
            Editado (50%)
          </div>
        </>
      )}

      {/* 4. Blink Mode */}
      {mode === 'blink' && (
        <>
          {showOriginalInBlink && (
            <div className="absolute inset-0 overflow-hidden pointer-events-none z-20">
              <img
                src={originalImageSrc}
                alt="Original"
                className="w-full h-full object-contain"
              />
              <div className="absolute top-12 left-1/2 -translate-x-1/2 bg-rose-950/90 border border-rose-600 text-rose-300 px-3 py-1 rounded-full text-xs font-mono font-bold shadow-lg">
                ★ ORIGINAL RAW ★
              </div>
            </div>
          )}

          {/* Blink Interactive Controller Widget at bottom */}
          <div className="absolute bottom-4 left-1/2 -translate-x-1/2 z-30 flex items-center gap-2 bg-neutral-900/90 backdrop-blur-md px-4 py-2 rounded-2xl border border-neutral-800 shadow-2xl">
            <button
              onMouseDown={() => setIsHoldingBlink(true)}
              onMouseUp={() => setIsHoldingBlink(false)}
              onTouchStart={() => setIsHoldingBlink(true)}
              onTouchEnd={() => setIsHoldingBlink(false)}
              className={`px-3 py-1.5 rounded-xl text-xs font-semibold select-none transition-all ${
                isHoldingBlink
                  ? 'bg-rose-600 text-white shadow-lg'
                  : 'bg-neutral-800 hover:bg-neutral-700 text-neutral-200'
              }`}
            >
              {isHoldingBlink ? 'Mostrando Original...' : 'Mantén para Original'}
            </button>

            <div className="w-px h-5 bg-neutral-800" />

            <button
              onClick={() => setIsAutoBlink(!isAutoBlink)}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold transition-all ${
                isAutoBlink
                  ? 'bg-sky-600 text-white shadow-lg'
                  : 'bg-neutral-800 hover:bg-neutral-700 text-neutral-300'
              }`}
            >
              {isAutoBlink ? <Pause className="w-3.5 h-3.5" /> : <Play className="w-3.5 h-3.5" />}
              <span>Auto-Blink</span>
            </button>
          </div>
        </>
      )}
    </>
  );
};
