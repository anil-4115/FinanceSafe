import { useState } from 'react';
import { api } from '../services/api';
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  CartesianGrid,
  XAxis,
  YAxis,
  Tooltip,
  Legend,
} from 'recharts';

const inr = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 0,
});

const initialForm = { initialInvestment: 50000, monthlyContribution: 5000, years: 10, annualReturnPct: 12 };

function InvestmentSimulatorPage() {
  const [form, setForm] = useState(initialForm);
  const [result, setResult] = useState(null);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function simulate(event) {
    event.preventDefault();
    setError('');
    setMessage('');
    setLoading(true);
    try {
      const { data } = await api.post('/simulator/investment', {
        initialInvestment: Number(form.initialInvestment),
        monthlyContribution: Number(form.monthlyContribution),
        years: Number(form.years),
        annualReturnPct: Number(form.annualReturnPct),
      });
      setResult(data);
      setMessage('Projection ready. Remember: this is a simulation, not a promise.');
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Could not run the simulation.');
    } finally {
      setLoading(false);
    }
  }

  const series = (result?.series || []).map((point) => ({
    year: point.year,
    value: Number(point.value),
    contributed: Number(point.contributed),
  }));

  return (
    <div className="page-shell">
      <h2>Investment Simulator</h2>
      <section className="data-grid">
        <form className="panel data-form scanner-form" onSubmit={simulate}>
          <div className="panel-header"><h3>Project a goal</h3><span>Initial + monthly contributions</span></div>
          <label>Initial investment (₹)<input type="number" min="0" step="any" value={form.initialInvestment} onChange={(event) => setForm({ ...form, initialInvestment: event.target.value })} required /></label>
          <label>Monthly contribution (₹)<input type="number" min="0" step="any" value={form.monthlyContribution} onChange={(event) => setForm({ ...form, monthlyContribution: event.target.value })} required /></label>
          <label>Years<input type="number" min="1" max="40" value={form.years} onChange={(event) => setForm({ ...form, years: event.target.value })} required /></label>
          <label>Assumed return (% p.a.)<input type="number" min="0" max="30" step="any" value={form.annualReturnPct} onChange={(event) => setForm({ ...form, annualReturnPct: event.target.value })} required /></label>
          <button className="primary-btn" disabled={loading}>{loading ? 'Calculating…' : 'Run simulation'}</button>
        </form>
        <aside className="panel safety-guide">
          <h3>Read the numbers right</h3>
          <ul className="check-list">
            <li>Contributions are what you actually put in</li>
            <li>Value adds assumed growth on top</li>
            <li>Real returns vary with markets and taxes</li>
          </ul>
          <p className="muted">Swap the return to 8–10% to see a more conservative picture.</p>
        </aside>
      </section>

      {message && <p className="form-success">{message}</p>}
      {error && <p className="form-error" role="alert">{error}</p>}

      {result && (
        <section className="panel result-panel">
          <div className="stats-grid three-wide">
            <div className="stat-card"><span>Total contributed</span><strong>{inr.format(Number(result.totalContribution))}</strong></div>
            <div className="stat-card"><span>Projected value</span><strong>{inr.format(Number(result.projectedValue))}</strong></div>
            <div className="stat-card"><span>Estimated gain</span><strong>{inr.format(Number(result.totalGain))}</strong></div>
          </div>
          <div className="chart-wrap">
            <ResponsiveContainer width="100%" height={280}>
              <AreaChart data={series}>
                <defs>
                  <linearGradient id="growthGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#2ec4b6" stopOpacity={0.35} />
                    <stop offset="95%" stopColor="#2ec4b6" stopOpacity={0.03} />
                  </linearGradient>
                  <linearGradient id="contributionGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#94a3b8" stopOpacity={0.3} />
                    <stop offset="95%" stopColor="#94a3b8" stopOpacity={0.03} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                <XAxis dataKey="year" stroke="#64748b" />
                <YAxis stroke="#64748b" />
                <Tooltip formatter={(value) => inr.format(value)} contentStyle={{ background: '#0f172a', border: '1px solid rgba(148,163,184,0.3)', borderRadius: 12 }} />
                <Legend />
                <Area type="monotone" dataKey="value" name="Projected value" stroke="#2ec4b6" fill="url(#growthGrad)" strokeWidth={2} />
                <Area type="monotone" dataKey="contributed" name="Contributed" stroke="#94a3b8" fill="url(#contributionGrad)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
          <p className="form-error disclaimer">{result.disclaimer}</p>
        </section>
      )}
    </div>
  );
}

export default InvestmentSimulatorPage;