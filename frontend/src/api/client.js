export async function request(path, { method = "GET", body, token, isForm } = {}) {
  const headers = {};
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  if (body && !isForm) {
    headers["Content-Type"] = "application/json";
  }

  const response = await fetch(path, {
    method,
    headers,
    body: body ? (isForm ? body : JSON.stringify(body)) : undefined,
  });

  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(data.error || "Request failed");
  }
  return data;
}
