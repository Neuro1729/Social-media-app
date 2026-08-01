import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { signup } from "../api/AuthApi";

export default function SignupPage({ onAuth }) {
  const navigate = useNavigate();
  const [form, setForm] = useState({ username: "", email: "", password: "" });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setBusy(true);
    try {
      const data = await signup(form);
      onAuth(data.token, data.user);
      navigate("/profile/edit");
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="auth-page">
      <div className="auth-panel">
        <p className="eyebrow">Create account</p>
        <h1>Join Pulse</h1>
        <p className="lede">Sign up to build your profile and share your presence.</p>
        <form onSubmit={handleSubmit} className="stack-form">
          <label>
            Username
            <input
              value={form.username}
              onChange={(e) => setForm({ ...form, username: e.target.value })}
              required
              autoComplete="username"
            />
          </label>
          <label>
            Email
            <input
              type="email"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              required
              autoComplete="email"
            />
          </label>
          <label>
            Password
            <input
              type="password"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              required
              minLength={6}
              autoComplete="new-password"
            />
          </label>
          {error && <p className="error">{error}</p>}
          <button type="submit" disabled={busy}>
            {busy ? "Creating…" : "Sign up"}
          </button>
        </form>
        <p className="switch">
          Already have an account? <Link to="/login">Log in</Link>
        </p>
      </div>
    </main>
  );
}
