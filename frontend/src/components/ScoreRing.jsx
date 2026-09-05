import { useCountUp } from './CountUp';

function scoreColor(score) {
  if (score >= 75) return '#059669';
  if (score >= 50) return '#d97706';
  if (score >= 25) return '#f59e0b';
  return '#dc2626';
}

export default function ScoreRing({ score, size = 160, stroke = 12, label = '/100', inverted = false }) {
  const animated = useCountUp(typeof score === 'number' ? score : 0);
  const clamped = Math.max(0, Math.min(100, Number(score) || 0));
  const radius = (size - stroke) / 2;
  const circumference = 2 * Math.PI * radius;
  const offset = circumference - (clamped / 100) * circumference;
  const color = inverted ? '#4f46e5' : scoreColor(clamped);

  return (
    <div className="score-ring" style={{ width: size, height: size }}>
      <svg width={size} height={size} className="score-ring-svg">
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke="#eef0f7"
          strokeWidth={stroke}
        />
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke={color}
          strokeWidth={stroke}
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          transform={`rotate(-90 ${size / 2} ${size / 2})`}
          style={{ transition: 'stroke-dashoffset 0.9s cubic-bezier(0.22, 1, 0.36, 1)' }}
        />
      </svg>
      <div className="score-ring-value">
        <strong>{Math.round(animated).toLocaleString('en-IN')}</strong>
        <span>{label}</span>
      </div>
    </div>
  );
}

