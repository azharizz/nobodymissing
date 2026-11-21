// Kit, the calm firefighter cat. Original hand-made SVG (Apache-2.0). High contrast, prints in black and white.
export type Pose = 'card' | 'lantern' | 'bag' | 'wave' | 'sit';

export function Kit({ pose = 'sit', size = 48, label = 'Kit' }: { pose?: Pose; size?: number; label?: string }) {
  return (
    <svg width={size} height={size} viewBox="0 0 64 64" role="img" aria-label={label}>
      <path d="M14 26 L18 10 L26 20 Z M50 26 L46 10 L38 20 Z" fill="#6b5a4a" stroke="#222" strokeWidth="1.5" />
      <circle cx="32" cy="30" r="15" fill="#d9b48f" stroke="#222" strokeWidth="1.5" />
      <path d="M15 24 Q32 6 49 24 L49 21 Q32 2 15 21 Z" fill="#d7261e" stroke="#222" strokeWidth="1.5" />
      <rect x="29" y="11" width="6" height="6" rx="1" fill="#ffd400" stroke="#222" />
      <circle cx="26" cy="30" r="2" fill="#222" /><circle cx="38" cy="30" r="2" fill="#222" />
      <path d="M29 36 Q32 38 35 36" stroke="#222" strokeWidth="1.5" fill="none" />
      <path d="M20 34 H12 M20 37 H13 M44 34 H52 M44 37 H51" stroke="#222" strokeWidth="1" />
      <path d="M18 45 Q32 40 46 45 L48 62 H16 Z" fill="#f28c00" stroke="#222" strokeWidth="1.5" />
      <path d="M18 52 H46" stroke="#e8ff3a" strokeWidth="3" />
      {pose === 'lantern' && <g><path d="M50 44 V48" stroke="#222" /><rect x="46" y="48" width="8" height="10" rx="2" fill="#ffd400" stroke="#222" /></g>}
      {pose === 'card' && <rect x="4" y="44" width="16" height="12" rx="2" fill="#fff" stroke="#222" strokeWidth="1.5" />}
      {pose === 'bag' && <rect x="44" y="46" width="14" height="12" rx="3" fill="#2a6fdb" stroke="#222" strokeWidth="1.5" />}
      {pose === 'wave' && <path d="M48 46 L56 36" stroke="#d9b48f" strokeWidth="5" strokeLinecap="round" />}
    </svg>
  );
}
