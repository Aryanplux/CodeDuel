export default function OpponentCard({ opponent }) {
  const activity = opponent?.activity === 'ACCEPTED' ? 'ACCEPTED! (Victory claimed)' : opponent?.activity === 'SUBMITTED' ? `Submitted! (Tests passed: ${opponent.testsPassed ?? 0}/${opponent.testsTotal ?? 0})` : opponent?.activity === 'CODING' ? 'Coding…' : opponent?.presence === 'OFFLINE' ? 'Disconnected' : 'Idle';
  return <div className="rounded-lg bg-slate-900 px-3 py-2 text-right text-xs">
    <div className="font-semibold">{opponent?.username || 'Opponent'}</div>
    <div className="text-slate-400">Elo {opponent?.rating ?? '—'} · <span className={opponent?.presence === 'ONLINE' ? 'text-success' : 'text-danger'}>{opponent?.presence || 'UNKNOWN'}</span></div>
    <div className="text-slate-300">{activity}</div>
  </div>;
}
