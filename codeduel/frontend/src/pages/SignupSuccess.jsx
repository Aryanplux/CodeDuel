import { Link } from 'react-router-dom';
import { CheckCircle2 } from 'lucide-react';
import Navbar from '../components/Navbar';
import Seo from '../components/Seo';

export default function SignupSuccess() {
  return <><Seo title="Account Created" description="Your CodeDuel account was created successfully." /><Navbar /><main className="grid min-h-[calc(100vh-73px)] place-items-center px-4"><section className="panel max-w-md p-9 text-center"><CheckCircle2 className="mx-auto text-accent" size={56} /><h1 className="mt-5 text-3xl font-black text-cream">Account created</h1><p className="mt-3 text-sage">Welcome to CodeDuel. Your ranked journey starts now.</p><Link to="/dashboard" className="mt-7 inline-block rounded-lg bg-burgundy px-5 py-3 font-bold text-cream">Open Dashboard</Link></section></main></>;
}
