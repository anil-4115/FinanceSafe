import CountUp from './CountUp';

export default function StatCard({ icon: Icon, iconTone, label, value, trend, decimals = 0, prefix = '', children }) {
  return (
    <div className="stat-card fade-up">
      {Icon && (
        <span className={`stat-icon ${iconTone || ''}`}>
          <Icon size={18} strokeWidth={2.2} />
        </span>
      )}
      <span>{label}</span>
      <strong>
        {prefix}
        {typeof value === 'number' || typeof value === 'string' && !Number.isNaN(Number(value))
          ? <CountUp value={value} decimals={decimals} />
          : value}
      </strong>
      {trend && (
        <span className={`trend-pill ${trend.direction === 'up' ? 'up' : trend.direction === 'down' ? 'down' : 'flat'}`}>
          {trend.text}
        </span>
      )}
      {children}
    </div>
  );
}