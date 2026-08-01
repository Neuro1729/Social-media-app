export default function UploadButton({ onFile, busy }) {
  return (
    <label className={`upload-btn ${busy ? "disabled" : ""}`}>
      {busy ? "Uploading…" : "Upload picture"}
      <input
        type="file"
        accept="image/jpeg,image/png,image/webp,image/gif"
        hidden
        disabled={busy}
        onChange={(e) => {
          const file = e.target.files?.[0];
          if (file) onFile(file);
          e.target.value = "";
        }}
      />
    </label>
  );
}
