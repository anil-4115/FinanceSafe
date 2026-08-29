import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import RequireAuth from './components/RequireAuth';
import MainLayout from './layouts/MainLayout';
import AlertsPage from './pages/Alerts';
import AssistantPage from './pages/Assistant';
import BudgetPage from './pages/Budget';
import ComparePage from './pages/Compare';
import DashboardPage from './pages/Dashboard';
import DecisionSafetyPage from './pages/DecisionSafety';
import EducationPage from './pages/Education';
import FinancialHealthPage from './pages/FinancialHealth';
import FraudHistoryPage from './pages/FraudHistory';
import GoalsPage from './pages/Goals';
import IncidentReportsPage from './pages/IncidentReports';
import InvestmentSimulatorPage from './pages/InvestmentSimulator';
import InvestmentsPage from './pages/Investments';
import LoginPage from './pages/Login';
import MarketsPage from './pages/Markets';
import ProductsPage from './pages/Products';
import ProfilePage from './pages/Profile';
import RegisterPage from './pages/Register';
import ScamScannerPage from './pages/ScamScanner';
import SpendingPage from './pages/Spending';
import TransactionSafetyPage from './pages/TransactionSafety';
import WhatIfSimulatorPage from './pages/WhatIfSimulator';
import './App.css';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route element={<RequireAuth />}>
          <Route element={<MainLayout />}>
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/spending" element={<SpendingPage />} />
            <Route path="/budget" element={<BudgetPage />} />
            <Route path="/goals" element={<GoalsPage />} />
            <Route path="/financial-health" element={<FinancialHealthPage />} />
            <Route path="/profile" element={<ProfilePage />} />
            <Route path="/alerts" element={<AlertsPage />} />
            <Route path="/products" element={<ProductsPage />} />
            <Route path="/compare" element={<ComparePage />} />
            <Route path="/investments" element={<InvestmentsPage />} />
            <Route path="/markets" element={<MarketsPage />} />
            <Route path="/simulator" element={<InvestmentSimulatorPage />} />
            <Route path="/scam-scanner" element={<ScamScannerPage />} />
            <Route path="/fraud-history" element={<FraudHistoryPage />} />
            <Route path="/transaction-safety" element={<TransactionSafetyPage />} />
            <Route path="/decision-safety" element={<DecisionSafetyPage />} />
            <Route path="/what-if" element={<WhatIfSimulatorPage />} />
            <Route path="/education" element={<EducationPage />} />
            <Route path="/incidents" element={<IncidentReportsPage />} />
            <Route path="/assistant" element={<AssistantPage />} />
          </Route>
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;