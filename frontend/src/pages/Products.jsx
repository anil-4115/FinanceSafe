import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../services/api';

function ProductsPage() {
  const [products, setProducts] = useState([]);
  const [category, setCategory] = useState('');
  const [selected, setSelected] = useState([]);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    api.get('/products', { params: category ? { category } : {} })
      .then(({ data }) => setProducts(data))
      .catch(() => setError('Could not load financial products.'));
  }, [category]);

  const categories = useMemo(() => {
    const all = [];
    products.forEach((product) => {
      if (product.category && !all.includes(product.category)) all.push(product.category);
    });
    return all.sort();
  }, [products]);

  function toggle(productId) {
    setSelected((current) => (current.includes(productId)
      ? current.filter((id) => id !== productId)
      : current.length >= 3 ? current : [...current, productId]));
  }

  function openCompare() {
    navigate(`/compare?ids=${selected.join(',')}`);
  }

  return (
    <div className="page-shell">
      <div className="page-head-actions">
        <h2>Financial Products</h2>
        <button className="primary-btn" disabled={selected.length < 2} onClick={openCompare}>
          Compare {selected.length > 0 ? `(${selected.length})` : '(pick 2–3)'}
        </button>
      </div>

      <div className="filter-row">
        <button className={`ghost-btn ${category === '' ? 'active-filter' : ''}`} onClick={() => setCategory('')}>All</button>
        {categories.map((item) => (
          <button key={item} className={`ghost-btn ${category === item ? 'active-filter' : ''}`} onClick={() => setCategory(item)}>{item}</button>
        ))}
      </div>

      {error && <p className="form-error" role="alert">{error}</p>}

      <section className="card-grid product-grid">
        {products.map((product) => (
          <article className="product-card" key={product.id}>
            <label className="product-pick">
              <input type="checkbox" checked={selected.includes(product.id)} onChange={() => toggle(product.id)} />
              <span>Compare</span>
            </label>
            <span className={`risk-badge level-${String(product.riskLevel || '').toLowerCase().replace(/\s+/g, '-')}`}>{product.riskLevel}</span>
            <h3>{product.name}</h3>
            <p className="muted">{product.category}</p>
            <div className="product-facts">
              <span>Expected return <strong>{product.expectedReturn}</strong></span>
              <span>Liquidity <strong>{product.liquidity}</strong></span>
              <span>Min amount <strong>₹{product.minAmount}</strong></span>
              <span>Tenure <strong>{product.tenure}</strong></span>
            </div>
            <p className="muted">Best for: {product.suitableFor}</p>
            <p className="product-desc">{product.description}</p>
            <Link className="ghost-btn inline-link" to={`/compare?ids=${product.id}`}>See in detail</Link>
          </article>
        ))}
      </section>
    </div>
  );
}

export default ProductsPage;