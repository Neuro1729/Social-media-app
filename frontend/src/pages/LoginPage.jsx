import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { login } from "../api/AuthApi";

export default function LoginPage({ onAuth }) {
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: "", password: "" });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setBusy(true);
    try {
      const data = await login(form);
      onAuth(data.token, data.user);
      navigate("/profile");
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="auth-page">
      <div className="auth-panel">
        <p className="eyebrow">Welcome back</p>
        <h1>Log in</h1>
        <p className="lede">Access your profile and keep editing your story.</p>
        <form onSubmit={handleSubmit} className="stack-form">
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
              autoComplete="current-password"
            />
          </label>
          {error && <p className="error">{error}</p>}
          <button type="submit" disabled={busy}>
            {busy ? "Signing in…" : "Log in"}
          </button>
        </form>
        <p className="switch">
          New here? <Link to="/signup">Create an account</Link>
        </p>
      </div>
    </main>
  );
}
