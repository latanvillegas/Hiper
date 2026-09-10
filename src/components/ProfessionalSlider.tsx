import React, { useState, useEffect } from 'react';

interface ProfessionalSliderProps {
  label: string;
  value: number;
  min: number;
  max: number;
  step?: number;
  unit?: string;
  defaultValue?: number;
  tooltip?: string;
  onChange: (val: number) => void;
  accentColor?: string;
}

export const ProfessionalSlider: React.FC<ProfessionalSliderProps> = ({
  label,
  value,
  min,
  max,
  step = 1,
  unit = '',
  defaultValue = 0,
  tooltip,
  onChange,
  accentColor = 'text-sky-400',
}) => {
  const [isEditing, setIsEditing] = useState(false);
  const [tempInput, setTempInput] = useState(value.toString());

  useEffect(() => {
    setTempInput(value.toString());
  }, [value]);

  const handleInputSubmit = () => {
    let num = parseFloat(tempInput);
    if (isNaN(num)) num = defaultValue;
    num = Math.max(min, Math.min(max, num));
    onChange(num);
    setIsEditing(false);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      handleInputSubmit();
    } else if (e.key === 'Escape') {
      setTempInput(value.toString());
      setIsEditing(false);
    }
  };

  const handleReset = () => {
    onChange(defaultValue);
  };

  // Calculate percentage relative to range
  const percent = ((value - min) / (max - min)) * 100;
  // If min < 0 and max > 0, calculate center percentage
  const hasCenterZero = min < 0 && max > 0;
  const zeroPercent = hasCenterZero ? ((-min) / (max - min)) * 100 : 0;
  const isPositive = value >= 0;

  return (
    <div className="group flex flex-col gap-1 py-1.5 px-2 rounded-lg hover:bg-neutral-900/60 transition-colors">
      {/* Header: Label + Precise Numeric Input */}
      <div className="flex items-center justify-between text-xs">
        <span
          className="text-neutral-300 font-medium cursor-pointer select-none flex items-center gap-1 hover:text-white"
          onDoubleClick={handleReset}
          title={tooltip || 'Doble clic para resetear'}
        >
          {label}
          {value !== defaultValue && (
            <span className="w-1.5 h-1.5 rounded-full bg-sky-400 animate-pulse" />
          )}
        </span>

        {isEditing ? (
          <input
            type="number"
            step={step}
            value={tempInput}
            onChange={(e) => setTempInput(e.target.value)}
            onBlur={handleInputSubmit}
            onKeyDown={handleKeyDown}
            autoFocus
            className="w-16 px-1.5 py-0.5 text-right font-mono text-xs bg-neutral-950 text-sky-400 border border-sky-500 rounded outline-none focus:ring-1 focus:ring-sky-400"
          />
        ) : (
          <button
            onClick={() => setIsEditing(true)}
            onDoubleClick={handleReset}
            className={`font-mono text-xs font-semibold px-1.5 py-0.5 rounded hover:bg-neutral-800 ${
              value !== defaultValue ? accentColor : 'text-neutral-400'
            }`}
            title="Clic para ingresar número exacto, doble clic para resetear"
          >
            {value > 0 && min < 0 ? `+${value}` : value}
            {unit}
          </button>
        )}
      </div>

      {/* Slider Track with Zero Indicator */}
      <div className="relative flex items-center h-4 cursor-pointer">
        {/* Background Track */}
        <div className="w-full h-1.5 bg-neutral-800 rounded-full overflow-hidden relative">
          {hasCenterZero ? (
            /* Bi-directional filled track from center zero */
            <div
              className={`absolute h-full ${
                isPositive ? 'bg-sky-500' : 'bg-rose-500'
              } transition-all duration-75`}
              style={{
                left: isPositive ? `${zeroPercent}%` : `${percent}%`,
                width: isPositive
                  ? `${percent - zeroPercent}%`
                  : `${zeroPercent - percent}%`,
              }}
            />
          ) : (
            /* Standard left-to-right track */
            <div
              className="h-full bg-sky-500 transition-all duration-75"
              style={{ width: `${percent}%` }}
            />
          )}
        </div>

        {/* Center Zero tick mark for bi-directional sliders */}
        {hasCenterZero && (
          <div
            className="absolute top-1/2 -translate-y-1/2 w-0.5 h-2.5 bg-neutral-500 z-0 pointer-events-none"
            style={{ left: `${zeroPercent}%` }}
          />
        )}

        {/* Native Input range overlay */}
        <input
          type="range"
          min={min}
          max={max}
          step={step}
          value={value}
          onChange={(e) => onChange(parseFloat(e.target.value))}
          className="absolute inset-0 opacity-0 cursor-pointer w-full h-full z-10"
        />

        {/* Custom Draggable Thumb */}
        <div
          className="absolute top-1/2 -translate-y-1/2 -translate-x-1/2 w-3.5 h-3.5 bg-white rounded-full shadow-md border border-neutral-700 pointer-events-none transition-transform group-hover:scale-110"
          style={{ left: `${percent}%` }}
        />
      </div>
    </div>
  );
};
