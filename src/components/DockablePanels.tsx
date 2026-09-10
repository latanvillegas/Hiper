import React from 'react';
import { DockablePanelState, DockPosition } from '../types';
import {
  Layout,
  MoveRight,
  MoveLeft,
  MoveDown,
  Eye,
  EyeOff,
  Maximize2,
  Minimize2,
  X,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';

interface DockablePanelsProps {
  panels: DockablePanelState[];
  position: DockPosition;
  onToggleCollapse: (id: string) => void;
  onClosePanel: (id: string) => void;
  onDockChange: (id: string, newPos: DockPosition) => void;
  renderPanelContent: (id: string) => React.ReactNode;
}

export const DockablePanels: React.FC<DockablePanelsProps> = ({
  panels,
  position,
  onToggleCollapse,
  onClosePanel,
  onDockChange,
  renderPanelContent,
}) => {
  const matchingPanels = panels.filter((p) => p.position === position && p.visible);

  if (matchingPanels.length === 0) return null;

  return (
    <div
      className={`flex flex-col gap-3 p-2 bg-neutral-900/95 backdrop-blur-md overflow-y-auto z-10 select-none custom-scrollbar ${
        position === 'left'
          ? 'w-80 border-r border-neutral-800'
          : position === 'right'
          ? 'w-84 border-l border-neutral-800'
          : 'w-full max-h-60 border-t border-neutral-800'
      }`}
    >
      {matchingPanels.map((panel) => {
        const content = renderPanelContent(panel.id);
        if (!content) return null;

        return (
          <div
            key={panel.id}
            className="flex flex-col rounded-2xl bg-neutral-900 border border-neutral-800 overflow-hidden shadow-lg transition-all"
          >
            {/* Panel Header */}
            <div className="flex items-center justify-between px-3 py-2 bg-neutral-950/80 border-b border-neutral-800/80">
              <span className="text-xs font-bold text-neutral-200 tracking-wide flex items-center gap-1.5">
                <span className="w-1.5 h-1.5 rounded-full bg-sky-500" />
                {panel.title}
              </span>

              <div className="flex items-center gap-1">
                {/* Dock Position Switchers */}
                {position !== 'left' && (
                  <button
                    onClick={() => onDockChange(panel.id, 'left')}
                    className="p-1 text-neutral-400 hover:text-white rounded hover:bg-neutral-800"
                    title="Mover a la izquierda"
                  >
                    <MoveLeft className="w-3 h-3" />
                  </button>
                )}
                {position !== 'right' && (
                  <button
                    onClick={() => onDockChange(panel.id, 'right')}
                    className="p-1 text-neutral-400 hover:text-white rounded hover:bg-neutral-800"
                    title="Mover a la derecha"
                  >
                    <MoveRight className="w-3 h-3" />
                  </button>
                )}

                {/* Collapse / Expand */}
                <button
                  onClick={() => onToggleCollapse(panel.id)}
                  className="p-1 text-neutral-400 hover:text-white rounded hover:bg-neutral-800"
                  title={panel.isCollapsed ? 'Expandir panel' : 'Colapsar panel'}
                >
                  {panel.isCollapsed ? (
                    <ChevronDown className="w-3.5 h-3.5" />
                  ) : (
                    <ChevronUp className="w-3.5 h-3.5" />
                  )}
                </button>

                {/* Close / Hide */}
                <button
                  onClick={() => onClosePanel(panel.id)}
                  className="p-1 text-neutral-500 hover:text-rose-400 rounded hover:bg-neutral-800"
                  title="Cerrar panel"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>

            {/* Panel Body */}
            {!panel.isCollapsed && <div className="p-2">{content}</div>}
          </div>
        );
      })}
    </div>
  );
};

interface DockablePanelsManagerProps {
  panels: DockablePanelState[];
  onToggleVisibility: (id: string) => void;
  onChangePosition: (id: string, pos: DockPosition) => void;
  onToggleCollapse: (id: string) => void;
  onResetLayout: () => void;
}

export const DockablePanelsManager: React.FC<DockablePanelsManagerProps> = ({
  panels,
  onToggleVisibility,
  onChangePosition,
  onResetLayout,
}) => {
  return (
    <div className="flex flex-col gap-3 p-3 bg-neutral-900/90 rounded-2xl border border-neutral-800 text-xs shadow-lg">
      <div className="flex items-center justify-between">
        <span className="font-bold text-neutral-100 flex items-center gap-1.5">
          <Layout className="w-4 h-4 text-sky-400" />
          Configuración del Workspace Dockable
        </span>
        <button
          onClick={onResetLayout}
          className="text-[11px] text-neutral-400 hover:text-white"
        >
          Restablecer
        </button>
      </div>

      <div className="space-y-1.5">
        {panels.map((p) => (
          <div
            key={p.id}
            className="flex items-center justify-between p-2 rounded-xl bg-neutral-950/70 border border-neutral-800/80 hover:border-neutral-700"
          >
            <div className="flex items-center gap-2">
              <button
                onClick={() => onToggleVisibility(p.id)}
                className={`p-1 rounded ${
                  p.visible ? 'text-sky-400' : 'text-neutral-600'
                }`}
                title={p.visible ? 'Ocultar panel' : 'Mostrar panel'}
              >
                {p.visible ? <Eye className="w-3.5 h-3.5" /> : <EyeOff className="w-3.5 h-3.5" />}
              </button>
              <span
                className={`font-medium ${
                  p.visible ? 'text-neutral-200' : 'text-neutral-500 line-through'
                }`}
              >
                {p.title}
              </span>
            </div>

            {/* Position Dock buttons */}
            <div className="flex items-center gap-1">
              <button
                onClick={() => onChangePosition(p.id, 'left')}
                className={`p-1 rounded text-[10px] ${
                  p.position === 'left'
                    ? 'bg-sky-600 text-white'
                    : 'text-neutral-400 hover:bg-neutral-800'
                }`}
                title="Acoplar a la izquierda"
              >
                <MoveLeft className="w-3 h-3" />
              </button>
              <button
                onClick={() => onChangePosition(p.id, 'right')}
                className={`p-1 rounded text-[10px] ${
                  p.position === 'right'
                    ? 'bg-sky-600 text-white'
                    : 'text-neutral-400 hover:bg-neutral-800'
                }`}
                title="Acoplar a la derecha"
              >
                <MoveRight className="w-3 h-3" />
              </button>
              <button
                onClick={() => onChangePosition(p.id, 'bottom')}
                className={`p-1 rounded text-[10px] ${
                  p.position === 'bottom'
                    ? 'bg-sky-600 text-white'
                    : 'text-neutral-400 hover:bg-neutral-800'
                }`}
                title="Acoplar abajo"
              >
                <MoveDown className="w-3 h-3" />
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
