import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, Swords, X } from 'lucide-react';
import Navbar from '../components/Navbar';
import { useAuth } from '../context/AuthContext';
import { useBattle } from '../context/BattleContext';
import { cancelQueue, getApiErrorMessage, joinQueue } from '../services/api';
import { websocket } from '../services/websocket';
import Seo from '../components/Seo';
import Footer from '../components/Footer';

export default function Lobby() {
  const { state, dispatch } = useBattle();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [error, setError] = useState('');
  const [elapsed, setElapsed] = useState(0);
  const [countdown, setCountdown] = useState(3);
  const estimate = useMemo(() => {
    const rating = user?.elo || state.user.rating;
    const base = rating >= 2200 ? 40 : rating >= 1800 ? 55 : rating >= 1500 ? 75 : rating >= 1250 ? 90 : 100;
    return Math.round(base * (Math.floor(elapsed / 15) + 1));
  }, [elapsed, user, state.user.rating]);

  useEffect(() => {
    if (!user) return undefined;
    return websocket.subscribe(`/topic/match/${user.id}`, (event) => {
      if (event.type === 'MATCH_FOUND') dispatch({ type: 'MATCH_FOUND', match: event });
    });
  }, [user, dispatch]);

  useEffect(() => {
    if (state.queue.status !== 'SEARCHING') return undefined;
    const timer = window.setInterval(() => setElapsed((Math.floor((Date.now() - state.queue.startedAt) / 1000))), 1000);
    return () => window.clearInterval(timer);
  }, [state.queue.status, state.queue.startedAt]);

  useEffect(() => {
    if (state.queue.status !== 'MATCH_FOUND') return undefined;
    setCountdown(3);
    const timer = window.setInterval(() => setCountdown((value) => {
      if (value <= 1) {
        window.clearInterval(timer);
        dispatch({ type: 'IN_GAME' });
        navigate(`/battle/${state.queue.match.matchId}`);
        return 0;
      }
      return value - 1;
    }), 1000);
    return () => window.clearInterval(timer);
  }, [state.queue.status, state.queue.match, dispatch, navigate]);

  const searching = state.queue.status === 'SEARCHING';
  const matchFound = state.queue.status === 'MATCH_FOUND';

  const start = async () => {
    if (!user) {
      navigate('/login');
      return;
    }
    setError('');
    try {
      await joinQueue(user.id, user.elo, user.username);
      setElapsed(0);
      dispatch({ type: 'QUEUE_SEARCHING' });
    } catch (requestError) {
      setError(getApiErrorMessage(requestError));
    }
  };

  const cancel = async () => {
    if (user) await cancelQueue(user.id).catch(() => setError('Queue cancellation failed.'));
    dispatch({ type: 'QUEUE_IDLE' });
  };

  return <><Seo title="Lobby" description="Find a ranked 1v1 coding opponent on CodeDuel and compete in real-time coding duels." /><Navbar /><main className="min-h-[calc(100vh-73px)] px-4 pb-24 pt-8 md:px-6 md:pb-10">
    <div className="mx-auto grid max-w-6xl gap-6 lg:grid-cols-[1.5fr_1fr]">
      <section className="panel relative overflow-hidden p-7 md:p-10">
        <div className="absolute right-0 top-0 h-32 w-32 rounded-full bg-burgundy/20 blur-3xl" />
        <p className="text-sm font-bold uppercase tracking-[0.2em] text-sage">Ranked arena</p>
        <h1 className="mt-4 max-w-xl text-4xl font-black leading-tight text-cream md:text-6xl">Outthink. Outcode. Outduel.</h1>
        <p className="mt-5 max-w-lg text-base leading-7 text-sage">Enter a synchronized competitive coding duel and climb the Elo ladder.</p>
        <div className="mt-8 flex flex-wrap items-center gap-4">
          <button onClick={start} disabled={searching || matchFound} className="flex items-center gap-2 rounded-lg bg-burgundy px-6 py-3.5 font-bold text-cream shadow-lg shadow-burgundy/20 transition hover:-translate-y-0.5 disabled:opacity-50"><Swords size={18} /> {searching ? 'Searching…' : 'Find Match'}</button>
          {user && <span className="rounded-full border border-sage/30 px-4 py-2 text-sm text-cream">Elo <strong>{user.elo}</strong> · {user.rank}</span>}
        </div>
        {error && <p className="mt-5 rounded-lg border border-danger/40 bg-danger/10 px-4 py-3 text-danger" role="alert">{error}</p>}
      </section>
      <section className="panel p-7"><h2 className="text-lg font-bold text-cream">Your rank</h2>{user ? <><div className="mt-5 text-5xl font-black text-accent">{user.elo}</div><p className="mt-2 text-sage">{user.rank} · {user.wins} wins</p><div className="mt-6 grid grid-cols-3 gap-3 text-center"><div className="rounded-lg bg-sage/10 p-3"><b>{user.matchesPlayed}</b><p className="text-xs text-sage">Matches</p></div><div className="rounded-lg bg-sage/10 p-3"><b>{user.wins}</b><p className="text-xs text-sage">Wins</p></div><div className="rounded-lg bg-sage/10 p-3"><b>{user.losses}</b><p className="text-xs text-sage">Losses</p></div></div></> : <p className="mt-5 text-sage">Sign in to track Elo and match history.</p>}</section>
    </div>
  </main>
  <Footer />
  <div className="fixed inset-x-0 bottom-0 z-30 border-t border-sage/20 bg-bg/95 p-3 backdrop-blur md:hidden"><button onClick={start} disabled={searching || matchFound} className="flex w-full items-center justify-center gap-2 rounded-lg bg-burgundy py-3 font-bold text-cream disabled:opacity-50"><Search size={18} /> Start Dueling</button></div>
  {(searching || matchFound) && <div className="fixed inset-0 z-50 grid place-items-center bg-black/80 p-4"><div className="panel w-full max-w-md p-8 text-center"><button onClick={cancel} className="float-right text-sage" aria-label="Cancel"><X /></button>{matchFound ? <><h2 className="text-2xl font-black text-cream">Match Found</h2><p className="mt-3 text-sage">Starting in <b className="text-cream">{countdown}</b></p><div className="mt-6 text-6xl font-black text-accent">{countdown}</div></> : <><div className="mx-auto h-14 w-14 animate-pulse rounded-full border-4 border-burgundy/30 p-3"><Search /></div><h2 className="mt-5 text-xl font-bold text-cream">Finding match…</h2><p className="mt-3 font-mono text-2xl text-cream">{String(Math.floor(elapsed/60)).padStart(2,'0')}:{String(elapsed%60).padStart(2,'0')}</p><p className="mt-2 text-sm text-sage">Searching within ±{estimate} Elo</p><button onClick={cancel} className="mt-6 rounded-lg border border-sage/30 px-4 py-2 text-cream">Cancel Queue</button></>}</div></div>}</>;
}
