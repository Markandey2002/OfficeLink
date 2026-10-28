import { createContext, useContext, useMemo, useState } from 'react';

const AuthContext = createContext(null);
const STORAGE_KEY = 'office_link_auth';

function readStoredAuth() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(() => readStoredAuth());

  const value = useMemo(
    () => ({
      auth,
      token: auth?.accessToken || null,
      isAdmin: auth?.role === 'ADMIN',
      loginSuccess(payload) {
        setAuth(payload);
        localStorage.setItem(STORAGE_KEY, JSON.stringify(payload));
      },
      logout() {
        setAuth(null);
        localStorage.removeItem(STORAGE_KEY);
      },
    }),
    [auth],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
