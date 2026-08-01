import { useState } from "react";

export default function PostForm({ onSubmit, busy }) {
  const [caption, setCaption] = useState("");
  const [image, setImage] = useState(null);
  const [preview, setPreview] = useState(null);
  const [error, setError] = useState("");

  function handleImage(file) {
    setImage(file || null);
    if (preview) {
      URL.revokeObjectURL(preview);
    }
    setPreview(file ? URL.createObjectURL(file) : null);
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    if (!caption.trim() && !image) {
      setError("Add a caption or an image.");
      return;
    }
    try {
      await onSubmit({ caption: caption.trim(), image });
      setCaption("");
      handleImage(null);
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <form className="post-form stack-form" onSubmit={handleSubmit}>
      <h2>Create a post</h2>
      <label>
        Caption
        <textarea
          value={caption}
          onChange={(e) => setCaption(e.target.value)}
          rows={3}
          maxLength={2200}
          placeholder="Write something…"
        />
      </label>
      <label className="upload-btn">
        {image ? "Change image" : "Add image"}
        <input
          type="file"
          accept="image/jpeg,image/png,image/webp,image/gif"
          hidden
          onChange={(e) => handleImage(e.target.files?.[0] || null)}
        />
      </label>
      {preview && (
        <div className="post-preview">
          <img src={preview} alt="" />
        </div>
      )}
      {error && <p className="error">{error}</p>}
      <button type="submit" disabled={busy}>
        {busy ? "Posting…" : "Post"}
      </button>
    </form>
  );
}
