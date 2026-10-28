import { useState } from 'react';
import { login, register } from '../api';
import { useAuth } from '../auth/AuthContext';

export default function LoginPage() {
  const { loginSuccess } = useAuth();
  const [mode, setMode] = useState('login');
  const [displayName, setDisplayName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function onSubmit(e) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const data =
        mode === 'signup'
          ? await register({ displayName, email, password })
          : await login(email, password);
      loginSuccess(data);
    } catch (err) {
      setError(err.message || (mode === 'signup' ? 'Signup failed' : 'Login failed'));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="login-shell">
      <div className="login-panel">
        <p className="brand">Office Link</p>
        <h1>Book desks, rooms, and gear without the scramble.</h1>
        <p className="lede">
          New employees can create an account. Admins manage roles after signup.
        </p>

        <div className="auth-tabs" role="tablist">
          <button
            type="button"
            role="tab"
            className={mode === 'login' ? 'tab active' : 'tab'}
            aria-selected={mode === 'login'}
            onClick={() => {
              setMode('login');
              setError('');
            }}
          >
            Sign in
          </button>
          <button
            type="button"
            role="tab"
            className={mode === 'signup' ? 'tab active' : 'tab'}
            aria-selected={mode === 'signup'}
            onClick={() => {
              setMode('signup');
              setError('');
            }}
          >
            Create account
          </button>
        </div>

        <form className="login-form" onSubmit={onSubmit}>
          {mode === 'signup' && (
            <label>
              Full name
              <input
                type="text"
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
                autoComplete="name"
                required
                maxLength={120}
              />
            </label>
          )}
          <label>
            Work email
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              autoComplete="username"
              required
            />
          </label>
          <label>
            Password
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete={mode === 'signup' ? 'new-password' : 'current-password'}
              required
              minLength={mode === 'signup' ? 8 : undefined}
            />
          </label>
          {mode === 'signup' && (
            <p className="hint">Password must be at least 8 characters. New accounts start as Employee (USER).</p>
          )}
          {error && <p className="error-banner" role="alert">{error}</p>}
          <button type="submit" disabled={loading}>
            {loading
              ? mode === 'signup'
                ? 'Creating account…'
                : 'Signing in…'
              : mode === 'signup'
                ? 'Create employee account'
                : 'Sign in'}
          </button>
        </form>

        <p className="hint">
          Bootstrap admin (first setup only): <code>admin@officlink.com</code> / <code>Admin123!</code>
        </p>
      </div>
      <div className="login-visual" aria-hidden="true" />
    </div>
  );
}
