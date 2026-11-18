import { useState } from 'react';

export default function CookieBanner() {
  const [visible, setVisible] = useState(() => localStorage.getItem('cd_cookie_consent') !== 'accepted');
  if (!visible) return null;
  const accept = () => {
    localStorage.setItem('cd_cookie_consent', 'accepted');
    setVisible(false);
  };
  return <aside className="fixed bottom-20 left-4 right-4 z-40 rounded-lg border border-sage/30 bg-surface p-4 shadow-2xl md:bottom-4 md:left-auto md:max-w-md" role="dialog" aria-label="Cookie consent"><p className="text-sm text-sage">CodeDuel uses essential storage and privacy-friendly analytics to improve the experience. <a href="/privacy" className="text-cream underline">Privacy policy</a></p><button onClick={accept} className="mt-3 rounded-md bg-burgundy px-4 py-2 text-sm font-bold text-cream">Accept</button></aside>;
}
