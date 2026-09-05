import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  ArrowRight,
  FilePlus2,
  HeartPulse,
  ScanSearch,
  ShieldCheck,
  Sparkles,
  TrendingUp,
  Upload,
  Wallet,
} from 'lucide-react';
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { api } from '../services/api';
import PageHeader from '../components/PageHeader';
import StatCard from '../components/StatCard';
import ScoreRing from '../components/ScoreRing';
import Skeleton from '../components/Skeleton';

const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 });
const inrTwo = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 });

const CATEGORY_COLORS = ['#5b7cff', '#14b8a6', '#f59e0b', '#f87171', '#a78bfa', '#10b981', '#38bdf8', '#fb923c'];

const TOOLTIP_STYLE = { background: '#ffffff', border: '1px solid #e7eaf3', borderRadius: 12, boxShadow: '0 12px 30px rgba(16,24,40,0.12)', fontSize: 13 };

function OverviewPage() {
  const navigate = useNavigate();
  const [data, setData] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [error, setError] = useState('');
  const [txWarning, setTxWarning] = useState('');
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    let alive = true;
    async function load() {
      try {
        const [dashboardSettled, txSettled] = await Promise.allSettled([
          api.get('/dashboard'),
          api.get('/transactions'),
        ]);
        if (!alive) return;
        if (dashboardSettled.status === 'fulfilled') setData(dashboardSettled.value.data);
        if (txSettled.status === 'fulfilled') setTransactions(Array.isArray(txSettled.value.data) ? txSettled.value.data : []);
        else setTxWarning('Some live figures could not be loaded.');
        if (dashboardSettled.status === 'rejected') setError('Could not load your dashboard. Refresh or sign in again.');
      } catch {
        if (alive) setError('Could not load your dashboard. Refresh or sign in again.');
      } finally {
        if (alive) setLoaded(true);
      }
    }
    load();
    return () => { alive = false; };
  }, []);

  if (error && !data) {
    return (
      <div className="page-shell">
        <p className="form-error" role="alert">{error}</p>
        <button className="ghost-btn" onClick={() => window.location.reload()}>Try again</button>
      </div>
    );
  }

  if (!data || !loaded) {
    return (
      <div className="page-shell">
        <Skeleton rows={4} cards={3} />
      </div>
    );
  }

  const charts = (data.monthlySpend || []).map((point) => ({
    month: point.month,
    Expense: Number(point.expense),
    Income: Number(point.income),
  }));

  const categories = (data.categorySpend || []).map((row) => ({
    name: row.category,
    value: Math.round(row.pct),
  }));

  const alerts = data.recentAlerts || [];
  const flagged = data.flaggedTransactions || [];
  const healthScore = Number(data.health?.score ?? 0);
  const fraudScore = data.fraudSafetyScore != null ? Number(data.fraudSafetyScore) : null;
  const recommendationTitle = data.recommendationTitle || 'Recommended move';

  const quickActions = [
    { label: 'Add transaction', hint: 'Manual entry', to: '/transactions', icon: FilePlus2, tone: '' },
    { label: 'Import CSV', hint: 'Bank statement', to: '/transactions', icon: Upload, tone: 'green' },
    { label: 'Scan a message', hint: 'Fraud scanner', to: '/fraud-scanner', icon: ScanSearch, tone: 'red' },
    { label: 'Risk analysis', hint: 'Full picture', to: '/risk-analysis', icon: Sparkles, tone: 'amber' },
  ];

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="Financial overview"
        title="Your dashboard"
        intro={(data.recommendationBody || 'Here is the current state of your money and fraud safety.') + (data.recommendationBody ? ` · ${recommendationTitle}.` : '')}
        actions={
          <>
            <button className="ghost-btn" onClick={() => navigate('/transactions')}>Transactions</button>
            <button className="primary-btn" onClick={() => navigate('/fraud-scanner')}>
              <ScanSearch size={16} /> Scan scam
            </button>
          </>
        }
      />

      {txWarning && <p className="form-warn" role="alert">{txWarning}</p>}

      <section className="content-grid two-column stagger" style={{ alignItems: 'stretch' }}>
        <div className="panel" style={{ display: 'flex', gap: 24, alignItems: 'center', flexWrap: 'wrap' }}>
          {fraudScore == null ? (
            <>
              <ScoreRing score={0} size={168} label="/ 100" />
              <div style={{ flex: 1, minWidth: 220 }}>
                <p className="eyebrow">Fraud safety score</p>
                <h3 style={{ margin: '6px 0 8px', fontSize: 20 }}>Pending</h3>
                <p className="muted" style={{ margin: 0, lineHeight: 1.7 }}>
                  The fraud-safety score is not available yet.
                </p>
              </div>
            </>
          ) : (
            <>
              <ScoreRing score={fraudScore} size={168} label="/ 100" />
              <div style={{ flex: 1, minWidth: 220 }}>
                <p className="eyebrow">Fraud safety score</p>
                <h3 style={{ margin: '6px 0 8px', fontSize: 20 }}>{fraudScore >= 70 ? 'You look protected today' : fraudScore >= 40 ? 'Some risk needs attention' : 'High-risk signals detected'}</h3>
                <p className="muted" style={{ margin: 0, lineHeight: 1.7 }}>
                  Derived from fraud analyses, transaction anomalies and your open alerts.
                </p>
                <div className="step-actions" style={{ marginTop: 14 }}>
                  <Link to="/risk-analysis" className="ghost-btn">Open risk analysis <ArrowRight size={14} /></Link>
                  <Link to="/security" className="link-btn">Security center</Link>
                </div>
              </div>
            </>
          )}
        </div>

        <div className="panel" style={{ display: 'flex', gap: 24, alignItems: 'center', flexWrap: 'wrap' }}>
          <ScoreRing score={healthScore} size={168} label="/ 100" inverted />
          <div style={{ flex: 1, minWidth: 220 }}>
            <p className="eyebrow">Financial health</p>
            <h3 style={{ margin: '6px 0 8px', fontSize: 20 }}>{data.health?.label || 'Fair'}</h3>
            <p className="muted" style={{ margin: 0, lineHeight: 1.7 }}>
              Strengths and weaknesses from your income, savings, debt and goals.
            </p>
            <div className="step-actions" style={{ marginTop: 14 }}>
              <Link to="/financial-health" className="ghost-btn">View health report <ArrowRight size={14} /></Link>
            </div>
          </div>
        </div>
      </section>

      <section className="stats-grid stagger">
        <StatCard icon={Wallet} label="Monthly income" value={Number(data.monthlyIncome || 0)} prefix="₹" />
        <StatCard icon={TrendingUp} iconTone="green" label="Monthly expenses" value={Number(data.monthlyExpenses || 0)} prefix="₹" />
        <StatCard icon={Wallet} iconTone="blue" label="Total savings" value={Number(data.savings || 0)} prefix="₹" />
        <StatCard icon={Sparkles} iconTone="violet" label="Goal progress" value={Number(data.goalProgress || 0)} prefix="" decimals={0}>
          <span className="trend-pill flat">across all goals</span>
        </StatCard>
        <StatCard icon={ShieldCheck} iconTone="green" label="Fraud safety" value={Number(data.fraudSafetyScore || 0)} prefix="" decimals={0}>
          <span className="trend-pill up">live</span>
        </StatCard>
        <StatCard icon={HeartPulse} iconTone="red" label="Financial health" value={Number(data.health?.score || 0)} prefix="" decimals={0}>
          <span className="trend-pill up">{String(data.health?.label || 'fair').toUpperCase()}</span>
        </StatCard>
      </section>

      <section className="content-grid two-column">
        <div className="panel">
          <div className="panel-header"><h3>Spending overview</h3><span>Income vs expenses</span></div>
          {charts.length === 0 ? <p className="muted">Add or import transactions to see the trend.</p> : (
            <div className="chart-wrap">
              <ResponsiveContainer width="100%" height={280}>
                <AreaChart data={charts}>
                  <defs>
                    <linearGradient id="ovExpense" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#f87171" stopOpacity={0.3} />
                      <stop offset="95%" stopColor="#f87171" stopOpacity={0.02} />
                    </linearGradient>
                    <linearGradient id="ovIncome" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#34d399" stopOpacity={0.28} />
                      <stop offset="95%" stopColor="#34d399" stopOpacity={0.02} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e6e9f2" />
                  <XAxis dataKey="month" stroke="#9aa3b2" tick={{ fontSize: 11 }} />
                  <YAxis stroke="#9aa3b2" tick={{ fontSize: 11 }} />
                  <Tooltip formatter={(value) => inrTwo.format(value)} contentStyle={TOOLTIP_STYLE} />
                  <Area type="monotone" dataKey="Expense" name="Spent" stroke="#f87171" fillOpacity={1} fill="url(#ovExpense)" strokeWidth={2} />
                  <Area type="monotone" dataKey="Income" name="Income" stroke="#34d399" fillOpacity={1} fill="url(#ovIncome)" strokeWidth={2} />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>

        <div className="panel">
          <div className="panel-header"><h3>Spending by category</h3><span>Share of expenses</span></div>
          {categories.length === 0 ? <p className="muted">No expense categories yet.</p> : (
            <div className="chart-wrap small-chart">
              <ResponsiveContainer width="100%" height={240}>
                <BarChart data={categories}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e6e9f2" />
                  <XAxis dataKey="name" stroke="#9aa3b2" tick={{ fontSize: 11 }} />
                  <YAxis stroke="#9aa3b2" tick={{ fontSize: 11 }} />
                  <Tooltip formatter={(value) => `${value}%`} contentStyle={TOOLTIP_STYLE} />
                  <Bar dataKey="value" radius={[6, 6, 0, 0]} maxBarSize={34}>
                    {categories.map((row, index) => <Cell key={row.name} fill={CATEGORY_COLORS[index % CATEGORY_COLORS.length]} />)}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>
      </section>

      <section className="content-grid two-column">
        <div className="panel">
          <div className="panel-header">
            <h3>Flagged transactions</h3>
            <Link to="/transactions" className="link-btn">View all →</Link>
          </div>
          {flagged.length === 0 ? (
            <p className="muted">No flagged transactions right now. Your recent spending looks normal.</p>
          ) : (
            <div className="transaction-list">
              {flagged.map((tx) => (
                <Link className="transaction-row" key={tx.id} to={`/transactions/${tx.id}`} style={{ textDecoration: 'none' }}>
                  <div>
                    <strong>{tx.merchant}</strong>
                    <span>{tx.transactionDate} · {tx.category} {tx.riskScore != null ? `· Risk ${tx.riskScore}/100` : '· risk pending'}</span>
                    {tx.riskScore != null && <span className={`risk-badge level-${String(tx.riskLevel || (tx.riskScore >= 70 ? 'high' : tx.riskScore >= 40 ? 'moderate' : 'low')).toLowerCase()}`}>{tx.riskScore}/100</span>}
                  </div>
                  <strong className="expense">{inr.format(Number(tx.amount))}</strong>
                </Link>
              ))}
            </div>
          )}
        </div>

        <div className="panel">
          <div className="panel-header"><h3>Recent alerts</h3><Link to="/alerts" className="link-btn">Manage →</Link></div>
          {alerts.length === 0 ? (
            <p className="muted">No open alerts. Your accounts look calm right now.</p>
          ) : (
            <div className="alert-list">
              {alerts.slice(0, 5).map((alert) => (
                <div key={alert.id} className={`alert-item ${String(alert.severity || 'info').toLowerCase()}`}>
                  <div>
                    <strong>{alert.title}</strong>
                    <p>{alert.message}</p>
                  </div>
                  <span>{String(alert.severity).toLowerCase()}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      </section>

      <section>
        <div className="panel-header"><h3>Quick actions</h3><span>{transactions.length} tracked transactions</span></div>
        <div className="quick-actions">
          {quickActions.map((action) => (
            <Link to={action.to} className="quick-action" key={action.label}>
              <span className={`qa-icon ${action.tone}`}><action.icon size={18} /></span>
              <strong>{action.label}</strong>
              <span>{action.hint}</span>
            </Link>
          ))}
        </div>
      </section>
    </div>
  );
}

export default OverviewPage;