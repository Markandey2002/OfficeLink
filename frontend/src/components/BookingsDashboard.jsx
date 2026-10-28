import { useCallback, useEffect, useState } from 'react';
import {
  cancelBooking,
  fetchAllBookings,
  fetchMyBookings,
  updateBooking,
} from '../api';
import { useAuth } from '../auth/AuthContext';
import { formatUtcRange, todayDateInputValue, toUtcIso } from '../utils/time';

export default function BookingsDashboard({ refreshKey }) {
  const { token, isAdmin } = useAuth();
  const [bookings, setBookings] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [editingId, setEditingId] = useState(null);
  const [editDate, setEditDate] = useState(todayDateInputValue());
  const [editStart, setEditStart] = useState('10:00');
  const [editEnd, setEditEnd] = useState('11:30');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = isAdmin ? await fetchAllBookings(token) : await fetchMyBookings(token);
      setBookings(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [token, isAdmin]);

  useEffect(() => {
    load();
  }, [load, refreshKey]);

  async function onCancel(id) {
    setError('');
    try {
      await cancelBooking(token, id);
      await load();
    } catch (err) {
      setError(err.message);
    }
  }

  async function onSaveEdit(id) {
    setError('');
    try {
      await updateBooking(token, id, {
        startUtc: toUtcIso(editDate, editStart),
        endUtc: toUtcIso(editDate, editEnd),
      });
      setEditingId(null);
      await load();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <section className="panel">
      <header className="panel-header">
        <div>
          <h2>{isAdmin ? 'All reservations' : 'My bookings'}</h2>
          <p>
            {isAdmin
              ? 'Admins can modify any confirmed reservation.'
              : 'Cancel a booking you no longer need.'}
          </p>
        </div>
        <button type="button" className="ghost" onClick={load}>
          Refresh
        </button>
      </header>

      {error && <p className="error-banner">{error}</p>}
      {loading ? (
        <p className="muted">Loading bookings…</p>
      ) : bookings.length === 0 ? (
        <p className="muted">No confirmed bookings yet.</p>
      ) : (
        <ul className="booking-list">
          {bookings.map((b) => (
            <li key={b.id} className="booking-item">
              <div>
                <strong>{b.resourceName}</strong>
                <p className="muted">{formatUtcRange(b.startUtc, b.endUtc)}</p>
                {isAdmin && (
                  <p className="muted">
                    {b.userDisplayName} · {b.userEmail}
                  </p>
                )}
              </div>

              {editingId === b.id ? (
                <div className="edit-row">
                  <input type="date" value={editDate} onChange={(e) => setEditDate(e.target.value)} />
                  <input type="time" value={editStart} onChange={(e) => setEditStart(e.target.value)} />
                  <input type="time" value={editEnd} onChange={(e) => setEditEnd(e.target.value)} />
                  <button type="button" onClick={() => onSaveEdit(b.id)}>
                    Save
                  </button>
                  <button type="button" className="ghost" onClick={() => setEditingId(null)}>
                    Cancel
                  </button>
                </div>
              ) : (
                <div className="actions">
                  {isAdmin && (
                    <button
                      type="button"
                      className="ghost"
                      onClick={() => {
                        setEditingId(b.id);
                        const start = new Date(b.startUtc);
                        const end = new Date(b.endUtc);
                        const pad = (n) => String(n).padStart(2, '0');
                        setEditDate(
                          `${start.getUTCFullYear()}-${pad(start.getUTCMonth() + 1)}-${pad(start.getUTCDate())}`,
                        );
                        setEditStart(`${pad(start.getUTCHours())}:${pad(start.getUTCMinutes())}`);
                        setEditEnd(`${pad(end.getUTCHours())}:${pad(end.getUTCMinutes())}`);
                      }}
                    >
                      Edit
                    </button>
                  )}
                  <button type="button" className="danger" onClick={() => onCancel(b.id)}>
                    Cancel booking
                  </button>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
