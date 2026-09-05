import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../services/api';

function ProductDetailsPage() {
  const { id } = useParams();
  const [product, setProduct] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get(`/products/${id}`)
      .then(({ data }) => { setProduct(data); setError(''); })
      .catch(() => setError('Could not load that product.'));
  }, [id]);

  return (
    <div className="page-shell">
      {error && <p className="form-error" role="alert">{error}</p>}

      {!product && !error && <div className="skeleton-row" />}

      {product && (
        <section className="panel detail-panel">
          <div className="detail-header">
            <div>
              <p className="eyebrow">{product.category}</p>
              <h3>{product.name}</h3>
              <span className="muted">{product.suitableFor}</span>
            </div>
            <div className="detail-metrics">
              <span className={`risk-badge level-${String(product.riskLevel || '').toLowerCase().replace(/\s+/g, '-')}`}>{product.riskLevel} risk</span>
              <span>Expected return <strong>{product.expectedReturn}</strong></span>
              <span>Liquidity <strong>{product.liquidity}</strong></span>
              <span>Min amount <strong>{product.minAmount}</strong></span>
            </div>
          </div>

          <div className="product-facts" style={{ width: '100%' }}>
            <span>Tenure <strong>{product.tenure}</strong></span>
            <span>Category <strong>{product.category}</strong></span>
            <span>Minimum amount <strong>{product.minAmount}</strong></span>
            <span>Expected return <strong>{product.expectedReturn}</strong></span>
          </div>

          <p className="product-desc">{product.description}</p>

          <div className="content-grid two-column result-columns" style={{ width: '100%' }}>
            <div>
              <h4>Pros</h4>
              <ul className="check-list">{product.pros.map((item, index) => <li key={index}>{item}</li>)}</ul>
              <h4>Cons</h4>
              <ul className="warning-list">{product.cons.map((item, index) => <li key={index}>{item}</li>)}</ul>
            </div>
            <div className="safety-guide">
              <h3>Best suited for</h3>
              <p className="muted">{product.suitableFor}</p>
              <p className="muted">Compare several products side by side before deciding, and confirm current terms with the provider.</p>
              <div className="step-actions" style={{ marginTop: 14 }}>
                <Link className="ghost-btn" to={`/products?compare=${product.id}`}>Compare with 2–3 products</Link>
                <Link className="ghost-btn" to="/products">Back to products</Link>
              </div>
            </div>
          </div>
        </section>
      )}
    </div>
  );
}

export default ProductDetailsPage;