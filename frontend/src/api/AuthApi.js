import { request } from "./client";

export function signup({ username, email, password }) {
  return request("/api/auth/signup", {
    method: "POST",
    body: { username, email, password },
  });
}

export function login({ email, password }) {
  return request("/api/auth/login", {
    method: "POST",
    body: { email, password },
  });
}

export function logout(token) {
  return request("/api/auth/logout", { method: "POST", token });
}
