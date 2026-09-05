import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { HeartPulse, PlusCircle } from 'lucide-react';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  PolarAngleAxis,
  RadialBar,
  RadialBarChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { api } from '../services/api';
import PageHeader from '../components/PageHeader';
import Skeleton from '../components/Skeleton';

const TOOLTIP_STYLE = { background: '#fff', border: '1px solid #e7eaf3', borderRadius: 12, boxShadow: '0 12px 30px rgba(16,24,40,0.12)', fontSize: 13 };
const TRACK_FILL = '#e9ecf6';

function componentColor(score) {
  if (score >= 70) return '#10b981';
  if (score >= 40) return '#f59e0b';
  return '#ef4444';
}

function FinancialHealthPage() {
  const [health, setHealth] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    let alive = true;
    api.get('/health-score')
      .then(({ data }) => {
        if (alive) setHealth(data);
      })
      .catch(() => {
        if (alive) setError('Could not calculate your health score. Add some transactions and a profile first.');
      });
    return () => { alive = false; };
  }, []);

  if (error) {
    return (
      <div className="page-shell">
        <p className="form-error" role="alert">{error}</p>
        <Link to="/settings" className="primary-btn"><PlusCircle size={16} /> Complete your financial profile</Link>
      </div>
    );
  }

  if (!health) {
    return (
      <div className="page-shell">
        <Skeleton rows={5} cards={3} />
      </div>
    );
  }

  const score = Number(health.score) || 0;
  const components = (health.components || []).map((component) => ({
    name: component.name,
    score: component.score,
  }));

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="Financial wellness"
        title="Financial Health"
        intro="A transparent score across your income stability, savings, debt and goal progress — with clear next steps."
        actions={<Link to="/settings" className="ghost-btn"><PlusCircle size={16} /> Refine profile</Link>}
      />

      <section className="health-hero-grid content-grid two-column" style={{ alignItems: 'stretch' }}>
        <div className="panel" style={{ display: 'flex', alignItems: 'center', gap: 28, flexWrap: 'wrap', justifyContent: 'center' }}>
          <div style={{ position: 'relative', width: 230, height: 230 }}>
            <ResponsiveContainer>
              <RadialBarChart
                innerRadius="72%"
                outerRadius="100%"
                data={[{ name: 'score', value: score, fill: score >= 70 ? '#10b981' : score >= 40 ? '#f59e0b' : '#ef4444' }]}
                startAngle={90}
                endAngle={-270}
              >
                <PolarAngleAxis type="number" domain={[0, 100]} tick={false} />
                <RadialBar dataKey="value" cornerRadius={18} background={{ fill: TRACK_FILL }} />
              </RadialBarChart>
            </ResponsiveContainer>
            <div className="score-ring-value" style={{ position: 'absolute', inset: 0 }}>
              <strong style={{ fontSize: 52 }}>{score}</strong>
              <span>/ 100</span>
            </div>
          </div>
          <div style={{ flex: 1, minWidth: 220 }}>
            <p className="eyebrow">Health score</p>
            <h3 style={{ margin: '6px 0' }}>{String(health.label || 'Fair').toUpperCase()}</h3>
            <p className="muted" style={{ margin: 0, lineHeight: 1.7 }}>
              {components.length > 0 && `Blend of ${components.length} weighted components.`}{' '}
              Track income, savings, debt and goals consistently for a fairer score.
            </p>
            <span className={`risk-badge level-${String(health.label || 'fair').toLowerCase().replace(/\s+/g, '-')}`}>{health.label || 'Fair'}</span>
          </div>
        </div>

        <div className="panel">
          <div className="panel-header"><h3>What your score is made of</h3><span>Weighted components</span></div>
          {components.length === 0 ? (
            <p className="muted">Complete your financial profile to unlock component scores.</p>
          ) : (
            <div className="chart-wrap small-chart">
              <ResponsiveContainer width="100%" height={230}>
                <BarChart data={components} layout="vertical" margin={{ left: 4, right: 8 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e6e9f2" horizontal={false} />
                  <XAxis type="number" domain={[0, 100]} stroke="#9aa3b2" />
                  <YAxis type="category" dataKey="name" width={140} stroke="#98a2b3" tick={{ fontSize: 12 }} />
                  <Tooltip contentStyle={TOOLTIP_STYLE} />
                  <Bar dataKey="score" radius={[0, 8, 8, 0]} maxBarSize={20}>
                    {components.map((row, index) => <Cell key={index} fill={componentColor(Number(row.score))} />)}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>
      </section>

      <section className="content-grid two-column">
        <div className="panel">
          <div className="panel-header"><h3>Strengths</h3><span>What is working</span></div>
          <ul className="check-list" style={{ margin: 0, paddingLeft: 22 }}>
            {(health.strengths || []).length === 0 ? <li className="muted">Nothing to report yet.</li> : health.strengths.map((item, index) => <li key={index}>{item}</li>)}
          </ul>
        </div>
        <div className="panel">
          <div className="panel-header"><h3>Weaknesses</h3><span>Where to focus</span></div>
          <ul className="warning-list" style={{ margin: 0, paddingLeft: 22 }}>
            {(health.weaknesses || []).length === 0 ? <li className="muted">Nothing to report yet.</li> : health.weaknesses.map((item, index) => <li key={index}>{item}</li>)}
          </ul>
        </div>
      </section>

      <section className="panel">
        <div className="panel-header"><h3>Recommended next steps</h3><HeartPulse size={18} style={{ color: 'var(--brand)' }} /></div>
        <ol className="number-list" style={{ margin: 0, paddingLeft: 22 }}>
          {(health.recommendations || []).map((item, index) => <li key={index}>{item}</li>)}
        </ol>
      </section>
    </div>
  );
}

export default FinancialHealthPage;