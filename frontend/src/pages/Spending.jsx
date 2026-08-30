import { useEffect, useState } from 'react';
import { api } from '../services/api';

const initialForm = { transactionDate: new Date().toISOString().slice(0, 10), merchant: '', amount: '', transactionType: 'EXPENSE', category: 'General', notes: '' };
const money = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 });

function SpendingPage() {
  const [transactions, setTransactions] = useState([]);
  const [form, setForm] = useState(initialForm);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [isImporting, setIsImporting] = useState(false);

  async function loadTransactions() {
    try { const { data } = await api.get('/transactions'); setTransactions(data); }
    catch { setError('Could not load transactions. Please sign in again and retry.'); }
  }
  useEffect(() => {
    api.get('/transactions')
      .then(({ data }) => setTransactions(data))
      .catch(() => setError('Could not load transactions. Please sign in again and retry.'));
  }, []);

  async function addTransaction(event) {
    event.preventDefault(); setError(''); setMessage(''); setIsSaving(true);
    try {
      await api.post('/transactions', { ...form, amount: Number(form.amount) });
      setForm(initialForm); setMessage('Transaction saved.'); await loadTransactions();
    } catch (requestError) { setError(requestError.response?.data?.message || 'Could not save the transaction.'); }
    finally { setIsSaving(false); }
  }

  async function importCsv(event) {
    const file = event.target.files?.[0]; if (!file) return;
    setError(''); setMessage(''); setIsImporting(true);
    try {
      const body = new FormData(); body.append('file', file);
      const { data } = await api.post('/transactions/import', body, { headers: { 'Content-Type': 'multipart/form-data' } });
      setMessage(`${data.imported} transaction(s) imported.${data.errors?.length ? ` ${data.errors.length} row(s) need attention.` : ''}`);
      if (data.errors?.length) setError(data.errors.slice(0, 3).join(' '));
      await loadTransactions();
    } catch (requestError) { setError(requestError.response?.data?.message || 'Could not import this CSV file.'); }
    finally { setIsImporting(false); event.target.value = ''; }
  }

  return (
    <div className="page-shell">
      <h2>Spending</h2>
      <section className="data-grid">
        <form className="panel data-form" onSubmit={addTransaction}>
          <div className="panel-header"><h3>Add transaction</h3><span>Manual entry</span></div>
          <label>Date<input type="date" value={form.transactionDate} onChange={(event) => setForm({ ...form, transactionDate: event.target.value })} required /></label>
          <label>Merchant or source<input value={form.merchant} onChange={(event) => setForm({ ...form, merchant: event.target.value })} placeholder="e.g. Grocery store" required /></label>
          <label>Amount (₹)<input type="number" min="0.01" step="0.01" value={form.amount} onChange={(event) => setForm({ ...form, amount: event.target.value })} required /></label>
          <label>Type<select value={form.transactionType} onChange={(event) => setForm({ ...form, transactionType: event.target.value })}><option value="EXPENSE">Expense</option><option value="INCOME">Income</option></select></label>
          <label>Category<input value={form.category} onChange={(event) => setForm({ ...form, category: event.target.value })} required /></label>
          <label>Notes (optional)<input value={form.notes} onChange={(event) => setForm({ ...form, notes: event.target.value })} /></label>
          <button className="primary-btn" disabled={isSaving}>{isSaving ? 'Saving...' : 'Save transaction'}</button>
        </form>
        <div className="panel csv-import">
          <div className="panel-header"><h3>Import bank statement</h3><span>CSV only</span></div>
          <p>CSV headers required: <code>date</code>, <code>amount</code> (or <code>credit</code>/<code>debit</code>, <code>deposit</code>/<code>withdrawal</code>). Optional: <code>merchant</code> (also <code>description</code>, <code>narration</code>, <code>payee</code>), <code>type</code>, <code>category</code>, <code>notes</code>. Rows without a merchant are shown as <code>Unknown</code>.</p>
          <label className="file-picker">{isImporting ? 'Importing...' : 'Choose CSV file'}<input type="file" accept=".csv,text/csv" onChange={importCsv} disabled={isImporting} /></label>
          <p className="muted">Negative amounts and debit rows are saved as expenses; other rows are treated as income.</p>
        </div>
      </section>
      {message && <p className="form-success">{message}</p>}
      {error && <p className="form-error" role="alert">{error}</p>}
      <section className="panel transaction-list"><div className="panel-header"><h3>Transaction history</h3><span>{transactions.length} record(s)</span></div>
        {transactions.length === 0 ? <p className="muted">No transactions yet. Add one manually or import a CSV statement.</p> : transactions.map((transaction) => <div className="transaction-row" key={transaction.id}><div><strong>{transaction.merchant}</strong><span>{transaction.transactionDate} · {transaction.category} · {transaction.source}</span></div><strong className={transaction.transactionType === 'EXPENSE' ? 'expense' : 'income'}>{transaction.transactionType === 'EXPENSE' ? '-' : '+'}{money.format(transaction.amount)}</strong></div>)}
      </section>
    </div>
  );
}

export default SpendingPage;
