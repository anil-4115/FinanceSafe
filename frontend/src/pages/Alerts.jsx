import { useEffect, useState } from 'react';
import { api } from '../services/api';

const blankReport = { channel: 'Phone call', description: '', amountAtRisk: '' };

function AlertsPage() {
  const [alerts, setAlerts] = useState([]);
  const [report, setReport] = useState(blankReport);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [isReporting, setIsReporting] = useState(false);

  async function loadAlerts() {
    try { const { data } = await api.get('/alerts'); setAlerts(data); }
    catch { setError('Could not load alerts. Please try again.'); }
  }
  useEffect(() => { api.get('/alerts').then(({ data }) => setAlerts(data)).catch(() => setError('Could not load alerts. Please try again.')); }, []);
  async function resolveAlert(id) {
    try { await api.patch(`/alerts/${id}/resolve`); await loadAlerts(); }
    catch { setError('Could not update this alert.'); }
  }
  async function submitReport(event) {
    event.preventDefault(); setError(''); setMessage(''); setIsReporting(true);
    try {
      const { data } = await api.post('/fraud/reports', { ...report, amountAtRisk: Number(report.amountAtRisk || 0) });
      setReport(blankReport); setMessage(`Scam report saved. Risk score: ${data.riskScore}/100. Review the guidance in the new alert.`); await loadAlerts();
    } catch (requestError) { setError(requestError.response?.data?.message || 'Could not submit your report.'); }
    finally { setIsReporting(false); }
  }
  return (
    <div className="page-shell">
      <h2>Alerts</h2>
      <section className="data-grid">
        <form className="panel data-form" onSubmit={submitReport}>
          <div className="panel-header"><h3>Report a suspected scam</h3><span>Immediate safety review</span></div>
          <label>How did it happen?<select value={report.channel} onChange={(event) => setReport({ ...report, channel: event.target.value })}><option>Phone call</option><option>SMS</option><option>WhatsApp</option><option>Email</option><option>Website or app</option><option>UPI request</option><option>Social media</option></select></label>
          <label>Amount at risk (₹)<input type="number" min="0" step="0.01" value={report.amountAtRisk} onChange={(event) => setReport({ ...report, amountAtRisk: event.target.value })} /></label>
          <label className="wide-field">What happened?<textarea rows="5" value={report.description} onChange={(event) => setReport({ ...report, description: event.target.value })} placeholder="Include the request, merchant, link, or message. Never enter OTPs, PINs, passwords, or account numbers." required /></label>
          <button className="primary-btn" disabled={isReporting}>{isReporting ? 'Analysing...' : 'Submit scam report'}</button>
        </form>
        <aside className="panel safety-guide"><h3>Do this first</h3><ol><li>Stop the payment or conversation.</li><li>Do not share OTP, PIN, password, or screen access.</li><li>Contact your bank through its official number.</li><li>For cyber fraud in India, call 1930 or report at cybercrime.gov.in.</li></ol></aside>
      </section>
      {message && <p className="form-success">{message}</p>}{error && <p className="form-error" role="alert">{error}</p>}
      <section className="panel transaction-list"><div className="panel-header"><h3>Fraud and safety alerts</h3><span>{alerts.filter((alert) => alert.status === 'OPEN').length} open</span></div>
        {alerts.length === 0 ? <p className="muted">No alerts yet. Suspicious transactions and scam reports will appear here.</p> : alerts.map((alert) => <article className={`alert-item ${alert.severity.toLowerCase()}`} key={alert.id}><div><strong>{alert.title}</strong><p>{alert.message}</p><small>Risk score: {alert.riskScore}/100 · {new Date(alert.createdAt).toLocaleString()}</small></div>{alert.status === 'OPEN' ? <button className="ghost-btn" onClick={() => resolveAlert(alert.id)}>Mark resolved</button> : <span>Resolved</span>}</article>)}
      </section>
    </div>
  );
}

export default AlertsPage;
