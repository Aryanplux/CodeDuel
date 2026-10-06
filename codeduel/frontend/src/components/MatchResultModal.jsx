import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Trophy, X } from 'lucide-react';
import { useBattle } from '../context/BattleContext';

export default function MatchResultModal({ result, onClose }) {
  const navigate = useNavigate();
  const { state } = useBattle();
  useEffect(() => {
    const onKey = (event) => event.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);
  if (!result) return null;
  const winner = result.winnerId === state.user.userId;
  const title = result.winnerId == null ? 'Draw' : winner ? 'Victory' : 'Defeat';
  const rating = result.ratings?.[state.user.userId];
  return <div className="fixed inset-0 z-20 grid place-items-center bg-black/70 p-4" role="dialog" aria-modal="true" aria-labelledby="result-title">
    <div className="panel w-full max-w-md p-8 text-center"><button onClick={onClose} className="float-right text-slate-400" aria-label="Close"><X /></button><Trophy className="mx-auto mb-4 text-warn" size={48} /><h2 id="result-title" className="text-3xl font-bold">{title}</h2><p className="mt-3 text-slate-300">{result.reason || 'Battle complete.'}</p>{rating && <p className="mt-2 text-slate-400">{rating.before} → {rating.after} <span className={rating.after >= rating.before ? 'text-success' : 'text-danger'}>({rating.after - rating.before >= 0 ? '+' : ''}{rating.after - rating.before})</span></p>}<div className="mt-6 flex justify-center gap-3"><button onClick={() => navigate('/')} className="rounded-lg bg-accent px-4 py-2 font-semibold">Play Again</button><button onClick={onClose} className="rounded-lg border border-slate-600 px-4 py-2">Close</button></div></div>
  </div>;
}
