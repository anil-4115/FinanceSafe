import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { ShieldCheck } from 'lucide-react';
import { api } from '../services/api';
import { saveSession } from '../services/auth';

function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setIsSubmitting(true);
    try {
      const { data } = await api.post('/auth/login', form);
      saveSession(data);
      navigate(location.state?.from?.pathname || '/overview', { replace: true });
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Unable to sign in. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <span className="brand-mark" style={{ marginBottom: 16 }}><ShieldCheck size={20} /></span>
        <p className="eyebrow">Secure access</p>
        <h1>Welcome back</h1>
        <p className="auth-sub">Sign in to your FinanceSafe dashboard.</p>
        <form className="auth-form" onSubmit={handleSubmit}>
          <label>
            Email
            <input type="email" placeholder="you@example.com" autoComplete="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} required />
          </label>
          <label>
            Password
            <input type="password" placeholder="Enter your password" autoComplete="current-password" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} required />
          </label>
          <div style={{ textAlign: 'right', marginTop: -6 }}>
            <Link to="/forgot-password" style={{ fontSize: 13, fontWeight: 600, textDecoration: 'none' }}>Forgot password?</Link>
          </div>
          {error && <p className="form-error" role="alert">{error}</p>}
          <button type="submit" className="primary-btn full-width" disabled={isSubmitting}>{isSubmitting ? 'Signing in…' : 'Sign in'}</button>
        </form>

        <p className="auth-link">New here? <Link to="/register">Create account</Link></p>
      </div>
    </div>
  );
}

export default LoginPage;