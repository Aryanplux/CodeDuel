import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { getCurrentUser, login, logout, signup } from '../services/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    getCurrentUser()
      .then((response) => active && setUser(response.data))
      .catch(() => active && setUser(null))
      .finally(() => active && setLoading(false));
    return () => { active = false; };
  }, []);

  const value = useMemo(() => ({
    user,
    loading,
    async signup(payload) {
      const response = await signup(payload);
      setUser(response.data);
      return response.data;
    },
    async login(payload) {
      const response = await login(payload);
      setUser(response.data);
      return response.data;
    },
    async logout() {
      await logout();
      setUser(null);
    },
  }), [user, loading]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside AuthProvider');
  return context;
}
