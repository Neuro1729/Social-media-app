import { useCallback, useMemo, useState } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import { logout as logoutRequest } from "./api/AuthApi";
import Navbar from "./components/Navbar";
import EditProfilePage from "./pages/EditProfilePage";
import LoginPage from "./pages/LoginPage";
import ProfilePage from "./pages/ProfilePage";
import SignupPage from "./pages/SignupPage";
import UserPostsPage from "./pages/UserPostsPage";

const TOKEN_KEY = "pulse_token";
const USER_KEY = "pulse_user";

function readStoredUser() {
  try {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export default function App() {
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_KEY));
  const [user, setUser] = useState(readStoredUser);

  const handleAuth = useCallback((nextToken, nextUser) => {
    localStorage.setItem(TOKEN_KEY, nextToken);
    localStorage.setItem(USER_KEY, JSON.stringify(nextUser));
    setToken(nextToken);
    setUser(nextUser);
  }, []);

  const handleUser = useCallback((nextUser) => {
    localStorage.setItem(USER_KEY, JSON.stringify(nextUser));
    setUser(nextUser);
  }, []);

  const handleLogout = useCallback(async () => {
    try {
      if (token) await logoutRequest(token);
    } catch {
      // clear local session even if request fails
    }
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    setToken(null);
    setUser(null);
  }, [token]);

  const authed = useMemo(() => Boolean(token), [token]);

  return (
    <div className="app-shell">
      <Navbar user={authed ? user : null} onLogout={handleLogout} />
      <Routes>
        <Route
          path="/"
          element={<Navigate to={authed ? "/profile" : "/login"} replace />}
        />
        <Route
          path="/signup"
          element={
            authed ? <Navigate to="/profile" replace /> : <SignupPage onAuth={handleAuth} />
          }
        />
        <Route
          path="/login"
          element={
            authed ? <Navigate to="/profile" replace /> : <LoginPage onAuth={handleAuth} />
          }
        />
        <Route
          path="/profile"
          element={
            authed ? (
              <ProfilePage token={token} onUser={handleUser} />
            ) : (
              <Navigate to="/login" replace />
            )
          }
        />
        <Route
          path="/profile/edit"
          element={
            authed ? (
              <EditProfilePage token={token} onUser={handleUser} />
            ) : (
              <Navigate to="/login" replace />
            )
          }
        />
        <Route
          path="/users/:username"
          element={
            authed ? (
              <UserPostsPage token={token} currentUser={user} />
            ) : (
              <Navigate to="/login" replace />
            )
          }
        />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </div>
  );
}
