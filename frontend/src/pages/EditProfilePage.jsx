import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getMyProfile, saveProfile, uploadProfilePicture } from "../api/ProfileApi";
import UploadButton from "../components/UploadButton";

export default function EditProfilePage({ token, onUser }) {
  const navigate = useNavigate();
  const [form, setForm] = useState({ fullName: "", bio: "" });
  const [picture, setPicture] = useState(null);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  const [uploading, setUploading] = useState(false);

  useEffect(() => {
    getMyProfile(token)
      .then((data) => {
        setForm({ fullName: data.fullName || "", bio: data.bio || "" });
        setPicture(data.profilePicture);
        onUser(data);
      })
      .catch((err) => setError(err.message));
  }, [token, onUser]);

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setMessage("");
    setBusy(true);
    try {
      const data = await saveProfile(token, form);
      onUser(data);
      setMessage("Profile saved.");
      navigate("/profile");
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  async function handleUpload(file) {
    setError("");
    setMessage("");
    setUploading(true);
    try {
      const data = await uploadProfilePicture(token, file);
      setPicture(data.profilePicture);
      onUser(data);
      setMessage("Picture updated.");
    } catch (err) {
      setError(err.message);
    } finally {
      setUploading(false);
    }
  }

  return (
    <main className="page narrow">
      <h1>Edit profile</h1>
      <p className="lede">Create or update how you appear on Pulse.</p>

      <div className="edit-avatar">
        <div className="avatar lg">
          {picture ? <img src={picture} alt="" /> : <span>?</span>}
        </div>
        <UploadButton onFile={handleUpload} busy={uploading} />
      </div>

      <form onSubmit={handleSubmit} className="stack-form">
        <label>
          Full name
          <input
            value={form.fullName}
            onChange={(e) => setForm({ ...form, fullName: e.target.value })}
            placeholder="Your name"
          />
        </label>
        <label>
          Bio
          <textarea
            value={form.bio}
            onChange={(e) => setForm({ ...form, bio: e.target.value })}
            rows={4}
            placeholder="A short intro"
            maxLength={500}
          />
        </label>
        {error && <p className="error">{error}</p>}
        {message && <p className="success">{message}</p>}
        <button type="submit" disabled={busy}>
          {busy ? "Saving…" : "Save profile"}
        </button>
      </form>
    </main>
  );
}
