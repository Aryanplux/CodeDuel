import { Link } from 'react-router-dom';
import Navbar from '../components/Navbar';
import Seo from '../components/Seo';

export default function NotFound() {
  return <><Seo title="Page Not Found" description="The requested CodeDuel page could not be found." /><Navbar /><main className="grid min-h-[calc(100vh-73px)] place-items-center px-4 text-center"><section><p className="text-8xl font-black text-burgundy">404</p><h1 className="mt-4 text-3xl font-black text-cream">This duel left the arena</h1><p className="mt-3 text-sage">The page you requested does not exist.</p><Link to="/" className="mt-7 inline-block rounded-lg bg-burgundy px-5 py-3 font-bold text-cream">Back to Lobby</Link></section></main></>;
}
