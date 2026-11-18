import { Link } from 'react-router-dom';
import { Wifi, WifiOff } from 'lucide-react';
import { useBattle } from '../context/BattleContext';
import { useAuth } from '../context/AuthContext';

export default function Navbar() {
  const { state } = useBattle();
  const { user, logout } = useAuth();
  const color = state.connection === 'CONNECTED' ? 'bg-success' : state.connection === 'RECONNECTING' ? 'bg-warn' : 'bg-danger';
  return <header className="sticky top-0 z-40 border-b border-sage/20 bg-bg/95 px-4 py-4 backdrop-blur md:px-6">
    <div className="mx-auto flex max-w-7xl items-center justify-between">
      <Link to="/" className="text-xl font-black tracking-tight text-cream">Code<span className="text-accent">Duel</span></Link>
      <nav className="flex items-center gap-3 text-sm text-sage md:gap-5">
        {user && <><Link to="/" className="hidden hover:text-cream sm:inline">Lobby</Link><Link to="/dashboard" className="hover:text-cream">Dashboard</Link></>}
        <span className="flex items-center gap-2" aria-label={`Connection ${state.connection}`}>
          <span className={`h-2.5 w-2.5 rounded-full ${color}`} />
          {state.connection.toLowerCase()}
        </span>
        {user ? <><span className="hidden md:inline">{user.username}</span><span className="rounded-full border border-sage/30 bg-sage/10 px-3 py-1 text-cream">Elo {user.elo}</span><button onClick={logout} className="text-sage hover:text-cream">Logout</button></> : <><Link to="/login" className="hover:text-cream">Log in</Link><Link to="/signup" className="rounded-lg bg-burgundy px-3 py-2 font-bold text-cream">Sign up</Link></>}
        {state.connection === 'CONNECTED' ? <Wifi size={16} /> : <WifiOff size={16} />}
      </nav>
    </div>
  </header>;
}
