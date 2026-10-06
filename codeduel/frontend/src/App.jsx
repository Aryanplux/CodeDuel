import { Component } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { BattleProvider } from './context/BattleContext';
import Lobby from './pages/Lobby';
import BattleArena from './pages/BattleArena';

class ErrorBoundary extends Component {
  state = { error: null };
  static getDerivedStateFromError(error) { return { error }; }
  render() {
    if (this.state.error) return <main className="grid min-h-screen place-items-center p-8 text-center"><div><h1 className="text-2xl font-bold">Something went wrong</h1><p className="mt-2 text-slate-400">{this.state.error.message}</p><button className="mt-5 rounded-lg bg-accent px-4 py-2" onClick={() => window.location.reload()}>Reload</button></div></main>;
    return this.props.children;
  }
}

export default function App() {
  return <ErrorBoundary><BattleProvider><Routes><Route path="/" element={<Lobby />} /><Route path="/battle/:battleId" element={<BattleArena />} /><Route path="*" element={<Navigate to="/" replace />} /></Routes></BattleProvider></ErrorBoundary>;
}
