import { useEffect } from 'react';

export default function Seo({ title, description }) {
  useEffect(() => {
    document.title = `${title} | CodeDuel`;
    let meta = document.querySelector('meta[name="description"]');
    if (!meta) {
      meta = document.createElement('meta');
      meta.name = 'description';
      document.head.appendChild(meta);
    }
    meta.content = description;
  }, [title, description]);
  return null;
}
