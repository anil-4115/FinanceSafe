import { useState } from 'react';
import { api } from '../services/api';

const inr = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 0,
});

const initialForm = { amount: '', timeHorizonYears: 5, riskTolerance: 'Moderate' };

function InvestmentsPage() {
  const [form, setForm] = useState(initialForm);
  const [result, setResult] = useState(null);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function recommend(event) {
    event.preventDefault();
    setError('');
    setMessage('');
    setLoading(true);
    try {
      const { data } = await api.post('/investments/recommendation', {
        amount: Number(form.amount),
        timeHorizonYears: Number(form.timeHorizonYears),
        riskTolerance: form.riskTolerance,
      });
      setResult(data);
      setMessage('Allocation plan ready. Review the guidance before acting.');
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Could not build a recommendation.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="page-shell">
      <h2>Investment Assistant</h2>
      <section className="data-grid">
        <form className="panel data-form scanner-form" onSubmit={recommend}>
          <div className="panel-header"><h3>Build an allocation plan</h3><span>Educational guidance</span></div>
          <label>Amount to invest (₹)<input type="number" min="0" step="any" value={form.amount} onChange={(event) => setForm({ ...form, amount: event.target.value })} required /></label>
          <label>Time horizon (years)<input type="number" min="1" max="40" value={form.timeHorizonYears} onChange={(event) => setForm({ ...form, timeHorizonYears: event.target.value })} required /></label>
          <label className="wide-field">Risk tolerance<select value={form.riskTolerance} onChange={(event) => setForm({ ...form, riskTolerance: event.target.value })}><option>Conservative</option><option>Moderate</option><option>High</option></select></label>
          <button className="primary-btn" disabled={loading}>{loading ? 'Building…' : 'Get recommendation'}</button>
        </form>
        <aside className="panel safety-guide">
          <h3>How allocations adapt</h3>
          <ul className="check-list">
            <li>Longer horizons can handle more equity</li>
            <li>Conservative profiles favour debt and gold</li>
            <li>Balanced plans mix equity, debt and gold</li>
          </ul>
          <p className="muted">This is education, not personalised advice. Confirm with a SEBI-registered advisor.</p>
        </aside>
      </section>

      {message && <p className="form-success">{message}</p>}
      {error && <p className="form-error" role="alert">{error}</p>}

      {result && (
        <section className="panel result-panel">
          <div className="result-facts">
            <span className={`risk-badge level-${String(result.riskProfile || '').toLowerCase()}`}>{result.riskProfile}</span>
            <h3>{result.timeHorizonYears} year horizon</h3>
            <p>{result.summary}</p>
          </div>
          <div className="allocation-list">
            {result.allocations.map((allocation) => (
              <div className="allocation-row" key={allocation.assetClass}>
                <div className="allocation-main">
                  <div className="alloc-label"><strong>{allocation.assetClass}</strong><span>{allocation.weightPct}% · {inr.format(Number(allocation.amount))}</span></div>
                  <div className="alloc-bar"><div style={{ width: `${allocation.weightPct}%` }} /></div>
                </div>
                <p>{allocation.guidance}</p>
                <div className="alloc-products">
                  {allocation.exampleProducts.map((product) => <span key={product} className="chip">{product}</span>)}
                </div>
              </div>
            ))}
          </div>
          <p className="form-error disclaimer">{result.disclaimer}</p>
        </section>
      )}
    </div>
  );
}

export default InvestmentsPage;