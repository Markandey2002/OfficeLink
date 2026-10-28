import { useState } from 'react';
import { checkConflict, createBooking } from '../api';
import { useAuth } from '../auth/AuthContext';
import { todayDateInputValue, toUtcIso } from '../utils/time';

export default function BookingForm({ resource, onBooked }) {
  const { token } = useAuth();
  const [date, setDate] = useState(todayDateInputValue());
  const [startTime, setStartTime] = useState('10:00');
  const [endTime, setEndTime] = useState('11:30');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  if (!resource) {
    return (
      <section className="panel">
        <header className="panel-header">
          <div>
            <h2>Book a slot</h2>
            <p>Select a resource from the catalog to continue.</p>
          </div>
        </header>
      </section>
    );
  }

  async function onSubmit(e) {
    e.preventDefault();
    setError('');
    setSuccess('');
    setLoading(true);
    try {
      const startUtc = toUtcIso(date, startTime);
      const endUtc = toUtcIso(date, endTime);
      if (new Date(endUtc) <= new Date(startUtc)) {
        throw new Error('End time must be after start time');
      }
      // Pre-flight availability check before committing the booking.
      const probe = await checkConflict(token, { resourceId: resource.id, startUtc, endUtc });
      if (probe.conflict) {
        throw new Error(`${resource.name} is already booked for that time window. Please choose a different slot.`);
      }
      const booking = await createBooking(token, {
        resourceId: resource.id,
        startUtc,
        endUtc,
      });
      setSuccess(`Booked ${resource.name} successfully.`);
      onBooked?.(booking);
    } catch (err) {
      setError(err.message || 'Booking failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="panel">
      <header className="panel-header">
        <div>
          <h2>Book {resource.name}</h2>
          <p>Choose a UTC date and time block. Conflicts are checked before commit.</p>
        </div>
      </header>

      <form className="booking-form" onSubmit={onSubmit}>
        <label>
          Date (UTC calendar day)
          <input type="date" value={date} onChange={(e) => setDate(e.target.value)} required />
        </label>
        <div className="time-row">
          <label>
            Start (UTC)
            <input
              type="time"
              value={startTime}
              onChange={(e) => setStartTime(e.target.value)}
              required
            />
          </label>
          <label>
            End (UTC)
            <input
              type="time"
              value={endTime}
              onChange={(e) => setEndTime(e.target.value)}
              required
            />
          </label>
        </div>
        {error && <p className="error-banner" role="alert">{error}</p>}
        {success && <p className="success-banner">{success}</p>}
        <button type="submit" disabled={loading}>
          {loading ? 'Checking availability…' : 'Confirm booking'}
        </button>
      </form>
    </section>
  );
}
