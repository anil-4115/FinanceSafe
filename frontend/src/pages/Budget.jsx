import { useEffect, useState } from 'react'; import { api } from '../services/api';
function BudgetPage() {
  const [items,setItems]=useState([]); const [form,setForm]=useState({category:'Food',monthlyLimit:''}); const [error,setError]=useState('');
  const load=()=>api.get('/budgets').then(({data})=>setItems(data)).catch(()=>setError('Could not load budgets.'));
  useEffect(()=>{load();},[]);
  async function submit(e){e.preventDefault();try{await api.post('/budgets',{...form,monthlyLimit:Number(form.monthlyLimit)});setForm({...form,monthlyLimit:''});load();}catch(x){setError(x.response?.data?.message||'Could not save budget.');}}
  return (
    <div className="page-shell">
      <h2>Budget</h2>
      <form className="panel data-form" onSubmit={submit}><div className="panel-header"><h3>Create category budget</h3><span>Monthly limit</span></div><label>Category<input value={form.category} onChange={e=>setForm({...form,category:e.target.value})} required /></label><label>Limit (₹)<input type="number" min="1" value={form.monthlyLimit} onChange={e=>setForm({...form,monthlyLimit:e.target.value})} required /></label><button className="primary-btn">Save budget</button></form>
      {error&&<p className="form-error">{error}</p>}<section className="panel transaction-list"><div className="panel-header"><h3>Your budgets</h3><span>{items.length} categories</span></div>{items.length?items.map(i=><div className="transaction-row" key={i.id}><strong>{i.category}</strong><strong>₹{Number(i.monthlyLimit).toLocaleString('en-IN')}</strong></div>):<p className="muted">Create a budget to receive overspending alerts.</p>}</section>
    </div>
  );
}

export default BudgetPage;
