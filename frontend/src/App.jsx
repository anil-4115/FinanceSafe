import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import RequireAuth from './components/RequireAuth';
import MainLayout from './layouts/MainLayout';
import { ToastProvider } from './components/Toast';
import LandingPage from './pages/Landing';
import LoginPage from './pages/Login';
import RegisterPage from './pages/Register';
import ForgotPasswordPage from './pages/ForgotPassword';
import OverviewPage from './pages/Overview';
import TransactionsPage from './pages/Transactions';
import TransactionDetailsPage from './pages/TransactionDetails';
import FraudScannerPage from './pages/FraudScanner';
import RiskAnalysisPage from './pages/RiskAnalysis';
import FinancialHealthPage from './pages/FinancialHealth';
import ReportsPage from './pages/Reports';
import SecurityCenterPage from './pages/SecurityCenter';
import SettingsPage from './pages/Settings';
import AlertsPage from './pages/Alerts';
import AssistantPage from './pages/Assistant';
import BudgetPage from './pages/Budget';
import ComparePage from './pages/Compare';
import DecisionSafetyPage from './pages/DecisionSafety';
import EducationPage from './pages/Education';
import FraudHistoryPage from './pages/FraudHistory';
import GoalsPage from './pages/Goals';
import IncidentReportsPage from './pages/IncidentReports';
import InvestmentSimulatorPage from './pages/InvestmentSimulator';
import InvestmentsPage from './pages/Investments';
import MarketsPage from './pages/Markets';
import ProductsPage from './pages/Products';
import ProductDetailsPage from './pages/ProductDetails';
import TransactionSafetyPage from './pages/TransactionSafety';
import WhatIfSimulatorPage from './pages/WhatIfSimulator';
import './App.css';

function App() {
  return (
    <BrowserRouter>
      <ToastProvider>
        <Routes>
          <Route path="/" element={<LandingPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />

          <Route element={<RequireAuth />}>
            <Route element={<MainLayout />}>
              <Route path="/overview" element={<OverviewPage />} />
              <Route path="/dashboard" element={<Navigate to="/overview" replace />} />
              <Route path="/transactions" element={<TransactionsPage />} />
              <Route path="/transactions/:id" element={<TransactionDetailsPage />} />
              <Route path="/spending" element={<Navigate to="/transactions" replace />} />
              <Route path="/fraud-scanner" element={<FraudScannerPage />} />
              <Route path="/scam-scanner" element={<Navigate to="/fraud-scanner" replace />} />
              <Route path="/risk-analysis" element={<RiskAnalysisPage />} />
              <Route path="/financial-health" element={<FinancialHealthPage />} />
              <Route path="/reports" element={<ReportsPage />} />
              <Route path="/security" element={<SecurityCenterPage />} />
              <Route path="/settings" element={<SettingsPage />} />
              <Route path="/profile" element={<Navigate to="/settings" replace />} />

              <Route path="/budget" element={<BudgetPage />} />
              <Route path="/goals" element={<GoalsPage />} />
              <Route path="/alerts" element={<AlertsPage />} />
              <Route path="/products" element={<ProductsPage />} />
              <Route path="/products/:id" element={<ProductDetailsPage />} />
              <Route path="/compare" element={<ComparePage />} />
              <Route path="/investments" element={<InvestmentsPage />} />
              <Route path="/markets" element={<MarketsPage />} />
              <Route path="/simulator" element={<InvestmentSimulatorPage />} />
              <Route path="/transaction-safety" element={<TransactionSafetyPage />} />
              <Route path="/decision-safety" element={<DecisionSafetyPage />} />
              <Route path="/fraud-history" element={<FraudHistoryPage />} />
              <Route path="/what-if" element={<WhatIfSimulatorPage />} />
              <Route path="/education" element={<EducationPage />} />
              <Route path="/incidents" element={<IncidentReportsPage />} />
              <Route path="/assistant" element={<AssistantPage />} />
            </Route>
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </ToastProvider>
    </BrowserRouter>
  );
}

export default App;