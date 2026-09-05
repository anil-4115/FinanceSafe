import { useState } from 'react';
import { ScanSearch, ShieldCheck } from 'lucide-react';
import { api } from '../services/api';
import PageHeader from '../components/PageHeader';
import ScoreRing from '../components/ScoreRing';

const examples = [
  'Your bank account will be blocked today.\nComplete KYC immediately:\nhttp://suspicious-link.example\nSend your OTP to verify.',
  'Congratulations! You won a lottery of ₹10,00,000. Click https://claim-lottery-winner.top and pay ₹5,000 processing fee to claim.',
];

const signatures = [
  { title: 'OTP / PIN harvesting language', text: 'Requests for verification codes, passwords or PINs.' },
  { title: 'Urgency and last-warning pressure', text: '"Blocked today", "final notice", "act now".' },
  { title: 'Impersonation', text: 'Bank, government, telecom or brand pretending to be official.' },
  { title: 'KYC / lottery / payment threats', text: 'Account-blocking KYC, fake winnings, advance-fee traps.' },
  { title: 'Suspicious links & lookalike domains', text: 'URLs that mimic real domains, often with a .top / .xyz twist.' },
];

function levelClass(level) {
  return String(level || '').toLowerCase().replace(/\s+/g, '-');
}

function FraudScannerPage() {
  const [content, setContent] = useState('');
  const [type, setType] = useState('');
  const [result, setResult] = useState(null);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function scan(event) {
    event.preventDefault();
    setError('');
    setMessage('');
    const text = content.trim();
    if (!text) {
      setError('Please paste a message to scan.');
      return;
    }
    if (text.length > 10000) {
      setError('Message is too long. Keep it under 10,000 characters.');
      return;
    }
    setResult(null);
    setLoading(true);
    try {
      const { data } = await api.post('/fraud/analyze', { content: text, type: type || null });
      setResult(data);
      setMessage('Analysis saved to your fraud history.');
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Could not scan this message. Try again.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="Fraud detection"
        title="Fraud Scanner"
        intro="Paste any suspicious SMS, email, UPI request or link. The explainable AI engine scores it against known scam patterns."
        actions={
          result && (
            <button className="ghost-btn" onClick={() => { setResult(null); setContent(''); setMessage(''); }}>
              Scan another message
            </button>
          )
        }
      />

      <section className="data-grid" style={{ gridTemplateColumns: 'minmax(0, 1.3fr) minmax(280px, 0.85fr)' }}>
        <form className="panel data-form scanner-form" onSubmit={scan}>
          <div className="panel-header"><h3>Paste a suspicious message or URL</h3><span>Explainable AI · no OTPs needed</span></div>
          <label className="wide-field">What kind of communication is this?
            <select value={type} onChange={(event) => setType(event.target.value)}>
              <option value="">Not sure</option>
              <option value="sms">SMS</option>
              <option value="email">Email</option>
              <option value="whatsapp">WhatsApp / chat</option>
              <option value="upi">UPI request / payment message</option>
              <option value="url">URL / link</option>
              <option value="phone">Phone call script</option>
            </select>
          </label>
          <label className="wide-field">Message content
            <textarea rows="7" value={content} onChange={(event) => setContent(event.target.value)} placeholder="Paste the full message, including any links and numbers." required />
          </label>
          <div className="wide-field chip-row">
            {examples.map((example) => (
              <button type="button" key={example} className="ghost-btn" onClick={() => setContent(example)}>Try an example</button>
            ))}
          </div>
          <button className="primary-btn" disabled={loading}>{loading ? 'Analysing…' : 'Scan for scam'}</button>
        </form>

        <aside className="panel safety-guide">
          <div className="panel-header" style={{ marginBottom: 10 }}><h3>What the scanner looks for</h3><ScanSearch size={18} style={{ color: 'var(--brand)' }} /></div>
          <div className="risk-list">
            {signatures.map((signature) => (
              <div key={signature.title} style={{ display: 'flex', gap: 10 }}>
                <ShieldCheck size={16} style={{ color: 'var(--success)', flexShrink: 0, marginTop: 3 }} />
                <p style={{ margin: 0, color: 'var(--text-2)', fontSize: 13.5, lineHeight: 1.55 }}>
                  <strong style={{ color: 'var(--text)', display: 'block' }}>{signature.title}</strong>
                  {signature.text}
                </p>
              </div>
            ))}
          </div>
          <p className="muted" style={{ margin: '12px 0 0' }}>Results are an AI risk score, not an official fraud verdict.</p>
        </aside>
      </section>

      {message && <p className="form-success">{message}</p>}
      {error && <p className="form-error" role="alert">{error}</p>}

      {result && (
        <section className="panel result-panel fade-up">
          <div className="result-head">
            <ScoreRing score={Number(result.riskScore) || 0} size={170} label="/ 100" />
            <div className="result-facts">
              <span className={`risk-badge level-${levelClass(result.riskLevel)}`}>{result.riskLevel}</span>
              <h3>{result.scamType || 'No specific pattern matched'}</h3>
              <p>{result.summary}</p>
              <small className="muted">Confidence: {result.confidence} · {new Date(result.createdAt).toLocaleString()}</small>
            </div>
          </div>

          <div className="content-grid two-column result-columns">
            <div>
              <h4>Detected scam DNA ({(result.indicators || []).length})</h4>
              {!result.indicators || result.indicators.length === 0 ? (
                <p className="muted">No obvious scam signals found.</p>
              ) : (
                <ul className="check-list" style={{ margin: 0, paddingLeft: 22 }}>
                  {result.indicators.map((indicator, index) => (
                    <li key={index}><strong>{indicator.label}</strong> <span className="muted">weight {indicator.weight}</span></li>
                  ))}
                </ul>
              )}
            </div>
            <div>
              <h4>Recommended actions</h4>
              <ol className="number-list" style={{ margin: 0, paddingLeft: 22 }}>
                {(result.recommendedActions || []).map((action, index) => <li key={index}>{action}</li>)}
              </ol>
            </div>
          </div>
        </section>
      )}
    </div>
  );
}

export default FraudScannerPage;