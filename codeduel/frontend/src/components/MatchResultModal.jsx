import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Trophy, X } from 'lucide-react';
import { useBattle } from '../context/BattleContext';
import { joinQueue } from '../services/api';

export default function MatchResultModal({ result, onClose }) {
  const navigate = useNavigate();
  const { state, dispatch } = useBattle();
  const [joining, setJoining] = useState(false);
  const [queueError, setQueueError] = useState('');
  useEffect(() => {
    const onKey = (event) => event.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);
  if (!result) return null;
  const winner = result.winnerId === state.user.userId;
  const title = result.winnerId == null ? 'Draw' : winner ? 'You Win' : 'You Lose';
  const score = result.scores?.[state.user.userId];
  const reason = result.reasons?.[state.user.userId] || 'Round complete.';

  const startNextRound = async () => {
    setJoining(true);
    setQueueError('');
    try {
      await joinQueue(state.user.userId, state.user.rating, state.user.username);
      dispatch({ type: 'RESET_BATTLE' });
      dispatch({ type: 'QUEUE_SEARCHING' });
      navigate('/');
    } catch (error) {
      setQueueError(error.response?.data || 'Unable to join the matchmaking queue.');
      setJoining(false);
    }
  };

  return <div className="fixed inset-0 z-20 grid place-items-center bg-black/70 p-4" role="dialog" aria-modal="true" aria-labelledby="result-title">
    <div className="panel w-full max-w-md p-8 text-center">
      <button onClick={onClose} className="float-right text-slate-400" aria-label="Close"><X /></button>
      <Trophy className="mx-auto mb-4 text-warn" size={48} />
      <h2 id="result-title" className="text-3xl font-bold">{title}</h2>
      <p className="mt-3 text-slate-300">{reason}</p>
      {score && <div className="mt-5 grid grid-cols-3 gap-2 text-sm">
        <div className="rounded bg-slate-800 p-2"><b>{score.testsPassed}/{score.totalTests}</b><p className="text-slate-400">Tests passed</p></div>
        <div className="rounded bg-slate-800 p-2"><b>{score.aiScore}</b><p className="text-slate-400">Optimality</p></div>
        <div className="rounded bg-accent/20 p-2"><b>{score.executionTime === 1.7976931348623157e+308 ? '—' : `${score.executionTime}s`}</b><p className="text-slate-400">Execution</p></div>
      </div>}
      {queueError && <p className="mt-4 text-danger" role="alert">{queueError}</p>}
      <div className="mt-6 flex justify-center gap-3">
        <button onClick={startNextRound} disabled={joining} className="rounded-lg bg-accent px-4 py-2 font-semibold disabled:opacity-50">
          {joining ? 'Joining Queue…' : 'Jump to Another Round'}
        </button>
        <button onClick={onClose} className="rounded-lg border border-slate-600 px-4 py-2">Close</button>
      </div>
    </div>
  </div>;
}
