import { useEffect, useMemo, useState } from 'react';
import { api } from '../services/api';
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  CartesianGrid,
  XAxis,
  YAxis,
  Tooltip,
  PieChart,
  Pie,
  Cell,
  Legend,
} from 'recharts';

const initialForm = { transactionDate: new Date().toISOString().slice(0, 10), merchant: '', amount: '', transactionType: 'EXPENSE', category: 'General', notes: '' };
const money = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 });
const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 });
const inrTwo = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 });

const CATEGORY_COLORS = ['#5b7cff', '#2ec4b6', '#fbbf24', '#f87171', '#a78bfa', '#34d399', '#38bdf8', '#fb923c'];

// Derive safety status from the project's actual fraud/anomaly analysis on the transaction.
function safetyStatus(transaction) {
  const level = transaction?.riskLevel;
  if (level === 'HIGH' || level === 'CRITICAL') return 'suspicious';
  if (level === 'MODERATE') return 'review';
  return 'normal'; // LOW or not-yet-analysed
}
const SAFETY_LABEL = { normal: 'Normal', review: 'Review', suspicious: 'Suspicious' };
const SAFETY_EMOJI = { normal: '🟢', review: '🟡', suspicious: '🔴' };

function SpendingPage() {
  const [transactions, setTransactions] = useState([]);
  const [form, setForm] = useState(initialForm);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [isImporting, setIsImporting] = useState(false);

  const [trendRange, setTrendRange] = useState('30D');
  const [showAll, setShowAll] = useState(false);
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState('ALL');
  const [categoryFilter, setCategoryFilter] = useState('ALL');
  const [safetyFilter, setSafetyFilter] = useState('ALL');
  const [sortBy, setSortBy] = useState('date');
  const [sortDir, setSortDir] = useState('desc');
  const [page, setPage] = useState(1);
  const PAGE_SIZE = 10;

  async function loadTransactions() {
    try { const { data } = await api.get('/transactions'); setTransactions(data); }
    catch { setError('Could not load transactions. Please sign in again and retry.'); }
  }
  useEffect(() => {
    loadTransactions();
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
      setMessage(`${data.imported} transaction(s) imported.${data.needsAttention?.length ? ` ${data.needsAttention.length} row(s) need attention.` : ''}`);
      if (data.needsAttention?.length) setError(data.needsAttention.slice(0, 3).join(' '));
      await loadTransactions();
    } catch (requestError) { setError(requestError.response?.data?.message || 'Could not import this CSV file.'); }
    finally { setIsImporting(false); event.target.value = ''; }
  }

  const summary = useMemo(() => {
    let income = 0, spent = 0, suspicious = 0;
    for (const tx of transactions) {
      const n = Number(tx.amount) || 0;
      if (tx.transactionType === 'INCOME') income += n;
      else spent += n;
      if (safetyStatus(tx) === 'suspicious') suspicious += 1;
    }
    return { balance: income - spent, income, spent, suspicious };
  }, [transactions]);

  const trendData = useMemo(() => {
    const now = new Date();
    const rangeDays = trendRange === '7D' ? 7 : trendRange === '30D' ? 30 : trendRange === '3M' ? 90 : 365;
    const start = new Date(now); start.setDate(start.getDate() - (rangeDays - 1)); start.setHours(0, 0, 0, 0);
    const labelFmt = trendRange === '1Y' ? { month: 'short', year: 'numeric' } : { day: 'numeric', month: 'short' };
    if (trendRange === '1Y') {
      const monthBuckets = Array.from({ length: 12 }, (_, i) => {
        const d = new Date(now.getFullYear(), now.getMonth() - 11 + i, 1);
        const key = `${d.getFullYear()}-${d.getMonth()}`;
        const label = d.toLocaleDateString('en-IN', { month: 'short', year: 'numeric' });
        return { key, label, Expense: 0, Income: 0 };
      });
      for (const tx of transactions) {
        const d = new Date(tx.transactionDate + 'T00:00:00');
        const key = `${d.getFullYear()}-${d.getMonth()}`;
        const bucket = monthBuckets.find((b) => b.key === key);
        if (!bucket) continue;
        const n = Number(tx.amount) || 0;
        if (tx.transactionType === 'INCOME') bucket.Income += n; else bucket.Expense += n;
      }
      return monthBuckets.map((b) => ({ ...b, label: b.label }));
    }
    const byDay = new Map();
    for (const tx of transactions) {
      const d = new Date(tx.transactionDate + 'T00:00:00');
      if (d < start || d > now) continue;
      const dayKey = d.toDateString();
      if (!byDay.has(dayKey)) byDay.set(dayKey, { key: d.toISOString(), label: d.toLocaleDateString('en-IN', labelFmt), Expense: 0, Income: 0 });
      const n = Number(tx.amount) || 0;
      const rec = byDay.get(dayKey);
      if (tx.transactionType === 'INCOME') rec.Income += n; else rec.Expense += n;
    }
    return [...byDay.values()].sort((a, b) => new Date(a.key) - new Date(b.key));
  }, [transactions, trendRange]);

  const categoryData = useMemo(() => {
    const map = {};
    for (const tx of transactions) {
      if (tx.transactionType !== 'EXPENSE') continue;
      const cat = tx.category || 'Other';
      map[cat] = (map[cat] || 0) + (Number(tx.amount) || 0);
    }
    return Object.entries(map)
      .map(([name, value]) => ({ name, value: Math.round(value) }))
      .sort((a, b) => b.value - a.value);
  }, [transactions]);

  const suspiciousTransactions = useMemo(
    () => transactions.filter((tx) => safetyStatus(tx) === 'suspicious'),
    [transactions],
  );

  const filteredTransactions = useMemo(() => {
    let list = [...transactions];
    const term = search.trim().toLowerCase();
    if (term) list = list.filter((tx) => (tx.merchant || '').toLowerCase().includes(term) || (tx.notes || '').toLowerCase().includes(term));
    if (typeFilter !== 'ALL') list = list.filter((tx) => tx.transactionType === typeFilter);
    if (categoryFilter !== 'ALL') list = list.filter((tx) => tx.category === categoryFilter);
    if (safetyFilter !== 'ALL') list = list.filter((tx) => safetyStatus(tx) === safetyFilter);
    const dir = sortDir === 'asc' ? 1 : -1;
    list.sort((a, b) => {
      if (sortBy === 'amount') return (Number(a.amount) - Number(b.amount)) * dir;
      return (new Date(a.transactionDate) - new Date(b.transactionDate)) * dir;
    });
    return list;
  }, [transactions, search, typeFilter, categoryFilter, safetyFilter, sortBy, sortDir]);

  const categories = useMemo(() => [...new Set(transactions.map((tx) => tx.category).filter(Boolean)).sort()], [transactions]);

  const pageCount = Math.max(1, Math.ceil(filteredTransactions.length / PAGE_SIZE));
  const currentPage = Math.min(page, pageCount);
  const pageRows = filteredTransactions.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);
  const recentRows = transactions.slice(0, 6);

  const toggleSort = (field) => {
    if (sortBy === field) setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
    else { setSortBy(field); setSortDir(field === 'amount' ? 'desc' : 'desc'); }
  };

  const renderRow = (tx, { showSafety = false, showSource = false } = {}) => {
    const status = safetyStatus(tx);
    return (
      <div className="transaction-row" key={tx.id}>
        <div>
          <strong>{tx.merchant || 'Unknown'}</strong>
          <span>{tx.transactionDate} · {tx.category}{showSource ? ` · ${tx.source}` : ''}</span>
          {showSafety && <span className={`safety-tag ${status}`}>{SAFETY_EMOJI[status]} {SAFETY_LABEL[status]}</span>}
        </div>
        <strong className={tx.transactionType === 'EXPENSE' ? 'expense' : 'income'}>
          {tx.transactionType === 'EXPENSE' ? '-' : '+'}{money.format(Number(tx.amount))}
        </strong>
      </div>
    );
  };

  return (
    <div className="page-shell">
      <div className="page-head-actions">
        <h2>Spending</h2>
        <div className="topbar-actions">
          <button className="ghost-btn" onClick={() => setShowAll(true)}>View All Transactions</button>
        </div>
      </div>

      {/* Summary cards */}
      <section className="stats-grid four-up">
        <div className="stat-card"><span>Total Balance</span><strong>{inr.format(summary.balance)}</strong></div>
        <div className="stat-card"><span>Total Income</span><strong className="income">{inr.format(summary.income)}</strong></div>
        <div className="stat-card"><span>Total Spent</span><strong className="expense">{inr.format(summary.spent)}</strong></div>
        <div className={`stat-card suspicious-card ${summary.suspicious > 0 ? 'has-suspicious' : ''}`}>
          <span>Suspicious Transactions</span>
          <strong>{summary.suspicious}</strong>
        </div>
      </section>

      {/* Spending trend */}
      <section className="panel">
        <div className="panel-header">
          <h3>Spending trend</h3>
          <div className="filter-row">
            {['7D', '30D', '3M', '1Y'].map((range) => (
              <button key={range} className={`ghost-btn ${trendRange === range ? 'active-filter' : ''}`} onClick={() => setTrendRange(range)}>{range}</button>
            ))}
          </div>
        </div>
        {trendData.length === 0 ? <p className="muted">No transactions in this period. Add or import transactions to see the trend.</p> : (
          <div className="chart-wrap">
            <ResponsiveContainer width="100%" height={280}>
              <AreaChart data={trendData}>
                <defs>
                  <linearGradient id="spendExpense" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#fca5a5" stopOpacity={0.45} />
                    <stop offset="95%" stopColor="#fca5a5" stopOpacity={0.05} />
                  </linearGradient>
                  <linearGradient id="spendIncome" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#86efac" stopOpacity={0.4} />
                    <stop offset="95%" stopColor="#86efac" stopOpacity={0.05} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                <XAxis dataKey="label" stroke="#64748b" tick={{ fontSize: 11 }} />
                <YAxis stroke="#64748b" tick={{ fontSize: 11 }} />
                <Tooltip formatter={(value) => inrTwo.format(value)} contentStyle={{ background: '#0f172a', border: '1px solid rgba(148,163,184,0.3)', borderRadius: 12 }} />
                <Area type="monotone" dataKey="Expense" name="Spent" stroke="#f87171" fillOpacity={1} fill="url(#spendExpense)" />
                <Area type="monotone" dataKey="Income" name="Income" stroke="#4ade80" fillOpacity={1} fill="url(#spendIncome)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        )}
      </section>

      {/* Category + Safety */}
      <section className="content-grid two-column">
        <div className="panel">
          <div className="panel-header"><h3>Spending by category</h3><span>Expenses only</span></div>
          {categoryData.length === 0 ? <p className="muted">No expense categories yet.</p> : (
            <div className="chart-wrap small-chart">
              <ResponsiveContainer width="100%" height={240}>
                <PieChart>
                  <Pie data={categoryData} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={85} label={({ name, value }) => `${name} · ${inr.format(value)}`}>
                    {categoryData.map((entry, index) => <Cell key={entry.name} fill={CATEGORY_COLORS[index % CATEGORY_COLORS.length]} />)}
                  </Pie>
                  <Tooltip formatter={(value) => inr.format(value)} contentStyle={{ background: '#0f172a', border: '1px solid rgba(148,163,184,0.3)', borderRadius: 12 }} />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>

        <div className="panel">
          <div className="panel-header"><h3>Transaction safety</h3><span>Fraud analysis</span></div>
          {transactions.length === 0 ? <p className="muted">No transactions to assess yet.</p> : (
            <div className="safety-list">
              <div className="safety-summary">
                <span className="safety-dot normal">🟢 <b>{transactions.filter((t) => safetyStatus(t) === 'normal').length}</b> normal</span>
                <span className="safety-dot review">🟡 <b>{transactions.filter((t) => safetyStatus(t) === 'review').length}</b> review</span>
                <span className="safety-dot suspicious">🔴 <b>{summary.suspicious}</b> suspicious</span>
              </div>
              {suspiciousTransactions.length > 0 && (
                <div className="suspicious-alerts">
                  {suspiciousTransactions.slice(0, 3).map((tx) => (
                    <div className="suspicious-alert" key={tx.id}>
                      <strong>⚠️ Unusual transaction detected</strong>
                      <p>{money.format(Number(tx.amount))} at {tx.merchant || 'an unfamiliar merchant'}.</p>
                      <p className="muted">{tx.riskReason || `Risk score ${tx.riskScore ?? 'high'} on your account analysis.`}</p>
                      <button className="inline-link link-btn" onClick={() => { setSearch(tx.merchant || ''); setShowAll(true); }}>Review Transaction →</button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      </section>

      {/* Entry forms */}
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
          <p>Bank-statement headers are auto-detected. Date can be <code>Date</code>, <code>Txn Date</code>, <code>Value Date</code> etc. in many formats (dd/MM/yyyy, dd-MM-yyyy, dd-MMM-yyyy...). Amount can be a single <code>amount</code> column or split <code>credit</code>/<code>debit</code> / <code>deposit</code>/<code>withdrawal</code> / <code>Withdrawal Amt.</code>/<code>Deposit Amount</code>. Merchant can be <code>merchant</code>, <code>narration</code>, <code>particulars</code>, <code>description</code>, <code>payee</code> etc. Unknown columns are ignored; unreadable rows are listed as needing attention without failing the whole file.</p>
          <label className="file-picker">{isImporting ? 'Importing...' : 'Choose CSV file'}<input type="file" accept=".csv,text/csv" onChange={importCsv} disabled={isImporting} /></label>
          <p className="muted">Negative amounts and debit rows are saved as expenses; other rows are treated as income.</p>
        </div>
      </section>

      {message && <p className="form-success">{message}</p>}
      {error && <p className="form-error" role="alert">{error}</p>}

      {/* Recent transactions */}
      <section className="panel transaction-list">
        <div className="panel-header"><h3>Recent transactions</h3><button className="ghost-btn" onClick={() => setShowAll(true)}>View All →</button></div>
        {transactions.length === 0 ? <p className="muted">No transactions yet. Add one manually or import a CSV statement.</p> : recentRows.map((tx) => renderRow(tx, { showSafety: true }))}
      </section>

      {/* All transactions modal */}
      {showAll && (
        <div className="modal-overlay" onClick={() => setShowAll(false)}>
          <div className="modal-panel" onClick={(e) => e.stopPropagation()}>
            <div className="panel-header">
              <h3>All transactions</h3>
              <button className="ghost-btn" onClick={() => setShowAll(false)}>Close</button>
            </div>
            <div className="table-controls">
              <input className="table-search" placeholder="Search merchant or notes…" value={search} onChange={(e) => { setSearch(e.target.value); setPage(1); }} />
              <select value={typeFilter} onChange={(e) => { setTypeFilter(e.target.value); setPage(1); }}>
                <option value="ALL">All types</option><option value="INCOME">Income</option><option value="EXPENSE">Expense</option>
              </select>
              <select value={categoryFilter} onChange={(e) => { setCategoryFilter(e.target.value); setPage(1); }}>
                <option value="ALL">All categories</option>
                {categories.map((cat) => <option key={cat} value={cat}>{cat}</option>)}
              </select>
              <select value={safetyFilter} onChange={(e) => { setSafetyFilter(e.target.value); setPage(1); }}>
                <option value="ALL">All safety status</option>
                <option value="normal">Normal</option><option value="review">Review</option><option value="suspicious">Suspicious</option>
              </select>
              <select value={sortBy} onChange={(e) => setSortBy(e.target.value)}>
                <option value="date">Sort by date</option><option value="amount">Sort by amount</option>
              </select>
              <button className="ghost-btn" onClick={() => toggleSort(sortBy)}>Order: {sortDir === 'desc' ? 'Newest / high' : 'Oldest / low'}</button>
            </div>

            <div className="table-wrap">
              <table className="compare-table data-table">
                <thead>
                  <tr>
                    <th>Transaction</th><th>Date</th><th>Category</th><th>Safety</th><th className="num">Amount</th>
                  </tr>
                </thead>
                <tbody>
                  {pageRows.length === 0 && <tr><td colSpan="5" className="muted" style={{ textAlign: 'center' }}>No matching transactions.</td></tr>}
                  {pageRows.map((tx) => {
                    const status = safetyStatus(tx);
                    return (
                      <tr key={tx.id}>
                        <td><strong>{tx.merchant || 'Unknown'}</strong><span className="row-sub">{tx.notes || ''}</span></td>
                        <td>{tx.transactionDate}</td>
                        <td>{tx.category}</td>
                        <td><span className={`safety-badge ${status}`}>{SAFETY_EMOJI[status]} {SAFETY_LABEL[status]}</span></td>
                        <td className={`num ${tx.transactionType === 'EXPENSE' ? 'expense' : 'income'}`}>{tx.transactionType === 'EXPENSE' ? '-' : '+'}{money.format(Number(tx.amount))}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            <div className="pagination">
              <button className="ghost-btn" disabled={currentPage <= 1} onClick={() => setPage(currentPage - 1)}>← Prev</button>
              <span className="muted">Page {currentPage} of {pageCount} · {filteredTransactions.length} transactions</span>
              <button className="ghost-btn" disabled={currentPage >= pageCount} onClick={() => setPage(currentPage + 1)}>Next →</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default SpendingPage;
