import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { BellRing, Gauge, ScanSearch, ShieldAlert } from 'lucide-react';
import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts';
import { api } from '../services/api';
import PageHeader from '../components/PageHeader';
import StatCard from '../components/StatCard';
import ScoreRing from '../components/ScoreRing';
import Skeleton from '../components/Skeleton';

const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 });
const TOOLTIP_STYLE = { background: '#fff', border: '1px solid #e7eaf3', borderRadius: 12, boxShadow: '0 12px 30px rgba(16,24,40,0.12)', fontSize: 13 };

function safetyStatus(transaction) {
  const level = transaction?.riskLevel;
  if (level === 'HIGH' || level === 'CRITICAL') return 'suspicious';
  if (level === 'MODERATE') return 'review';
  if (level == null || transaction?.riskScore == null) return 'unanalyzed';
  return 'normal';
}

const SAFETY_COLOR = { normal: '#10b981', review: '#f59e0b', suspicious: '#ef4444', unanalyzed: '#94a3b8' };
const SAFETY_LABEL = { normal: 'Normal', review: 'Review', suspicious: 'Suspicious', unanalyzed: 'Unanalyzed' };

function RiskAnalysisPage() {
  const [dashboard, setDashboard] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [history, setHistory] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [error, setError] = useState('');
  const [loaded, setLoaded] = useState(false);
  const [failedFeeds, setFailedFeeds] = useState([]);

  useEffect(() => {
    let alive = true;
    async function load() {
      try {
        const [dashboardRes, txRes, historyRes, alertsRes] = await Promise.allSettled([
          api.get('/dashboard'),
          api.get('/transactions'),
          api.get('/fraud/history'),
          api.get('/alerts'),
        ]);
        if (!alive) return;
        const failed = [];
        if (dashboardRes.status === 'fulfilled') setDashboard(dashboardRes.value.data); else failed.push('dashboard');
        if (txRes.status === 'fulfilled') setTransactions(Array.isArray(txRes.value.data) ? txRes.value.data : []); else failed.push('transactions');
        if (historyRes.status === 'fulfilled') setHistory(Array.isArray(historyRes.value.data) ? historyRes.value.data : []); else failed.push('scans');
        if (alertsRes.status === 'fulfilled') setAlerts(Array.isArray(alertsRes.value.data) ? alertsRes.value.data : []); else failed.push('alerts');
        setFailedFeeds(failed);
        const allFailed = [dashboardRes, txRes, historyRes, alertsRes].every((r) => r.status === 'rejected');
        if (allFailed) setError('Could not load the full risk picture. Refresh or sign in again.');
      } catch {
        if (alive) setError('Could not load the full risk picture. Refresh or sign in again.');
      } finally {
        if (alive) setLoaded(true);
      }
    }
    load();
    return () => { alive = false; };
  }, []);

  const distribution = useMemo(() => {
    const counts = { normal: 0, review: 0, suspicious: 0 };
    for (const tx of transactions) counts[safetyStatus(tx)] += 1;
    return Object.entries(counts).map(([key, value]) => ({
      name: SAFETY_LABEL[key],
      value,
      color: SAFETY_COLOR[key],
      key,
    }));
  }, [transactions]);

  const flagged = useMemo(() => transactions.filter((tx) => safetyStatus(tx) === 'suspicious'), [transactions]);

  const scamTypes = useMemo(() => {
    const map = {};
    for (const item of history) {
      const key = item.scamType || item.inputType || 'Other';
      map[key] = (map[key] || 0) + 1;
    }
    const rows = Object.entries(map).map(([name, value]) => ({ name, value })).sort((a, b) => b.value - a.value);
    const max = Math.max(1, ...rows.map((row) => row.value));
    return rows.map((row) => ({ ...row, pct: Math.round((row.value / max) * 100) }));
  }, [history]);

  const avgRisk = useMemo(() => {
    const scored = transactions.filter((tx) => tx.riskScore != null);
    if (scored.length === 0) return null;
    return Math.round(scored.reduce((sum, tx) => sum + Number(tx.riskScore) || sum, 0) / scored.length);
  }, [transactions]);

  const openAlerts = alerts.filter((alert) => alert.status === 'OPEN');
  const fraudScore = Number(dashboard?.fraudSafetyScore ?? 0);
  const totalRiskEvents = history.length + flagged.length + openAlerts.length;
  const noTransactions = failedFeeds.includes('transactions');
  const noHistory = failedFeeds.includes('scans');
  const noAlertsFeed = failedFeeds.includes('alerts');

  if (!loaded) {
    return (
      <div className="page-shell">
        <Skeleton rows={5} cards={3} />
      </div>
    );
  }

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="Fraud intelligence"
        title="Risk Analysis"
        intro="One view of fraud pressure on your account — real transactions, live scam analyses and open alerts."
        actions={<Link to="/fraud-scanner" className="primary-btn"><ScanSearch size={16} /> Run a new scan</Link>}
      />

      {error && <p className="form-error" role="alert">{error}</p>}
      {!error && failedFeeds.length > 0 && (
        <p className="form-warn" role="alert">Some data could not be loaded ({failedFeeds.join(', ')}) — sections marked unavailable below are missing, not empty.</p>
      )}

      <section className="content-grid two-column" style={{ alignItems: 'stretch' }}>
        <div className="panel" style={{ display: 'flex', gap: 24, alignItems: 'center', flexWrap: 'wrap' }}>
          {dashboard ? (
            <>
              <ScoreRing score={fraudScore} size={172} label="/ 100" />
              <div style={{ flex: 1, minWidth: 220 }}>
                <p className="eyebrow">Overall fraud safety</p>
                <h3 style={{ margin: '6px 0 8px' }}>
                  {fraudScore >= 70 ? 'Strong protection' : fraudScore >= 40 ? 'Elevated risk' : 'Needs attention'}
                </h3>
                <p className="muted" style={{ margin: 0, lineHeight: 1.7 }}>
                  Blends anomaly analysis on {transactions.length} transaction(s), {history.length} scam scan(s) and {openAlerts.length} open alert(s).
                </p>
                <div className="step-actions" style={{ marginTop: 14 }}>
                  <Link to="/fraud-history" className="ghost-btn">Scan history <ShieldAlert size={15} /></Link>
                  <Link to="/security" className="link-btn">Security center →</Link>
                </div>
              </div>
            </>
          ) : (
            <div style={{ flex: 1 }}>
              <p className="eyebrow">Overall fraud safety</p>
              <p className="muted" style={{ margin: '6px 0 0', lineHeight: 1.7 }}>
                The fraud-safety composite could not be loaded right now. The transaction, scan and alert breakdowns below are still live.
              </p>
            </div>
          )}
        </div>

        <div className="panel">
          <div className="panel-header"><h3>Transaction risk distribution</h3><span>{transactions.length} transactions</span></div>
          {noTransactions ? (
            <p className="muted">Transaction data unavailable.</p>
          ) : transactions.length === 0 ? (
            <p className="muted">No transactions to classify yet. Add or import them and the engine will flag each one.</p>
          ) : (
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
              <ResponsiveContainer width="55%" height={210} minWidth={180}>
                <PieChart>
                  <Pie
                    data={distribution}
                    dataKey="value"
                    nameKey="name"
                    innerRadius={58}
                    outerRadius={86}
                    paddingAngle={3}
                    strokeWidth={0}
                  >
                    {distribution.map((row) => <Cell key={row.key} fill={row.color} />)}
                  </Pie>
                  <Tooltip contentStyle={TOOLTIP_STYLE} />
                </PieChart>
              </ResponsiveContainer>
              <div style={{ flex: 1, minWidth: 150, display: 'flex', flexDirection: 'column', gap: 10 }}>
                {distribution.map((row) => (
                  <div key={row.key} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8 }}>
                    <span style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, color: 'var(--text-2)' }}>
                      <span style={{ width: 10, height: 10, borderRadius: 3, background: row.color }} />
                      {row.name}
                    </span>
                    <strong style={{ fontSize: 14 }}>{row.value}</strong>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </section>

      <section className="stats-grid stagger">
        <StatCard icon={Gauge} label="Average transaction risk" value={avgRisk ?? '—'} prefix={avgRisk == null ? '' : ''} />
        <StatCard icon={ShieldAlert} iconTone="red" label="Flagged transactions" value={flagged.length} prefix="" decimals={0} />
        <StatCard icon={ScanSearch} iconTone="blue" label="Scam scans on record" value={history.length} prefix="" decimals={0} />
        <StatCard icon={BellRing} iconTone={openAlerts.length > 0 ? 'amber' : 'green'} label="Open alerts" value={openAlerts.length} prefix="" decimals={0} />
        <StatCard icon={ShieldAlert} iconTone={totalRiskEvents > 0 ? 'red' : 'green'} label="Total risk events" value={totalRiskEvents} prefix="" decimals={0} />
      </section>

      <section className="content-grid two-column">
        <div className="panel">
          <div className="panel-header"><h3>Scam patterns seen</h3><span>From your scan history</span></div>
          {noHistory ? (
            <p className="muted">Scan history unavailable.</p>
          ) : scamTypes.length === 0 ? (
            <p className="muted">Run the Fraud Scanner to start building your scam-pattern profile.</p>
          ) : (
            <div className="risk-list">
              {scamTypes.map((row) => (
                <div key={row.name}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                    <strong style={{ fontSize: 13.5, color: 'var(--text)' }}>{row.name}</strong>
                    <span className="muted" style={{ fontSize: 13 }}>{row.value} scan(s)</span>
                  </div>
                  <div className="progress-track">
                    <div className={`progress-fill ${row.pct >= 70 ? 'red' : row.pct >= 40 ? 'amber' : 'green'}`} style={{ width: `${row.pct}%` }} />
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="panel">
          <div className="panel-header"><h3>Flagged transactions</h3><Link to="/transactions" className="link-btn">All →</Link></div>
          {noTransactions ? (
            <p className="muted">Flagged list unavailable.</p>
          ) : flagged.length === 0 ? (
            <p className="muted">Nothing flagged currently. Your recent spending is within normal patterns.</p>
          ) : (
            <div className="transaction-list">
              {flagged.map((tx) => (
                <Link className="transaction-row" key={tx.id} to={`/transactions/${tx.id}`} style={{ textDecoration: 'none' }}>
                  <div>
                    <strong>{tx.merchant}</strong>
                    <span>{tx.transactionDate} · {tx.category} · Risk {tx.riskScore}/100</span>
                  </div>
                  <strong className="expense">{inr.format(Number(tx.amount))}</strong>
                </Link>
              ))}
            </div>
          )}
        </div>
      </section>

      <section className="panel">
        <div className="panel-header"><h3>Open alerts</h3><Link to="/alerts" className="link-btn">Manage →</Link></div>
        {noAlertsFeed ? (
          <p className="muted">Alert feed unavailable. Open the Alerts page to retry.</p>
        ) : openAlerts.length === 0 ? (
          <p className="muted">No open alerts right now.</p>
        ) : (
          <div className="alert-list">
            {openAlerts.map((alert) => (
              <div key={alert.id} className={`alert-item ${String(alert.severity || 'info').toLowerCase()}`}>
                <div>
                  <strong>{alert.title}</strong>
                  <p>{alert.message}</p>
                  <small>Risk score: {alert.riskScore}/100 · {new Date(alert.createdAt).toLocaleString()}</small>
                </div>
                <span>{String(alert.severity).toLowerCase()}</span>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

export default RiskAnalysisPage;