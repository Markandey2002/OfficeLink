import { useState } from 'react';
import { useAuth } from './auth/AuthContext';
import BookingForm from './components/BookingForm';
import BookingsDashboard from './components/BookingsDashboard';
import EmployeeAdmin from './components/EmployeeAdmin';
import LoginPage from './components/LoginPage';
import ResourceCatalog from './components/ResourceCatalog';

export default function App() {
  const { auth, isAdmin, logout } = useAuth();
  const [selected, setSelected] = useState(null);
  const [refreshKey, setRefreshKey] = useState(0);

  if (!auth) {
    return <LoginPage />;
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <div>
          <p className="brand">Office Link</p>
          <p className="muted">
            Signed in as {auth.displayName} ({auth.role})
          </p>
        </div>
        <button type="button" className="ghost" onClick={logout}>
          Sign out
        </button>
      </header>

      <main className="workspace">
        <ResourceCatalog selectedId={selected?.id} onSelect={setSelected} />
        <BookingForm
          resource={selected}
          onBooked={() => setRefreshKey((k) => k + 1)}
        />
        <BookingsDashboard refreshKey={refreshKey} />
        {isAdmin && <EmployeeAdmin />}
      </main>
    </div>
  );
}
