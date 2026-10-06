import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import Navbar from '../components/Navbar';
import ProblemView from '../components/ProblemView';
import CodeEditor from '../components/CodeEditor';
import ConsoleOutput from '../components/ConsoleOutput';
import OpponentCard from '../components/OpponentCard';
import MatchResultModal from '../components/MatchResultModal';
import { useBattle } from '../context/BattleContext';
import { getBattle, submitCode } from '../services/api';
import { websocket } from '../services/websocket';

export default function BattleArena() {
  const { battleId } = useParams();
  const navigate = useNavigate();
  const { state, dispatch } = useBattle();
  const [language, setLanguage] = useState('python');
  const [code, setCode] = useState({});
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [remaining, setRemaining] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const lastCoding = useRef(0);

  useEffect(() => {
    let active = true;
    getBattle(battleId).then(({ data }) => {
      if (!active) return;
      dispatch({ type: 'BATTLE_LOADED', battle: data });
      if (data.endsAt && data.serverTime) setRemaining({ endsAt: new Date(data.endsAt).getTime(), offset: new Date(data.serverTime).getTime() - Date.now() });
    }).catch(() => setError('Unable to load this battle.')).finally(() => active && setLoading(false));
    const unsubscribe = websocket.subscribe(`/topic/battle/${battleId}`, (event) => {
      dispatch({ type: 'EVENT', event: event.type, userId: event.userId, payload: event.payload || event });
      if (event.type === 'BATTLE_STARTED') setRemaining({ endsAt: new Date(event.payload.endsAt).getTime(), offset: new Date(event.payload.serverTime).getTime() - Date.now() });
      if (event.type === 'TIMER_SYNC') setRemaining((current) => current ? { ...current, endsAt: Date.now() + event.payload.remainingMs - current.offset } : current);
    });
    return () => { active = false; unsubscribe(); dispatch({ type: 'RESET_BATTLE' }); };
  }, [battleId, dispatch]);

  useEffect(() => {
    if (!remaining) return undefined;
    const timer = window.setInterval(() => setRemaining((current) => current ? { ...current } : current), 1000);
    return () => window.clearInterval(timer);
  }, [remaining?.endsAt, remaining?.offset]);

  const secondsLeft = remaining ? Math.max(0, Math.ceil((remaining.endsAt - (Date.now() + remaining.offset)) / 1000)) : null;
  const isFinished = state.battle?.status === 'FINISHED';
  const currentCode = code[language] ?? state.battle?.problem?.starterCode?.[language] ?? '';
  const displayTime = secondsLeft == null ? '--:--' : `${String(Math.floor(secondsLeft / 60)).padStart(2, '0')}:${String(secondsLeft % 60).padStart(2, '0')}`;
  const updateCode = (value) => {
    setCode((current) => ({ ...current, [language]: value }));
    if (Date.now() - lastCoding.current > 5000) {
      websocket.publish(`/app/battle/${battleId}/coding`, { type: 'PLAYER_CODING', userId: state.user.userId });
      lastCoding.current = Date.now();
    }
  };
  const run = async (mode) => {
    setSubmitting(true); setError('');
    try { const { data } = await submitCode({ battleId, userId: state.user.userId, language, sourceCode: currentCode, mode }); setResult(data); } catch { setError('Submission failed. Check the server and try again.'); } finally { setSubmitting(false); }
  };
  if (loading) return <><Navbar /><main className="p-8 text-slate-400">Loading battle…</main></>;
  if (error && !state.battle) return <><Navbar /><main className="p-8 text-danger">{error}</main></>;
  return <><Navbar /><main className="h-[calc(100vh-73px)] p-4"><div className="grid h-full grid-cols-1 gap-4 lg:grid-cols-2"><div className="flex min-h-0 flex-col gap-4"><ProblemView problem={state.battle?.problem} /><ConsoleOutput result={result} onClear={() => setResult(null)} /></div><div className="flex min-h-0 flex-col gap-3"><div className="flex items-center justify-between"><select value={language} onChange={(event) => setLanguage(event.target.value)} disabled={isFinished} className="rounded-lg border border-slate-700 bg-surface px-3 py-2"><option value="python">Python 3.8</option><option value="java">Java 13</option><option value="cpp">C++ 17</option></select><div className={`font-mono text-lg ${secondsLeft !== null && secondsLeft < 60 ? 'text-danger' : ''}`} aria-live="polite">{displayTime}</div><OpponentCard opponent={state.opponent} /></div><div className="min-h-0 flex-1"><CodeEditor language={language} problem={state.battle?.problem} value={currentCode} onChange={updateCode} disabled={isFinished || submitting} /></div><div className="flex justify-end gap-3"><button onClick={() => run('RUN')} disabled={submitting || isFinished} className="rounded-lg border border-slate-600 px-4 py-2 disabled:opacity-50">{submitting ? 'Running…' : 'Run Sample Tests'}</button><button onClick={() => run('SUBMIT')} disabled={submitting || isFinished} className="rounded-lg bg-accent px-4 py-2 font-semibold disabled:opacity-50">{submitting ? 'Submitting…' : 'Submit Solution'}</button></div>{error && <p className="text-sm text-danger">{error}</p>}</div></div></main>{state.result && <MatchResultModal result={state.result} onClose={() => dispatch({ type: 'RESET_BATTLE' })} />}</>;
}
