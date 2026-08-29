import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { api } from '../services/api';

const rows = [
  ['Category', 'category'],
  ['Risk level', 'riskLevel'],
  ['Expected return', 'expectedReturn'],
  ['Liquidity', 'liquidity'],
  ['Minimum amount', 'minAmount'],
  ['Tenure', 'tenure'],
  ['Best suited for', 'suitableFor'],
];

function ComparePage() {
  const [params] = useSearchParams();
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  const idsParam = params.get('ids') || '';
  const hasSelection = idsParam.split(',').some((value) => Number(value) > 0);

  useEffect(() => {
    if (!hasSelection) return;
    const ids = idsParam
      .split(',')
      .map((value) => Number(value))
      .filter((value) => value > 0);
    api.get('/products/compare', { params: { ids } })
      .then(({ data }) => { setData(data); setError(''); })
      .catch(() => setError('Could not load the comparison.'));
  }, [idsParam, hasSelection]);

  if (error) return <div className="page-shell"><p className="form-error" role="alert">{error}</p></div>;

  return (
    <div className="page-shell">
      <h2>Compare Products</h2>
      {!hasSelection || !data || data.products.length === 0 ? (
        <div className="panel info-box">
          <p>Pick 2–3 products from the <Link to="/products">Products</Link> page and press compare, or add <code>?ids=</code> like <code>/compare?ids=1,3,9</code>.</p>
        </div>
      ) : (
        <>
          <div className="panel table-wrap">
            <table className="compare-table">
              <thead>
                <tr>
                  <th>Feature</th>
                  {data.products.map((product) => <th key={product.id}>{product.name}</th>)}
                </tr>
              </thead>
              <tbody>
                {rows.map(([label, key]) => (
                  <tr key={key}>
                    <td>{label}</td>
                    {data.products.map((product) => <td key={product.id}>{product[key]}</td>)}
                  </tr>
                ))}
                <tr>
                  <td>Pros</td>
                  {data.products.map((product) => <td key={product.id}><ul className="check-list">{product.pros.map((item, index) => <li key={index}>{item}</li>)}</ul></td>)}
                </tr>
                <tr>
                  <td>Cons</td>
                  {data.products.map((product) => <td key={product.id}><ul className="warning-list">{product.cons.map((item, index) => <li key={index}>{item}</li>)}</ul></td>)}
                </tr>
              </tbody>
            </table>
          </div>
          <section className="panel">
            <div className="panel-header"><h3>Guidance</h3><span>Educational, not advice</span></div>
            <ol className="number-list">{data.guidance.map((item, index) => <li key={index}>{item}</li>)}</ol>
          </section>
        </>
      )}
    </div>
  );
}

export default ComparePage;