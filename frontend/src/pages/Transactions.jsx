import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  FileUp,
  Plus,
  ShieldAlert,
  Wallet,
} from 'lucide-react';
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { api } from '../services/api';
import PageHeader from '../components/PageHeader';
import StatCard from '../components/StatCard';
import Modal from '../components/Modal';
import EmptyState from '../components/EmptyState';
import Skeleton from '../components/Skeleton';
import { useToast } from '../components/Toast';

const money = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 });
const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 });
const CATEGORY_COLORS = ['#5b7cff', '#14b8a6', '#f59e0b', '#f87171', '#a78bfa', '#10b981', '#38bdf8', '#fb923c'];

const initialForm = {
  transactionDate: '',
  merchant: '',
  amount: '',
  transactionType: 'EXPENSE',
  category: 'General',
  notes: '',
};

function blankForm() {
  return { ...initialForm, transactionDate: new Date().toISOString().slice(0, 10) };
}

function toJsDate(iso) {
  const d = new Date(`${iso}T00:00:00`);
  return isNaN(d.getTime()) ? null : d;
}

function safetyStatus(transaction) {
  const level = transaction?.riskLevel;
  if (level === 'HIGH' || level === 'CRITICAL') return 'suspicious';
  if (level === 'MODERATE') return 'review';
  if (level == null || transaction?.riskScore == null) return 'unanalyzed';
  return 'normal';
}

const SAFETY_LABEL = { normal: 'Normal', review: 'Review', suspicious: 'Suspicious', unanalyzed: 'Unanalyzed' };

function TransactionsPage() {
  const toast = useToast();
  const navigate = useNavigate();
  const [transactions, setTransactions] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const [form, setForm] = useState(blankForm);
  const [isSaving, setIsSaving] = useState(false);
  const [isImporting, setIsImporting] = useState(false);

  const [modalMode, setModalMode] = useState(null);
  const openAdd = () => { setForm(blankForm()); setError(''); setModalMode('add'); };

  const [trendRange, setTrendRange] = useState('30D');
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState('ALL');
  const [categoryFilter, setCategoryFilter] = useState('ALL');
  const [safetyFilter, setSafetyFilter] = useState('ALL');
  const [sortBy, setSortBy] = useState('date');
  const [sortDir, setSortDir] = useState('desc');
  const [page, setPage] = useState(1);
  const pageSize = 10;

  async function loadTransactions() {
    try {
      const { data } = await api.get('/transactions');
      setTransactions(data);
      setError('');
    } catch {
      setError('Could not load transactions. Please sign in again and retry.');
    }
  }

  useEffect(() => {
    let alive = true;
    api.get('/transactions')
      .then(({ data }) => { if (alive) { setTransactions(data); setError(''); } })
      .catch(() => { if (alive) setError('Could not load transactions. Please sign in again and retry.'); })
      .finally(() => { if (alive) setLoading(false); });
    return () => { alive = false; };
  }, []);

  async function addTransaction(event) {
    event.preventDefault();
    setError('');
    setIsSaving(true);
    try {
      await api.post('/transactions', { ...form, amount: Number(form.amount) });
      setForm(blankForm());
      setModalMode(null);
      toast('Transaction saved.');
      await loadTransactions();
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Could not save the transaction.');
    } finally {
      setIsSaving(false);
    }
  }

  async function importCsv(event) {
    const file = event.target.files?.[0];
    if (!file) return;
    setError('');
    setIsImporting(true);
    try {
      const body = new FormData();
      body.append('file', file);
      const { data } = await api.post('/transactions/import', body, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      toast(`${data.imported} transaction(s) imported.`);
      if (data.needsAttention?.length) {
        setError(data.needsAttention.slice(0, 3).join(' '));
      }
      setModalMode(null);
      await loadTransactions();
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Could not import this CSV file.');
    } finally {
      setIsImporting(false);
      event.target.value = '';
    }
  }

  const summary = useMemo(() => {
    let income = 0;
    let spent = 0;
    let suspicious = 0;
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
    const start = new Date(now);
    start.setDate(start.getDate() - (rangeDays - 1));
    start.setHours(0, 0, 0, 0);
    const labelFmt = trendRange === '1Y' ? { month: 'short', year: 'numeric' } : { day: 'numeric', month: 'short' };
    if (trendRange === '1Y') {
      const monthBuckets = Array.from({ length: 12 }, (_, i) => {
        const d = new Date(now.getFullYear(), now.getMonth() - 11 + i, 1);
        const key = `${d.getFullYear()}-${d.getMonth()}`;
        const label = d.toLocaleDateString('en-IN', { month: 'short', year: 'numeric' });
        return { key, label, Expense: 0, Income: 0 };
      });
      for (const tx of transactions) {
        const d = toJsDate(tx.transactionDate);
        if (!d) continue;
        const key = `${d.getFullYear()}-${d.getMonth()}`;
        const bucket = monthBuckets.find((b) => b.key === key);
        if (!bucket) continue;
        const n = Number(tx.amount) || 0;
        if (tx.transactionType === 'INCOME') bucket.Income += n;
        else bucket.Expense += n;
      }
      return monthBuckets;
    }
    const byDay = new Map();
    for (const tx of transactions) {
      const d = toJsDate(tx.transactionDate);
      if (!d || d < start || d > now) continue;
      const dayKey = d.toDateString();
      if (!byDay.has(dayKey)) {
        byDay.set(dayKey, { key: dayKey, label: d.toLocaleDateString('en-IN', labelFmt), Expense: 0, Income: 0 });
      }
      const n = Number(tx.amount) || 0;
      const rec = byDay.get(dayKey);
      if (tx.transactionType === 'INCOME') rec.Income += n;
      else rec.Expense += n;
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

  const categories = useMemo(() => [...new Set(transactions.map((tx) => tx.category).filter(Boolean))].sort(), [transactions]);

  const filtered = useMemo(() => {
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

  const pageCount = Math.max(1, Math.ceil(filtered.length / pageSize));
  const currentPage = Math.min(page, pageCount);
  const pageRows = filtered.slice((currentPage - 1) * pageSize, currentPage * pageSize);

  function toggleSort(field) {
    if (sortBy === field) setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
    else {
      setSortBy(field);
      setSortDir('desc');
    }
  }

  const counts = useMemo(() => ({
    normal: transactions.filter((t) => safetyStatus(t) === 'normal').length,
    review: transactions.filter((t) => safetyStatus(t) === 'review').length,
    suspicious: summary.suspicious,
    unanalyzed: transactions.filter((t) => safetyStatus(t) === 'unanalyzed').length,
  }), [transactions, summary.suspicious]);

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="Money activity"
        title="Transactions"
        intro="Add, import and review every transaction. Any row flagged by risk analysis links to a full review."
        actions={
          <>
            <button className="ghost-btn" onClick={() => setModalMode('import')}><FileUp size={16} /> Import CSV</button>
            <button className="primary-btn" onClick={openAdd}><Plus size={16} /> Add transaction</button>
          </>
        }
      />

      {error && <p className="form-error" role="alert">{error}</p>}

      {loading ? (
        <section className="stats-grid stagger"><Skeleton cards={4} rows={0} /></section>
      ) : (
        <section className="stats-grid stagger">
          <StatCard icon={Wallet} label="Total balance" value={summary.balance} prefix="₹" />
          <StatCard icon={Wallet} iconTone="green" label="Total income" value={summary.income} prefix="₹" />
          <StatCard icon={Wallet} iconTone="red" label="Total spent" value={summary.spent} prefix="₹" />
          <StatCard icon={ShieldAlert} iconTone={summary.suspicious > 0 ? 'red' : 'green'} label="Suspicious transactions" value={summary.suspicious} prefix="" decimals={0}>
            <button
              type="button"
              className="link-btn"
              onClick={() => { setSafetyFilter('suspicious'); setPage(1); }}
            >
              Show flagged →
            </button>
          </StatCard>
        </section>
      )}

      <section className="panel">
        <div className="panel-header">
          <h3>Spending trend</h3>
          <div className="filter-row">
            {['7D', '30D', '3M', '1Y'].map((range) => (
              <button key={range} className={`ghost-btn ${trendRange === range ? 'active-filter' : ''}`} onClick={() => setTrendRange(range)}>{range}</button>
            ))}
          </div>
        </div>
        {trendData.length === 0 ? (
          <EmptyState
            icon={Wallet}
            title="No activity in this period"
            text="Add a transaction manually or import a CSV statement to see your spending trend."
            action={<button className="primary-btn" onClick={openAdd}><Plus size={15} /> Add transaction</button>}
          />
        ) : (
          <div className="chart-wrap">
            <ResponsiveContainer width="100%" height={280}>
              <AreaChart data={trendData}>
                <defs>
                  <linearGradient id="txExpense" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#f87171" stopOpacity={0.3} />
                    <stop offset="95%" stopColor="#f87171" stopOpacity={0.02} />
                  </linearGradient>
                  <linearGradient id="txIncome" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#34d399" stopOpacity={0.28} />
                    <stop offset="95%" stopColor="#34d399" stopOpacity={0.02} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#e6e9f2" />
                <XAxis dataKey="label" stroke="#9aa3b2" tick={{ fontSize: 11 }} />
                <YAxis stroke="#9aa3b2" tick={{ fontSize: 11 }} />
                <Tooltip formatter={(value) => money.format(Number(value))} contentStyle={{ background: '#fff', border: '1px solid #e7eaf3', borderRadius: 12 }} />
                <Area type="monotone" dataKey="Expense" name="Spent" stroke="#f87171" fillOpacity={1} fill="url(#txExpense)" strokeWidth={2} />
                <Area type="monotone" dataKey="Income" name="Income" stroke="#34d399" fillOpacity={1} fill="url(#txIncome)" strokeWidth={2} />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        )}
      </section>

      <section className="content-grid two-column">
        <div className="panel">
          <div className="panel-header"><h3>Spending by category</h3><span>Expenses only</span></div>
          {categoryData.length === 0 ? <p className="muted">No expense categories yet.</p> : (
            <div className="chart-wrap small-chart">
              <ResponsiveContainer width="100%" height={240}>
                <BarChart layout="vertical" data={categoryData} margin={{ left: 8, right: 8 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e6e9f2" horizontal={false} />
                  <XAxis type="number" stroke="#9aa3b2" tick={{ fontSize: 11 }} />
                  <YAxis type="category" dataKey="name" width={110} stroke="#9aa3b2" tick={{ fontSize: 12 }} />
                  <Tooltip formatter={(value) => inr.format(value)} contentStyle={{ background: '#fff', border: '1px solid #e7eaf3', borderRadius: 12 }} />
                  <Bar dataKey="value" radius={[0, 6, 6, 0]} maxBarSize={22}>
                    {categoryData.map((row, index) => <Cell key={row.name} fill={CATEGORY_COLORS[index % CATEGORY_COLORS.length]} />)}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>

        <div className="panel">
          <div className="panel-header"><h3>Transaction safety</h3><span>Fraud analysis</span></div>
          {transactions.length === 0 ? (
            <p className="muted">No transactions to assess yet.</p>
          ) : (
            <div className="safety-list">
              <div className="safety-summary">
                <span className="safety-dot">🟢 <b>{counts.normal}</b> normal</span>
                <span className="safety-dot">🟡 <b>{counts.review}</b> review</span>
                <span className="safety-dot">🔴 <b>{counts.suspicious}</b> suspicious</span>
                <span className="safety-dot">🩶 <b>{counts.unanalyzed}</b> unanalyzed</span>
              </div>
              {transactions.filter((t) => safetyStatus(t) === 'suspicious').slice(0, 3).map((tx) => (
                <div className="suspicious-alert" key={tx.id}>
                  <strong>⚠️ Unusual transaction detected</strong>
                  <p>{money.format(Number(tx.amount))} at {tx.merchant || 'an unfamiliar merchant'}.</p>
                  <Link className="link-btn" to={`/transactions/${tx.id}`}>Review transaction →</Link>
                </div>
              ))}
            </div>
          )}
        </div>
      </section>

      {loading ? (
        <Skeleton rows={6} cards={1} />
      ) : (
        <div className="panel">
          <div className="panel-header">
            <h3>All transactions</h3>
            <span>{filtered.length} of {transactions.length}</span>
          </div>

          <div className="table-controls" style={{ marginBottom: 14 }}>
            <input
              className="table-search"
              placeholder="Search merchant or notes…"
              value={search}
              onChange={(event) => { setSearch(event.target.value); setPage(1); }}
            />
            <select value={typeFilter} onChange={(event) => { setTypeFilter(event.target.value); setPage(1); }}>
              <option value="ALL">All types</option>
              <option value="INCOME">Income</option>
              <option value="EXPENSE">Expense</option>
            </select>
            <select value={categoryFilter} onChange={(event) => { setCategoryFilter(event.target.value); setPage(1); }}>
              <option value="ALL">All categories</option>
              {categories.map((cat) => <option key={cat} value={cat}>{cat}</option>)}
            </select>
            <select value={safetyFilter} onChange={(event) => { setSafetyFilter(event.target.value); setPage(1); }}>
              <option value="ALL">All safety status</option>
              <option value="normal">Normal</option>
              <option value="review">Review</option>
              <option value="suspicious">Suspicious</option>
              <option value="unanalyzed">Unanalyzed</option>
            </select>
            <button className="ghost-btn" onClick={() => toggleSort(sortBy)}>
              {sortBy === 'amount' ? 'Amount' : 'Date'} · {sortDir === 'desc' ? 'newest' : 'oldest'}
            </button>
          </div>

          <div className="table-wrap">
            <table className="compare-table data-table">
              <thead>
                <tr>
                  <th>Transaction</th>
                  <th>Date</th>
                  <th>Category</th>
                  <th>Safety</th>
                  <th className="num">Amount</th>
                </tr>
              </thead>
              <tbody>
                {pageRows.length === 0 && (
                  <tr>
                    <td colSpan="5" className="muted" style={{ textAlign: 'center' }}>No matching transactions.</td>
                  </tr>
                )}
                {pageRows.map((tx) => {
                  const status = safetyStatus(tx);
                  return (
                    <tr key={tx.id} style={{ cursor: 'pointer' }} onClick={() => navigate(`/transactions/${tx.id}`)}>
                      <td>
                        <strong>{tx.merchant || 'Unknown'}</strong>
                        <span className="row-sub">{tx.notes || ''}</span>
                      </td>
                      <td>{tx.transactionDate}</td>
                      <td>{tx.category}</td>
                      <td><span className={`safety-badge ${status}`}>{SAFETY_LABEL[status]}</span></td>
                      <td className={`num ${tx.transactionType === 'EXPENSE' ? 'expense' : 'income'}`}>
                        {tx.transactionType === 'EXPENSE' ? '-' : '+'}{money.format(Number(tx.amount))}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          {filtered.length > pageSize && (
            <div className="pagination">
              <button className="ghost-btn" disabled={currentPage <= 1} onClick={() => setPage(currentPage - 1)}>← Prev</button>
              <span className="muted">Page {currentPage} of {pageCount}</span>
              <button className="ghost-btn" disabled={currentPage >= pageCount} onClick={() => setPage(currentPage + 1)}>Next →</button>
            </div>
          )}
        </div>
      )}

      <Modal
        open={modalMode === 'add'}
        onClose={() => { setModalMode(null); setError(''); }}
        title="Add transaction"
        subtitle="Manual entry — risk analysis runs automatically on your account."
      >
        <form className="data-form" onSubmit={addTransaction}>
          <label>Date<input type="date" value={form.transactionDate} onChange={(event) => setForm({ ...form, transactionDate: event.target.value })} required /></label>
          <label>Merchant or source<input value={form.merchant} onChange={(event) => setForm({ ...form, merchant: event.target.value })} placeholder="e.g. Grocery store" required /></label>
          <label>Amount (₹)<input type="number" min="0.01" step="0.01" value={form.amount} onChange={(event) => setForm({ ...form, amount: event.target.value })} required /></label>
          <label>Type<select value={form.transactionType} onChange={(event) => setForm({ ...form, transactionType: event.target.value })}><option value="EXPENSE">Expense</option><option value="INCOME">Income</option></select></label>
          <label>Category<input value={form.category} onChange={(event) => setForm({ ...form, category: event.target.value })} required /></label>
          <label>Notes (optional)<input value={form.notes} onChange={(event) => setForm({ ...form, notes: event.target.value })} /></label>
          {error && <p className="form-error" role="alert">{error}</p>}
          <button className="primary-btn" disabled={isSaving}>{isSaving ? 'Saving...' : 'Save transaction'}</button>
        </form>
      </Modal>

      <Modal
        open={modalMode === 'import'}
        onClose={() => { setModalMode(null); setError(''); }}
        title="Import bank statement"
        subtitle="CSV only · headers are auto-detected"
      >
        <p className="muted" style={{ margin: 0, lineHeight: 1.7 }}>
          Date can be <code>Date</code>, <code>Txn Date</code> or <code>Value Date</code> in formats like
          dd/MM/yyyy, dd-MM-yyyy or dd-MMM-yyyy. Amount can be a single <code>amount</code> column or split
          <code> credit</code>/<code>debit</code> / <code>deposit</code>/<code>withdrawal</code>.
          Merchant can be <code>narration</code>, <code>particulars</code> or <code>description</code>.
        </p>
        {error && <p className="form-error" role="alert">{error}</p>}
        <label className="file-picker" style={{ alignSelf: 'flex-start' }}>
          {isImporting ? 'Importing...' : 'Choose CSV file'}
          <input type="file" accept=".csv,text/csv" onChange={importCsv} disabled={isImporting} />
        </label>
        <p className="muted" style={{ margin: 0 }}>Negative amounts and debit rows are saved as expenses; other rows are treated as income.</p>
      </Modal>
    </div>
  );
}

export default TransactionsPage;