import { useEffect, useState } from 'react';
import { api } from '../services/api';

function BudgetPage() {
  const [items, setItems] = useState([]);
  const [form, setForm] = useState({ category: 'Food', monthlyLimit: '' });
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const load = () => api.get('/budgets')
    .then(({ data }) => { setItems(data); setError(''); })
    .catch(() => setError('Could not load budgets.'));

  useEffect(() => {
    api.get('/budgets')
      .then(({ data }) => { setItems(data); setError(''); })
      .catch(() => setError('Could not load budgets.'));
  }, []);

  async function submit(event) {
    event.preventDefault();
    setError('');
    const category = form.category.trim();
    if (!category) {
      setError('Enter a category name.');
      return;
    }
    if (items.some((item) => item.category.toLowerCase() === category.toLowerCase())) {
      setError(`A budget for "${category}" already exists. Delete it and re-create to change the limit.`);
      return;
    }
    setSaving(true);
    try {
      await api.post('/budgets', { category, monthlyLimit: Number(form.monthlyLimit) });
      setForm({ ...form, monthlyLimit: '' });
      load();
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Could not save budget.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="page-shell">
      <h2>Budget</h2>
      <form className="panel data-form" onSubmit={submit}>
        <div className="panel-header"><h3>Create category budget</h3><span>Monthly limit</span></div>
        <label>Category<input value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })} required /></label>
        <label>Limit (₹)<input type="number" min="1" value={form.monthlyLimit} onChange={(e) => setForm({ ...form, monthlyLimit: e.target.value })} required /></label>
        <button className="primary-btn" disabled={saving}>{saving ? 'Saving…' : 'Save budget'}</button>
      </form>
      {error && <p className="form-error">{error}</p>}
      <section className="panel transaction-list">
        <div className="panel-header"><h3>Your budgets</h3><span>{items.length} categories</span></div>
        {items.length ? items.map((item) => (<div className="transaction-row" key={item.id}><strong>{item.category}</strong><strong>₹{Number(item.monthlyLimit).toLocaleString('en-IN')}</strong></div>)) : <p className="muted">Create a budget to receive overspending alerts.</p>}
      </section>
    </div>
  );
}

export default BudgetPage;