import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 8000,
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('cd_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('cd_token');
      window.location.assign('/login');
    }
    return Promise.reject(error);
  },
);

export const LANGUAGE_IDS = { python: 71, java: 62, cpp: 54 };

export const joinQueue = (userId, rating, username) => api.post('/matchmaking/join', { userId, rating, username });
export const cancelQueue = (userId) => api.post('/matchmaking/leave', { userId });
export const getApiErrorMessage = (error) => {
  if (error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT') return 'The matchmaking request timed out. Check your connection and retry.';
  if (!error.response) return 'The server could not be reached. Check that CodeDuel is running and retry.';
  return typeof error.response.data === 'string' && error.response.data.trim()
    ? error.response.data
    : `The server returned an error (${error.response.status}).`;
};
export const getBattle = (battleId) => api.get(`/battles/${encodeURIComponent(battleId)}`);
export const submitCode = (payload) => api.post('/submissions', {
  ...payload,
  languageId: LANGUAGE_IDS[payload.language],
});
