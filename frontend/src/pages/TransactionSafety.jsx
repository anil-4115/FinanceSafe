import { useEffect, useState } from 'react';
import { api } from '../services/api';

const inr = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 2,
});

const initialForm = {
  merchant: '',
  amount: '',
  category: 'General',
  transactionDate: new Date().toISOString().slice(0, 10),
};

function TransactionSafetyPage() {
  const [transactions, setTransactions] = useState([]);
  const [form, setForm] = useState(initialForm);
  const [assessment, setAssessment] = useState(null);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    api.get('/transactions')
      .then(({ data }) => setTransactions(data))
      .catch(() => setError('Could not load your transactions.'));
  }, []);

  async function assess(event) {
    event.preventDefault();
    setError('');
    setMessage('');
    setLoading(true);
    try {
      const { data } = await api.post('/fraud/transaction-risk', {
        merchant: form.merchant,
        amount: Number(form.amount),
        category: form.category,
        transactionDate: form.transactionDate,
      });
      setAssessment(data);
      setMessage('Check complete. It is compared against your normal spending behaviour.');
      // refresh list to show the latest risk flags
      try {
        const refresh = await api.get('/transactions');
        setTransactions(refresh.data);
      } catch { /* list refresh is best-effort */ }
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Could not assess this transaction.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="page-shell">
      <h2>Transaction Safety</h2>
      <section className="data-grid">
        <form className="panel data-form scanner-form" onSubmit={assess}>
          <div className="panel-header"><h3>Assess a transaction</h3><span>Anomaly detection vs your history</span></div>
          <label>Merchant or name<input value={form.merchant} onChange={(event) => setForm({ ...form, merchant: event.target.value })} placeholder="e.g. New merchant name" required /></label>
          <label>Amount (₹)<input type="number" min="0.01" step="0.01" value={form.amount} onChange={(event) => setForm({ ...form, amount: event.target.value })} required /></label>
          <label>Category<input value={form.category} onChange={(event) => setForm({ ...form, category: event.target.value })} required /></label>
          <label>Transaction date<input type="date" value={form.transactionDate} onChange={(event) => setForm({ ...form, transactionDate: event.target.value })} required /></label>
          <button className="primary-btn" disabled={loading}>{loading ? 'Checking…' : 'Check risk'}</button>
        </form>
        <aside className="panel safety-guide">
          <h3>What is checked</h3>
          <ul className="check-list">
            <li>Amount versus your normal range</li>
            <li>Unusual merchants and categories</li>
            <li>New or rare merchant patterns</li>
            <li>Category spending spikes</li>
          </ul>
          <p className="muted">The score learns from the transactions you have tracked. Track more to improve accuracy.</p>
        </aside>
      </section>

      {message && <p className="form-success">{message}</p>}
      {error && <p className="form-error" role="alert">{error}</p>}

      {assessment && (
        <section className="panel result-panel">
          <div className="result-head">
            <div className="gauge" style={{ background: `conic-gradient(${assessment.riskScore >= 70 ? '#f87171' : assessment.riskScore >= 40 ? '#facc15' : '#4ade80'} ${assessment.riskScore * 3.6}deg, rgba(148,163,184,0.18) 0deg)` }}>
              <div className="gauge-inner"><strong dangerouslySetInnerHTML={{ __html: `${assessment.riskScore}<span>/100</span>` }} /></div>
            </div>
            <div className="result-facts">
              <span className={`risk-badge level-${String(assessment.riskLevel || '').toLowerCase()}`}>{assessment.riskLevel}</span>
              <h3>Anomaly assessment</h3>
              <p>Compared against your recent spending patterns.</p>
            </div>
          </div>
          <h4>Why this score</h4>
          <ul className="check-list">
            {assessment.reasons.length === 0 ? <li className="muted">No unusual signals detected for this input.</li> : assessment.reasons.map((reason, index) => <li key={index}>{reason}</li>)}
          </ul>
        </section>
      )}

      <section className="panel transaction-list">
        <div className="panel-header"><h3>Your transactions</h3><span>risk flagged where present</span></div>
        {transactions.length === 0 ? (
          <p className="muted">No transactions yet. Add them under Spending or import a CSV statement.</p>
        ) : transactions.map((transaction) => (
          <div className="transaction-row" key={transaction.id}>
            <div>
              <strong>{transaction.merchant}</strong>
              <span>{transaction.transactionDate} · {transaction.category} · {transaction.source}</span>
              {transaction.riskScore != null && (
                <span className={`risk-badge level-${String(transaction.riskLevel || '').toLowerCase()}`}>risk {transaction.riskScore}/100{transaction.riskReason ? ` — ${transaction.riskReason}` : ''}</span>
              )}
            </div>
            <strong className={transaction.transactionType === 'EXPENSE' ? 'expense' : 'income'}>{transaction.transactionType === 'EXPENSE' ? '-' : '+'}{inr.format(Number(transaction.amount))}</strong>
          </div>
        ))}
      </section>
    </div>
  );
}

export default TransactionSafetyPage;