import { useEffect, useState } from 'react';
import Navbar from '../components/Navbar';
import Seo from '../components/Seo';
import Footer from '../components/Footer';
import { getDashboard } from '../services/api';

export default function Dashboard() {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    getDashboard().then((response) => setData(response.data)).catch((requestError) => setError(requestError.response?.data?.error || 'Unable to load dashboard.'));
  }, []);

  return <><Seo title="Player Dashboard" description="Track CodeDuel Elo, rank, wins, losses and recent competitive programming match history." /><Navbar /><main className="min-h-[calc(100vh-73px)] px-4 py-8 md:px-6"><div className="mx-auto max-w-6xl">{error ? <section className="panel p-8 text-danger" role="alert">{error}</section> : !data ? <section className="grid animate-pulse gap-5 md:grid-cols-4">{[1,2,3,4].map((item)=><div key={item} className="h-32 rounded-xl bg-sage/10" />)}</section> : <><div className="mb-6"><p className="text-sm text-sage">Ranked profile</p><h1 className="mt-1 text-4xl font-black text-cream">{data.profile.username}</h1></div><section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">{[[ 'Elo', data.profile.elo ],['Rank',data.profile.rank],['Matches',data.profile.matchesPlayed],['Wins',data.profile.wins],['Win rate',`${data.profile.winRate}%`]].map(([label,value])=><div key={label} className="panel p-5"><p className="text-sm text-sage">{label}</p><p className="mt-2 text-2xl font-black text-cream">{value}</p></div>)}</section><section className="panel mt-6 overflow-hidden"><div className="border-b border-sage/20 px-5 py-4"><h2 className="font-bold text-cream">Recent matches</h2></div><div className="overflow-x-auto"><table className="w-full min-w-[620px] text-left text-sm"><thead className="text-sage"><tr><th className="px-5 py-3">Problem</th><th>Opponent</th><th>Result</th><th>Date</th></tr></thead><tbody>{data.recentMatches.length ? data.recentMatches.map((match)=><tr key={match.battleId} className="border-t border-sage/10"><td className="px-5 py-4 text-cream">{match.problem}</td><td className="text-sage">{match.opponent}</td><td><span className={match.result==='WIN'?'text-accent':match.result==='LOSS'?'text-danger':'text-sage'}>{match.result}</span></td><td className="text-sage">{match.date ? new Date(match.date).toLocaleString() : '—'}</td></tr>) : <tr><td colSpan="4" className="px-5 py-8 text-center text-sage">No completed matches yet.</td></tr>}</tbody></table></div></section></>}</div></main><Footer /></>;
}
