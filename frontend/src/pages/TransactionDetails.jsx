import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, ShieldAlert, ShieldCheck, Sparkles } from 'lucide-react';
import { api } from '../services/api';
import ScoreRing from '../components/ScoreRing';
import EmptyState from '../components/EmptyState';
import Skeleton from '../components/Skeleton';
import { useToast } from '../components/Toast';

const money = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 });

function TransactionDetailsPage() {
  const { id } = useParams();
  const toast = useToast();
  const [transactions, setTransactions] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [marking, setMarking] = useState(false);

  useEffect(() => {
    let alive = true;
    api.get('/transactions')
      .then(({ data }) => {
        if (alive) { setTransactions(Array.isArray(data) ? data : []); setError(''); }
      })
      .catch(() => {
        if (alive) setError('Could not load transactions.');
      })
      .finally(() => {
        if (alive) setLoading(false);
      });
    return () => { alive = false; };
  }, [id]);

  const transaction = useMemo(
    () => transactions.find((item) => String(item.id) === String(id)),
    [transactions, id],
  );

  const recent = useMemo(() => transactions.filter((item) => String(item.id) !== String(id)).slice(0, 5), [transactions, id]);

  function handleMarkSafe() {
    setMarking(true);
    toast('Marking transactions safe is not available in this build yet. The backend has no endpoint for it.', { type: 'info' });
    setMarking(false);
  }

  if (loading) {
    return (
      <div className="page-shell">
        <Skeleton rows={5} cards={2} />
      </div>
    );
  }

  if (error && !transaction) {
    return (
      <div className="page-shell">
        <p className="form-error" role="alert">{error}</p>
        <Link to="/transactions" className="ghost-btn"><ArrowLeft size={16} /> Back to transactions</Link>
      </div>
    );
  }

  if (!transaction) {
    return (
      <div className="page-shell">
        <EmptyState
          icon={ShieldAlert}
          title="Transaction not found"
          text="This transaction doesn't exist or may have been removed."
          action={<Link to="/transactions" className="ghost-btn"><ArrowLeft size={16} /> Back to transactions</Link>}
        />
      </div>
    );
  }

  const hasRisk = transaction.riskScore != null;
  const riskScore = Number(transaction.riskScore) || 0;
  const rawReasons = transaction.riskReason;
  const reasons = Array.isArray(rawReasons)
    ? rawReasons
    : typeof rawReasons === 'string' && rawReasons.trim()
      ? rawReasons.split(/\s*(?:\n+|;\s*|\.\s+(?=[A-Z]))\s*/).map((item) => item.trim()).filter(Boolean)
      : [];

  return (
    <div className="page-shell">
      <Link to="/transactions" className="ghost-btn" style={{ width: 'fit-content' }}>
        <ArrowLeft size={16} /> All transactions
      </Link>

      <div className="panel" style={{ position: 'relative', overflow: 'hidden' }}>
        <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(135deg, rgba(79,70,229,0.05), transparent 55%)', pointerEvents: 'none' }} />
        <div className="detail-header" style={{ position: 'relative' }}>
          <div>
            <p className="eyebrow">Transaction detail · #{transaction.id}</p>
            <h2 style={{ margin: '6px 0', fontSize: '1.8rem', letterSpacing: '-0.02em' }}>{transaction.merchant || 'Unknown merchant'}</h2>
            <div className="detail-metrics">
              <span>{transaction.transactionDate}</span>
              <span>{transaction.category}</span>
              {transaction.source && <span>Source: {transaction.source}</span>}
            </div>
          </div>
          <div style={{ textAlign: 'right' }}>
            <p className="eyebrow">{transaction.transactionType === 'EXPENSE' ? 'Debit' : 'Credit'}</p>
            <strong className={transaction.transactionType === 'EXPENSE' ? 'expense' : 'income'} style={{ fontSize: '2rem', letterSpacing: '-0.02em' }}>
              {transaction.transactionType === 'EXPENSE' ? '-' : '+'}{money.format(Number(transaction.amount))}
            </strong>
          </div>
        </div>

        {transaction.notes && (
          <p className="muted" style={{ position: 'relative', margin: '12px 0 0' }}>
            <strong style={{ color: 'var(--text-2)' }}>Notes: </strong>{transaction.notes}
          </p>
        )}
      </div>

      {hasRisk ? (
        <section className="content-grid two-column" style={{ alignItems: 'stretch' }}>
          <div className="panel" style={{ display: 'flex', gap: 24, alignItems: 'center', flexWrap: 'wrap' }}>
            <ScoreRing score={riskScore} size={170} label="/ 100" />
            <div style={{ flex: 1, minWidth: 220 }}>
              <p className="eyebrow">Risk assessment</p>
              <h3 style={{ margin: '6px 0' }}>{String(transaction.riskLevel || 'Unknown').toUpperCase()}</h3>
              <p className="muted" style={{ margin: 0 }}>
                Score assigned by the anomaly engine after comparing this transaction against your spending history.
              </p>
            </div>
          </div>

          <div className="panel">
            <div className="panel-header"><h3>Why this score</h3><span>Explainable flags</span></div>
            {reasons.length === 0 ? (
              <p className="muted">No specific signals were recorded for this transaction.</p>
            ) : (
              <ul className="check-list" style={{ margin: 0, paddingLeft: 22 }}>
                {reasons.map((reason, index) => <li key={index}>{reason}</li>)}
              </ul>
            )}
            <div className="step-actions" style={{ marginTop: 16 }}>
              <button className="ghost-btn" onClick={handleMarkSafe} disabled={marking}>
                <ShieldCheck size={15} /> Mark as safe
              </button>
              <Link to="/incidents" className="ghost-btn" style={{ color: 'var(--danger-ink)', borderColor: 'rgba(220,38,38,0.4)' }}>
                <ShieldAlert size={15} /> Report as fraud
              </Link>
            </div>
          </div>
        </section>
      ) : (
        <section className="panel">
          <div className="empty-state" style={{ border: 'none', background: 'transparent', padding: '24px' }}>
            <span className="empty-icon"><Sparkles size={24} /></span>
            <h3>Not analyzed yet</h3>
            <p>This transaction hasn't been through the risk engine. Run a check on it or let the next scan cover your history.</p>
            <Link to="/transaction-safety" className="primary-btn">Assess a transaction</Link>
          </div>
        </section>
      )}

      {recent.length > 0 && (
        <section className="panel">
          <div className="panel-header"><h3>Recent activity</h3><Link to="/transactions" className="link-btn">View all →</Link></div>
          <div className="transaction-list">
            {recent.map((tx) => (
              <Link className="transaction-row" key={tx.id} to={`/transactions/${tx.id}`} style={{ textDecoration: 'none' }}>
                <div>
                  <strong>{tx.merchant || 'Unknown'}</strong>
                  <span>{tx.transactionDate} · {tx.category}</span>
                </div>
                <strong className={tx.transactionType === 'EXPENSE' ? 'expense' : 'income'}>
                  {tx.transactionType === 'EXPENSE' ? '-' : '+'}{money.format(Number(tx.amount))}
                </strong>
              </Link>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}

export default TransactionDetailsPage;