import { oidcConfig } from '../config/oidc.js';

const OIDC_KEY = `oidc.user:${oidcConfig.authority}:${oidcConfig.client_id}`;

function parseJwtPayload(token) {
  const base64Url = token.split('.')[1];
  const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
  const padded = base64.padEnd(base64.length + ((4 - base64.length % 4) % 4), '=');
  return JSON.parse(atob(padded));
}

export function getCurrentUser() {
  const oidcStorage = sessionStorage.getItem(OIDC_KEY) || localStorage.getItem(OIDC_KEY);
  
  if (oidcStorage) {
    try {
      const oidcUser = JSON.parse(oidcStorage);
      const token = oidcUser.access_token;
      
      // Parse JWT for roles
      const payload = parseJwtPayload(token);
      const roles = payload.realm_access?.roles || [];
      const role = roles.includes('ORGANIZER') ? 'ORGANIZER' : roles.includes('STAFF') ? 'STAFF' : 'STUDENT';

      return {
        ...oidcUser.profile,
        id: oidcUser.profile.sub,
        studentId: payload.studentId || oidcUser.profile.studentId || oidcUser.profile.preferred_username,
        email: oidcUser.profile.email,
        name: oidcUser.profile.name || oidcUser.profile.preferred_username,
        role: role,
        roles: roles,
        accessToken: token
      };
    } catch (e) {
      console.error('Failed to parse OIDC user from storage', e);
    }
  }
  
  return null;
}

export function isAuthenticated() {
  return Boolean(getCurrentUser());
}

export function hasRole(allowedRoles) {
  const user = getCurrentUser();
  if (!user) return false;
  const roles = Array.isArray(allowedRoles) ? allowedRoles : [allowedRoles];
  return roles.includes(user.role) || user.roles.some(r => roles.includes(r));
}

// These are no longer needed for manual login but kept as empty for now to avoid breaking imports
export async function login() {
  console.warn('Manual login() called. This is deprecated. Use OIDC instead.');
}

export function logout() {
  console.warn('Manual logout() called. This is deprecated. Use OIDC instead.');
}
