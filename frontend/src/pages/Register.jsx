import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ShieldCheck } from 'lucide-react';
import { api } from '../services/api';
import { saveSession } from '../services/auth';

function RegisterPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ fullName: '', email: '', password: '' });
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setIsSubmitting(true);
    try {
      const { data } = await api.post('/auth/register', form);
      saveSession(data);
      navigate('/overview', { replace: true });
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Unable to create your account. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <span className="brand-mark" style={{ marginBottom: 16 }}><ShieldCheck size={20} /></span>
        <p className="eyebrow">Create account</p>
        <h1>Start protecting your money</h1>
        <p className="auth-sub">Register once — scan scams and track your financial health forever.</p>
        <form className="auth-form" onSubmit={handleSubmit}>
          <label>
            Full name
            <input type="text" placeholder="Riya Sharma" autoComplete="name" value={form.fullName} onChange={(event) => setForm({ ...form, fullName: event.target.value })} required />
          </label>
          <label>
            Email
            <input type="email" placeholder="you@example.com" autoComplete="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} required />
          </label>
          <label>
            Password
            <input type="password" placeholder="Create a strong password (min. 8 characters)" autoComplete="new-password" minLength="8" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} required />
          </label>
          {error && <p className="form-error" role="alert">{error}</p>}
          <button type="submit" className="primary-btn full-width" disabled={isSubmitting}>{isSubmitting ? 'Creating account…' : 'Create account'}</button>
        </form>

        <p className="auth-link">
          Already registered? <Link to="/login">Login</Link>
        </p>
      </div>
    </div>
  );
}

export default RegisterPage;