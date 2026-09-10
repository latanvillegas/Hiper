import React from 'react';
import { HistorySnapshot } from '../types';
import { History, Camera, Undo2, Redo2, RotateCcw } from 'lucide-react';

interface HistoryPanelProps {
  history: HistorySnapshot[];
  currentIndex: number;
  onSelectSnapshot: (index: number) => void;
  onTakeSnapshot: () => void;
  onUndo: () => void;
  onRedo: () => void;
  canUndo: boolean;
  canRedo: boolean;
}

export const HistoryPanel: React.FC<HistoryPanelProps> = ({
  history,
  currentIndex,
  onSelectSnapshot,
  onTakeSnapshot,
  onUndo,
  onRedo,
  canUndo,
  canRedo,
}) => {
  return (
    <div className="flex flex-col gap-3 h-full">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-1.5">
          <History className="w-4 h-4 text-sky-400" />
          <span className="text-xs font-bold text-neutral-200">
            Historial de Acciones Ilimitado
          </span>
        </div>
        <div className="flex items-center gap-1">
          <button
            onClick={onUndo}
            disabled={!canUndo}
            className={`p-1.5 rounded-lg border text-xs ${
              canUndo
                ? 'bg-neutral-800 hover:bg-neutral-700 text-neutral-200 border-neutral-700'
                : 'opacity-40 cursor-not-allowed text-neutral-600 border-transparent'
            }`}
            title="Deshacer (Ctrl+Z)"
          >
            <Undo2 className="w-3.5 h-3.5" />
          </button>
          <button
            onClick={onRedo}
            disabled={!canRedo}
            className={`p-1.5 rounded-lg border text-xs ${
              canRedo
                ? 'bg-neutral-800 hover:bg-neutral-700 text-neutral-200 border-neutral-700'
                : 'opacity-40 cursor-not-allowed text-neutral-600 border-transparent'
            }`}
            title="Rehacer (Ctrl+Y)"
          >
            <Redo2 className="w-3.5 h-3.5" />
          </button>
          <button
            onClick={onTakeSnapshot}
            className="flex items-center gap-1 px-2.5 py-1 rounded-lg bg-sky-600 hover:bg-sky-500 text-white text-xs font-medium shadow-sm transition-colors"
            title="Capturar snapshot manual del estado actual"
          >
            <Camera className="w-3.5 h-3.5" />
            <span>Snapshot</span>
          </button>
        </div>
      </div>

      {/* Snapshot Cards List */}
      <div className="flex-1 overflow-y-auto space-y-2 pr-1 scrollbar-thin">
        {history.map((item, index) => {
          const isActive = index === currentIndex;
          const isFuture = index > currentIndex;

          return (
            <div
              key={item.id}
              onClick={() => onSelectSnapshot(index)}
              className={`flex items-center gap-3 p-2 rounded-xl border cursor-pointer transition-all ${
                isActive
                  ? 'bg-sky-950/40 border-sky-500 shadow-md ring-1 ring-sky-500/50'
                  : isFuture
                  ? 'bg-neutral-900/30 border-neutral-800/40 opacity-50 hover:opacity-80'
                  : 'bg-neutral-900/80 border-neutral-800 hover:bg-neutral-800/80'
              }`}
            >
              {/* Snapshot Mini Thumbnail */}
              <div className="w-12 h-12 rounded-lg bg-neutral-950 overflow-hidden flex-shrink-0 border border-neutral-800 relative">
                {item.thumbnail ? (
                  <img
                    src={item.thumbnail}
                    alt={item.title}
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <div className="w-full h-full flex items-center justify-center text-neutral-600">
                    <Camera className="w-4 h-4" />
                  </div>
                )}
                {isActive && (
                  <div className="absolute top-0.5 right-0.5 w-2 h-2 rounded-full bg-sky-400 animate-pulse" />
                )}
              </div>

              {/* Action Info */}
              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between">
                  <h4
                    className={`text-xs font-semibold truncate ${
                      isActive ? 'text-sky-300' : 'text-neutral-200'
                    }`}
                  >
                    {item.title}
                  </h4>
                  <span className="text-[10px] font-mono text-neutral-500">
                    {item.timestamp}
                  </span>
                </div>
                <p className="text-[10px] text-neutral-400 mt-0.5">
                  Paso #{index + 1} {isActive ? '• Estado actual' : ''}
                </p>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
