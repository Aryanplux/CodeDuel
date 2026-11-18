/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        bg: '#151714',
        surface: '#242720',
        sage: '#A5A79B',
        cream: '#E8D2B5',
        burgundy: '#590907',
        accent: '#590907',
        success: '#22c55e',
        danger: '#ef4444',
        warn: '#f59e0b',
      },
    },
  },
  plugins: [],
};
