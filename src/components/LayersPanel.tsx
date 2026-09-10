import React from 'react';
import { BlendMode, LayerItem } from '../types';
import { Layers, Eye, EyeOff, Plus, Trash2 } from 'lucide-react';

interface LayersPanelProps {
  layers: LayerItem[];
  selectedLayerId: string;
  onSelectLayer: (id: string) => void;
  onUpdateLayer: (id: string, updates: Partial<LayerItem>) => void;
  onAddLayer: () => void;
  onDeleteLayer: (id: string) => void;
}

const BLEND_MODES: BlendMode[] = [
  'Normal',
  'Multiply',
  'Screen',
  'Overlay',
  'Soft Light',
  'Hard Light',
  'Color Dodge',
  'Color Burn',
  'Difference',
  'Exclusion',
  'Linear Dodge (Add)',
];

export const LayersPanel: React.FC<LayersPanelProps> = ({
  layers,
  selectedLayerId,
  onSelectLayer,
  onUpdateLayer,
  onAddLayer,
  onDeleteLayer,
}) => {
  const selectedLayer = layers.find((l) => l.id === selectedLayerId) || layers[0];

  return (
    <div className="flex flex-col gap-3">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Layers className="w-4 h-4 text-purple-400" />
          <span className="text-xs font-semibold text-slate-200">Capas & Blend Modes</span>
        </div>
        <button
          onClick={onAddLayer}
          className="p-1 rounded bg-slate-800 border border-slate-700 hover:bg-slate-700 text-slate-300 transition-colors"
          title="Añadir capa"
        >
          <Plus className="w-3.5 h-3.5" />
        </button>
      </div>

      {/* Selected Layer Properties */}
      {selectedLayer && (
        <div className="bg-slate-900/80 rounded-xl p-3 border border-slate-800/80 space-y-2.5">
          <div className="flex items-center justify-between gap-2">
            <span className="text-xs text-slate-400">Modo de Fusión:</span>
            <select
              value={selectedLayer.blendMode}
              onChange={(e) => onUpdateLayer(selectedLayer.id, { blendMode: e.target.value as BlendMode })}
              className="bg-slate-800 border border-slate-700 text-xs text-slate-200 rounded-lg px-2 py-1 outline-none focus:border-sky-500"
            >
              {BLEND_MODES.map((mode) => (
                <option key={mode} value={mode}>
                  {mode}
                </option>
              ))}
            </select>
          </div>

          <div className="space-y-1">
            <div className="flex justify-between text-[11px] text-slate-400">
              <span>Opacidad</span>
              <span className="font-mono text-slate-300">{Math.round(selectedLayer.opacity * 100)}%</span>
            </div>
            <input
              type="range"
              min={0}
              max={1}
              step={0.01}
              value={selectedLayer.opacity}
              onChange={(e) => onUpdateLayer(selectedLayer.id, { opacity: parseFloat(e.target.value) })}
              className="w-full accent-purple-400 h-1.5 bg-slate-800 rounded-lg cursor-pointer"
            />
          </div>
        </div>
      )}

      {/* Layers List */}
      <div className="space-y-1.5 max-h-48 overflow-y-auto pr-1">
        {layers.map((layer) => {
          const isSelected = layer.id === selectedLayerId;
          return (
            <div
              key={layer.id}
              onClick={() => onSelectLayer(layer.id)}
              className={`flex items-center justify-between p-2 rounded-lg border text-xs cursor-pointer transition-all ${
                isSelected
                  ? 'bg-slate-800 border-purple-500/80 shadow-sm text-white'
                  : 'bg-slate-900/40 border-slate-800/80 hover:bg-slate-800/60 text-slate-400'
              }`}
            >
              <div className="flex items-center gap-2">
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    onUpdateLayer(layer.id, { visible: !layer.visible });
                  }}
                  className="text-slate-400 hover:text-slate-200"
                >
                  {layer.visible ? <Eye className="w-3.5 h-3.5 text-sky-400" /> : <EyeOff className="w-3.5 h-3.5 text-slate-600" />}
                </button>
                <span className="font-medium">{layer.name}</span>
              </div>

              <div className="flex items-center gap-2">
                <span className="text-[10px] text-slate-500 font-mono">{layer.blendMode}</span>
                {layers.length > 1 && (
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      onDeleteLayer(layer.id);
                    }}
                    className="text-slate-500 hover:text-red-400"
                  >
                    <Trash2 className="w-3 h-3" />
                  </button>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
