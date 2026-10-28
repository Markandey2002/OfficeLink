const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080';

function authHeaders(token) {
  return token
    ? { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }
    : { 'Content-Type': 'application/json' };
}

async function parseResponse(res) {
  const text = await res.text();
  const data = text ? JSON.parse(text) : null;
  if (!res.ok) {
    const message = data?.message || `Request failed (${res.status})`;
    const error = new Error(message);
    error.status = res.status;
    error.data = data;
    throw error;
  }
  return data;
}

export async function login(email, password) {
  const res = await fetch(`${API_BASE}/api/auth/login`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify({ email, password }),
  });
  return parseResponse(res);
}

export async function register({ displayName, email, password }) {
  const res = await fetch(`${API_BASE}/api/auth/register`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify({ displayName, email, password }),
  });
  return parseResponse(res);
}

export async function fetchUsers(token) {
  const res = await fetch(`${API_BASE}/api/admin/users`, {
    headers: authHeaders(token),
  });
  return parseResponse(res);
}

export async function updateUserRole(token, id, role) {
  const res = await fetch(`${API_BASE}/api/admin/users/${id}/role`, {
    method: 'PUT',
    headers: authHeaders(token),
    body: JSON.stringify({ role }),
  });
  return parseResponse(res);
}

export async function fetchResources(token, type) {
  const query = type ? `?type=${encodeURIComponent(type)}` : '';
  const res = await fetch(`${API_BASE}/api/resources${query}`, {
    headers: authHeaders(token),
  });
  return parseResponse(res);
}

export async function createBooking(token, payload) {
  const res = await fetch(`${API_BASE}/api/bookings`, {
    method: 'POST',
    headers: authHeaders(token),
    body: JSON.stringify(payload),
  });
  return parseResponse(res);
}

export async function fetchMyBookings(token) {
  const res = await fetch(`${API_BASE}/api/bookings/mine`, {
    headers: authHeaders(token),
  });
  return parseResponse(res);
}

export async function cancelBooking(token, id) {
  const res = await fetch(`${API_BASE}/api/bookings/${id}`, {
    method: 'DELETE',
    headers: authHeaders(token),
  });
  return parseResponse(res);
}

export async function fetchAllBookings(token) {
  const res = await fetch(`${API_BASE}/api/admin/bookings`, {
    headers: authHeaders(token),
  });
  return parseResponse(res);
}

export async function updateBooking(token, id, payload) {
  const res = await fetch(`${API_BASE}/api/admin/bookings/${id}`, {
    method: 'PUT',
    headers: authHeaders(token),
    body: JSON.stringify(payload),
  });
  return parseResponse(res);
}
