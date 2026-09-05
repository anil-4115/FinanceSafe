import { useEffect, useState } from 'react';
import { api } from '../services/api';

function GoalsPage() {
  const [items, setItems] = useState([]);
  const [form, setForm] = useState({ name: '', targetAmount: '', currentAmount: '', deadline: '', monthlyContribution: '' });
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const load = () => api.get('/goals')
    .then(({ data }) => { setItems(data); setError(''); })
    .catch(() => setError('Could not load goals.'));

  useEffect(() => {
    api.get('/goals')
      .then(({ data }) => { setItems(data); setError(''); })
      .catch(() => setError('Could not load goals.'));
  }, []);

  async function submit(event) {
    event.preventDefault();
    setError('');
    setSaving(true);
    try {
      await api.post('/goals', {
        ...form,
        name: form.name.trim(),
        targetAmount: Number(form.targetAmount),
        currentAmount: Number(form.currentAmount || 0),
        deadline: form.deadline || null,
        monthlyContribution: Number(form.monthlyContribution || 0),
      });
      setForm({ name: '', targetAmount: '', currentAmount: '', deadline: '', monthlyContribution: '' });
      load();
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Could not save goal.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="page-shell">
      <h2>Financial Goals</h2>
      <form className="panel data-form" onSubmit={submit}>
        <div className="panel-header"><h3>Create a goal</h3><span>Plan ahead</span></div>
        <label>Name<input value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} required /></label>
        <label>Target amount (₹)<input type="number" min="1" value={form.targetAmount} onChange={e => setForm({ ...form, targetAmount: e.target.value })} required /></label>
        <label>Saved so far (₹)<input type="number" min="0" value={form.currentAmount} onChange={e => setForm({ ...form, currentAmount: e.target.value })} /></label>
        <label>Deadline<input type="date" value={form.deadline} onChange={e => setForm({ ...form, deadline: e.target.value })} /></label>
        <label>Monthly contribution (₹)<input type="number" min="0" value={form.monthlyContribution} onChange={e => setForm({ ...form, monthlyContribution: e.target.value })} /></label>
        <button className="primary-btn" disabled={saving}>{saving ? 'Saving…' : 'Save goal'}</button>
      </form>
      {error && <p className="form-error">{error}</p>}
      <section className="panel transaction-list">
        <div className="panel-header"><h3>Your goals</h3><span>{items.length} goals</span></div>
        {items.map((item) => {
          const pct = Math.min(100, Math.round(Number(item.currentAmount) / Number(item.targetAmount) * 100));
          return (
            <div className="transaction-row" key={item.id}>
              <div><strong>{item.name}</strong><span>{pct}% complete {item.deadline ? `· deadline ${item.deadline}` : ''}</span></div>
              <strong>{pct}%</strong>
            </div>
          );
        })}
        {!items.length && <p className="muted">Create a goal such as an emergency fund, laptop, or travel fund.</p>}
      </section>
    </div>
  );
}

export default GoalsPage;