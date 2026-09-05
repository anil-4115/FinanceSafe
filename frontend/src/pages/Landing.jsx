import { Link } from 'react-router-dom';
import {
  ArrowRight,
  BellRing,
  GraduationCap,
  HeartPulse,
  ReceiptText,
  ShieldCheck,
  TrendingUp,
} from 'lucide-react';
import CountUp from '../components/CountUp';

const heroFeatures = [
  { icon: ShieldCheck, title: 'Scam scanner', text: 'Paste any suspicious SMS, email or link and get an explainable fraud risk score instantly.' },
  { icon: ReceiptText, title: 'Transaction safety', text: 'Every transaction is checked against your own history to flag unusual spending.' },
  { icon: HeartPulse, title: 'Financial health score', text: 'A single score built from your income, savings, goals and risk posture.' },
  { icon: BellRing, title: 'Fraud alerts', text: 'Report attempts and get an immediate risk verdict with clear next steps.' },
  { icon: TrendingUp, title: 'Smarter money decisions', text: 'Check affordability, simulate what-if scenarios, and compare products before you commit.' },
  { icon: GraduationCap, title: 'Learn as you go', text: 'Short, practical lessons and quizzes that raise your financial literacy score.' },
];

const steps = [
  { title: 'Create your account', text: 'Register in seconds. No OTPs, no documents required to get started.' },
  { title: 'Paste or import', text: 'Add transactions manually or import a CSV bank statement in one click.' },
  { title: 'Stay protected', text: 'Scan suspicious messages, watch your risk score, and act on alerts before damage happens.' },
];

function LandingPage() {
  return (
    <div className="landing">
      <nav className="landing-nav">
        <div className="landing-nav-inner">
          <Link to="/" className="brand-lockup">
            <span className="brand-mark">F</span>
            FinanceSafe
          </Link>
          <div className="landing-nav-links">
            <a href="#features">Features</a>
            <a href="#how-it-works">How it works</a>
            <a href="#protect">Protection</a>
            <Link to="/login" className="ghost-btn">Sign in</Link>
            <Link to="/register" className="primary-btn">Get started</Link>
          </div>
        </div>
      </nav>

      <header className="landing-hero">
        <div>
          <span className="hero-badge">
            <ShieldCheck size={14} /> SIH 2026 · Smart Financial Fraud Assistant
          </span>
          <h1>
            Your money, watched by{' '}
            <span className="grad">intelligent fraud defence</span>.
          </h1>
          <p className="lead">
            FinanceSafe scans suspicious messages, flags risky transactions, scores your
            financial health, and coaches safer money decisions — all in one clean dashboard.
          </p>
          <div className="hero-cta">
            <Link to="/register" className="primary-btn">
              Create free account <ArrowRight size={16} />
            </Link>
            <Link to="/login" className="ghost-btn">Sign in</Link>
          </div>
        </div>

        <div className="hero-mock">
          <div className="hero-noise" />
          <div className="hero-card fade-up">
            <div className="hero-card-row">
              <div>
                <p className="eyebrow">Fraud safety</p>
                <div className="hero-score-ring">
                  <CountUp value={82} />
                  <strong style={{ fontSize: 22 }}>/ 100</strong>
                </div>
              </div>
              <div className="hero-flag">
                <ShieldCheck size={16} />
                Suspicious message blocked
              </div>
            </div>
            <div className="hero-cards-grid">
              <div className="hero-mini-card">
                <span>Transactions</span>
                <strong>1,284</strong>
              </div>
              <div className="hero-mini-card">
                <span>Risk flags</span>
                <strong>2 today</strong>
              </div>
              <div className="hero-mini-card">
                <span>Health score</span>
                <strong>76 / 100</strong>
              </div>
            </div>
          </div>
        </div>
      </header>

      <section className="landing-section" id="features">
        <p className="landing-section-label">Everything in one place</p>
        <h2>Built to protect, designed to simplify</h2>
        <p className="section-lead">
          From scam detection to financial literacy, every tool shares one goal — safer
          money with clearer confidence.
        </p>
        <div className="feature-grid">
          {heroFeatures.map((feature) => (
            <article className="feature-card" key={feature.title}>
              <span className="feature-icon"><feature.icon size={20} strokeWidth={2.2} /></span>
              <h3>{feature.title}</h3>
              <p>{feature.text}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="landing-section" id="how-it-works">
        <p className="landing-section-label">Three steps</p>
        <h2>Protected in minutes</h2>
        <div className="steps-grid">
          {steps.map((step, index) => (
            <article className="step-card" key={step.title}>
              <span className="step-number">{index + 1}</span>
              <h3>{step.title}</h3>
              <p>{step.text}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="landing-section" id="protect">
        <div className="landing-review">
          <div>
            <p className="eyebrow" style={{ color: 'rgba(255,255,255,0.7)' }}>Stay one step ahead</p>
            <h2 style={{ fontSize: 'clamp(1.6rem, 3vw, 2.2rem)' }}>If it feels urgent, it might be a scam.</h2>
            <p>
              OTP requests, blocked-account threats, lottery winnings and KYC links are the
              most reported traps. Paste anything suspicious into the scanner before clicking.
              For immediate danger, call 1930 or report at cybercrime.gov.in.
            </p>
          </div>
          <div className="landing-review-links">
            <Link to="/register" className="primary-btn" style={{ background: '#fff', color: '#4338ca', boxShadow: 'none' }}>
              Start scanning free <ArrowRight size={16} />
            </Link>
            <Link to="/login" className="ghost-btn">Already have an account</Link>
          </div>
        </div>
      </section>

      <footer className="landing-footer">
        <div className="landing-footer-inner">
          <div>
            <Link to="/" className="brand-lockup" style={{ fontSize: 16 }}>
              <span className="brand-mark" style={{ width: 32, height: 32, fontSize: 15 }}>F</span>
              FinanceSafe
            </Link>
            <p>
              Educational fraud-safety and financial-health assistance. Not financial, legal
              or investment advice.
            </p>
          </div>
          <div style={{ maxWidth: 300 }}>
            <strong>Get help</strong>
            <p>For cyber fraud in India call 1930 or visit cybercrime.gov.in. Report incidents to your bank on its official number.</p>
          </div>
          <div>
            <strong>Quick links</strong>
            <p>
              <Link to="/login">Sign in</Link> · <Link to="/register">Register</Link>
            </p>
          </div>
        </div>
      </footer>
    </div>
  );
}

export default LandingPage;