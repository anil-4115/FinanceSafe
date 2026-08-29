import { useEffect, useState } from 'react';
import { api } from '../services/api';

const blankProfile = { ageRange: '', employmentType: '', monthlyIncome: '', monthlyFixedExpenses: '', savings: '', existingInvestments: '', debt: '', riskTolerance: '', investmentExperience: '', preferredCategories: '' };

function ProfilePage() {
  const [profile, setProfile] = useState(blankProfile);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  useEffect(() => {
    api.get('/profile')
      .then(({ data }) => { if (data) setProfile({ ...blankProfile, ...data }); })
      .catch(() => setError('Could not load your profile.'));
  }, []);
  async function save(event) {
    event.preventDefault(); setError(''); setMessage(''); setIsSaving(true);
    const moneyFields = ['monthlyIncome', 'monthlyFixedExpenses', 'savings', 'existingInvestments', 'debt'];
    const payload = Object.fromEntries(Object.entries(profile).map(([key, value]) => [key, moneyFields.includes(key) ? Number(value || 0) : value]));
    try { const { data } = await api.put('/profile', payload); setProfile({ ...blankProfile, ...data }); setMessage('Financial profile saved.'); }
    catch (requestError) { setError(requestError.response?.data?.message || 'Could not save your profile.'); }
    finally { setIsSaving(false); }
  }
  return (
    <div className="page-shell">
      <h2>Profile</h2>
      <form className="panel data-form profile-form" onSubmit={save}>
        <p className="muted">This context makes later budget, goal, and fraud-risk recommendations more useful.</p>
        <label>Age range<select value={profile.ageRange} onChange={(event) => setProfile({ ...profile, ageRange: event.target.value })}><option value="">Select</option><option>18–24</option><option>25–34</option><option>35–44</option><option>45–54</option><option>55+</option></select></label>
        <label>Employment type<input value={profile.employmentType} onChange={(event) => setProfile({ ...profile, employmentType: event.target.value })} placeholder="Student, salaried, self-employed..." /></label>
        {[['monthlyIncome', 'Monthly income'], ['monthlyFixedExpenses', 'Monthly fixed expenses'], ['savings', 'Savings'], ['existingInvestments', 'Existing investments'], ['debt', 'Outstanding debt']].map(([field, label]) => <label key={field}>{label} (₹)<input type="number" min="0" step="0.01" value={profile[field]} onChange={(event) => setProfile({ ...profile, [field]: event.target.value })} /></label>)}
        <label>Risk tolerance<select value={profile.riskTolerance} onChange={(event) => setProfile({ ...profile, riskTolerance: event.target.value })}><option value="">Select</option><option>Conservative</option><option>Moderate</option><option>High</option></select></label>
        <label>Investment experience<select value={profile.investmentExperience} onChange={(event) => setProfile({ ...profile, investmentExperience: event.target.value })}><option value="">Select</option><option>Beginner</option><option>Intermediate</option><option>Experienced</option></select></label>
        <label className="wide-field">Preferred spending/investment categories<input value={profile.preferredCategories} onChange={(event) => setProfile({ ...profile, preferredCategories: event.target.value })} placeholder="e.g. food, travel, mutual funds" /></label>
        <button className="primary-btn" disabled={isSaving}>{isSaving ? 'Saving...' : 'Save profile'}</button>
      </form>
      {message && <p className="form-success">{message}</p>}{error && <p className="form-error" role="alert">{error}</p>}
    </div>
  );
}

export default ProfilePage;
