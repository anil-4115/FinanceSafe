import { useState } from 'react';
import { Link } from 'react-router-dom';
import { MailCheck, ShieldCheck } from 'lucide-react';

function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [submitted, setSubmitted] = useState(false);

  function handleSubmit(event) {
    event.preventDefault();
    setSubmitted(true);
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <span className="brand-mark" style={{ marginBottom: 16 }}><ShieldCheck size={20} /></span>
        <p className="eyebrow">Account recovery</p>
        <h1>Reset your password</h1>
        {!submitted ? (
          <>
            <p className="auth-sub">Enter the email address linked to your account and we will guide you through recovery.</p>
            <form className="auth-form" onSubmit={handleSubmit}>
              <label>
                Email
                <input type="email" placeholder="you@example.com" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} required />
              </label>
              <button type="submit" className="primary-btn full-width">Send reset link</button>
            </form>
          </>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            <div className="form-success">
              <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <MailCheck size={18} /> Request received
              </span>
              <p style={{ margin: '8px 0 0', lineHeight: 1.6 }}>
                Password reset is not connected to the backend yet. In this build, contact your
                account administrator or support team to recover access to <strong>{email || 'your account'}</strong>.
              </p>
            </div>
            <p className="muted">
              For urgent fraud-related account lockouts, call your bank through its official
              number or dial 1930 (National Cyber Crime Helpline).
            </p>
          </div>
        )}

        <p className="auth-link"><Link to="/login">← Back to login</Link></p>
        <p className="auth-link">New here? <Link to="/register">Create account</Link></p>
      </div>
    </div>
  );
}

export default ForgotPasswordPage;