import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  deletePost,
  editPostCaption,
  getPostsByUsername,
} from "../api/PostApi";
import PostCard from "../components/PostCard";
import ProfileCard from "../components/ProfileCard";

export default function UserPostsPage({ token, currentUser }) {
  const { username } = useParams();
  const [user, setUser] = useState(null);
  const [posts, setPosts] = useState([]);
  const [error, setError] = useState("");

  useEffect(() => {
    let alive = true;
    setError("");
    setUser(null);
    setPosts([]);
    getPostsByUsername(token, username)
      .then((data) => {
        if (!alive) return;
        setUser(data.user);
        setPosts(data.posts);
      })
      .catch((err) => {
        if (alive) setError(err.message);
      });
    return () => {
      alive = false;
    };
  }, [token, username]);

  async function handleEdit(postId, caption) {
    const updated = await editPostCaption(token, postId, caption);
    setPosts((prev) => prev.map((p) => (p.id === postId ? updated : p)));
  }

  async function handleDelete(postId) {
    await deletePost(token, postId);
    setPosts((prev) => prev.filter((p) => p.id !== postId));
  }

  if (error) {
    return (
      <main className="page">
        <p className="error">{error}</p>
        <p className="hint">
          <Link to="/profile">Back to your profile</Link>
        </p>
      </main>
    );
  }

  if (!user) {
    return (
      <main className="page">
        <p className="muted">Loading posts…</p>
      </main>
    );
  }

  const isSelf =
    currentUser &&
    currentUser.username &&
    currentUser.username.toLowerCase() === user.username.toLowerCase();

  return (
    <main className="page">
      <ProfileCard user={user} />
      {isSelf ? (
        <p className="hint">
          This is you. <Link to="/profile">Manage your posts here</Link>
        </p>
      ) : null}

      <section className="posts-section">
        <h2>{isSelf ? "Your posts" : `Posts by @${user.username}`}</h2>
        {posts.length === 0 ? (
          <p className="muted">No posts yet.</p>
        ) : (
          <div className="post-list">
            {posts.map((post) => (
              <PostCard
                key={post.id}
                post={post}
                onEdit={handleEdit}
                onDelete={handleDelete}
              />
            ))}
          </div>
        )}
      </section>
    </main>
  );
}
