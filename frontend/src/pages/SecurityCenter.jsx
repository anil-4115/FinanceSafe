import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  BellRing,
  FileText,
  KeyRound,
  Lock,
  Mail,
  MonitorSmartphone,
  ScanSearch,
  ShieldAlert,
  ShieldCheck,
} from 'lucide-react';
import { api } from '../services/api';
import PageHeader from '../components/PageHeader';
import StatCard from '../components/StatCard';
import ScoreRing from '../components/ScoreRing';
import Skeleton from '../components/Skeleton';

function detectDevice() {
  const ua = navigator.userAgent;
  let browser = 'Browser';
  if (/Edg\//.test(ua)) browser = 'Edge';
  else if (/Chrome\//.test(ua)) browser = 'Chrome';
  else if (/Firefox\//.test(ua)) browser = 'Firefox';
  else if (/Safari\//.test(ua)) browser = 'Safari';
  let os = 'Unknown device';
  if (/Windows/.test(ua)) os = 'Windows';
  else if (/Mac OS|Macintosh/.test(ua)) os = 'macOS';
  else if (/Android/.test(ua)) os = 'Android';
  else if (/iPhone|iPad/.test(ua)) os = 'iOS';
  else if (/Linux/.test(ua)) os = 'Linux';
  return { browser, os };
}

function dotFor(type, severity) {
  const level = String(severity || '').toLowerCase();
  if (level.includes('critical') || level.includes('high') || level.includes('suspicious') || Number(severity) >= 70) return 'red';
  if (level.includes('moderate') || level.includes('warning') || level.includes('review') || (Number(severity) >= 40 && Number(severity) < 70)) return 'amber';
  return 'green';
}

function SecurityCenterPage() {
  const [alerts, setAlerts] = useState([]);
  const [history, setHistory] = useState([]);
  const [incidents, setIncidents] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [fraudScore, setFraudScore] = useState(null);
  const [error, setError] = useState('');
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    let alive = true;
    async function load() {
      try {
        const results = await Promise.allSettled([
          api.get('/alerts'),
          api.get('/fraud/history'),
          api.get('/incidents'),
          api.get('/transactions'),
          api.get('/dashboard'),
        ]);
        if (!alive) return;
        const [alertsRes, historyRes, incidentsRes, txRes, dashRes] = results;
        if (alertsRes.status === 'fulfilled') setAlerts(Array.isArray(alertsRes.value.data) ? alertsRes.value.data : []);
        if (historyRes.status === 'fulfilled') setHistory(Array.isArray(historyRes.value.data) ? historyRes.value.data : []);
        if (incidentsRes.status === 'fulfilled') setIncidents(Array.isArray(incidentsRes.value.data) ? incidentsRes.value.data : []);
        if (txRes.status === 'fulfilled') setTransactions(Array.isArray(txRes.value.data) ? txRes.value.data : []);
        if (dashRes.status === 'fulfilled') setFraudScore(Number(dashRes.value.data?.fraudSafetyScore ?? 0));
        if (results.every((r) => r.status === 'rejected')) setError('Could not load the security picture. Try again in a moment.');
      } catch {
        if (alive) setError('Could not load the security picture. Try again in a moment.');
      } finally {
        if (alive) setLoaded(true);
      }
    }
    load();
    return () => { alive = false; };
  }, []);

  const flaggedCount = transactions.filter((tx) => {
    const level = tx?.riskLevel;
    return level === 'HIGH' || level === 'CRITICAL';
  }).length;

  const openAlerts = alerts.filter((alert) => alert.status === 'OPEN');

  const timeline = useMemo(() => {
    const events = [];
    for (const alert of alerts) {
      events.push({
        id: `a-${alert.id}`,
        type: 'alert',
        title: alert.title,
        text: alert.message,
        time: alert.createdAt,
        severity: alert.severity,
      });
    }
    for (const item of history) {
      events.push({
        id: `h-${item.id}`,
        type: 'scan',
        title: item.scamType || String(item.inputType || 'Scam scan'),
        text: item.input && item.input.length > 90 ? `${item.input.slice(0, 90)}…` : item.input,
        time: item.createdAt,
        severity: item.riskScore,
      });
    }
    for (const incident of incidents) {
      events.push({
        id: `i-${incident.id}`,
        type: 'incident',
        title: `Incident report · ${incident.channel}`,
        text: incident.description,
        time: incident.createdAt,
        severity: incident.riskScore,
      });
    }
    return events
      .filter((event) => event.time)
      .sort((a, b) => new Date(b.time) - new Date(a.time))
      .slice(0, 10);
  }, [alerts, history, incidents]);

  const device = detectDevice();

  const tools = [
    { icon: KeyRound, title: 'Password', text: 'Changing your password needs a backend reset flow that is not connected yet.', locked: true },
    { icon: Lock, title: 'Two-factor authentication', text: '2FA is not available in this build. The backend has no enrollment endpoint.', locked: true },
    { icon: Mail, title: 'Email notifications', text: 'Notification preferences are not persisted by the backend yet.', locked: true },
    { icon: MonitorSmartphone, title: 'Active sessions', text: 'Session management is handled by your login workflow. Only this device is shown.', locked: true },
  ];

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
        eyebrow="Account protection"
        title="Security Center"
        intro="See every security event, review protection options and know exactly where your account stands."
        actions={
          <Link to="/alerts" className="primary-btn"><ShieldAlert size={16} /> Review alerts</Link>
        }
      />

      {error && <p className="form-error" role="alert">{error}</p>}

      <section className="content-grid two-column" style={{ alignItems: 'stretch' }}>
        <div className="panel" style={{ display: 'flex', gap: 24, alignItems: 'center', flexWrap: 'wrap' }}>
          <ScoreRing score={fraudScore} size={168} label="/ 100" />
          <div style={{ flex: 1, minWidth: 220 }}>
            <p className="eyebrow">Fraud safety score</p>
            <h3 style={{ margin: '6px 0 8px' }}>{fraudScore >= 70 ? 'Strong protection active' : fraudScore >= 40 ? 'Elevated risk' : 'Act now'}</h3>
            <p className="muted" style={{ margin: 0, lineHeight: 1.7 }}>
              Live composite of open alerts, flagged transactions, incident reports and your scan history.
            </p>
          </div>
        </div>

        <div className="panel">
          <div className="panel-header"><h3>Current session</h3><ShieldCheck size={18} style={{ color: 'var(--success)' }} /></div>
          <div style={{ display: 'flex', gap: 14, alignItems: 'center' }}>
            <span className="avatar" style={{ background: 'linear-gradient(135deg, #10b981, #059669)' }}><MonitorSmartphone size={18} /></span>
            <div>
              <strong style={{ display: 'block', color: 'var(--text)' }}>This device</strong>
              <p className="muted" style={{ margin: '2px 0 0' }}>{device.browser} · {device.os} · signed in via FinanceSafe</p>
            </div>
          </div>
          <div className="safety-summary" style={{ marginTop: 14 }}>
            <span className="safety-dot">Open alerts <b>{openAlerts.length}</b></span>
            <span className="safety-dot">Flagged <b>{flaggedCount}</b></span>
            <span className="safety-dot">Scans <b>{history.length}</b></span>
            <span className="safety-dot">Reports <b>{incidents.length}</b></span>
          </div>
        </div>
      </section>

      <section className="stats-grid stagger">
        <StatCard icon={BellRing} iconTone={openAlerts.length > 0 ? 'amber' : 'green'} label="Open alerts" value={openAlerts.length} prefix="" decimals={0} />
        <StatCard icon={ShieldAlert} iconTone={flaggedCount > 0 ? 'red' : 'green'} label="Flagged transactions" value={flaggedCount} prefix="" decimals={0} />
        <StatCard icon={ScanSearch} iconTone="blue" label="Scam scans" value={history.length} prefix="" decimals={0} />
        <StatCard icon={FileText} iconTone="violet" label="Incident reports" value={incidents.length} prefix="" decimals={0} />
      </section>

      <section className="content-grid two-column" style={{ alignItems: 'start' }}>
        <div className="panel">
          <div className="panel-header"><h3>Recent security events</h3><Link to="/alerts" className="link-btn">Alerts →</Link></div>
          <div className="timeline">
            {timeline.length === 0 && <p className="muted">No security events yet. Any scan, alert or report will appear here.</p>}
            {timeline.map((event) => (
              <div className="timeline-item" key={event.id}>
                <span className={`timeline-dot ${dotFor(event.type, event.severity)}`}>
                  {event.type === 'scan' ? <ScanSearch size={15} /> : event.type === 'incident' ? <FileText size={15} /> : <BellRing size={15} />}
                </span>
                <div className="timeline_body">
                  <strong>{event.title}</strong>
                  <p>{event.text}</p>
                  <small>{new Date(event.time).toLocaleString('en-IN')}</small>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div className="panel">
            <div className="panel-header"><h3>Protection options</h3><span>Account hardening</span></div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              {tools.map((tool) => (
                <div className="locked-card" key={tool.title}>
                  <span className="locked-icon"><tool.icon size={18} /></span>
                  <span className="locked-body">
                    <strong>{tool.title}</strong>
                    <p>{tool.text}</p>
                  </span>
                  <span className="coming-soon">Soon</span>
                </div>
              ))}
            </div>
          </div>

          <div className="panel">
            <div className="panel-header"><h3>Useful tools</h3></div>
            <div className="risk-list">
              <Link to="/transaction-safety" className="ghost-btn" style={{ justifyContent: 'flex-start' }}>Check a transaction risk</Link>
              <Link to="/incidents" className="ghost-btn" style={{ justifyContent: 'flex-start' }}>Report a scam attempt</Link>
              <Link to="/fraud-history" className="ghost-btn" style={{ justifyContent: 'flex-start' }}>View scan history</Link>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}

export default SecurityCenterPage;