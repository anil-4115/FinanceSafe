import { useEffect, useState } from 'react';
import { api } from '../services/api';
import {
  ResponsiveContainer,
  LineChart,
  Line,
  CartesianGrid,
  XAxis,
  YAxis,
  Tooltip,
} from 'recharts';

const inr = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 0,
});

function MarketsPage() {
  const [universe, setUniverse] = useState([]);
  const [query, setQuery] = useState('');
  const [symbol, setSymbol] = useState(null);
  const [detail, setDetail] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => { loadUniverse(); }, []);

  async function loadUniverse(term = '') {
    try {
      const { data } = await api.get('/market/search', { params: term ? { q: term } : {} });
      setUniverse(data);
      setError('');
    } catch {
      setError('Could not load the market universe.');
    }
  }

  async function openSymbol(next) {
    setSymbol(next);
    setDetail(null);
    setError('');
    try {
      const { data } = await api.get(`/market/${next}`);
      setDetail(data);
    } catch {
      setError('Could not load that symbol. Showing the list again below.');
      setSymbol(null);
      setDetail(null);
    }
  }

  function search(event) {
    event.preventDefault();
    loadUniverse(query.trim());
  }

  const history = (detail?.history || []).map((point) => ({
    date: point.date.slice(0, 10),
    price: Number(point.price),
  }));

  return (
    <div className="page-shell">
      <h2>Markets</h2>
      {error && <p className="form-error" role="alert">{error}</p>}

      <form className="search-bar" onSubmit={search}>
        <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search by symbol, name or sector e.g. RELIANCE, IT, Banking" />
        <button className="primary-btn">Search</button>
      </form>

      {!symbol && (
        <section className="card-grid market-grid">
          {universe.length === 0 && !error ? (
            <p className="muted">No symbols match "{query}". Try a symbol, sector or asset type.</p>
          ) : universe.map((item) => (
            <button type="button" className="market-card" key={item.symbol} onClick={() => openSymbol(item.symbol)}>
              <strong>{item.symbol}</strong>
              <span>{item.name}</span>
              <small>{item.assetType} · {item.sector}</small>
              <span className="market-details-link">View details →</span>
            </button>
          ))}
        </section>
      )}

      {symbol && !detail && !error && (
        <p className="muted">Loading {symbol}…</p>
      )}

      {symbol && detail && (
        <section className="panel detail-panel">
          <div className="detail-header">
            <div>
              <p className="eyebrow">{detail.symbol}</p>
              <h3>{detail.name}</h3>
              <span className="muted">{detail.sector}</span>
            </div>
            <div className="detail-metrics">
              <span className={`risk-badge level-${String(detail.riskLevel || '').toLowerCase().replace(/\s+/g, '-')}`}>{detail.riskLevel} risk</span>
              <span>Trend <strong>{detail.trend}</strong></span>
              <span>Volatility <strong>{detail.volatilityPct}%</strong></span>
              <span>Period change <strong>{Number(detail.changePct) >= 0 ? '+' : ''}{detail.changePct}%</strong></span>
            </div>
          </div>
          <div className="chart-wrap">
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={history}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e6e9f2" />
                <XAxis dataKey="date" stroke="#64748b" tick={{ fontSize: 11 }} />
                <YAxis stroke="#64748b" domain={['auto', 'auto']} />
                <Tooltip formatter={(value) => inr.format(value)} contentStyle={{ background: '#ffffff', border: '1px solid rgba(15,23,42,0.15)', borderRadius: 12 }} labelStyle={{ color: '#0f172a' }} itemStyle={{ color: '#0f172a' }} />
                <Line type="monotone" dataKey="price" stroke="#5b7cff" dot={false} strokeWidth={2} />
              </LineChart>
            </ResponsiveContainer>
          </div>
          <div className="content-grid two-column result-columns">
            <div>
              <h4>Why the market sees it this way</h4>
              <ul className="check-list">{detail.rationale.map((item, index) => <li key={index}>{item}</li>)}</ul>
            </div>
            <div className="safety-guide">
              <h3>Disclaimer</h3>
              <p className="muted">{detail.disclaimer}</p>
              <button className="ghost-btn" onClick={() => { setSymbol(null); setDetail(null); }}>Back to list</button>
            </div>
          </div>
        </section>
      )}
    </div>
  );
}

export default MarketsPage;