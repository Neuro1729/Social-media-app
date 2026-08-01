import { request } from "./client";

export function createPost(token, { caption, image }) {
  const form = new FormData();
  if (caption != null) {
    form.append("caption", caption);
  }
  if (image) {
    form.append("image", image);
  }
  return request("/api/posts", {
    method: "POST",
    token,
    body: form,
    isForm: true,
  });
}

export function getMyPosts(token) {
  return request("/api/posts/me", { token });
}

export function getPostsByUsername(token, username) {
  return request(`/api/posts/user/${encodeURIComponent(username)}`, { token });
}

export function editPostCaption(token, postId, caption) {
  return request(`/api/posts/${postId}`, {
    method: "PUT",
    token,
    body: { caption },
  });
}

export function deletePost(token, postId) {
  return request(`/api/posts/${postId}`, {
    method: "DELETE",
    token,
  });
}
