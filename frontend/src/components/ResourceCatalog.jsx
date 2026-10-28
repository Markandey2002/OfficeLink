import { useEffect, useState } from 'react';
import { fetchResources } from '../api';
import { useAuth } from '../auth/AuthContext';

const TYPES = [
  { value: '', label: 'All types' },
  { value: 'DESK', label: 'Desks' },
  { value: 'MEETING_ROOM', label: 'Meeting rooms' },
  { value: 'EQUIPMENT', label: 'Equipment' },
];

export default function ResourceCatalog({ selectedId, onSelect }) {
  const { token } = useAuth();
  const [type, setType] = useState('');
  const [resources, setResources] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      setLoading(true);
      setError('');
      try {
        const data = await fetchResources(token, type || undefined);
        if (!cancelled) setResources(data);
      } catch (err) {
        if (!cancelled) setError(err.message);
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [token, type]);

  return (
    <section className="panel">
      <header className="panel-header">
        <div>
          <h2>Resources</h2>
          <p>Filter by type, then pick what you need.</p>
        </div>
        <select value={type} onChange={(e) => setType(e.target.value)} aria-label="Filter by type">
          {TYPES.map((t) => (
            <option key={t.value || 'all'} value={t.value}>
              {t.label}
            </option>
          ))}
        </select>
      </header>

      {error && <p className="error-banner">{error}</p>}
      {loading ? (
        <p className="muted">Loading resources…</p>
      ) : (
        <ul className="resource-list">
          {resources.map((r) => (
            <li key={r.id}>
              <button
                type="button"
                className={selectedId === r.id ? 'resource-item active' : 'resource-item'}
                onClick={() => onSelect(r)}
              >
                <span className="resource-name">{r.name}</span>
                <span className="resource-meta">
                  {r.type.replace('_', ' ')} · {r.location}
                </span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
