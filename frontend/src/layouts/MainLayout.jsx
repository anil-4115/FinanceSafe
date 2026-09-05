import { useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import {
  BadgeCheck,
  BellRing,
  Bot,
  CheckCircle2,
  ClipboardList,
  FileBarChart,
  FlaskConical,
  Gauge,
  GraduationCap,
  HeartPulse,
  History,
  LayoutGrid,
  LineChart,
  LogOut,
  Menu,
  Plus,
  ReceiptText,
  ScanSearch,
  Settings2,
  ShieldCheck,
  Sparkles,
  Tags,
  Target,
  TrendingUp,
  Wallet,
} from 'lucide-react';
import { clearSession, getUser } from '../services/auth';
import { api } from '../services/api';
import { useToast } from '../components/Toast';

const primaryNav = [
  { label: 'Overview', to: '/overview', icon: LayoutGrid },
  { label: 'Transactions', to: '/transactions', icon: ReceiptText },
  { label: 'Fraud Scanner', to: '/fraud-scanner', icon: ScanSearch },
  { label: 'Risk Analysis', to: '/risk-analysis', icon: Gauge },
  { label: 'Financial Health', to: '/financial-health', icon: HeartPulse },
  { label: 'Reports', to: '/reports', icon: FileBarChart },
  { label: 'Security Center', to: '/security', icon: ShieldCheck },
  { label: 'Settings', to: '/settings', icon: Settings2 },
];

const legacyGroups = [
  {
    label: 'Finance',
    items: [
      { label: 'Budget', to: '/budget', icon: Wallet },
      { label: 'Goals', to: '/goals', icon: Target },
      { label: 'Alerts', to: '/alerts', icon: BellRing },
    ],
  },
  {
    label: 'Invest',
    items: [
      { label: 'Products', to: '/products', icon: Tags },
      { label: 'Compare', to: '/compare', icon: CheckCircle2 },
      { label: 'Investments', to: '/investments', icon: TrendingUp },
      { label: 'Markets', to: '/markets', icon: LineChart },
      { label: 'Investment Simulator', to: '/simulator', icon: FlaskConical },
    ],
  },
  {
    label: 'Protect',
    items: [
      { label: 'Transaction Safety', to: '/transaction-safety', icon: BadgeCheck },
      { label: 'Decision Safety', to: '/decision-safety', icon: ShieldCheck },
      { label: 'Fraud History', to: '/fraud-history', icon: History },
      { label: 'Incident Reports', to: '/incidents', icon: ClipboardList },
    ],
  },
  {
    label: 'Learn & Assist',
    items: [
      { label: 'Financial Education', to: '/education', icon: GraduationCap },
      { label: 'AI Assistant', to: '/assistant', icon: Bot },
      { label: 'What-if Simulator', to: '/what-if', icon: Sparkles },
    ],
  },
];

function initialsOf(fullName) {
  return (fullName || 'U')
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0].toUpperCase())
    .join('');
}

function MainLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const toast = useToast();
  const user = getUser();
  const [fraudScore, setFraudScore] = useState(null);
  const [openAlerts, setOpenAlerts] = useState(0);
  const [drawerOpen, setDrawerOpen] = useState(false);

  useEffect(() => {
    let alive = true;
    api.get('/dashboard')
      .then(({ data }) => { if (alive) setFraudScore(data.fraudSafetyScore); })
      .catch(() => { if (alive) setFraudScore(null); });
    api.get('/alerts')
      .then(({ data }) => { if (alive) setOpenAlerts((data || []).filter((alert) => alert.status === 'OPEN').length); })
      .catch(() => { if (alive) setOpenAlerts(0); });
    return () => { alive = false; };
  }, []);

  useEffect(() => {
    const id = requestAnimationFrame(() => setDrawerOpen(false));
    return () => cancelAnimationFrame(id);
  }, [location.pathname]);

  useEffect(() => {
    if (drawerOpen) document.body.classList.add('sidebar-open');
    else document.body.classList.remove('sidebar-open');
    return () => document.body.classList.remove('sidebar-open');
  }, [drawerOpen]);

  function handleLogout() {
    clearSession();
    toast('Signed out safely. See you soon.', { type: 'info' });
    navigate('/login', { replace: true });
  }

  const today = new Date().toLocaleDateString('en-IN', { weekday: 'long', day: 'numeric', month: 'long' });

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand-block">
          <span className="brand-mark">F</span>
          <div>
            <p className="brand-tagline">Smart Fraud Assistant</p>
            <h2>FinanceSafe</h2>
          </div>
        </div>

        <nav className="nav" aria-label="Main navigation">
          <div className="nav-group">
            <span className="nav-group-label">Dashboard</span>
            {primaryNav.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to === '/overview'}
                className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
              >
                <span className="nav-icon"><item.icon size={17} strokeWidth={2.2} /></span>
                {item.label}
              </NavLink>
            ))}
          </div>

          {legacyGroups.map((group) => (
            <div className="nav-group" key={group.label}>
              <span className="nav-group-label">More · {group.label}</span>
              {group.items.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
                >
                  <span className="nav-icon"><item.icon size={16} strokeWidth={2.2} /></span>
                  {item.label}
                </NavLink>
              ))}
            </div>
          ))}
        </nav>

        <div className="sidebar-card">
          <p className="eyebrow" style={{ color: 'var(--text-3)' }}>Fraud safety</p>
          <strong>{fraudScore == null ? '—' : `${fraudScore}`}<span style={{ fontSize: 14, fontWeight: 600, marginLeft: 4 }}>/ 100</span></strong>
          <span>AI safety score · updated live</span>
        </div>
      </aside>

      <div className="drawer-overlay" onClick={() => setDrawerOpen(false)} />

      <main className="main-panel">
        <header className="topbar">
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <button type="button" className="hamburger" onClick={() => setDrawerOpen(true)} aria-label="Open menu">
              <Menu size={20} />
            </button>
            <div>
              <p className="greeting">Good to see you · {today}</p>
              <h1>Hello, {user?.fullName?.split(' ')[0] || 'there'} 👋</h1>
            </div>
          </div>

          <div className="topbar-actions">
            <button className="primary-btn" onClick={() => navigate('/fraud-scanner')}>
              <Plus size={16} strokeWidth={2.6} /> Scan scam
            </button>
            <button type="button" className={`icon-btn notification-dot`} onClick={() => navigate('/alerts')} aria-label="Alerts">
              <BellRing size={18} />
            </button>
            <button type="button" className="user-chip" onClick={() => navigate('/settings')} style={{ border: 'none', boxShadow: 'var(--shadow-md)' }}>
              <span className="avatar">{initialsOf(user?.fullName)}</span>
              <span className="user-meta">
                <strong>{user?.fullName || 'User'}</strong>
                <span>{openAlerts > 0 ? `${openAlerts} open alert${openAlerts > 1 ? 's' : ''}` : 'Account healthy'}</span>
              </span>
            </button>
            <button type="button" className="icon-btn" onClick={handleLogout} aria-label="Sign out">
              <LogOut size={18} />
            </button>
          </div>
        </header>

        <Outlet />
      </main>
    </div>
  );
}

export default MainLayout;