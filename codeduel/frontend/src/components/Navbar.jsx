import { Link } from 'react-router-dom';
import { Wifi, WifiOff } from 'lucide-react';
import { useBattle } from '../context/BattleContext';

export default function Navbar() {
  const { state } = useBattle();
  const color = state.connection === 'CONNECTED' ? 'bg-success' : state.connection === 'RECONNECTING' ? 'bg-warn' : 'bg-danger';
  return <header className="border-b border-slate-800 bg-bg/90 px-6 py-4">
    <div className="mx-auto flex max-w-7xl items-center justify-between">
      <Link to="/" className="text-xl font-black tracking-tight text-sky-400">CodeDuel</Link>
      <nav className="flex items-center gap-5 text-sm text-slate-300">
        <Link to="/" className="hover:text-white">Lobby</Link>
        <span className="flex items-center gap-2" aria-label={`Connection ${state.connection}`}>
          <span className={`h-2.5 w-2.5 rounded-full ${color}`} />
          {state.connection.toLowerCase()}
        </span>
        <span>{state.user.username}</span>
        <span className="rounded-full bg-sky-900 px-3 py-1 text-sky-200">Elo {state.user.rating}</span>
        {state.connection === 'CONNECTED' ? <Wifi size={16} /> : <WifiOff size={16} />}
      </nav>
    </div>
  </header>;
}
