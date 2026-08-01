import { request } from "./client";

export function getMyProfile(token) {
  return request("/api/profile/me", { token });
}

export function saveProfile(token, { fullName, bio }) {
  return request("/api/profile", {
    method: "PUT",
    token,
    body: { fullName, bio },
  });
}

export function uploadProfilePicture(token, file) {
  const form = new FormData();
  form.append("file", file);
  return request("/api/profile/picture", {
    method: "POST",
    token,
    body: form,
    isForm: true,
  });
}
