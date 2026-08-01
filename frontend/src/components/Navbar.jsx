import { NavLink, useNavigate } from "react-router-dom";

export default function Navbar({ user, onLogout }) {
  const navigate = useNavigate();

  async function handleLogout() {
    await onLogout();
    navigate("/login");
  }

  return (
    <header className="nav">
      <NavLink to={user ? "/profile" : "/"} className="brand">
        Pulse
      </NavLink>
      <nav className="nav-links">
        {user ? (
          <>
            <NavLink to="/profile">Profile</NavLink>
            <NavLink to="/profile/edit">Edit</NavLink>
            <button type="button" className="link-btn" onClick={handleLogout}>
              Logout
            </button>
          </>
        ) : (
          <>
            <NavLink to="/login">Login</NavLink>
            <NavLink to="/signup" className="nav-cta">
              Sign up
            </NavLink>
          </>
        )}
      </nav>
    </header>
  );
}
