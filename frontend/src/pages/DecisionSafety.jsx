import { useState } from 'react';
import { api } from '../services/api';

const inr = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 0,
});

const types = [
  { value: 'PURCHASE', label: 'Purchase', blurb: 'Can I afford this buying decision?' },
  { value: 'LOAN', label: 'Loan', blurb: 'Is this loan affordable for me?' },
  { value: 'INVESTMENT', label: 'Investment', blurb: 'Does this investment fit my risk profile?' },
  { value: 'PAYMENT_REQUEST', label: 'Payment request', blurb: 'Is this payment request suspicious?' },
];

const initialForm = {
  decisionType: 'PURCHASE',
  amount: '',
  description: '',
  monthlyCost: '',
  tenureMonths: '',
  interestRatePct: '',
  riskTolerance: '',
};

function DecisionSafetyPage() {
  const [form, setForm] = useState(initialForm);
  const [result, setResult] = useState(null);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function analyze(event) {
    event.preventDefault();
    setError('');
    setMessage('');
    setResult(null);
    setLoading(true);
    const payload = {
      decisionType: form.decisionType,
      amount: Number(form.amount),
      description: form.description || null,
      monthlyCost: form.monthlyCost ? Number(form.monthlyCost) : null,
      tenureMonths: form.tenureMonths ? Math.round(Number(form.tenureMonths)) : null,
      interestRatePct: form.interestRatePct ? Number(form.interestRatePct) : null,
      riskTolerance: form.riskTolerance || null,
    };
    try {
      const { data } = await api.post('/decision/analyze', payload);
      setResult(data);
      setMessage('Decision check complete.');
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Could not analyse this decision.');
    } finally {
      setLoading(false);
    }
  }

  const showLoan = form.decisionType === 'LOAN';
  const showInvestment = form.decisionType === 'INVESTMENT';
  const showCost = form.decisionType === 'PURCHASE';

  return (
    <div className="page-shell">
      <h2>Decision Safety</h2>
      <section className="data-grid">
        <form className="panel data-form scanner-form" onSubmit={analyze}>
          <div className="panel-header"><h3>Check before you commit</h3><span>Affordability + impact + fraud signals</span></div>
          <label className="wide-field">Type of decision
            <select value={form.decisionType} onChange={(event) => setForm({ ...form, decisionType: event.target.value })}>
              {types.map((type) => <option key={type.value} value={type.value}>{type.label}</option>)}
            </select>
          </label>
          <p className="muted wide-field">{types.find((type) => type.value === form.decisionType)?.blurb}</p>
          <label>Amount (₹)<input type="number" min="0.01" step="any" value={form.amount} onChange={(event) => setForm({ ...form, amount: event.target.value })} required /></label>
          {showCost && <label>Monthly cost already being paid (₹)<input type="number" min="0" step="any" value={form.monthlyCost} onChange={(event) => setForm({ ...form, monthlyCost: event.target.value })} /></label>}
          {showLoan && <label>Interest rate (% p.a.)<input type="number" min="0" step="any" value={form.interestRatePct} onChange={(event) => setForm({ ...form, interestRatePct: event.target.value })} placeholder="default 12" /></label>}
          {showLoan && <label>Tenure (months)<input type="number" min="1" value={form.tenureMonths} onChange={(event) => setForm({ ...form, tenureMonths: event.target.value })} placeholder="default 36" /></label>}
          {showInvestment && <label>Your risk tolerance<select value={form.riskTolerance} onChange={(event) => setForm({ ...form, riskTolerance: event.target.value })}><option value="">Not set</option><option>Conservative</option><option>Moderate</option><option>High</option></select></label>}
          <label className="wide-field">Description or suspicious message
            <textarea rows="3" value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} placeholder="e.g. For a payment request, paste the message here. For investments, describe the product." />
          </label>
          <button className="primary-btn" disabled={loading}>{loading ? 'Analysing…' : 'Run safety check'}</button>
        </form>
        <aside className="panel safety-guide">
          <h3>What the score combines</h3>
          <ul className="check-list">
            <li>Affordability from your income and expenses</li>
            <li>Impact on your financial health score</li>
            <li>Effect on your goal progress</li>
            <li>Fraud signals in the description</li>
          </ul>
          <p className="muted">Always sit on big decisions and verify sellers through official channels.</p>
        </aside>
      </section>

      {message && <p className="form-success">{message}</p>}
      {error && <p className="form-error" role="alert">{error}</p>}

      {result && (
        <section className="panel result-panel">
          <div className="result-head">
            <div className="gauge" style={{ background: `conic-gradient(${result.score >= 75 ? '#4ade80' : result.score >= 55 ? '#facc15' : '#f87171'} ${result.score * 3.6}deg, rgba(148,163,184,0.18) 0deg)` }}>
              <div className="gauge-inner"><strong dangerouslySetInnerHTML={{ __html: `${result.score}<span>/100</span>` }} /></div>
            </div>
            <div className="result-facts">
              <span className={`risk-badge level-${String(result.assessment || '').toLowerCase()}`}>{result.assessment}</span>
              <h3>{result.summary}</h3>
              {result.goalImpact && <p>{result.goalImpact}</p>}
              {result.projectedMonthlyCost != null && <p className="muted">Estimated monthly cost: {inr.format(Number(result.projectedMonthlyCost))}</p>}
            </div>
          </div>
          <div className="health-compare">
            <span>Health before <strong>{result.healthBefore ?? '—'}</strong></span>
            <span className="arrow">→</span>
            <span>Health after <strong>{result.healthAfter ?? '—'}</strong></span>
          </div>
          <div className="content-grid two-column result-columns">
            <div>
              <h4>Why this score</h4>
              <ol className="number-list">{result.reasons.map((reason, index) => <li key={index}>{reason}</li>)}</ol>
            </div>
            <div>
              <h4>Recommendations</h4>
              <ul className="check-list">{result.recommendations.map((item, index) => <li key={index}>{item}</li>)}</ul>
            </div>
          </div>
        </section>
      )}
    </div>
  );
}

export default DecisionSafetyPage;