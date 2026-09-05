import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { BellRing, Database, KeyRound, LogOut, Mail, ShieldCheck, User } from 'lucide-react';
import { api } from '../services/api';
import { clearSession, getUser } from '../services/auth';
import { useToast } from '../components/Toast';
import PageHeader from '../components/PageHeader';
import Skeleton from '../components/Skeleton';

const blankProfile = { ageRange: '', employmentType: '', monthlyIncome: '', monthlyFixedExpenses: '', savings: '', existingInvestments: '', debt: '', riskTolerance: '', investmentExperience: '', preferredCategories: '' };
const moneyFields = ['monthlyIncome', 'monthlyFixedExpenses', 'savings', 'existingInvestments', 'debt'];

function SettingsPage() {
  const notify = useToast();
  const user = getUser();
  const [profile, setProfile] = useState(blankProfile);
  const [loading, setLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    let alive = true;
    api.get('/profile')
      .then(({ data }) => { if (alive && data) setProfile({ ...blankProfile, ...data }); })
      .catch(() => { if (alive) setError('Could not load your financial profile.'); })
      .finally(() => { if (alive) setLoading(false); });
    return () => { alive = false; };
  }, []);

  async function save(event) {
    event.preventDefault();
    setError('');
    setIsSaving(true);
    const payload = Object.fromEntries(Object.entries(profile).map(([key, value]) => [key, moneyFields.includes(key) ? Number(value || 0) : value]));
    try {
      const { data } = await api.put('/profile', payload);
      setProfile({ ...blankProfile, ...data });
      notify('Financial profile saved.', 'success');
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Could not save your profile.');
    } finally {
      setIsSaving(false);
    }
  }

  function signOut() {
    clearSession();
    window.location.assign('/login');
  }

  if (loading) {
    return (
      <div className="page-shell">
        <Skeleton rows={5} cards={3} />
      </div>
    );
  }

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="Your account"
        title="Settings"
        intro="Your financial context, account details and the option to leave. Save right — the profile fuels every recommendation in FinanceSafe."
      />

      <section className="content-grid two-column" style={{ alignItems: 'start' }}>
        <form className="panel data-form profile-form" onSubmit={save}>
          <div className="panel-header"><h3>Financial profile</h3><span>Feeds budget, goals & risk scoring</span></div>
          {error && <p className="form-error" style={{ marginTop: 0 }}>{error}</p>}
          <label>Age range
            <select value={profile.ageRange} onChange={(event) => setProfile({ ...profile, ageRange: event.target.value })}>
              <option value="">Select</option><option>18–24</option><option>25–34</option><option>35–44</option><option>45–54</option><option>55+</option>
            </select>
          </label>
          <label>Employment type
            <input value={profile.employmentType} onChange={(event) => setProfile({ ...profile, employmentType: event.target.value })} placeholder="Student, salaried, self-employed…" />
          </label>
          {[['monthlyIncome', 'Monthly income'], ['monthlyFixedExpenses', 'Monthly fixed expenses'], ['savings', 'Savings'], ['existingInvestments', 'Existing investments'], ['debt', 'Outstanding debt']].map(([field, label]) => (
            <label key={field}>{label} (₹)
              <input type="number" min="0" step="0.01" value={profile[field]} onChange={(event) => setProfile({ ...profile, [field]: event.target.value })} />
            </label>
          ))}
          <label>Risk tolerance
            <select value={profile.riskTolerance} onChange={(event) => setProfile({ ...profile, riskTolerance: event.target.value })}>
              <option value="">Select</option><option>Conservative</option><option>Moderate</option><option>High</option>
            </select>
          </label>
          <label>Investment experience
            <select value={profile.investmentExperience} onChange={(event) => setProfile({ ...profile, investmentExperience: event.target.value })}>
              <option value="">Select</option><option>Beginner</option><option>Intermediate</option><option>Experienced</option>
            </select>
          </label>
          <label className="wide-field">Preferred spending/investment categories
            <input value={profile.preferredCategories} onChange={(event) => setProfile({ ...profile, preferredCategories: event.target.value })} placeholder="e.g. food, travel, mutual funds" />
          </label>
          <button className="primary-btn" disabled={isSaving}>{isSaving ? 'Saving…' : 'Save profile'}</button>
        </form>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div className="panel">
            <div className="panel-header"><h3>Account</h3><User size={18} style={{ color: 'var(--brand)' }} /></div>
            <div className="account-row">
              <span className="avatar"><User size={18} /></span>
              <div style={{ flex: 1 }}>
                <strong>{user?.fullName || 'FinanceSafe user'}</strong>
                <p className="muted" style={{ margin: '2px 0 0' }}>{user?.email || 'Signed in'}</p>
              </div>
            </div>
            <div className="risk-list" style={{ marginTop: 12 }}>
              <Link to="/security" className="ghost-btn" style={{ justifyContent: 'flex-start' }}><ShieldCheck size={16} /> Security center</Link>
              <Link to="/alerts" className="ghost-btn" style={{ justifyContent: 'flex-start' }}><BellRing size={16} /> Manage alerts</Link>
            </div>
            <button className="danger-outline-btn" onClick={signOut} style={{ marginTop: 12, width: '100%' }}>
              <LogOut size={15} /> Sign out of FinanceSafe
            </button>
            <p className="muted" style={{ fontSize: 12, marginTop: 10 }}>Signing out keeps your data. Log back in with the same email and password.</p>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {[
              { icon: KeyRound, title: 'Password', text: 'Changing your password needs a backend reset flow that is not connected yet.' },
              { icon: Mail, title: 'Email notifications', text: 'Notification preferences are not persisted by the backend yet.' },
              { icon: Database, title: 'Data & export', text: 'FinanceSafe offers printable reports instead of a data export endpoint.' },
            ].map((tool) => (
              <div className="locked-card" key={tool.title}>
                <span className="locked-icon"><tool.icon size={18} /></span>
                <span className="locked-body">
                  <strong>{tool.title}</strong>
                  <p>{tool.text}</p>
                </span>
                <span className="coming-soon">Soon</span>
              </div>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}

export default SettingsPage;