import React from 'react';

interface HealingPoint {
  target: { x: number; y: number };
  donor: { x: number; y: number };
  radius: number;
}

interface HealingOverlayProps {
  healingPoints: HealingPoint[];
  activeRadius: number;
  width: number;
  height: number;
}

export const HealingOverlay: React.FC<HealingOverlayProps> = ({
  healingPoints,
  width,
  height,
}) => {
  if (healingPoints.length === 0) return null;

  return (
    <svg
      width={width}
      height={height}
      className="absolute inset-0 pointer-events-none z-10 w-full h-full"
    >
      {healingPoints.map((pt, idx) => (
        <g key={idx}>
          {/* Connecting line */}
          <line
            x1={pt.donor.x}
            y1={pt.donor.y}
            x2={pt.target.x}
            y2={pt.target.y}
            stroke="#22c55e"
            strokeWidth="1.5"
            strokeDasharray="4 2"
          />

          {/* Donor circle (green) */}
          <circle
            cx={pt.donor.x}
            cy={pt.donor.y}
            r={pt.radius}
            fill="rgba(34, 197, 94, 0.2)"
            stroke="#22c55e"
            strokeWidth="1.5"
          />
          <text
            x={pt.donor.x}
            y={pt.donor.y - pt.radius - 4}
            fill="#22c55e"
            fontSize="10"
            fontFamily="monospace"
            textAnchor="middle"
          >
            Muestreo Donante
          </text>

          {/* Target circle (red) */}
          <circle
            cx={pt.target.x}
            cy={pt.target.y}
            r={pt.radius}
            fill="rgba(239, 68, 68, 0.2)"
            stroke="#ef4444"
            strokeWidth="1.5"
          />
          <text
            x={pt.target.x}
            y={pt.target.y - pt.radius - 4}
            fill="#ef4444"
            fontSize="10"
            fontFamily="monospace"
            textAnchor="middle"
          >
            Healing Target
          </text>
        </g>
      ))}
    </svg>
  );
};
