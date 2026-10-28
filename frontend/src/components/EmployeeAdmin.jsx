import { useCallback, useEffect, useState } from 'react';
import { fetchUsers, updateUserRole } from '../api';
import { useAuth } from '../auth/AuthContext';

export default function EmployeeAdmin() {
  const { token, auth } = useAuth();
  const [users, setUsers] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setUsers(await fetchUsers(token));
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    load();
  }, [load]);

  async function onRoleChange(userId, role) {
    setError('');
    try {
      await updateUserRole(token, userId, role);
      await load();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <section className="panel">
      <header className="panel-header">
        <div>
          <h2>Employees</h2>
          <p>Promote or demote accounts. Public signup always creates USER employees.</p>
        </div>
        <button type="button" className="ghost" onClick={load}>
          Refresh
        </button>
      </header>

      {error && <p className="error-banner">{error}</p>}
      {loading ? (
        <p className="muted">Loading employees…</p>
      ) : (
        <ul className="booking-list">
          {users.map((u) => (
            <li key={u.id} className="booking-item">
              <div>
                <strong>{u.displayName}</strong>
                <p className="muted">{u.email}</p>
              </div>
              <label className="role-select">
                Role
                <select
                  value={u.role}
                  disabled={u.id === auth.userId}
                  onChange={(e) => onRoleChange(u.id, e.target.value)}
                >
                  <option value="USER">Employee (USER)</option>
                  <option value="ADMIN">Admin</option>
                </select>
              </label>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
