import Navbar from '../components/Navbar';
import Footer from '../components/Footer';
import Seo from '../components/Seo';

export default function PolicyPage({ mode='privacy' }) {
  const privacy = mode === 'privacy';
  return <><Seo title={privacy?'Privacy Policy':'Terms of Use'} description={privacy?'Learn how CodeDuel handles account, match and consent data.':'Review CodeDuel platform usage terms and fair-play rules.'} /><Navbar /><main className="px-4 py-10"><article className="panel mx-auto max-w-3xl p-7 md:p-10"><h1 className="text-4xl font-black text-cream">{privacy?'Privacy Policy':'Terms of Use'}</h1><div className="mt-6 space-y-5 leading-7 text-sage">{privacy ? <><p>CodeDuel stores account details, Elo progression, submitted source code and match history to operate the competitive platform.</p><p>Passwords are hashed using BCrypt and are never displayed or returned by the API. Session cookies are used for authenticated access.</p><p>Users may request account or data removal by contacting support@codeduel.dev.</p></> : <><p>Use CodeDuel respectfully and submit only your own work. Automated abuse, account sharing and attempts to disrupt matchmaking or Judge0 are prohibited.</p><p>Match outcomes, Elo changes and leaderboard-style statistics are provided for platform use and may be reset during maintenance.</p><p>By creating an account, you agree not to submit malicious code or use the platform for unlawful activity.</p></>}</div></article></main><Footer /></>;
}
