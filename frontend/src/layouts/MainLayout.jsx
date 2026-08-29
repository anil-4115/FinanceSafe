import { useEffect, useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { clearSession, getUser } from '../services/auth';
import { api } from '../services/api';

const navGroups = [
  {
    label: 'Finance',
    items: [
      { label: 'Dashboard', to: '/dashboard' },
      { label: 'Spending', to: '/spending' },
      { label: 'Budget', to: '/budget' },
      { label: 'Goals', to: '/goals' },
      { label: 'Financial Health', to: '/financial-health' },
    ],
  },
  {
    label: 'Invest',
    items: [
      { label: 'Products', to: '/products' },
      { label: 'Compare', to: '/compare' },
      { label: 'Investments', to: '/investments' },
      { label: 'Markets', to: '/markets' },
      { label: 'Simulator', to: '/simulator' },
    ],
  },
  {
    label: 'Protect',
    items: [
      { label: 'Scam Scanner', to: '/scam-scanner' },
      { label: 'Transaction Safety', to: '/transaction-safety' },
      { label: 'Decision Safety', to: '/decision-safety' },
      { label: 'Fraud History', to: '/fraud-history' },
      { label: 'Incident Reports', to: '/incidents' },
    ],
  },
  {
    label: 'Learn & Assist',
    items: [
      { label: 'Financial Education', to: '/education' },
      { label: 'AI Assistant', to: '/assistant' },
      { label: 'What-if Simulator', to: '/what-if' },
      { label: 'Alerts', to: '/alerts' },
      { label: 'Profile', to: '/profile' },
    ],
  },
];

function MainLayout() {
  const navigate = useNavigate();
  const user = getUser();
  const [fraudScore, setFraudScore] = useState(null);

  useEffect(() => {
    api.get('/dashboard')
      .then(({ data }) => setFraudScore(data.fraudSafetyScore))
      .catch(() => setFraudScore(null));
  }, []);

  function handleLogout() {
    clearSession();
    navigate('/login', { replace: true });
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand-block">
          <div className="brand-mark">F</div>
          <div>
            <p className="eyebrow">Smart India Hackathon</p>
            <h2>FinanceSafe</h2>
          </div>
        </div>

        <nav className="nav">
          {navGroups.map((group) => (
            <div className="nav-group" key={group.label}>
              <span className="nav-group-label">{group.label}</span>
              {group.items.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.to === '/dashboard'}
                  className={({ isActive }) =>
                    `nav-item ${isActive ? 'active' : ''}`
                  }
                >
                  {item.label}
                </NavLink>
              ))}
            </div>
          ))}
        </nav>

        <div className="sidebar-card">
          <p className="eyebrow">Fraud safety</p>
          <strong>{fraudScore == null ? '—' : `${fraudScore} / 100`}</strong>
          <span>AI safety score</span>
        </div>
      </aside>

      <main className="main-panel">
        <header className="topbar">
          <div>
            <p className="eyebrow">Welcome back</p>
            <h1>Hello, {user?.fullName || 'there'}</h1>
          </div>

          <div className="topbar-actions">
            <button type="button" className="ghost-btn" onClick={handleLogout}>Sign out</button>
            <button className="primary-btn" onClick={() => navigate('/scam-scanner')}>+ Scan scam</button>
          </div>
        </header>

        <Outlet />
      </main>
    </div>
  );
}

export default MainLayout;