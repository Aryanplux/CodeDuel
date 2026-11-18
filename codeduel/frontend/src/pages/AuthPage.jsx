import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import Navbar from '../components/Navbar';
import Seo from '../components/Seo';

export default function AuthPage({ mode = 'login' }) {
  const isSignup = mode === 'signup';
  const auth = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ username: '', email: '', password: '' });
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  const validate = () => {
    const next = {};
    if (!form.username.trim()) next.username = 'Username is required.';
    if (isSignup && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) next.email = 'Enter a valid email address.';
    if (!form.password) next.password = 'Password is required.';
    else if (isSignup && form.password.length < 8) next.password = 'Use at least 8 characters.';
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const submit = async (event) => {
    event.preventDefault();
    if (!validate()) return;
    setSubmitting(true);
    setErrors({});
    try {
      await (isSignup ? auth.signup(form) : auth.login({ username: form.username, password: form.password }));
      navigate(isSignup ? '/signup-success' : (location.state?.from || '/dashboard'), { replace: true });
    } catch (error) {
      setErrors({ form: error.response?.data?.error || 'Unable to process your request.' });
    } finally {
      setSubmitting(false);
    }
  };

  return <><Seo title={isSignup?'Create Account':'Log In'} description={isSignup?'Create a secure CodeDuel account and start ranked 1v1 programming matches.':'Log in to CodeDuel to access ranked battles and your player dashboard.'} /><Navbar /><main className="grid min-h-[calc(100vh-73px)] place-items-center px-4 py-10"><section className="panel w-full max-w-md p-7 sm:p-9"><p className="text-sm font-semibold text-accent">{isSignup ? 'Create your account' : 'Welcome back'}</p><h1 className="mt-2 text-3xl font-black text-cream">{isSignup ? 'Join CodeDuel' : 'Log in to duel'}</h1><form className="mt-7 space-y-5" onSubmit={submit} noValidate>{['username', ...(isSignup ? ['email'] : []), 'password'].map((field) => <div key={field}><label className="mb-2 block text-sm font-medium text-sage" htmlFor={field}>{field === 'username' ? (isSignup ? 'Username' : 'Username or email') : field[0].toUpperCase() + field.slice(1)}</label><input id={field} type={field === 'password' ? 'password' : field === 'email' ? 'email' : 'text'} autoComplete={field === 'password' ? (isSignup ? 'new-password' : 'current-password') : field === 'email' ? 'email' : 'username'} value={form[field]} onChange={(event) => setForm({ ...form, [field]: event.target.value })} aria-invalid={Boolean(errors[field])} className={`w-full rounded-lg border bg-bg px-4 py-3 text-cream outline-none transition focus:ring-2 focus:ring-accent/40 ${errors[field] ? 'border-danger' : 'border-sage/30'}`} />{errors[field] && <p className="mt-2 text-sm text-danger" role="alert">{errors[field]}</p>}</div>)}{errors.form && <p className="rounded-lg border border-danger/40 bg-danger/10 px-4 py-3 text-sm text-danger" role="alert">{errors.form}</p>}<button disabled={submitting} className="flex w-full items-center justify-center rounded-lg bg-burgundy px-4 py-3 font-bold text-cream shadow-lg disabled:opacity-60">{submitting ? <><span className="mr-2 h-4 w-4 animate-spin rounded-full border-2 border-cream border-t-transparent" />Please wait…</> : isSignup ? 'Create Account' : 'Log In'}</button></form><p className="mt-6 text-center text-sm text-sage">{isSignup ? 'Already have an account?' : 'New to CodeDuel?'} <Link className="font-semibold text-accent hover:text-cream" to={isSignup ? '/login' : '/signup'}>{isSignup ? 'Log in' : 'Create account'}</Link></p></section></main></>;
}
