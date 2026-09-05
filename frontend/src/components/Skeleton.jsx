export default function Skeleton({ rows = 4, cards = 3 }) {
  return (
    <div className="stagger">
      {Array.from({ length: Math.max(1, cards) }).map((_, index) => (
        <div className="skeleton-card" key={index}>
          <div className="skeleton" style={{ width: '38%', height: 18 }} />
          {Array.from({ length: rows }).map((__, rowIndex) => (
            <div className="skeleton" key={rowIndex} style={{ width: `${100 - rowIndex * 14}%`, height: 14 }} />
          ))}
        </div>
      ))}
    </div>
  );
}