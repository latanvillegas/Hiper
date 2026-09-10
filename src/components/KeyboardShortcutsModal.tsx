import React, { useState } from 'react';
import { KeyboardShortcut } from '../types';
import { X, Keyboard, RotateCcw, Search } from 'lucide-react';

interface KeyboardShortcutsModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const DEFAULT_SHORTCUTS: KeyboardShortcut[] = [
  { id: '1', key: 'Espacio + Arrastre', label: 'Mover lienzo (Mano / Pan)', category: 'Navegación' },
  { id: '2', key: '+ / -', label: 'Aumentar / Reducir zoom', category: 'Navegación' },
  { id: '3', key: '1 / 0', label: 'Vista 1:1 al 100% / Ajustar pantalla', category: 'Navegación' },
  { id: '4', key: 'Doble Clic', label: 'Restablecer zoom y centrar', category: 'Navegación' },
  { id: '5', key: 'Ctrl + Z', label: 'Deshacer última acción', category: 'Edición' },
  { id: '6', key: 'Ctrl + Y', label: 'Rehacer acción', category: 'Edición' },
  { id: '7', key: 'Ctrl + S', label: 'Crear Snapshot de estado', category: 'Edición' },
  { id: '8', key: 'Ctrl + E', label: 'Exportar imagen renderizada', category: 'Edición' },
  { id: '9', key: '\\', label: 'Alternar comparación Antes / Después', category: 'Visualización' },
  { id: '10', key: 'B', label: 'Mantener presionado para Blink original', category: 'Visualización' },
  { id: '11', key: 'O', label: 'Activar/desactivar máscara de clipping', category: 'Visualización' },
  { id: '12', key: 'Tab', label: 'Ocultar / Mostrar todos los paneles', category: 'Visualización' },
  { id: '13', key: '[ / ]', label: 'Disminuir / Aumentar radio del pincel', category: 'Herramientas' },
  { id: '14', key: 'H', label: 'Activar Color Grading Ruedas', category: 'Herramientas' },
  { id: '15', key: 'C', label: 'Herramienta Curvas RGB 14 Puntos', category: 'Herramientas' },
  { id: '16', key: 'M', label: 'Máscaras AI y Segmentación', category: 'Herramientas' },
];

export const KeyboardShortcutsModal: React.FC<KeyboardShortcutsModalProps> = ({
  isOpen,
  onClose,
}) => {
  const [shortcuts, setShortcuts] = useState<KeyboardShortcut[]>(DEFAULT_SHORTCUTS);
  const [searchQuery, setSearchQuery] = useState('');
  const [activeCategory, setActiveCategory] = useState<string>('Todos');

  if (!isOpen) return null;

  const categories = ['Todos', 'Navegación', 'Edición', 'Visualización', 'Herramientas'];

  const filteredShortcuts = shortcuts.filter((sc) => {
    const matchCat = activeCategory === 'Todos' || sc.category === activeCategory;
    const matchSearch =
      sc.label.toLowerCase().includes(searchQuery.toLowerCase()) ||
      sc.key.toLowerCase().includes(searchQuery.toLowerCase());
    return matchCat && matchSearch;
  });

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-in fade-in duration-150">
      <div className="w-full max-w-xl bg-neutral-900 border border-neutral-800 rounded-2xl shadow-2xl overflow-hidden flex flex-col max-h-[85vh]">
        {/* Header */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-neutral-800">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-xl bg-sky-950 border border-sky-800 flex items-center justify-center text-sky-400">
              <Keyboard className="w-4 h-4" />
            </div>
            <div>
              <h2 className="text-sm font-bold text-neutral-100">
                Atajos de Teclado Profesionales
              </h2>
              <p className="text-[11px] text-neutral-400">
                Optimizado para tablets con teclado y estaciones de trabajo
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-neutral-400 hover:text-white hover:bg-neutral-800 transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Search & Filter bar */}
        <div className="px-5 py-3 border-b border-neutral-800/80 bg-neutral-950/40 flex flex-col sm:flex-row gap-2.5 items-center justify-between">
          <div className="relative w-full sm:w-64">
            <Search className="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-neutral-500" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Buscar atajo..."
              className="w-full pl-8 pr-3 py-1.5 rounded-xl bg-neutral-900 border border-neutral-800 text-xs text-neutral-200 outline-none focus:border-sky-500"
            />
          </div>

          <div className="flex items-center gap-1 overflow-x-auto w-full sm:w-auto scrollbar-none">
            {categories.map((cat) => (
              <button
                key={cat}
                onClick={() => setActiveCategory(cat)}
                className={`px-2.5 py-1 rounded-lg text-xs font-medium transition-colors whitespace-nowrap ${
                  activeCategory === cat
                    ? 'bg-sky-600 text-white'
                    : 'text-neutral-400 hover:text-white hover:bg-neutral-800'
                }`}
              >
                {cat}
              </button>
            ))}
          </div>
        </div>

        {/* Shortcuts List */}
        <div className="flex-1 overflow-y-auto p-5 space-y-2 scrollbar-thin">
          {filteredShortcuts.map((sc) => (
            <div
              key={sc.id}
              className="flex items-center justify-between p-2.5 rounded-xl bg-neutral-950/60 border border-neutral-800/70 hover:border-neutral-700 transition-colors"
            >
              <div className="flex flex-col">
                <span className="text-xs font-medium text-neutral-200">
                  {sc.label}
                </span>
                <span className="text-[10px] text-neutral-500 font-mono">
                  {sc.category}
                </span>
              </div>

              <kbd className="px-2.5 py-1 rounded-lg bg-neutral-800 border border-neutral-700 text-sky-400 font-mono text-xs font-bold shadow-inner">
                {sc.key}
              </kbd>
            </div>
          ))}
        </div>

        {/* Footer */}
        <div className="px-5 py-3 border-t border-neutral-800 bg-neutral-950/80 flex items-center justify-between text-xs text-neutral-400">
          <span>Compatible con Magic Keyboard, Galaxy Tab S-Pen & Teclados USB</span>
          <button
            onClick={() => setShortcuts(DEFAULT_SHORTCUTS)}
            className="flex items-center gap-1 hover:text-white"
          >
            <RotateCcw className="w-3 h-3" />
            <span>Restablecer atajos</span>
          </button>
        </div>
      </div>
    </div>
  );
};
