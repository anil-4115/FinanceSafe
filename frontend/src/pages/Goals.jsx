import { useEffect, useState } from 'react'; import { api } from '../services/api';
function GoalsPage() {
 const [items,setItems]=useState([]);const [form,setForm]=useState({name:'',targetAmount:'',currentAmount:'',deadline:'',monthlyContribution:''});const [error,setError]=useState(''); const load=()=>api.get('/goals').then(({data})=>setItems(data)).catch(()=>setError('Could not load goals.'));useEffect(()=>{load();},[]);
 async function submit(e){e.preventDefault();try{await api.post('/goals',{...form,targetAmount:Number(form.targetAmount),currentAmount:Number(form.currentAmount||0),monthlyContribution:Number(form.monthlyContribution||0)});setForm({name:'',targetAmount:'',currentAmount:'',deadline:'',monthlyContribution:''});load();}catch(x){setError(x.response?.data?.message||'Could not save goal.');}}
  return (
    <div className="page-shell">
      <h2>Financial Goals</h2>
      <form className="panel data-form" onSubmit={submit}><div className="panel-header"><h3>Create a goal</h3><span>Plan ahead</span></div><label>Name<input value={form.name} onChange={e=>setForm({...form,name:e.target.value})} required /></label><label>Target amount (₹)<input type="number" min="1" value={form.targetAmount} onChange={e=>setForm({...form,targetAmount:e.target.value})} required /></label><label>Saved so far (₹)<input type="number" min="0" value={form.currentAmount} onChange={e=>setForm({...form,currentAmount:e.target.value})}/></label><label>Deadline<input type="date" value={form.deadline} onChange={e=>setForm({...form,deadline:e.target.value})}/></label><label>Monthly contribution (₹)<input type="number" min="0" value={form.monthlyContribution} onChange={e=>setForm({...form,monthlyContribution:e.target.value})}/></label><button className="primary-btn">Save goal</button></form>
      {error&&<p className="form-error">{error}</p>}<section className="panel transaction-list"><div className="panel-header"><h3>Your goals</h3><span>{items.length} goals</span></div>{items.map(i=>{const p=Math.min(100,Math.round(Number(i.currentAmount)/Number(i.targetAmount)*100));return <div className="transaction-row" key={i.id}><div><strong>{i.name}</strong><span>{p}% complete {i.deadline?`· deadline ${i.deadline}`:''}</span></div><strong>{p}%</strong></div>})}{!items.length&&<p className="muted">Create a goal such as an emergency fund, laptop, or travel fund.</p>}</section>
    </div>
  );
}

export default GoalsPage;
