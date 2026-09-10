export interface SamplePhoto {
  id: string;
  title: string;
  category: 'Retrato' | 'Paisaje' | 'Cine Urbano' | 'Estudio';
  url: string;
  description: string;
}

export const SAMPLE_PHOTOS: SamplePhoto[] = [
  {
    id: 'portrait',
    title: 'Retrato Editorial (Beauty)',
    category: 'Retrato',
    url: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=1000&auto=format&fit=crop',
    description: 'Ideal para Face Mesh 468, suavizado bilateral, slimming y maquillaje virtual.',
  },
  {
    id: 'architecture',
    title: 'Arquitectura & Perspectiva',
    category: 'Paisaje',
    url: 'https://images.unsplash.com/photo-1513694203232-719a280e022f?q=80&w=1000&auto=format&fit=crop',
    description: 'Ideal para corrección de perspectiva, keystone vertical/horizontal y curvas RGB.',
  },
  {
    id: 'night_street',
    title: 'Cine Urbano Nocturno',
    category: 'Cine Urbano',
    url: 'https://images.unsplash.com/photo-1509198397868-475647b2a1e5?q=80&w=1000&auto=format&fit=crop',
    description: 'Ideal para halation Kodak 2383, bloom óptico, grano analógico y bokeh.',
  },
  {
    id: 'landscape',
    title: 'Naturaleza & Cielo',
    category: 'Paisaje',
    url: 'https://images.unsplash.com/photo-1506744038136-46273834b3fb?q=80&w=1000&auto=format&fit=crop',
    description: 'Ideal para máscaras AI (cielo, agua, vegetación) y HSL de 8 canales.',
  },
];
