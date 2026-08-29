import { useEffect, useState } from 'react';
import { api } from '../services/api';

function levelClass(level) {
  return String(level || '').toLowerCase().replace(/\s+/g, '-');
}

function FraudHistoryPage() {
  const [items, setItems] = useState([]);
  const [selected, setSelected] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get('/fraud/history')
      .then(({ data }) => setItems(data))
      .catch(() => setError('Could not load your fraud analysis history.'));
  }, []);

  async function openDetail(id) {
    setSelected(null);
    try {
      const { data } = await api.get(`/fraud/history/${id}`);
      setSelected(data);
    } catch {
      setError('Could not load that analysis.');
    }
  }

  if (error) return <div className="page-shell"><p className="form-error" role="alert">{error}</p></div>;

  return (
    <div className="page-shell">
      <h2>Fraud History</h2>
      {items.length === 0 ? (
        <div className="panel info-box">
          <p>Nothing scanned yet. Paste a suspicious message into the <strong>Scam Scanner</strong> and every analysis will be saved here.</p>
        </div>
      ) : (
        <section className="data-grid">
          <div className="panel">
            <div className="panel-header"><h3>Past scans</h3><span>{items.length} record(s)</span></div>
            <div className="history-list">
              {items.map((item) => (
                <button type="button" key={item.id} className={`history-row ${selected?.id === item.id ? 'selected' : ''}`} onClick={() => openDetail(item.id)}>
                  <div>
                    <strong>{item.scamType || item.inputType}</strong>
                    <span>{item.input.length > 90 ? `${item.input.slice(0, 90)}…` : item.input}</span>
                    <small>{new Date(item.createdAt).toLocaleString()}</small>
                  </div>
                  <span className={`risk-badge level-${levelClass(item.riskLevel)}`}>{item.riskScore}/100</span>
                </button>
              ))}
            </div>
          </div>

          <div className="panel">
            {selected ? (
              <>
                <div className="panel-header"><h3>Analysis detail</h3><span>#{selected.id}</span></div>
                <div className="result-facts">
                  <span className={`risk-badge level-${levelClass(selected.riskLevel)}`}>{selected.riskLevel}</span>
                  <h3>{selected.scamType || 'No specific pattern matched'}</h3>
                  <p>{selected.summary}</p>
                  <blockquote className="quote">{selected.input}</blockquote>
                </div>
                <h4>Detected indicators</h4>
                <ul className="check-list">
                  {selected.indicators.length === 0 ? <li className="muted">None found.</li> : selected.indicators.map((indicator, index) => <li key={index}>{indicator.label}</li>)}
                </ul>
                <h4>Recommended actions</h4>
                <ol className="number-list">{selected.recommendedActions.map((action, index) => <li key={index}>{action}</li>)}</ol>
              </>
            ) : (
              <p className="muted">Select a scan on the left to see the full explanation.</p>
            )}
          </div>
        </section>
      )}
    </div>
  );
}

export default FraudHistoryPage;