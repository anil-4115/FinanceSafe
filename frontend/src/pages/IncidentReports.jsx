import { useEffect, useState } from 'react';
import { api } from '../services/api';

const inr = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 0,
});

const initialForm = { channel: 'PHONE', description: '', amountAtRisk: '' };

function IncidentReportsPage() {
  const [incidents, setIncidents] = useState([]);
  const [form, setForm] = useState(initialForm);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    api.get('/incidents')
      .then(({ data }) => { setIncidents(data); setError(''); })
      .catch(() => setError('Could not load incident reports.'));
  }, []);

  async function load() {
    try {
      const { data } = await api.get('/incidents');
      setIncidents(data);
    } catch {
      setError('Could not load incident reports.');
    }
  }

  async function report(event) {
    event.preventDefault();
    setError('');
    setMessage('');
    setLoading(true);
    try {
      const { data } = await api.post('/fraud/reports', {
        channel: form.channel,
        description: form.description,
        amountAtRisk: form.amountAtRisk ? Number(form.amountAtRisk) : null,
      });
      setForm(initialForm);
      setMessage(`Incident logged and scored (risk ${data.riskScore}). Our team will review it.`);
      load();
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Could not save the report.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="page-shell">
      <h2>Incident Reports</h2>
      <p className="muted">Something reached out through SMS, phone or email? Log it here so it gets investigated.</p>

      <section className="data-grid">
        <form className="panel data-form scanner-form" onSubmit={report}>
          <div className="panel-header"><h3>Report a scam attempt</h3><span>Every report trains the assistant</span></div>
          <label className="wide-field">Channel<select value={form.channel} onChange={(event) => setForm({ ...form, channel: event.target.value })}><option value="PHONE">Phone call</option><option value="SMS">SMS / text</option><option value="EMAIL">Email</option><option value="WHATSAPP">WhatsApp</option><option value="SOCIAL">Social media</option><option value="OTHER">Other</option></select></label>
          <label className="wide-field">What happened?
            <textarea rows="4" value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} placeholder="Include the number, message or email snippet, and what they asked for." required />
          </label>
          <label>Potential amount at risk (₹)<input type="number" min="0" step="any" value={form.amountAtRisk} onChange={(event) => setForm({ ...form, amountAtRisk: event.target.value })} placeholder="Optional" /></label>
          <button className="primary-btn" disabled={loading}>{loading ? 'Sending…' : 'Submit report'}</button>
        </form>
        <aside className="panel safety-guide">
          <h3>What we do with it</h3>
          <ul className="check-list">
            <li>Every report is logged with a risk score</li>
            <li>Patterns feed fraud education</li>
            <li>Share it with your bank or cyber police</li>
          </ul>
          <p className="muted">For immediate danger call your bank and 1930 (National Cyber Crime Helpline).</p>
        </aside>
      </section>

      {message && <p className="form-success">{message}</p>}
      {error && <p className="form-error" role="alert">{error}</p>}

      <section className="panel">
        <div className="panel-header"><h3>Past reports</h3><span>{incidents.length} total</span></div>
        <div className="history-list">
          {incidents.length === 0 && <p className="muted">No reports yet.</p>}
          {incidents.map((incident) => (
            <div className="history-row" key={incident.id}>
              <div className="history-main">
                <span className={`risk-badge level-${incident.riskScore >= 70 ? 'critical' : incident.riskScore >= 40 ? 'warning' : 'safe'}`}>{incident.riskScore}</span>
                <div>
                  <strong>{incident.channel}</strong>
                  <p>{incident.description}</p>
                </div>
              </div>
              <div className="history-extra">
                <span className="muted">{new Date(incident.createdAt).toLocaleString()}</span>
                {incident.amountAtRisk != null && Number(incident.amountAtRisk) > 0 && <span>At risk: {inr.format(Number(incident.amountAtRisk))}</span>}
              </div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}

export default IncidentReportsPage;