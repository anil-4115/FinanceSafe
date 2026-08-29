import { useEffect, useState } from 'react';
import { api } from '../services/api';
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  CartesianGrid,
  XAxis,
  YAxis,
  Tooltip,
  BarChart,
  Bar,
} from 'recharts';

const inr = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 0,
});

const inrTwo = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 2,
});

function DashboardPage() {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get('/dashboard')
      .then(({ data }) => setData(data))
      .catch(() => setError('Could not load your dashboard. Refresh or sign in again.'));
  }, []);

  if (error) return <div className="page-shell"><p className="form-error" role="alert">{error}</p></div>;
  if (!data) return <div className="page-shell"><p className="muted">Loading your financial picture…</p></div>;

  const charts = (data.monthlySpend || []).map((point) => ({
    month: point.month,
    Expense: Number(point.expense),
    Income: Number(point.income),
  }));
  const categories = (data.categorySpend || []).map((row) => ({
    name: row.category,
    value: Math.round(row.pct),
  }));

  return (
    <div className="dashboard-page">
      <section className="stats-grid">
        <div className="stat-card"><span>Financial Health</span><strong>{data.health.score} / 100</strong></div>
        <div className="stat-card"><span>Fraud Safety</span><strong>{data.fraudSafetyScore} / 100</strong></div>
        <div className="stat-card"><span>Savings</span><strong>{inr.format(Number(data.savings || 0))}</strong></div>
        <div className="stat-card"><span>Monthly Income</span><strong>{inr.format(Number(data.monthlyIncome || 0))}</strong></div>
        <div className="stat-card"><span>Monthly Expenses</span><strong>{inr.format(Number(data.monthlyExpenses || 0))}</strong></div>
        <div className="stat-card"><span>Goal Progress</span><strong>{data.goalProgress}%</strong></div>
      </section>

      <section className="content-grid two-column">
        <div className="panel">
          <div className="panel-header"><h3>Spending overview</h3><span>Income vs expenses</span></div>
          <div className="chart-wrap">
            <ResponsiveContainer width="100%" height={260}>
              <AreaChart data={charts}>
                <defs>
                  <linearGradient id="colorExpense" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#fca5a5" stopOpacity={0.45} />
                    <stop offset="95%" stopColor="#fca5a5" stopOpacity={0.05} />
                  </linearGradient>
                  <linearGradient id="colorIncome" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#86efac" stopOpacity={0.4} />
                    <stop offset="95%" stopColor="#86efac" stopOpacity={0.05} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                <XAxis dataKey="month" stroke="#64748b" />
                <YAxis stroke="#64748b" />
                <Tooltip formatter={(value) => inrTwo.format(value)} contentStyle={{ background: '#0f172a', border: '1px solid rgba(148,163,184,0.3)', borderRadius: 12 }} />
                <Area type="monotone" dataKey="Expense" stroke="#f87171" fillOpacity={1} fill="url(#colorExpense)" />
                <Area type="monotone" dataKey="Income" stroke="#4ade80" fillOpacity={1} fill="url(#colorIncome)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="panel">
          <div className="panel-header"><h3>Recent alerts</h3><span>Severity</span></div>
          <div className="alert-list">
            {data.recentAlerts.length === 0 && <p className="muted">No open alerts. Your accounts look calm right now.</p>}
            {data.recentAlerts.map((alert) => (
              <div key={alert.id} className={`alert-item ${String(alert.severity).toLowerCase()}`}>
                <div><strong>{alert.title}</strong><p>{alert.message}</p></div>
                <span>{String(alert.severity).toLowerCase()}</span>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="content-grid two-column lower-grid">
        <div className="panel">
          <div className="panel-header"><h3>{data.recommendationTitle}</h3><span>Personalised suggestion</span></div>
          <div className="recommendation-box">
            <p>{data.recommendationBody}</p>
          </div>
          {data.flaggedTransactions.length > 0 && (
            <div className="flagged-block">
              <h4>Flagged transactions needing attention</h4>
              {data.flaggedTransactions.map((tx) => (
                <div className="transaction-row" key={tx.id}>
                  <div><strong>{tx.merchant}</strong><span>{tx.transactionDate} · {tx.category} · Risk {tx.riskScore}/100</span></div>
                  <strong className="expense">{inr.format(Number(tx.amount))}</strong>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="panel">
          <div className="panel-header"><h3>Spending by category</h3><span>Share of expenses</span></div>
          <div className="chart-wrap small-chart">
            <ResponsiveContainer width="100%" height={220}>
              <BarChart data={categories}>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                <XAxis dataKey="name" stroke="#64748b" tick={{ fontSize: 11 }} />
                <YAxis stroke="#64748b" />
                <Tooltip formatter={(value) => `${value}%`} contentStyle={{ background: '#0f172a', border: '1px solid rgba(148,163,184,0.3)', borderRadius: 12 }} />
                <Bar dataKey="value" fill="#2ec4b6" radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </section>
    </div>
  );
}

export default DashboardPage;