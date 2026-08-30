import { useState } from 'react';
import { api } from '../services/api';

const examples = [
  'Your bank account will be blocked today.\nComplete KYC immediately:\nhttp://suspicious-link.example\nSend your OTP to verify.',
  'Congratulations! You won a lottery of ₹10,00,000. Click https://claim-lottery-winner.top and pay ₹5,000 processing fee to claim.',
];

function levelClass(level) {
  return String(level || '').toLowerCase().replace(/\s+/g, '-');
}

function RiskGauge({ score }) {
  const color = score >= 75 ? '#f87171' : score >= 50 ? '#fb923c' : score >= 25 ? '#facc15' : '#4ade80';
  return (
    <div className="gauge" style={{ background: `conic-gradient(${color} ${score * 3.6}deg, rgba(148,163,184,0.18) 0deg)` }}>
      <div className="gauge-inner"><strong dangerouslySetInnerHTML={{ __html: `${score}<span>/100</span>` }} /></div>
    </div>
  );
}

function ScamScannerPage() {
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
    setLoading(true);
    try {
      const { data } = await api.post('/fraud/analyze', { content, type: type || null });
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
      <h2>Scam Scanner</h2>
      <section className="data-grid">
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
          <h3>What the scanner looks for</h3>
          <ul className="check-list">
            <li>OTP / PIN harvesting language</li>
            <li>Urgency and last-warning pressure</li>
            <li>Bank, government or brand impersonation</li>
            <li>KYC, lottery, payment and account-blocking threats</li>
            <li>Suspicious links and lookalike domains</li>
          </ul>
          <p className="muted">Results are an AI risk score, not an official fraud verdict.</p>
        </aside>
      </section>

      {message && <p className="form-success">{message}</p>}
      {error && <p className="form-error" role="alert">{error}</p>}

      {result && (
        <section className="panel result-panel">
          <div className="result-head">
            <RiskGauge score={result.riskScore} />
            <div className="result-facts">
              <span className={`risk-badge level-${levelClass(result.riskLevel)}`}>{result.riskLevel}</span>
              <h3>{result.scamType || 'No specific pattern matched'}</h3>
              <p>{result.summary}</p>
              <small>Confidence: {result.confidence} · {new Date(result.createdAt).toLocaleString()}</small>
            </div>
          </div>

          <div className="content-grid two-column result-columns">
            <div>
              <h4>Detected scam DNA ({result.indicators.length})</h4>
              {result.indicators.length === 0 ? <p className="muted">No obvious scam signals found.</p> : (
                <ul className="check-list">
                  {result.indicators.map((indicator, index) => (
                    <li key={index}><strong>{indicator.label}</strong> <small>weight {indicator.weight}</small></li>
                  ))}
                </ul>
              )}
            </div>
            <div>
              <h4>Recommended actions</h4>
              <ol className="number-list">
                {result.recommendedActions.map((action, index) => <li key={index}>{action}</li>)}
              </ol>
            </div>
          </div>
        </section>
      )}
    </div>
  );
}

export default ScamScannerPage;