import { useEffect, useState } from 'react';
import { api } from '../services/api';
import {
  ResponsiveContainer,
  RadialBarChart,
  RadialBar,
  PolarAngleAxis,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Cell,
} from 'recharts';

function FinancialHealthPage() {
  const [health, setHealth] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get('/health-score')
      .then(({ data }) => setHealth(data))
      .catch(() => setError('Could not calculate your health score. Add some transactions and a profile first.'));
  }, []);

  if (error) return <div className="page-shell"><p className="form-error" role="alert">{error}</p></div>;
  if (!health) return <div className="page-shell"><p className="muted">Calculating your financial health…</p></div>;

  const components = (health.components || []).map((component) => ({
    name: component.name,
    score: component.score,
    full: component.name,
  }));

  return (
    <div className="page-shell">
      <h2>Financial Health</h2>

      <section className="health-hero-grid">
        <div className="panel health-gauge-card">
          <p className="eyebrow">Health Score</p>
          <div className="chart-wrap gauge-wrap">
            <ResponsiveContainer width="100%" height={240}>
              <RadialBarChart
                innerRadius="72%"
                outerRadius="100%"
                data={[{ name: 'score', value: health.score, fill: health.score >= 70 ? '#4ade80' : '#facc15' }]}
                startAngle={90}
                endAngle={-270}
              >
                <PolarAngleAxis type="number" domain={[0, 100]} tick={false} />
                <RadialBar dataKey="value" cornerRadius={16} background={{ fill: 'rgba(148,163,184,0.15)' }} />
              </RadialBarChart>
            </ResponsiveContainer>
          </div>
          <div className="gauge-label">{health.score}<span>/ 100</span></div>
          <span className={`risk-badge level-${String(health.label || 'fair').toLowerCase().replace(/\s+/g, '-')}`}>{health.label || 'Fair'}</span>
        </div>

        <div className="panel">
          <div className="panel-header"><h3>What your score is made of</h3><span>Weighted components</span></div>
          <div className="chart-wrap small-chart">
            <ResponsiveContainer width="100%" height={220}>
              <BarChart data={components} layout="vertical" margin={{ left: 4, right: 8 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" horizontal={false} />
                <XAxis type="number" domain={[0, 100]} stroke="#64748b" />
                <YAxis type="category" dataKey="name" width={140} stroke="#94a3b8" tick={{ fontSize: 12 }} />
                <Tooltip contentStyle={{ background: '#0f172a', border: '1px solid rgba(148,163,184,0.3)', borderRadius: 12 }} />
                <Bar dataKey="score" radius={[0, 8, 8, 0]}>
                  {components.map((entry, index) => (
                    <Cell key={index} fill={entry.score >= 70 ? '#2ec4b6' : entry.score >= 40 ? '#facc15' : '#f87171'} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </section>

      <section className="content-grid two-column">
        <div className="panel">
          <div className="panel-header"><h3>Strengths</h3><span>What is working</span></div>
          <ul className="check-list">{health.strengths.length === 0 ? <li className="muted">Nothing to report yet.</li> : health.strengths.map((item, index) => <li key={index}>{item}</li>)}</ul>
        </div>
        <div className="panel">
          <div className="panel-header"><h3>Weaknesses</h3><span>Where to focus</span></div>
          <ul className="warning-list">{health.weaknesses.length === 0 ? <li className="muted">Nothing to report yet.</li> : health.weaknesses.map((item, index) => <li key={index}>{item}</li>)}</ul>
        </div>
      </section>

      <section className="panel">
        <div className="panel-header"><h3>Recommended next steps</h3><span>Actionable</span></div>
        <ol className="number-list">{(health.recommendations || []).map((item, index) => <li key={index}>{item}</li>)}</ol>
      </section>
    </div>
  );
}

export default FinancialHealthPage;