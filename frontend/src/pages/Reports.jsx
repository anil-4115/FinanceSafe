import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { Download, Printer } from 'lucide-react';
import { api } from '../services/api';
import { getUser } from '../services/auth';
import PageHeader from '../components/PageHeader';
import ScoreRing from '../components/ScoreRing';
import Skeleton from '../components/Skeleton';

const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 });

function safetyStatus(transaction) {
  const level = transaction?.riskLevel;
  if (level === 'HIGH' || level === 'CRITICAL') return 'suspicious';
  if (level === 'MODERATE') return 'review';
  return 'normal';
}

const SAFETY_COLOR = { normal: '#10b981', review: '#f59e0b', suspicious: '#ef4444' };

function ReportsPage() {
  const user = getUser();
  const [dashboard, setDashboard] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [history, setHistory] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [health, setHealth] = useState(null);
  const [error, setError] = useState('');
  const [failedFeeds, setFailedFeeds] = useState([]);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    let alive = true;
    async function load() {
      try {
        const results = await Promise.allSettled([
          api.get('/dashboard'),
          api.get('/transactions'),
          api.get('/fraud/history'),
          api.get('/alerts'),
          api.get('/health-score'),
        ]);
        if (!alive) return;
        const [dashboardRes, txRes, historyRes, alertsRes, healthRes] = results;
        const failed = [];
        if (dashboardRes.status === 'fulfilled') setDashboard(dashboardRes.value.data); else failed.push('dashboard');
        if (txRes.status === 'fulfilled') setTransactions(Array.isArray(txRes.value.data) ? txRes.value.data : []); else failed.push('transactions');
        if (historyRes.status === 'fulfilled') setHistory(Array.isArray(historyRes.value.data) ? historyRes.value.data : []); else failed.push('scans');
        if (alertsRes.status === 'fulfilled') setAlerts(Array.isArray(alertsRes.value.data) ? alertsRes.value.data : []); else failed.push('alerts');
        if (healthRes.status === 'fulfilled') setHealth(healthRes.value.data); else failed.push('health');
        setFailedFeeds(failed);
        if (results.every((r) => r.status === 'rejected')) setError('Could not assemble your reports. Try again in a moment.');
      } catch {
        if (alive) setError('Could not assemble your reports. Try again in a moment.');
      } finally {
        if (alive) setLoaded(true);
      }
    }
    load();
    return () => { alive = false; };
  }, []);

  const totals = useMemo(() => {
    let income = 0;
    let spent = 0;
    let flagged = 0;
    const categories = {};
    for (const tx of transactions) {
      const n = Number(tx.amount) || 0;
      if (tx.transactionType === 'INCOME') income += n;
      else {
        spent += n;
        if (safetyStatus(tx) === 'suspicious') flagged += 1;
      }
      const cat = tx.category || 'Other';
      categories[cat] = (categories[cat] || 0) + n;
    }
    const top = Object.entries(categories)
      .map(([name, value]) => ({ name, value: Math.round(value) }))
      .sort((a, b) => b.value - a.value)
      .slice(0, 6);
    const max = Math.max(1, ...top.map((row) => row.value));
    return {
      income,
      spent,
      balance: income - spent,
      count: transactions.length,
      flagged,
      categories: top.map((row) => ({ ...row, pct: Math.round((row.value / max) * 100) })),
    };
  }, [transactions]);

  const scamTypes = useMemo(() => {
    const map = {};
    for (const item of history) {
      const key = item.scamType || item.inputType || 'Other';
      map[key] = (map[key] || 0) + 1;
    }
    return Object.entries(map).map(([name, value]) => ({ name, value })).sort((a, b) => b.value - a.value).slice(0, 5);
  }, [history]);

  const openAlerts = alerts.filter((alert) => alert.status === 'OPEN');
  const fraudScore = dashboard ? Number(dashboard.fraudSafetyScore ?? 0) : null;
  const healthScore = health ? Number(health.score ?? 0) : null;
  const noDashboard = failedFeeds.includes('dashboard');
  const noTransactions = failedFeeds.includes('transactions');
  const noHistory = failedFeeds.includes('scans');
  const noAlerts = failedFeeds.includes('alerts');
  const noHealth = failedFeeds.includes('health');
  const generatedAt = new Date().toLocaleString('en-IN');

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
        eyebrow="Compliance-friendly summaries"
        title="Reports"
        intro="Printable, honest summaries built from your real account data. The backend has no report endpoint, so these are assembled and rendered client-side — perfect for your own record."
      />

      {error && <p className="form-error" role="alert">{error}</p>}
      {!error && failedFeeds.length > 0 && (
        <p className="form-warn" role="alert">
          Some data could not be loaded ({failedFeeds.join(', ')}) — this report is incomplete. Sections marked "Unavailable" are missing, not zero.
        </p>
      )}

      <div className="report-toolbar">
        <Link to="/risk-analysis" className="ghost-btn">Full risk analysis</Link>
        <button className="primary-btn" onClick={() => window.print()}>
          <Printer size={16} /> Print / save as PDF
        </button>
      </div>

      <div className="print-area">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', borderBottom: '2px solid var(--border)', paddingBottom: 16, marginBottom: 24, gap: 16, flexWrap: 'wrap' }}>
          <div>
            <p className="eyebrow">FinanceSafe · Financial summary</p>
            <h2 style={{ margin: '6px 0' }}>{user?.fullName || 'User'}</h2>
            <p className="muted" style={{ margin: 0 }}>Generated {generatedAt}</p>
          </div>
          <Download size={26} style={{ color: 'var(--muted)' }} />
        </div>

        <section className="content-grid two-column" style={{ marginBottom: 20 }}>
          <div className="panel" style={{ display: 'flex', gap: 20, alignItems: 'center', boxShadow: 'none' }}>
            {noDashboard ? (
              <div>
                <p className="eyebrow">Fraud safety score</p>
                <strong style={{ fontSize: 20 }}>Unavailable</strong>
                <p className="muted" style={{ margin: '6px 0 0', fontSize: 13 }}>Dashboard feed failed to load.</p>
              </div>
            ) : (
              <>
                <ScoreRing score={fraudScore} size={120} label="/ 100" />
                <div>
                  <p className="eyebrow">Fraud safety score</p>
                  <strong style={{ fontSize: 20 }}>{fraudScore >= 70 ? 'Protected' : fraudScore >= 40 ? 'Elevated' : 'High risk'}</strong>
                  <p className="muted" style={{ margin: '6px 0 0', fontSize: 13 }}>
                    {noAlerts ? 'alerts unavailable' : `${openAlerts.length} open alert(s)`} · {noTransactions ? 'flagged unavailable' : `${totals.flagged} flagged transaction(s)`} · {noHistory ? 'scans unavailable' : `${history.length} scan(s)`}
                  </p>
                </div>
              </>
            )}
          </div>
          <div className="panel" style={{ display: 'flex', gap: 20, alignItems: 'center', boxShadow: 'none' }}>
            {noHealth ? (
              <div>
                <p className="eyebrow">Financial health score</p>
                <strong style={{ fontSize: 20 }}>Unavailable</strong>
                <p className="muted" style={{ margin: '6px 0 0', fontSize: 13 }}>Health report feed failed to load.</p>
              </div>
            ) : (
              <>
                <ScoreRing score={healthScore} size={120} label="/ 100" inverted />
                <div>
                  <p className="eyebrow">Financial health score</p>
                  <strong style={{ fontSize: 20 }}>{String(health?.label || 'Fair').toUpperCase()}</strong>
                  <p className="muted" style={{ margin: '6px 0 0', fontSize: 13 }}>
                    {health?.weaknesses?.length ? `${health.weaknesses.length} focus area(s)` : 'All clear'}
                  </p>
                </div>
              </>
            )}
          </div>
        </section>

        <section className="content-grid two-column" style={{ marginBottom: 20 }}>
          <div className="panel" style={{ boxShadow: 'none' }}>
            <div className="panel-header"><h3>Transaction activity</h3><Link to="/transactions" className="link-btn">Open →</Link></div>
            <div className="safety-summary" style={{ marginBottom: 14 }}>
              <span className="safety-dot">Income <b>{inr.format(totals.income)}</b></span>
              <span className="safety-dot">Expenses <b>{inr.format(totals.spent)}</b></span>
              <span className="safety-dot">Balance <b>{inr.format(totals.balance)}</b></span>
              <span className="safety-dot">Rows <b>{totals.count}</b></span>
            </div>
            <h4 style={{ margin: '0 0 10px', fontSize: 13.5 }}>Top categories (expenses)</h4>
            <div className="risk-list">
              {noTransactions ? <p className="muted">Transaction data unavailable.</p> : totals.categories.length === 0 ? <p className="muted">No expense categories yet.</p> : totals.categories.map((row) => (
                <div key={row.name}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                    <strong style={{ fontSize: 13, color: 'var(--text)' }}>{row.name}</strong>
                    <span className="muted" style={{ fontSize: 12 }}>{inr.format(row.value)}</span>
                  </div>
                  <div className="progress-track"><div className="progress-fill" style={{ width: `${row.pct}%`, background: '#4f46e5' }} /></div>
                </div>
              ))}
            </div>
          </div>

          <div className="panel" style={{ boxShadow: 'none' }}>
            <div className="panel-header"><h3>Risk and safety</h3><Link to="/security" className="link-btn">Security center →</Link></div>
            <div className="risk-list">
              {noHistory ? <p className="muted">Scan history unavailable.</p> : scamTypes.length === 0 ? <p className="muted">No scam scans on record yet.</p> : scamTypes.map((row) => (
                <div key={row.name} style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid var(--border)' }}>
                  <span style={{ fontSize: 13.5, color: 'var(--text-2)' }}>{row.name}</span>
                  <strong style={{ fontSize: 13 }}>{row.value} scan(s)</strong>
                </div>
              ))}
              <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', marginTop: 10 }}>
                {Object.entries(SAFETY_COLOR).map(([key, color]) => {
                  const count = transactions.filter((tx) => safetyStatus(tx) === key).length;
                  return (
                    <span key={key} className="safety-dot" style={{ borderColor: color, color }}>
                      {key} <b style={{ color: 'var(--text)' }}>{count}</b>
                    </span>
                  );
                })}
              </div>
            </div>
          </div>
        </section>

        <section className="panel" style={{ boxShadow: 'none' }}>
          <div className="panel-header"><h3>Focus areas from your health report</h3></div>
          {noHealth ? (
            <p className="muted">Health data unavailable — this section is missing from your report.</p>
          ) : !health || (health.weaknesses || []).length === 0 ? (
            <p className="muted">Nothing pressing to report. Keep tracking your transactions and goals.</p>
          ) : (
            <ol className="number-list" style={{ margin: 0, paddingLeft: 22 }}>
              {(health.weaknesses || []).map((item, index) => <li key={index}>{item}</li>)}
            </ol>
          )}
        </section>

        <p className="muted" style={{ fontSize: 12, marginTop: 20, borderTop: '1px solid var(--border)', paddingTop: 14 }}>
          This report is generated from live data in your FinanceSafe account. Numbers are refreshed each time you open the page.
          This is educational information, not financial, legal or investment advice. For cyber fraud in India, call 1930 or report at cybercrime.gov.in.
        </p>
      </div>
    </div>
  );
}

export default ReportsPage;