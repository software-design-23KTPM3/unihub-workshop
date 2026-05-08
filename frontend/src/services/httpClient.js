import { API_BASE_URL } from '../config/api.js';
import { oidcConfig } from '../config/oidc.js';

export function buildUrl(endpoint, query) {
  const normalizedBaseUrl = API_BASE_URL.replace(/\/$/, '');
  const normalizedEndpoint = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
  const url = new URL(`${normalizedBaseUrl}${normalizedEndpoint}`);

  Object.entries(query || {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      url.searchParams.set(key, value);
    }
  });

  return url.toString();
}

export function getAuthToken() {
  const key = `oidc.user:${oidcConfig.authority}:${oidcConfig.client_id}`;
  const oidcStorage = sessionStorage.getItem(key) || localStorage.getItem(key);

  if (oidcStorage) {
    try {
      const user = JSON.parse(oidcStorage);
      return user?.access_token || null;
    } catch (e) {
      console.error('Failed to parse OIDC user from storage', e);
    }
  }

  const fallbackKey = Object.keys(sessionStorage)
    .concat(Object.keys(localStorage))
    .find((storageKey) => storageKey.startsWith('oidc.user:') && storageKey.endsWith(`:${oidcConfig.client_id}`));

  if (fallbackKey) {
    try {
      const user = JSON.parse(sessionStorage.getItem(fallbackKey) || localStorage.getItem(fallbackKey));
      return user?.access_token || null;
    } catch (e) {
      console.error('Failed to parse fallback OIDC user from storage', e);
    }
  }

  return null;
}

async function parseResponse(response) {
  const contentType = response.headers.get('content-type') || '';

  if (response.status === 204) {
    return null;
  }

  if (contentType.includes('application/json')) {
    return response.json();
  }

  return response.text();
}

async function request(endpoint, options = {}) {
  const { method = 'GET', body, query, headers = {}, signal } = options;
  const token = getAuthToken();

  const response = await fetch(buildUrl(endpoint, query), {
    method,
    signal,
    headers: {
      Accept: 'application/json',
      ...(body instanceof FormData ? {} : { 'Content-Type': 'application/json' }),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...headers,
    },
    body: body instanceof FormData ? body : body !== undefined ? JSON.stringify(body) : undefined,
  });

  const data = await parseResponse(response);

  if (!response.ok) {
    const message =
      typeof data === 'object' && data
        ? data.message || data.error || `HTTP ${response.status}`
        : data || `HTTP ${response.status}`;

    const error = new Error(message);
    error.status = response.status;
    error.data = data;
    throw error;
  }

  return data;
}

async function requestBlob(endpoint, options = {}) {
  const { query, headers = {}, signal } = options;
  const token = getAuthToken();

  const response = await fetch(buildUrl(endpoint, query), {
    method: 'GET',
    signal,
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...headers,
    },
  });

  if (!response.ok) {
    const data = await parseResponse(response);
    const message =
      typeof data === 'object' && data
        ? data.message || data.error || `HTTP ${response.status}`
        : data || `HTTP ${response.status}`;
    const error = new Error(message);
    error.status = response.status;
    error.data = data;
    throw error;
  }

  return response.blob();
}

export const httpClient = {
  get(endpoint, options) {
    return request(endpoint, { ...options, method: 'GET' });
  },
  post(endpoint, body, options) {
    return request(endpoint, { ...options, method: 'POST', body });
  },
  put(endpoint, body, options) {
    return request(endpoint, { ...options, method: 'PUT', body });
  },
  patch(endpoint, body, options) {
    return request(endpoint, { ...options, method: 'PATCH', body });
  },
  delete(endpoint, options) {
    return request(endpoint, { ...options, method: 'DELETE' });
  },
  blob(endpoint, options) {
    return requestBlob(endpoint, options);
  },
};
