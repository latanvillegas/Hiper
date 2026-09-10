import React, { useState } from 'react';
import {
  Sparkles,
  Sliders,
  Eye,
  Activity,
  ChevronRight,
  ChevronLeft,
  X,
  Compass,
  CheckCircle2,
} from 'lucide-react';

interface OnboardingTutorialProps {
  isOpen: boolean;
  onClose: () => void;
}

interface TutorialStep {
  title: string;
  subtitle: string;
  description: string;
  badge: string;
  icon: any;
  tip: string;
}

const TUTORIAL_STEPS: TutorialStep[] = [
  {
    title: 'Bienvenido a PhotoEngine Pro',
    subtitle: 'Motor de Edición Fotográfica con Vulkan GPU',
    description:
      'Has iniciado la estación de trabajo profesional para Android & Web con procesamiento nativo de 32 bits punto flotante en GPU sin latencia.',
    badge: 'Arquitectura GPU',
    icon: Sparkles,
    tip: 'El motor compila shaders GLSL ES 3.00 y Vulkan Compute para garantizar 60 FPS en tiempo real.',
  },
  {
    title: 'Lienzo Interactivo y Gestos Táctiles',
    subtitle: 'Navegación Fluida con Zoom y Paneo',
    description:
      'Utiliza pellizco para zoom (10% a 500%), arrastra con dos dedos para navegar el encuadre, y pulsa dos veces sobre la imagen para regresar al 100% 1:1 o ajustar a pantalla.',
    badge: 'Gestos Multi-Touch',
    icon: Compass,
    tip: 'El minimap del Navegador en la esquina superior izquierda te permite ubicarte cuando trabajas a máximo zoom.',
  },
  {
    title: 'Histograma RGB y Detección de Clipping',
    subtitle: 'Control de Rango Dinámico en Tiempo Real',
    description:
      'El histograma de 256 bins analiza los canales R, G, B y Luma. Detecta recorte crítico: rojo para altas luces quemadas y azul para sombras empastadas. Activa el "Overlay Zebra" para ver los píxeles afectados.',
    badge: 'Warnings de Clipping',
    icon: Activity,
    tip: 'Ajusta los deslizadores de Punto Negro y Punto Blanco para maximizar el rango dinámico sin perder detalle.',
  },
  {
    title: 'Ruedas de Color Grading (3-Way)',
    subtitle: 'Gradación Cinematográfica Profesional',
    description:
      'Tres ruedas de color dedicadas al estilo DaVinci Resolve: Sombras (Lift), Medios Tonos (Gamma) y Altas Luces (Gain). Arrastra el disco cromático para inyectar tono y ajusta la luminancia inferior.',
    badge: 'Color Grading',
    icon: Sliders,
    tip: 'La combinación clásica Teal & Orange inyecta cyan frío en sombras y ámbar cálido en las altas luces.',
  },
  {
    title: 'Comparación Antes / Después e Historial',
    subtitle: 'Control Total de tus Decisiones de Edición',
    description:
      'Alterna entre 4 modos de inspección: Slider horizontal, Slider vertical, Split 50/50 y Modo Blink. Además, cuentas con un historial ilimitado con snapshots visuales para regresar a cualquier paso.',
    badge: 'Inspección A/B',
    icon: Eye,
    tip: 'Presiona la tecla B o mantén pulsado el botón Blink para alternar instantáneamente con la foto original.',
  },
];

export const OnboardingTutorial: React.FC<OnboardingTutorialProps> = ({
  isOpen,
  onClose,
}) => {
  const [currentStep, setCurrentStep] = useState(0);

  if (!isOpen) return null;

  const step = TUTORIAL_STEPS[currentStep];
  const Icon = step.icon;
  const isLast = currentStep === TUTORIAL_STEPS.length - 1;

  const handleNext = () => {
    if (isLast) {
      onClose();
    } else {
      setCurrentStep((prev) => prev + 1);
    }
  };

  const handlePrev = () => {
    setCurrentStep((prev) => Math.max(0, prev - 1));
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-in fade-in duration-200">
      <div className="w-full max-w-lg bg-neutral-900 border border-neutral-800 rounded-3xl shadow-2xl overflow-hidden flex flex-col">
        {/* Header Ribbon */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-neutral-800 bg-neutral-950/60">
          <div className="flex items-center gap-2">
            <span className="text-xs font-mono font-bold px-2 py-0.5 rounded-full bg-sky-950 text-sky-400 border border-sky-800">
              {step.badge}
            </span>
            <span className="text-[11px] text-neutral-400 font-mono">
              Paso {currentStep + 1} de {TUTORIAL_STEPS.length}
            </span>
          </div>

          <button
            onClick={onClose}
            className="p-1.5 rounded-xl text-neutral-400 hover:text-white hover:bg-neutral-800 transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Step Body */}
        <div className="p-6 flex flex-col gap-4">
          <div className="w-14 h-14 rounded-2xl bg-gradient-to-tr from-sky-600 to-indigo-600 flex items-center justify-center shadow-lg shadow-sky-950/50">
            <Icon className="w-7 h-7 text-white" />
          </div>

          <div>
            <h2 className="text-lg font-bold text-neutral-100">{step.title}</h2>
            <h3 className="text-xs font-semibold text-sky-400 mt-0.5">
              {step.subtitle}
            </h3>
            <p className="text-xs text-neutral-300 leading-relaxed mt-2.5">
              {step.description}
            </p>
          </div>

          {/* Pro Tip Box */}
          <div className="bg-neutral-950 p-3 rounded-2xl border border-neutral-800 flex items-start gap-2.5">
            <Sparkles className="w-4 h-4 text-amber-400 flex-shrink-0 mt-0.5" />
            <p className="text-[11px] text-neutral-400 leading-normal font-mono">
              <span className="text-amber-300 font-semibold">Consejo Pro: </span>
              {step.tip}
            </p>
          </div>

          {/* Progress Indicators */}
          <div className="flex items-center gap-1.5 pt-2">
            {TUTORIAL_STEPS.map((_, idx) => (
              <div
                key={idx}
                className={`h-1.5 rounded-full transition-all duration-300 ${
                  idx === currentStep
                    ? 'w-8 bg-sky-400'
                    : idx < currentStep
                    ? 'w-2 bg-neutral-600'
                    : 'w-2 bg-neutral-800'
                }`}
              />
            ))}
          </div>
        </div>

        {/* Action Footer */}
        <div className="flex items-center justify-between px-6 py-4 border-t border-neutral-800 bg-neutral-950/80">
          <button
            onClick={handlePrev}
            disabled={currentStep === 0}
            className={`flex items-center gap-1 text-xs font-semibold px-3 py-2 rounded-xl transition-colors ${
              currentStep === 0
                ? 'opacity-0 pointer-events-none'
                : 'text-neutral-400 hover:text-white hover:bg-neutral-800'
            }`}
          >
            <ChevronLeft className="w-4 h-4" />
            <span>Anterior</span>
          </button>

          <button
            onClick={handleNext}
            className="flex items-center gap-2 text-xs font-bold px-5 py-2.5 rounded-xl bg-sky-600 hover:bg-sky-500 text-white shadow-lg shadow-sky-950 transition-all hover:scale-105"
          >
            <span>{isLast ? 'Comenzar a Editar' : 'Siguiente'}</span>
            {isLast ? <CheckCircle2 className="w-4 h-4" /> : <ChevronRight className="w-4 h-4" />}
          </button>
        </div>
      </div>
    </div>
  );
};
