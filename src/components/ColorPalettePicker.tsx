import React, { useState } from 'react';
import { Pipette, Check, Sparkles } from 'lucide-react';

interface ColorPalettePickerProps {
  selectedColor: string;
  onSelectColor: (hex: string) => void;
  label?: string;
}

const PRO_PALETTES = [
  {
    name: 'Teal & Orange',
    colors: ['#044343', '#007f7f', '#00b4d8', '#f77f00', '#fcbf49', '#eae2b7'],
  },
  {
    name: 'Kodak Portra Film',
    colors: ['#2e2925', '#6b584a', '#a68a74', '#dfc2a6', '#f4e9dc', '#e07a5f'],
  },
  {
    name: 'Golden Hour',
    colors: ['#3d0c02', '#851e07', '#d94e1f', '#f28e2b', '#f9c74f', '#f9f871'],
  },
  {
    name: 'Cyberpunk Neon',
    colors: ['#03071e', '#370617', '#9d0208', '#ff0054', '#7209b7', '#4cc9f0'],
  },
  {
    name: 'Retrato Fine Art',
    colors: ['#301b1b', '#623b3b', '#9b6a6a', '#d4a5a5', '#f3d8d8', '#fbf5f5'],
  },
  {
    name: 'Monocromo Graduado',
    colors: ['#0d0d0d', '#262626', '#525252', '#737373', '#a3a3a3', '#e5e5e5'],
  },
];

export const ColorPalettePicker: React.FC<ColorPalettePickerProps> = ({
  selectedColor,
  onSelectColor,
  label = 'Paleta de Selección de Color Precisa',
}) => {
  const [customHex, setCustomHex] = useState(selectedColor);
  const hasEyeDropper = typeof window !== 'undefined' && 'EyeDropper' in window;

  const handleEyedropper = async () => {
    if (hasEyeDropper) {
      try {
        const eyeDropper = new (window as any).EyeDropper();
        const result = await eyeDropper.open();
        if (result && result.sRGBHex) {
          onSelectColor(result.sRGBHex);
          setCustomHex(result.sRGBHex);
        }
      } catch (e) {
        // Canceled
      }
    }
  };

  const handleHexChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value;
    setCustomHex(val);
    if (/^#[0-9A-Fa-f]{6}$/.test(val)) {
      onSelectColor(val);
    }
  };

  return (
    <div className="flex flex-col gap-3 p-3 bg-neutral-900/90 rounded-2xl border border-neutral-800 text-xs">
      <div className="flex items-center justify-between">
        <span className="font-semibold text-neutral-200">{label}</span>
        <div className="flex items-center gap-2">
          {hasEyeDropper && (
            <button
              onClick={handleEyedropper}
              className="flex items-center gap-1 px-2 py-1 rounded bg-neutral-800 hover:bg-neutral-700 text-neutral-200 text-[11px]"
              title="Cuentagotas: Muestrear color exacto de la pantalla"
            >
              <Pipette className="w-3 h-3 text-sky-400" />
              <span>Gotero</span>
            </button>
          )}

          {/* Color Preview & Native Picker */}
          <div className="relative flex items-center">
            <input
              type="color"
              value={selectedColor}
              onChange={(e) => {
                onSelectColor(e.target.value);
                setCustomHex(e.target.value);
              }}
              className="opacity-0 absolute inset-0 w-6 h-6 cursor-pointer"
            />
            <div
              className="w-5 h-5 rounded-full border border-neutral-600 shadow-sm"
              style={{ backgroundColor: selectedColor }}
            />
          </div>

          <input
            type="text"
            value={customHex}
            onChange={handleHexChange}
            className="w-18 px-1.5 py-0.5 font-mono text-[11px] bg-neutral-950 border border-neutral-700 rounded text-sky-400"
            placeholder="#000000"
          />
        </div>
      </div>

      {/* Palettes grid */}
      <div className="flex flex-col gap-2">
        {PRO_PALETTES.map((pal) => (
          <div key={pal.name} className="flex flex-col gap-1">
            <span className="text-[10px] text-neutral-400 font-medium">
              {pal.name}
            </span>
            <div className="flex items-center gap-1.5">
              {pal.colors.map((c) => {
                const isSelected =
                  selectedColor.toLowerCase() === c.toLowerCase();
                return (
                  <button
                    key={c}
                    onClick={() => {
                      onSelectColor(c);
                      setCustomHex(c);
                    }}
                    className={`group relative flex-1 h-6 rounded-md transition-transform hover:scale-105 border ${
                      isSelected
                        ? 'border-white ring-2 ring-sky-500 scale-105 z-10'
                        : 'border-neutral-700/60'
                    }`}
                    style={{ backgroundColor: c }}
                    title={c}
                  >
                    {isSelected && (
                      <Check className="w-3 h-3 text-white absolute inset-0 m-auto drop-shadow" />
                    )}
                  </button>
                );
              })}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
