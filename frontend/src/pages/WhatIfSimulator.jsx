import { useState } from 'react';
import { api } from '../services/api';

const inr = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 0,
});

const scenarios = [
  { value: 'INCREASE_SAVINGS', label: 'Increase my monthly savings', hint: 'How much extra savings per month?' },
  { value: 'MONTHLY_INVESTMENT', label: 'Invest every month', hint: 'How much per month?' },
  { value: 'DECREASE_SPENDING', label: 'Cut my monthly spending', hint: 'How much less per month?' },
  { value: 'EXPENSE_INCREASE', label: 'My expenses go up by %', hint: 'Enter an amount, or 0 to use the % field' },
  { value: 'ONE_TIME_PURCHASE', label: 'Buy something one-time', hint: 'Purchase price (₹)' },
  { value: 'LOAN', label: 'Take a loan', hint: 'Loan amount (₹)' },
];

const initialForm = { scenario: 'INCREASE_SAVINGS', amount: '', expensePctChange: '' };

function WhatIfSimulatorPage() {
  const [form, setForm] = useState(initialForm);
  const [result, setResult] = useState(null);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function simulate(event) {
    event.preventDefault();
    setError('');
    setMessage('');
    const requiresAmount = form.scenario !== 'EXPENSE_INCREASE';
    if (requiresAmount && (!form.amount || Number(form.amount) <= 0)) {
      setError('Enter an amount for this scenario to see its effect.');
      return;
    }
    setResult(null);
    setLoading(true);
    try {
      const { data } = await api.post('/simulator/what-if', {
        scenario: form.scenario,
        amount: Number(form.amount || 0),
        expensePctChange: form.expensePctChange ? Number(form.expensePctChange) : null,
      });
      setResult(data);
      setMessage('Scenario applied to your current numbers.');
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Could not run this scenario.');
    } finally {
      setLoading(false);
    }
  }

  const active = scenarios.find((scenario) => scenario.value === form.scenario);

  return (
    <div className="page-shell">
      <h2>What-if Simulator</h2>
      <section className="data-grid">
        <form className="panel data-form scanner-form" onSubmit={simulate}>
          <div className="panel-header"><h3>Try a change</h3><span>See the effect before you act</span></div>
          <label className="wide-field">Scenario
            <select value={form.scenario} onChange={(event) => setForm({ ...form, scenario: event.target.value })}>
              {scenarios.map((scenario) => <option key={scenario.value} value={scenario.value}>{scenario.label}</option>)}
            </select>
          </label>
          <p className="muted wide-field">{active?.hint}</p>
          {form.scenario !== 'EXPENSE_INCREASE' || !form.expensePctChange ? (
            <label>Amount (₹)<input type="number" min="0" step="any" value={form.amount} onChange={(event) => setForm({ ...form, amount: event.target.value })} required={form.scenario !== 'EXPENSE_INCREASE'} />
              <small className="muted">Required for savings, investing, purchases, loans and spending cuts</small>
            </label>
          ) : null}
          {form.scenario === 'EXPENSE_INCREASE' && (
            <label>Expense increase (%)<input type="number" min="0" max="100" step="any" value={form.expensePctChange} onChange={(event) => setForm({ ...form, expensePctChange: event.target.value })} />
              <small className="muted">Used when amount is left at 0</small>
            </label>
          )}
          <button className="primary-btn" disabled={loading}>{loading ? 'Simulating…' : 'Run what-if'}</button>
        </form>
        <aside className="panel safety-guide">
          <h3>Example questions</h3>
          <ul className="check-list">
            <li>What if I save ₹5,000 more each month?</li>
            <li>What if my expenses rise by 10%?</li>
            <li>What if I buy a ₹30,000 laptop?</li>
            <li>What if I invest ₹5,000 monthly?</li>
            <li>What if I take a ₹2,00,000 loan?</li>
          </ul>
        </aside>
      </section>

      {message && <p className="form-success">{message}</p>}
      {error && <p className="form-error" role="alert">{error}</p>}

      {result && (
        <section className="panel result-panel">
          <div className="compare-grid before-after">
            <div className="compare-col">
              <p className="eyebrow">Current</p>
              <h3>Financial health</h3>
              <strong className="big-number">{result.healthBefore}</strong>
              <p className="muted">Savings {inr.format(Number(result.savingsBefore))}</p>
              <p className="muted">Goal progress {result.goalProgressBefore}%</p>
            </div>
            <div className="arrow-column">→</div>
            <div className="compare-col highlight">
              <p className="eyebrow">After change</p>
              <h3>Financial health</h3>
              <strong className="big-number">{result.healthAfter}</strong>
              <p className="muted">Savings {inr.format(Number(result.savingsAfter))}</p>
              <p className="muted">Goal progress {result.goalProgressAfter}%</p>
            </div>
          </div>
          <div className="content-grid two-column result-columns">
            <div>
              <h4>Why it moves</h4>
              <ul className="check-list">{result.explanations.map((item, index) => <li key={index}>{item}</li>)}</ul>
            </div>
            <div>
              <h4>Recommendations</h4>
              <ul className="check-list">{result.recommendations.length === 0 ? <li className="muted">No extra guidance for this scenario.</li> : result.recommendations.map((item, index) => <li key={index}>{item}</li>)}</ul>
            </div>
          </div>
        </section>
      )}
    </div>
  );
}

export default WhatIfSimulatorPage;