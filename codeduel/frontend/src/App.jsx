import { Component } from 'react';
import { Route, Routes } from 'react-router-dom';
import { BattleProvider } from './context/BattleContext';
import Lobby from './pages/Lobby';
import BattleArena from './pages/BattleArena';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import AuthPage from './pages/AuthPage';
import SignupSuccess from './pages/SignupSuccess';
import Dashboard from './pages/Dashboard';
import PolicyPage from './pages/PolicyPage';
import NotFound from './pages/NotFound';
import CookieBanner from './components/CookieBanner';

class ErrorBoundary extends Component {
  state = { error: null };
  static getDerivedStateFromError(error) { return { error }; }
  render() {
    if (this.state.error) return <main className="grid min-h-screen place-items-center p-8 text-center"><div><h1 className="text-2xl font-bold">Something went wrong</h1><p className="mt-2 text-slate-400">{this.state.error.message}</p><button className="mt-5 rounded-lg bg-accent px-4 py-2" onClick={() => window.location.reload()}>Reload</button></div></main>;
    return this.props.children;
  }
}

export default function App() {
  return <ErrorBoundary><AuthProvider><BattleProvider><Routes>
    <Route path="/" element={<Lobby />} />
    <Route path="/login" element={<AuthPage mode="login" />} />
    <Route path="/signup" element={<AuthPage mode="signup" />} />
    <Route path="/signup-success" element={<SignupSuccess />} />
    <Route path="/dashboard" element={<ProtectedRoute><Dashboard /></ProtectedRoute>} />
    <Route path="/battle/:battleId" element={<ProtectedRoute><BattleArena /></ProtectedRoute>} />
    <Route path="/privacy" element={<PolicyPage mode="privacy" />} />
    <Route path="/terms" element={<PolicyPage mode="terms" />} />
    <Route path="*" element={<NotFound />} />
  </Routes><CookieBanner /></BattleProvider></AuthProvider></ErrorBoundary>;
}
