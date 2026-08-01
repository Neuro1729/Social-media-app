import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getMyProfile } from "../api/ProfileApi";
import ProfileCard from "../components/ProfileCard";

export default function ProfilePage({ token, onUser }) {
  const [user, setUser] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let alive = true;
    getMyProfile(token)
      .then((data) => {
        if (!alive) return;
        setUser(data);
        onUser(data);
      })
      .catch((err) => {
        if (alive) setError(err.message);
      });
    return () => {
      alive = false;
    };
  }, [token, onUser]);

  if (error) {
    return (
      <main className="page">
        <p className="error">{error}</p>
      </main>
    );
  }

  if (!user) {
    return (
      <main className="page">
        <p className="muted">Loading profile…</p>
      </main>
    );
  }

  const needsProfile = !user.fullName && !user.bio;

  return (
    <main className="page">
      <ProfileCard user={user} />
      {needsProfile && (
        <p className="hint">
          Your profile is empty. <Link to="/profile/edit">Create your profile</Link>
        </p>
      )}
      <div className="actions">
        <Link className="button-link" to="/profile/edit">
          Edit profile
        </Link>
      </div>
    </main>
  );
}
