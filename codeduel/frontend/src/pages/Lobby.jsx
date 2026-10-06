import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, X } from 'lucide-react';
import Navbar from '../components/Navbar';
import { useBattle } from '../context/BattleContext';
import { cancelQueue, getApiErrorMessage, joinQueue } from '../services/api';
import { websocket } from '../services/websocket';

export default function Lobby() {
  const { state, dispatch } = useBattle();
  const navigate = useNavigate();
  const [error, setError] = useState('');
  const [elapsed, setElapsed] = useState(0);
  const [countdown, setCountdown] = useState(3);
  const estimate = useMemo(() => 100 * (Math.floor(elapsed / 10) + 1), [elapsed]);

  useEffect(() => {
    const unsubscribe = websocket.subscribe(`/topic/match/${state.user.userId}`, (event) => {
      if (event.type !== 'MATCH_FOUND') return;
      dispatch({ type: 'MATCH_FOUND', match: event });
    });
    return unsubscribe;
  }, [state.user.userId, dispatch]);

  useEffect(() => {
    if (state.queue.status !== 'SEARCHING') return undefined;
    const timer = window.setInterval(() => setElapsed(Math.floor((Date.now() - state.queue.startedAt) / 1000)), 1000);
    return () => window.clearInterval(timer);
  }, [state.queue.status, state.queue.startedAt]);

  useEffect(() => {
    if (state.queue.status !== 'MATCH_FOUND') return undefined;
    setCountdown(3);
    const timer = window.setInterval(() => {
      setCountdown((value) => {
        if (value <= 1) {
          window.clearInterval(timer);
          dispatch({ type: 'IN_GAME' });
          navigate(`/battle/${state.queue.match.matchId}`);
          return 0;
        }
        return value - 1;
      });
    }, 1000);
    return () => window.clearInterval(timer);
  }, [state.queue.status, state.queue.match, dispatch, navigate]);

  useEffect(() => {
    if (state.queue.status !== 'SEARCHING') return undefined;
    return () => {
      cancelQueue(state.user.userId).catch((requestError) => {
        console.warn('Unable to clean up matchmaking queue membership', requestError.message);
      });
    };
  }, [state.queue.status, state.user.userId]);

  const start = async () => {
    setError('');
    try {
      const payload = { userId: state.user.userId, rating: state.user.rating, username: state.user.username };
      console.info('Joining matchmaking queue', payload);
      const response = await joinQueue(payload.userId, payload.rating, payload.username);
      console.info('Matchmaking queue response', response.status, response.data);
      setElapsed(0);
      dispatch({ type: 'QUEUE_SEARCHING' });
    } catch (requestError) {
      console.error('Matchmaking queue join failed', {
        status: requestError.response?.status,
        response: requestError.response?.data,
        code: requestError.code,
        cause: requestError.message,
      });
      setError(getApiErrorMessage(requestError));
    }
  };
  const cancel = async () => {
    try {
      await cancelQueue(state.user.userId);
    } catch (requestError) {
      console.error('Matchmaking queue cancellation failed', {
        status: requestError.response?.status,
        response: requestError.response?.data,
        code: requestError.code,
        cause: requestError.message,
      });
      setError(`Queue cancellation failed: ${getApiErrorMessage(requestError)}`);
    }
    dispatch({ type: 'QUEUE_IDLE' });
  };

  const searching = state.queue.status === 'SEARCHING';
  const matchFound = state.queue.status === 'MATCH_FOUND';
  return <><Navbar /><main className="flex min-h-[calc(100vh-73px)] items-center px-6 py-10"><div className="mx-auto w-full max-w-6xl"><div className="grid gap-6 lg:grid-cols-[1.5fr_1fr]"><section className="panel flex min-h-64 flex-col justify-center p-8"><p className="text-sm text-slate-400">Ready for your next duel?</p><h1 className="mt-2 text-4xl font-black">Competitive coding, head to head.</h1><div className="mt-8 flex flex-wrap items-center gap-4"><span className="rounded-full bg-sky-900 px-4 py-2 text-sky-200">Elo {state.user.rating}</span><button onClick={start} disabled={searching || matchFound} className="flex items-center gap-2 rounded-lg bg-accent px-5 py-3 font-semibold disabled:opacity-50"><Search size={18} /> Find Match</button></div>{error && <p className="mt-4 text-danger" role="alert">{error}</p>}</section><section className="panel flex min-h-64 flex-col justify-center p-8"><h2 className="text-lg font-semibold">Your stats</h2><div className="mt-5 grid grid-cols-3 gap-3 text-center"><div><b>12</b><p className="text-xs text-slate-400">Wins</p></div><div><b>8</b><p className="text-xs text-slate-400">Losses</p></div><div><b>60%</b><p className="text-xs text-slate-400">Win rate</p></div></div></section></div><section className="panel mt-6 p-6"><div className="flex flex-wrap items-center justify-between gap-3"><div><h2 className="text-lg font-semibold">Matchmaking status</h2><p className="mt-1 text-sm text-slate-400">{searching ? 'Finding match…' : matchFound ? 'Starting match…' : 'Ready when you are. Start a match to begin coding.'}</p></div><span className="rounded-full bg-slate-800 px-3 py-1 text-sm text-slate-300">{state.queue.status.toLowerCase()}</span></div></section></div></main>{(searching || matchFound) && <div className="fixed inset-0 z-10 grid place-items-center bg-black/70 p-4"><div className="panel w-full max-w-md p-8 text-center"><button onClick={cancel} className="float-right text-slate-400" aria-label="Cancel"><X /></button>{matchFound ? <><div className="text-2xl font-bold text-success">Match Found!</div><p className="mt-2 text-slate-400">Starting match…</p><div className="mt-6 flex items-center justify-center gap-4"><div><b>{state.user.username}</b><p className="text-sm text-slate-400">Elo {state.user.rating}</p></div><span className="text-slate-500">vs</span><div><b>{state.queue.match.opponentName}</b><p className="text-sm text-slate-400">Elo {state.queue.match.opponentElo}</p></div></div><div className="mt-6 text-6xl font-black text-accent" aria-live="polite">{countdown}</div></> : <><div className="mx-auto mb-5 h-14 w-14 animate-pulse rounded-full bg-accent/40 p-4"><Search /></div><h2 className="text-xl font-bold">Finding match…</h2><p className="mt-3 font-mono text-2xl" aria-live="polite">{String(Math.floor(elapsed / 60)).padStart(2, '0')}:{String(elapsed % 60).padStart(2, '0')}</p><p className="mt-2 text-sm text-slate-400">Elo {state.user.rating} · Searching within ±{estimate} Elo</p><button onClick={cancel} className="mt-6 rounded-lg border border-slate-600 px-4 py-2">Cancel Queue</button></>}</div></div>}</>;
}
