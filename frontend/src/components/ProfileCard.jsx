export default function ProfileCard({ user }) {
  const picture = user.profilePicture || null;
  const initial = (user.fullName || user.username || "?").charAt(0).toUpperCase();

  return (
    <section className="profile-card">
      <div className="avatar" aria-hidden={!picture}>
        {picture ? <img src={picture} alt="" /> : <span>{initial}</span>}
      </div>
      <div className="profile-meta">
        <h1>{user.fullName || user.username}</h1>
        <p className="handle">@{user.username}</p>
        <p className="email">{user.email}</p>
        {user.bio ? <p className="bio">{user.bio}</p> : <p className="bio muted">No bio yet.</p>}
      </div>
    </section>
  );
}
